package com.empresa.order.controller;

import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderRequestDto;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.exception.ErrorResponse;
import com.empresa.order.service.OrderService;
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

@Tag(name = "Pedidos", description = "Registro, consulta, cancelacion e historial de pedidos")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Registrar un pedido",
            description = "Registra el pedido en PENDING, valida y descuenta stock en Inventory y lo confirma (CONFIRMED).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido confirmado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos (INVALID_REQUEST) o sin stock (STOCK_INSUFFICIENT)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, invalido o expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Inventory no disponible (INVENTORY_UNAVAILABLE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @Operation(summary = "Consultar un pedido por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente (ORDER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Consultar el historial de estados de un pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial ordenado cronologicamente"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente (ORDER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderHistory(id));
    }

    @Operation(summary = "Cancelar un pedido",
            description = "Solo se puede cancelar un pedido PENDING o CONFIRMED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente (ORDER_NOT_FOUND)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transicion invalida (INVALID_TRANSITION)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}