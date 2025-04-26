package com.hidarisoft.pospedidomicroservice.service;

import com.hidarisoft.pospedidomicroservice.client.EntregaClient;
import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.enums.StatusPedido;
import com.hidarisoft.pospedidomicroservice.exception.PedidoJaEntregueException;
import com.hidarisoft.pospedidomicroservice.mapper.PedidoMapper;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import com.hidarisoft.pospedidomicroservice.repository.PedidoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@Slf4j
public class PedidoSagaService {

    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final EntregaClient entregaClient;

    public PedidoSagaService(PedidoRepository pedidoRepository, PedidoMapper pedidoMapper, EntregaClient entregaClient) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoMapper = pedidoMapper;
        this.entregaClient = entregaClient;
    }

    public PedidoDTO criarPedido(PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoMapper.toEntity(pedidoDTO);
        pedido = pedidoRepository.save(pedido);
        pedidoDTO.setDataCriacao(pedido.getDataCriacao());

        try {
            chamaServicoEntrega(pedidoDTO, pedido);
            // 3. Se sucesso, atualiza o pedido
            pedido.setStatus(StatusPedido.CRIADO);
            pedidoDTO.setStatus(StatusPedido.CRIADO);
            pedidoRepository.save(pedido);
            return pedidoDTO;
        } catch (Exception ex) {
            // 4. Compensação: cancela o pedido
            pedidoDTO.setStatus(StatusPedido.CANCELADO);
            pedido.setStatus(StatusPedido.CANCELADO);
            pedidoRepository.save(pedido);
            throw new RuntimeException("Erro ao criar entrega, pedido cancelado");
        }
    }

    private void chamaServicoEntrega(PedidoDTO pedidoDTO, Pedido pedido) {
        // Solicitar criação de entrega para o pedido
        try {
            CriacaoEntregaDTO entregaDTO = criarEntregaDTO(pedidoDTO, pedido);
            var response = entregaClient.criarEntrega(entregaDTO);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Entrega criada com ID: {}", Objects.requireNonNull(response.getBody()).getId());
                pedidoDTO.setStatus(StatusPedido.EM_TRANSPORTE);
                return;
            }

            pedidoDTO.setStatus(StatusPedido.PROCESSANDO);
            log.warn("Criação de entrega retornou status inesperado: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar entrega: {}", e.getMessage(), e);
            throw e;
        }
    }

    private CriacaoEntregaDTO criarEntregaDTO(PedidoDTO pedidoDTO, Pedido pedido) {
        CriacaoEntregaDTO entregaDTO = new CriacaoEntregaDTO();
        entregaDTO.setPedidoId(pedido.getId());
        entregaDTO.setTipo(pedidoDTO.getTipoEntrega());
        entregaDTO.setEnderecoEntrega(pedido.getEnderecoEntrega());
        entregaDTO.setValorPedido(calcularValorTotal(pedido));
        entregaDTO.setObservacoes(pedidoDTO.getObservacoes());

        return entregaDTO;
    }


    private BigDecimal calcularValorTotal(Pedido pedido) {
        return pedido.getItens().stream()
                .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void excluirPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        // Verificar se o pedido pode ser excluído
        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new PedidoJaEntregueException(pedidoId);
        }

        try
        {
            ResponseEntity<EntregaDTO> response = entregaClient.buscarEntregaPorPedidoId(pedidoId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                // Excluir a entrega antes de excluir o pedido
                Long entregaId = Objects.requireNonNull(response.getBody(), "Response body is null").getId();

                entregaClient.excluirEntrega(entregaId);
            }

            pedidoRepository.deleteById(pedidoId); // ou atualiza status para "CANCELADO"
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir entrega. Pedido não removido.");
        }
    }
}
