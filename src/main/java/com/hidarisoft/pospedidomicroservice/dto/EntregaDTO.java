package com.hidarisoft.pospedidomicroservice.dto;

import com.hidarisoft.pospedidomicroservice.enums.StatusEntrega;
import com.hidarisoft.pospedidomicroservice.enums.TipoEntrega;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDTO {
    private Long id;
    private Long pedidoId;
    private Long entregadorId;
    private String nomeEntregador;
    private StatusEntrega status;
    private TipoEntrega tipo;
    private String enderecoEntrega;
    private BigDecimal valorEntrega;
    private BigDecimal valorPedido;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer tempoEstimado;
    private String observacoes;
}