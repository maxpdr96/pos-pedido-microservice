package com.hidarisoft.pospedidomicroservice.controller;


import com.hidarisoft.pospedidomicroservice.dto.AtualizacaoStatusDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoResponseDTO;
import com.hidarisoft.pospedidomicroservice.service.PedidoSagaService;
import com.hidarisoft.pospedidomicroservice.service.PedidoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@Slf4j
public class PedidoController {
    private final PedidoService pedidoService;
    private final PedidoSagaService pedidoSagaService;

    public PedidoController(PedidoService pedidoService, PedidoSagaService pedidoSagaService) {
        this.pedidoService = pedidoService;
        this.pedidoSagaService = pedidoSagaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<?> criarPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        try {
        PedidoDTO novoPedido = pedidoSagaService.criarPedido(pedidoDTO);
        log.info("Novo pedido criado: {}", novoPedido);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criar pedido: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE', 'ENTREGADOR')")
    public ResponseEntity<PedidoDTO> buscarPorId(@PathVariable Long id) {
        PedidoDTO pedido = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(pedido);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        List<PedidoResponseDTO> pedidos = pedidoService.listarTodos();
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','ENTREGADOR')")
    public ResponseEntity<PedidoDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizacaoStatusDTO statusDTO) {
        PedidoDTO pedidoAtualizado = pedidoService.atualizarStatus(id, statusDTO);
        return ResponseEntity.ok(pedidoAtualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> excluirPedido(@PathVariable Long id) {
        try {
            pedidoSagaService.excluirPedido(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao excluir pedido: " + e.getMessage());
        }
    }
}
