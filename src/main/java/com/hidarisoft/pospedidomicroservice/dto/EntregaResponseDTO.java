package com.hidarisoft.pospedidomicroservice.dto;

import com.hidarisoft.pospedidomicroservice.enums.StatusEntrega;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaResponseDTO {
    private Long id;
    private Long pedidoId;
    private Long entregadorId;
    private String nomeEntregador;
    private StatusEntrega status;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

}