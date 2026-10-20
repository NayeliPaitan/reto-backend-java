package com.empresa.order.client;

import com.empresa.order.config.TraceIdFilter;
import com.empresa.order.dto.StockCheckResponseDto;
import com.empresa.order.exception.InventoryServiceException;
import com.empresa.order.exception.StockInsufficientException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Component
public class WebClientInventoryClient implements InventoryClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public WebClientInventoryClient(@Qualifier("inventoryWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public StockAvailability checkStock(String productCode, int quantity) {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        String bearer = currentBearerToken();
        try {
            StockCheckResponseDto dto = webClient.get()
                    .uri(b -> b.path("/api/v1/inventory/check")
                            .queryParam("productCode", productCode)
                            .queryParam("quantity", quantity)
                            .build())
                    .headers(h -> addHeaders(h, traceId, bearer))
                    .retrieve()
                    .bodyToMono(StockCheckResponseDto.class)
                    .block(TIMEOUT);

            if (dto == null) {
                throw new InventoryServiceException("Respuesta vacia de Inventory Service", null);
            }
            return new StockAvailability(dto.isAvailable(), dto.getCurrentStock(), dto.getPrice());
        } catch (WebClientException | IllegalStateException ex) {
            throw new InventoryServiceException("No fue posible consultar Inventory Service", ex);
        }
    }

    @Override
    public void reduceStock(String productCode, int quantity) {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        String bearer = currentBearerToken();
        try {
            webClient.put()
                    .uri(b -> b.path("/api/v1/inventory/reduce-stock")
                            .queryParam("productCode", productCode)
                            .queryParam("quantity", quantity)
                            .build())
                    .headers(h -> addHeaders(h, traceId, bearer))
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);
        } catch (WebClientResponseException.Conflict ex) {
            // Inventory rechazo el descuento: otro pedido consumio el stock entre la consulta y el descuento
            throw new StockInsufficientException(
                    "El producto " + productCode + " no cuenta con stock suficiente.");
        } catch (WebClientException | IllegalStateException ex) {
            throw new InventoryServiceException("No fue posible descontar stock en Inventory Service", ex);
        }
    }

    /** Token JWT del usuario que invoca el pedido (se reenvia a Inventory, que tambien valida seguridad). */
    private String currentBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        return null;
    }

    private void addHeaders(HttpHeaders headers, String traceId, String bearer) {
        if (traceId != null && !traceId.isBlank()) {
            headers.set(TraceIdFilter.HEADER, traceId);
        }
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
    }
}
