package com.fujitsu.deliveryfeecalculator.exception;

/**
 * Exception thrown when there is an issue with delivery fee calculation.
 * This can occur if the requested vehicle type is forbidden due to
 * weather conditions or if there's an issue with the calculation process.
 */
public class DeliveryFeeCalculationException extends RuntimeException {

    /**
     * Constructs a new delivery fee calculation exception with the specified detail message.
     *
     * @param message the detail message
     */
    public DeliveryFeeCalculationException(String message) {
        super(message);
    }
}