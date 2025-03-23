package com.fujitsu.deliveryfeecalculator.model.weather;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

import java.util.List;

/**
 * Model representing the XML response from the Estonian Environment Agency weather API.
 */
@Data
@XmlRootElement(name = "observations")
public class WeatherResponse {

    private List<WeatherStation> stations;

    @XmlElement(name = "station")
    public List<WeatherStation> getStations() {
        return stations;
    }
}