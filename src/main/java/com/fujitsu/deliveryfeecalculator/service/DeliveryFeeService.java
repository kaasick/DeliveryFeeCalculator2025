package com.fujitsu.deliveryfeecalculator.service;


import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.enums.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for calculating delivery fees based on city, vehicle type, and weather conditions.
 */
public interface DeliveryFeeService {

    /**
     * Calculates the delivery fee based on city, vehicle type, and current weather conditions.
     *
     * @param city        the city where delivery is taking place
     * @param vehicleType the type of vehicle used for delivery
     * @return the calculated delivery fee
     * @throws IllegalArgumentException if vehicle usage is forbidden due to weather conditions
     */
    BigDecimal calculateFee(City city, VehicleType vehicleType);

    /**
     * Calculates the delivery fee based on city, vehicle type, and weather conditions at the specified time.
     *
     * @param city        the city where delivery is taking place
     * @param vehicleType the type of vehicle used for delivery
     * @param timestamp   the time for which to calculate the fee (uses historical weather data)
     * @return the calculated delivery fee
     * @throws IllegalArgumentException if vehicle usage is forbidden due to weather conditions
     */
    BigDecimal calculateFee(City city, VehicleType vehicleType, LocalDateTime timestamp);
}