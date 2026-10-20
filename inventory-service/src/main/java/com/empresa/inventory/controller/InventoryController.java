package com.empresa.inventory.controller;

import com.empresa.inventory.dto.StockCheckResponse;
import com.empresa.inventory.exception.ErrorResponse;
import com.empresa.inventory.exception.InvalidRequestException;
import com.empresa.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventario", description = "Disponibilidad y descuento de stock")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductService productService;

    @Operation(summary = "Consultar disponibilidad",
            description = "Indica si hay stock suficiente para la cantidad solicitada y devuelve el precio unitario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad consultada"),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos (INVALID_REQUEST)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check")
    public ResponseEntity<StockCheckResponse> checkStock(
            @RequestParam String productCode,
            @RequestParam Integer quantity) {

        validateQuantity(quantity);
        return ResponseEntity.ok(productService.checkStock(productCode, quantity));
    }

    @Operation(summary = "Descontar stock",
            description = "Reduce el stock de forma atomica. Lo invoca Order Service al confirmar un pedido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock descontado"),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos (INVALID_REQUEST)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto inexistente (PRODUCT_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente (STOCK_INSUFFICIENT)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/reduce-stock")
    public ResponseEntity<Void> reduceStock(
            @RequestParam String productCode,
            @RequestParam Integer quantity) {

        validateQuantity(quantity);
        productService.reduceStock(productCode, quantity);
        return ResponseEntity.ok().build();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidRequestException("quantity debe ser mayor o igual a 1");
        }
    }
}
