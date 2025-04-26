package com.hidarisoft.pospedidomicroservice.mapper;


import com.hidarisoft.pospedidomicroservice.dto.ItemPedidoDTO;
import com.hidarisoft.pospedidomicroservice.model.ItemPedido;
import com.hidarisoft.pospedidomicroservice.model.Pedido;
import java.util.Collections;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemPedidoMapper {

    @Mapping(target = "pedido", ignore = true)
    ItemPedido toEntity(ItemPedidoDTO dto);

    ItemPedidoDTO toDto(ItemPedido entity);

    List<ItemPedidoDTO> toDtoList(List<ItemPedido> entities);

    List<ItemPedido> toEntityList(List<ItemPedidoDTO> dtos);

    @AfterMapping
    default void setPedido(@MappingTarget ItemPedido entity, @Context Pedido pedido) {
        if (pedido != null) {
            entity.setPedido(pedido);
        }
    }

    @Named("mapItemsWithPedido")
    default List<ItemPedido> mapItemsWithPedido(List<ItemPedidoDTO> dtos, @Context Pedido pedido) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<ItemPedido> itens = toEntityList(dtos);
        itens.forEach(item -> item.setPedido(pedido));
        return itens;
    }
}
