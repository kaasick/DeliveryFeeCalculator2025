package com.fujitsu.deliveryfeecalculator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryFeeResponse {

    private BigDecimal fee;
    private String message;

    /**
     * Constructor for successful fee calculation.
     */
    public DeliveryFeeResponse(BigDecimal fee) {
        this.fee = fee;
    }

    /**
     * Constructor for error response.
     */
    public DeliveryFeeResponse(String errorMessage) {
        this.message = errorMessage;
    }
}
