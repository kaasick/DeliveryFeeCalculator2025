package com.fujitsu.deliveryfeecalculator.model.enums;


import lombok.Getter;

/**
 * Enumeration representing available cities for delivery service.
 * Each city has an associated weather station name.
 */
@Getter
public enum City {
    TALLINN("Tallinn-Harku"),
    TARTU("Tartu-Tõravere"),
    PARNU("Pärnu");

    private final String stationName;

    City(String stationName) {
        this.stationName = stationName;
    }

    /**
     * Returns the weather station name associated with this city.
     *
     * @return the name of the weather station
     */
    public String getStationName() {
        return stationName;
    }
}
