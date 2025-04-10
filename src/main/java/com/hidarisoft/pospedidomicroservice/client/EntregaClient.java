package com.hidarisoft.pospedidomicroservice.client;

import com.hidarisoft.pospedidomicroservice.config.FeignClientConfig;
import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "entregas-service", url = "${entregas.service.url}", configuration = FeignClientConfig.class)
public interface EntregaClient {

    @PostMapping("/entregas")
    ResponseEntity<EntregaResponseDTO> criarEntrega(@RequestBody CriacaoEntregaDTO entregaDTO);

    @GetMapping("/entregas/pedido/{pedidoId}")
    ResponseEntity<EntregaDTO> buscarEntregaPorPedidoId(@PathVariable("pedidoId") Long pedidoId);

    @DeleteMapping("/entregas/{id}")
    ResponseEntity<Void> excluirEntrega(@PathVariable("id") Long id);
}