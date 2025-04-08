package com.hidarisoft.pospedidomicroservice.mapper;

import com.hidarisoft.pospedidomicroservice.dto.PedidoDTO;
import com.hidarisoft.pospedidomicroservice.model.ItemPedido;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ItemPedidoMapper.class})
public interface PedidoMapper {

    @Mapping(target = "itens", ignore = true)
    Pedido toEntity(PedidoDTO dto);

    PedidoDTO toDto(Pedido entity);

    List<PedidoDTO> toDtoList(List<Pedido> entities);

    @AfterMapping
    default void mapItens(PedidoDTO dto, @MappingTarget Pedido entity) {
        if (dto.getItens() != null) {
            entity.setItens(new java.util.ArrayList<>());
            dto.getItens().forEach(itemDto -> {
                ItemPedido item = new ItemPedido();
                item.setProduto(itemDto.getProduto());
                item.setQuantidade(itemDto.getQuantidade());
                item.setPedido(entity);
                item.setPrecoUnitario(itemDto.getPrecoUnitario());
                entity.getItens().add(item);
            });
        }
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PedidoDTO dto, @MappingTarget Pedido entity);
}