package com.hidarisoft.pospedidomicroservice.service;

import com.hidarisoft.pospedidomicroservice.dto.AtualizacaoStatusDTO;
import com.hidarisoft.pospedidomicroservice.dto.ItemPedidoDTO;
import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.model.ItemPedido;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import com.hidarisoft.pospedidomicroservice.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoService {
    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));
        return converterParaDTO(pedido);
    }

    @Transactional
    public PedidoDTO criar(PedidoDTO pedidoDTO) {
        Pedido pedido = converterParaEntidade(pedidoDTO);
        pedido = pedidoRepository.save(pedido);
        return converterParaDTO(pedido);
    }

    @Transactional
    public PedidoDTO atualizarStatus(Long id, AtualizacaoStatusDTO statusDTO) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com ID: " + id));

        pedido.setStatus(statusDTO.getStatus());
        pedido = pedidoRepository.save(pedido);

        return converterParaDTO(pedido);
    }

    private PedidoDTO converterParaDTO(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setClienteId(pedido.getClienteId());
        dto.setEnderecoEntrega(pedido.getEnderecoEntrega());
        dto.setStatus(pedido.getStatus());
        dto.setDataCriacao(pedido.getDataCriacao());

        List<ItemPedidoDTO> itensDTOs = pedido.getItens().stream()
                .map(item -> {
                    ItemPedidoDTO itemDTO = new ItemPedidoDTO();
                    itemDTO.setProduto(item.getProduto());
                    itemDTO.setQuantidade(item.getQuantidade());
                    return itemDTO;
                })
                .toList();

        dto.setItens(itensDTOs);
        return dto;
    }

    private Pedido converterParaEntidade(PedidoDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setEnderecoEntrega(dto.getEnderecoEntrega());
        pedido.setStatus(dto.getStatus());

        List<ItemPedido> itens = dto.getItens().stream()
                .map(itemDTO -> {
                    ItemPedido item = new ItemPedido();
                    item.setProduto(itemDTO.getProduto());
                    item.setQuantidade(itemDTO.getQuantidade());
                    item.setPedido(pedido);
                    return item;
                })
                .toList();

        pedido.setItens(itens);
        return pedido;
    }
}