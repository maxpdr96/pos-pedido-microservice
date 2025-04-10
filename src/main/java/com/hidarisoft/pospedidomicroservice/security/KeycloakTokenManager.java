package com.hidarisoft.pospedidomicroservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Scope("singleton") // Escopo singleton explícito
@Slf4j
public class KeycloakTokenManager {

    private static final String REFRESH_TOKEN = "refresh_token";
    // Tempo antes da expiração para renovar o token (em segundos)
    private static final int REFRESH_MARGIN = 60;

    private static final ReentrantLock lock = new ReentrantLock();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${keycloak.auth-server-url:http://localhost:8090/auth}")
    private String keycloakAuthUrl;

    @Value("${keycloak.realm:entrega-app}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Getter
    private String accessToken;
    private long expiresAt;
    private String refreshToken;

    @PostConstruct
    public void init() {
        fetchNewToken();

        // Configura o scheduler para verificar o token antes da expiração
        scheduler.scheduleAtFixedRate(this::checkTokenExpiration, 1, 1, TimeUnit.MINUTES);
    }

    private void checkTokenExpiration() {
        lock.lock();
        try {
            long now = System.currentTimeMillis() / 1000; // Tempo atual em segundos
            if (expiresAt - now < REFRESH_MARGIN) {
                log.info("Token is about to expire. Refreshing...");
                if (refreshToken != null) {
                    refreshExistingToken();
                } else {
                    fetchNewToken();
                }
            }
        } catch (Exception e) {
            log.error("Error while checking token expiration", e);
        } finally {
            lock.unlock();
        }
    }

    public String getValidToken() {
        lock.lock();
        try {
            long now = System.currentTimeMillis() / 1000;
            if (expiresAt - now < REFRESH_MARGIN) {
                if (refreshToken != null) {
                    refreshExistingToken();
                } else {
                    fetchNewToken();
                }
            }
            return accessToken;
        } finally {
            lock.unlock();
        }
    }

    private void fetchNewToken() {
        try {
            String tokenUrl = keycloakAuthUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            processTokenResponse(response.getBody());
            log.info("Successfully fetched new token. Expires in {} seconds", (expiresAt - (System.currentTimeMillis() / 1000)));
        } catch (Exception e) {
            log.error("Error fetching token from Keycloak", e);
        }
    }

    private void refreshExistingToken() {
        try {
            String tokenUrl = keycloakAuthUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", REFRESH_TOKEN);
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add(REFRESH_TOKEN, refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            processTokenResponse(response.getBody());
            log.info("Successfully refreshed token. Expires in {} seconds", (expiresAt - (System.currentTimeMillis() / 1000)));
        } catch (Exception e) {
            log.error("Error refreshing token, fetching new token instead", e);
            fetchNewToken();
        }
    }

    private void processTokenResponse(String responseBody) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

        // Obtém o access token
        accessToken = (String) responseMap.get("access_token");

        // Obtém o refresh token (se disponível)
        refreshToken = (String) responseMap.get(REFRESH_TOKEN);

        // Obtém o tempo de expiração diretamente da resposta
        if (responseMap.containsKey("expires_in")) {
            int expiresIn = ((Number) responseMap.get("expires_in")).intValue();
            expiresAt = (System.currentTimeMillis() / 1000) + expiresIn;
        } else {
            // Se não encontrar expires_in na resposta, extrai do token JWT
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);

            // Get 'exp' claim from token
            Number exp = (Number) payloadMap.get("exp");
            expiresAt = exp.longValue();
        }
    }
}