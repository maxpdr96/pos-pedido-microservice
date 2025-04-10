package com.hidarisoft.pospedidomicroservice.exception;

public class PedidoJaEntregueException extends RuntimeException {

    private Long pedidoId;

    public PedidoJaEntregueException(Long pedidoId) {
        super("Não é possível excluir o pedido #" + pedidoId + " pois ele já foi entregue");
        this.pedidoId = pedidoId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }
}
