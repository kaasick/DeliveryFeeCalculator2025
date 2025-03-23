package com.fujitsu.deliveryfeecalculator.model.weather;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

/**
 * Model representing a weather station in the XML response.
 */
@Data
public class WeatherStation {

    private String name;
    private String wmoCode;
    private Double airTemperature;
    private Double windSpeed;
    private String phenomenon;

    @XmlElement(name = "name")
    public String getName() {
        return name;
    }

    @XmlElement(name = "wmocode")
    public String getWmoCode() {
        return wmoCode;
    }

    @XmlElement(name = "airtemperature")
    public Double getAirTemperature() {
        return airTemperature;
    }

    @XmlElement(name = "windspeed")
    public Double getWindSpeed() {
        return windSpeed;
    }

    @XmlElement(name = "phenomenon")
    public String getPhenomenon() {
        return phenomenon;
    }
}
