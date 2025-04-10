package com.hidarisoft.pospedidomicroservice.service;

import com.hidarisoft.pospedidomicroservice.client.EntregaClient;
import com.hidarisoft.pospedidomicroservice.dto.AtualizacaoStatusDTO;
import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.enums.StatusPedido;
import com.hidarisoft.pospedidomicroservice.exception.PedidoJaEntregueException;
import com.hidarisoft.pospedidomicroservice.mapper.PedidoMapper;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import com.hidarisoft.pospedidomicroservice.repository.PedidoRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PedidoService {
    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final EntregaClient entregaClient;

    public PedidoService(PedidoRepository pedidoRepository, PedidoMapper pedidoMapper, EntregaClient entregaClient) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoMapper = pedidoMapper;
        this.entregaClient = entregaClient;
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        return pedidoMapper.toDtoList(pedidos);
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));
        return pedidoMapper.toDto(pedido);
    }


    public PedidoDTO criar(PedidoDTO pedidoDTO) {
        Pedido pedido = salvaPedido(pedidoDTO);
        chamaServicoEntrega(pedidoDTO, pedido);
        return pedidoDTO;
    }

    private Pedido salvaPedido(PedidoDTO pedidoDTO) {
        // Converter para entidade e salvar
        Pedido pedido = pedidoMapper.toEntity(pedidoDTO);
        pedido = pedidoRepository.save(pedido);
        log.info("Pedido criado com ID: {}", pedido.getId());
        return pedido;
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

        } catch (FeignException e) {
            // Loga o erro, mas não impedir a criação do pedido
            log.error("Erro ao solicitar criação de entrega: {}", e.getMessage(), e);
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

    @Transactional
    public PedidoDTO atualizarStatus(Long id, AtualizacaoStatusDTO statusDTO) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));

        pedido.setStatus(statusDTO.getStatus());
        pedido = pedidoRepository.save(pedido);

        return pedidoMapper.toDto(pedido);
    }

    @Transactional
    public void excluirPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));

        // Verificar se o pedido pode ser excluído
        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new PedidoJaEntregueException(id);
        }

        // Verificar se existe uma entrega associada a este pedido
        try {
            ResponseEntity<EntregaDTO> response = entregaClient.buscarEntregaPorPedidoId(id);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                // Excluir a entrega antes de excluir o pedido
                Long entregaId = response.getBody().getId();
                entregaClient.excluirEntrega(entregaId);
            }
        } catch (Exception e) {
            // Log o erro, mas continue com a exclusão do pedido
            log.error("Erro ao tentar excluir entrega associada ao pedido {}: {}", id, e.getMessage());
        }

        // Excluir o pedido (e seus itens em cascata)
        pedidoRepository.delete(pedido);
    }
}