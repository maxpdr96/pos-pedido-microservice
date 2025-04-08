package com.hidarisoft.pospedidomicroservice.dto;

import com.hidarisoft.pospedidomicroservice.enums.TipoEntrega;
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
    private TipoEntrega tipo; // NORMAL, EXPRESSA, RETIRADA
    private String enderecoEntrega;
    private BigDecimal valorPedido;
    private String observacoes;
}