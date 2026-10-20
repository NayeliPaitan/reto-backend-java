package com.empresa.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos para registrar un producto")
public class ProductRequest {

    @NotBlank(message = "es obligatorio")
    @Size(max = 50, message = "no debe superar los 50 caracteres")
    @Schema(example = "PROD-003")
    private String code;

    @NotBlank(message = "es obligatorio")
    @Size(max = 120, message = "no debe superar los 120 caracteres")
    @Schema(example = "Mouse Inalambrico")
    private String name;

    @NotNull(message = "es obligatorio")
    @Min(value = 0, message = "no puede ser negativo")
    @Schema(example = "25")
    private Integer stock;

    @NotNull(message = "es obligatorio")
    @DecimalMin(value = "0.01", message = "debe ser mayor a 0")
    @Schema(example = "35.50")
    private BigDecimal price;
}
