package com.empresa.inventory.controller;

import com.empresa.inventory.dto.ProductRequest;
import com.empresa.inventory.dto.ProductResponse;
import com.empresa.inventory.dto.StockUpdateRequest;
import com.empresa.inventory.exception.ErrorResponse;
import com.empresa.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Productos", description = "Administracion de productos y stock")
@RestController
@RequestMapping("/api/v1/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Registrar un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (INVALID_REQUEST)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Codigo duplicado (PRODUCT_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @Operation(summary = "Listar productos")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @Operation(summary = "Consultar un producto por codigo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto inexistente (PRODUCT_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{code}")
    public ResponseEntity<ProductResponse> get(@PathVariable String code) {
        return ResponseEntity.ok(productService.get(code));
    }

    @Operation(summary = "Ajustar el stock de un producto",
            description = "Reemplaza el stock actual por el valor indicado (no negativo).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (INVALID_REQUEST)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto inexistente (PRODUCT_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{code}/stock")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable String code,
                                                       @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(productService.updateStock(code, request.getStock()));
    }
}
