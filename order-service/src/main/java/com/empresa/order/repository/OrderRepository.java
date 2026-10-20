package com.empresa.order.repository;

import com.empresa.order.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Lectura con bloqueo de fila - esto hace que se evita que dos cancelaciones simultaneas pasen la validacion. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithLockById(Long id);
}