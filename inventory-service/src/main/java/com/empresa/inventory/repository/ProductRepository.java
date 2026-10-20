package com.empresa.inventory.repository;

import com.empresa.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * Descuento atomico: la condicion {@code stock >= quantity} se evalua en la propia sentencia SQL,
     * por lo que dos pedidos simultaneos no pueden sobrevender ni dejar el stock negativo.
     *
     * @return filas afectadas (0 si el producto no existe o no hay stock suficiente)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :quantity where p.code = :code and p.stock >= :quantity")
    int decrementStock(@Param("code") String code, @Param("quantity") int quantity);
}
