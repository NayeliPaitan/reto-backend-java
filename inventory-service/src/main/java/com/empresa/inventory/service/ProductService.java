package com.empresa.inventory.service;

import com.empresa.inventory.dto.ProductRequest;
import com.empresa.inventory.dto.ProductResponse;
import com.empresa.inventory.dto.StockCheckResponse;
import com.empresa.inventory.exception.InsufficientStockException;
import com.empresa.inventory.exception.ProductAlreadyExistsException;
import com.empresa.inventory.exception.ProductNotFoundException;
import com.empresa.inventory.model.Product;
import com.empresa.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Logica de negocio de Inventory: productos, stock y disponibilidad. */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(String productCode, int quantity) {
        Product product = productRepository.findByCode(productCode).orElse(null);

        if (product == null || product.getStock() < quantity) {
            return StockCheckResponse.builder()
                    .productCode(productCode)
                    .available(false)
                    .currentStock(product != null ? product.getStock() : 0)
                    .build();
        }
        return StockCheckResponse.builder()
                .productCode(productCode)
                .available(true)
                .currentStock(product.getStock())
                .price(product.getPrice())
                .build();
    }

    /** Descuento atomico: nunca deja el stock negativo ni sobrevende bajo concurrencia. */
    @Transactional
    public void reduceStock(String productCode, int quantity) {
        if (productRepository.decrementStock(productCode, quantity) == 0) {
            if (!productRepository.existsByCode(productCode)) {
                throw new ProductNotFoundException(productCode);
            }
            throw new InsufficientStockException(productCode);
        }
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByCode(request.getCode())) {
            throw new ProductAlreadyExistsException(request.getCode());
        }
        Product saved = productRepository.save(Product.builder()
                .code(request.getCode())
                .name(request.getName())
                .stock(request.getStock())
                .price(request.getPrice())
                .build());
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String code) {
        return ProductResponse.from(find(code));
    }

    @Transactional
    public ProductResponse updateStock(String code, int newStock) {
        Product product = find(code);
        product.setStock(newStock);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product find(String code) {
        return productRepository.findByCode(code).orElseThrow(() -> new ProductNotFoundException(code));
    }
}
