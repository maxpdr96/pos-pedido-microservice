package com.hidarisoft.pospedidomicroservice.config;

import com.hidarisoft.pospedidomicroservice.security.KeycloakTokenManager;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignClientConfig {

    private final KeycloakTokenManager keycloakTokenManager;

    public FeignClientConfig(KeycloakTokenManager keycloakTokenManager) {
        this.keycloakTokenManager = keycloakTokenManager;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Tenta obter token do contexto de segurança (usuário autenticado)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                requestTemplate.header("Authorization", "Bearer " + token);
            } else {
                // Se não houver token de usuário, usa o token de serviço
                String serviceToken = keycloakTokenManager.getValidToken();
                requestTemplate.header("Authorization", "Bearer " + serviceToken);
            }
        };
    }
}