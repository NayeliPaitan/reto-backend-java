package com.empresa.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Nuevo valor absoluto de stock")
public class StockUpdateRequest {

    @NotNull(message = "es obligatorio")
    @Min(value = 0, message = "no puede ser negativo")
    @Schema(example = "50")
    private Integer stock;
}
