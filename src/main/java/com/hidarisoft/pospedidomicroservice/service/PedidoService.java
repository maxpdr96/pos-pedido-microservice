package com.hidarisoft.pospedidomicroservice.service;

import com.hidarisoft.pospedidomicroservice.client.EntregaClient;
import com.hidarisoft.pospedidomicroservice.dto.AtualizacaoStatusDTO;
import com.hidarisoft.pospedidomicroservice.dto.CriacaoEntregaDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.enums.StatusPedido;
import com.hidarisoft.pospedidomicroservice.mapper.PedidoMapper;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import com.hidarisoft.pospedidomicroservice.repository.PedidoRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @Transactional
    public PedidoDTO criar(PedidoDTO pedidoDTO) {
        // Converter para entidade e salvar
        Pedido pedido = pedidoMapper.toEntity(pedidoDTO);
        pedido = pedidoRepository.save(pedido);
        log.info("Pedido criado com ID: {}", pedido.getId());

        // Solicitar criação de entrega para o pedido
        try {
            CriacaoEntregaDTO entregaDTO = new CriacaoEntregaDTO();
            entregaDTO.setPedidoId(pedido.getId());
            entregaDTO.setTipo(pedidoDTO.getTipoEntrega());
            entregaDTO.setEnderecoEntrega(pedido.getEnderecoEntrega());

            // Calcular o valor total do pedido a partir dos itens
            BigDecimal valorTotal = pedido.getItens().stream()
                    .map(item -> item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            entregaDTO.setValorPedido(valorTotal);

            entregaDTO.setObservacoes(entregaDTO.getObservacoes());

            var response = entregaClient.criarEntrega(entregaDTO);
            if (Boolean.TRUE.equals(HttpStatus.valueOf(response.getStatusCode().value()).is2xxSuccessful())) {
                log.info("entrega criado com ID: {}", Objects.requireNonNull(response.getBody()).getId());
            }

        } catch (FeignException e) {
            // Podemos logar o erro, mas não impedir a criação do pedido
            log.error("Erro ao solicitar criação de entrega: {}", e.getMessage());
        }

        return pedidoDTO;
    }

    @Transactional
    public PedidoDTO atualizarStatus(Long id, AtualizacaoStatusDTO statusDTO) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));

        pedido.setStatus(statusDTO.getStatus());
        pedido = pedidoRepository.save(pedido);

        return pedidoMapper.toDto(pedido);
    }
}