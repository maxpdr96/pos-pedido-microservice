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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
        // Definir status inicial do pedido
        if (pedidoDTO.getStatus() == null) {
            pedidoDTO.setStatus(StatusPedido.CRIADO);
        }

        // Converter para entidade e salvar
        Pedido pedido = pedidoMapper.toEntity(pedidoDTO);
        pedido = pedidoRepository.save(pedido);

        // Converter de volta para DTO
        PedidoDTO novoPedidoDTO = pedidoMapper.toDto(pedido);

        // Solicitar criação de entrega para o pedido
        try {
            CriacaoEntregaDTO entregaDTO = new CriacaoEntregaDTO();
            entregaDTO.setPedidoId(pedido.getId());
            entregaDTO.setTipo("NORMAL"); // Tipo padrão, mas pode ser personalizado com base em algum campo adicional no pedido
            entregaDTO.setEnderecoEntrega(pedido.getEnderecoEntrega());

            // Calcular o valor total do pedido a partir dos itens
            BigDecimal valorTotal = pedido.getItens().stream()
                    .map(item -> {
                        // Aqui seria ideal ter o preço unitário do produto, mas vamos assumir um valor fixo para exemplo
                        BigDecimal precoUnitario = new BigDecimal("10.00"); // Valor fictício para exemplo
                        return precoUnitario.multiply(new BigDecimal(item.getQuantidade()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            entregaDTO.setValorPedido(valorTotal);

            // Observações (opcional)
            // entregaDTO.setObservacoes("Observações do pedido");

            entregaClient.criarEntrega(entregaDTO);
        } catch (FeignException e) {
            // Podemos logar o erro, mas não impedir a criação do pedido
            log.error("Erro ao solicitar criação de entrega: {}", e.getMessage());
        }

        return novoPedidoDTO;
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