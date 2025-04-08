package com.hidarisoft.pospedidomicroservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriacaoEntregaDTO {
    private Long pedidoId;
    private Long entregadorId;
    private String tipo; // NORMAL, EXPRESSA, RETIRADA
    private String enderecoEntrega;
    private BigDecimal valorPedido;
    private String observacoes;
}