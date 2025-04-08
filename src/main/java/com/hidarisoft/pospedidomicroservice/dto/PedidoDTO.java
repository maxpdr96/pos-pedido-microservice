package com.hidarisoft.pospedidomicroservice.dto;


import com.hidarisoft.pospedidomicroservice.enums.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

    @NotNull(message = "O ID do cliente é obrigatório")
    private Long clienteId;

    @NotBlank(message = "O endereço de entrega é obrigatório")
    private String enderecoEntrega;

    private StatusPedido status;

    private LocalDateTime dataCriacao;

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    private List<ItemPedidoDTO> itens;
}