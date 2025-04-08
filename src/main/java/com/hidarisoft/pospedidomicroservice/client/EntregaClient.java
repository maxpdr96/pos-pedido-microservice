package com.hidarisoft.pospedidomicroservice.client;

import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "entregas-service", url = "${entregas.service.url}")
public interface EntregaClient {

    @PostMapping("/entregas")
    ResponseEntity<EntregaResponseDTO> criarEntrega(@RequestBody CriacaoEntregaDTO entregaDTO);
}