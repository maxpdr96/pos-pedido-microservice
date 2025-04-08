package com.hidarisoft.pospedidomicroservice.dto;

import com.hidarisoft.pospedidomicroservice.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizacaoStatusDTO {
    @NotNull(message = "O status é obrigatório")
    private StatusPedido status;
}