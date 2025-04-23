package com.hidarisoft.pospedidomicroservice.service;

import com.hidarisoft.pospedidomicroservice.client.EntregaClient;
import com.hidarisoft.pospedidomicroservice.dto.AtualizacaoStatusDTO;
import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.EntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoResponseDTO;
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
    public List<PedidoResponseDTO> listarTodos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        return pedidoMapper.toDtoResponseList(pedidos);
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));
        return pedidoMapper.toDto(pedido);
    }


    //public PedidoDTO criar(PedidoDTO pedidoDTO) {
     //   Pedido pedido = salvaPedido(pedidoDTO);
      //  chamaServicoEntrega(pedidoDTO, pedido);
     //   return pedidoDTO;
  //  }

    private Pedido salvaPedido(PedidoDTO pedidoDTO) {
        // Converter para entidade e salvar
        Pedido pedido = pedidoMapper.toEntity(pedidoDTO);
        pedido = pedidoRepository.save(pedido);
        pedidoDTO.setDataCriacao(pedido.getDataCriacao());
        log.info("Pedido criado com ID: {}", pedido.getId());
        return pedido;
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