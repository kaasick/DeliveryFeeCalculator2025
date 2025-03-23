package com.fujitsu.deliveryfeecalculator.model.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration representing available weather phenomena
 * Each phenomena falls into a specific category.
 */
public enum WeatherPhenomenon {
    SNOW(new HashSet<>(Arrays.asList(
            "light snow shower", "moderate snow shower", "heavy snow shower",
            "light snowfall", "moderate snowfall", "heavy snowfall"
    ))),
    SLEET(new HashSet<>(Arrays.asList("light sleet", "moderate sleet"))),
    RAIN(new HashSet<>(Arrays.asList(
            "light shower", "moderate shower", "heavy shower",
            "light rain", "moderate rain", "heavy rain"
    ))),
    FORBIDDEN(new HashSet<>(Arrays.asList("glaze", "hail", "thunder", "thunderstorm"))),
    NORMAL(new HashSet<>(Arrays.asList(
            "clear", "few clouds", "variable clouds", "cloudy with clear spells",
            "overcast", "mist", "fog"
    )));

    private final Set<String> phenomena;

    WeatherPhenomenon(Set<String> phenomena) {
        this.phenomena = phenomena;
    }

    public Set<String> getPhenomena() {
        return phenomena;
    }

    /** Categorize a weather phenomenon string into one of the enum values. */
    public static WeatherPhenomenon categorize(String phenomenonStr) {
        if (phenomenonStr == null || phenomenonStr.trim().isEmpty()) {
            return NORMAL;
        }

        // Normalizing just in case
        String normalized = phenomenonStr.toLowerCase().trim()
                .replaceAll("\\s+", " ")
                .replaceAll("-", " ");

        // Exact match
        for (WeatherPhenomenon category : values()) {
            if (category.phenomena.contains(normalized)) {
                return category;
            }
        }

        // Partial match with priority
        if (FORBIDDEN.phenomena.stream().anyMatch(normalized::contains)) {
            return FORBIDDEN;
        }
        if (SNOW.phenomena.stream().anyMatch(normalized::contains)) {
            return SNOW;
        }
        if (SLEET.phenomena.stream().anyMatch(normalized::contains)) {
            return SLEET;
        }
        if (RAIN.phenomena.stream().anyMatch(normalized::contains)) {
            return RAIN;
        }

        return NORMAL;
    }

    /** Check if this weather category forbids vehicle usage. */
    public boolean isUsageForbidden() {
        return this == FORBIDDEN;
    }
}