package com.empresa.order.mapper;

import com.empresa.order.dto.OrderHistoryResponse;
import com.empresa.order.dto.OrderResponse;
import com.empresa.order.model.Order;
import com.empresa.order.model.OrderHistory;
import org.mapstruct.Mapper;

/** Entidad -> DTO generado en compilacion por MapStruct. El controller no expone entidades JPA. */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    OrderHistoryResponse toHistoryResponse(OrderHistory history);
}
