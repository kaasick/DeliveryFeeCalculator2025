package com.fujitsu.deliveryfeecalculator.model.enums;


/**
 * Enumeration representing available cities for delivery service.
 * Each city has an associated weather station name.
 */
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

    /**
     * Find a city by station name.
     *
     * @param stationName the name of the weather station
     * @return the matching City or null if not found
     */
    public static City findByStationName(String stationName) {
        for (City city : values()) {
            if (city.getStationName().equals(stationName)) {
                return city;
            }
        }
        return null;
    }
}
