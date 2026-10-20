package com.empresa.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para registrar un pedido")
public class OrderRequestDto {

    @NotBlank(message = "es obligatorio")
    @Size(max = 50, message = "no debe superar los 50 caracteres")
    @Schema(description = "Codigo del producto", example = "PROD-001")
    private String productCode;

    @NotNull(message = "es obligatorio")
    @Min(value = 1, message = "debe ser mayor o igual a 1")
    @Max(value = 10000, message = "no debe superar 10000 unidades")
    @Schema(description = "Cantidad solicitada", example = "2")
    private Integer quantity;
}