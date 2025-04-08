package com.hidarisoft.pospedidomicroservice.repository;

import com.hidarisoft.pospedidomicroservice.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
