package com.fujitsu.deliveryfeecalculator.service;

import com.fujitsu.deliveryfeecalculator.exception.WeatherDataNotFoundException;
import com.fujitsu.deliveryfeecalculator.model.entity.WeatherData;
import com.fujitsu.deliveryfeecalculator.model.enums.City;
import com.fujitsu.deliveryfeecalculator.model.weather.WeatherResponse;
import com.fujitsu.deliveryfeecalculator.model.weather.WeatherStation;
import com.fujitsu.deliveryfeecalculator.repository.WeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherDataRepository weatherDataRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Captor
    private ArgumentCaptor<List<WeatherData>> weatherDataCaptor;

    private final String TEST_API_URL = "https://test-api-url.com";

    @BeforeEach
    void setUp() {
        // Set the weatherApiUrl field value using reflection
        ReflectionTestUtils.setField(weatherService, "weatherApiUrl", TEST_API_URL);
    }

    @Test
    @DisplayName("Should get latest weather data for a city")
    void getLatestWeatherData_shouldReturnData() {
        // Arrange
        String stationName = City.TALLINN.getStationName();
        WeatherData expectedData = WeatherData.builder()
                .stationName(stationName)
                .airTemperature(10.0)
                .windSpeed(5.0)
                .weatherPhenomenon("clear")
                .timestamp(LocalDateTime.now())
                .build();

        when(weatherDataRepository.findLatestByStationName(stationName))
                .thenReturn(Optional.of(expectedData));

        // Act
        WeatherData result = weatherService.getLatestWeatherData(City.TALLINN);

        // Assert
        assertNotNull(result);
        assertEquals(stationName, result.getStationName());
        assertEquals(expectedData.getAirTemperature(), result.getAirTemperature());
        assertEquals(expectedData.getWindSpeed(), result.getWindSpeed());
        assertEquals(expectedData.getWeatherPhenomenon(), result.getWeatherPhenomenon());
    }

    @Test
    @DisplayName("Should throw exception when no latest weather data found")
    void getLatestWeatherData_shouldThrowException() {
        // Arrange
        String stationName = City.TALLINN.getStationName();
        when(weatherDataRepository.findLatestByStationName(stationName))
                .thenReturn(Optional.empty());

        // Act & Assert
        WeatherDataNotFoundException exception = assertThrows(
                WeatherDataNotFoundException.class,
                () -> weatherService.getLatestWeatherData(City.TALLINN)
        );

        assertTrue(exception.getMessage().contains(stationName));
    }

    @Test
    @DisplayName("Should get weather data by timestamp")
    void getWeatherDataByTimestamp_dataExists_returnsData() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);
        String stationName = City.TALLINN.getStationName();

        WeatherData expectedData = WeatherData.builder()
                .stationName(stationName)
                .airTemperature(-2.0)
                .windSpeed(6.5)
                .weatherPhenomenon("light snow")
                .timestamp(testTime.minusHours(1)) // One hour before the requested time
                .build();

        when(weatherDataRepository.findClosestByStationNameAndTimestamp(stationName, testTime))
                .thenReturn(Optional.of(expectedData));

        // Act
        WeatherData result = weatherService.getWeatherDataByTimestamp(City.TALLINN, testTime);

        // Assert
        assertNotNull(result);
        assertEquals(stationName, result.getStationName());
        assertEquals(testTime.minusHours(1), result.getTimestamp());
        assertEquals(-2.0, result.getAirTemperature());
        assertEquals(6.5, result.getWindSpeed());
        assertEquals("light snow", result.getWeatherPhenomenon());
    }

    @Test
    @DisplayName("Should throw exception when no historical data found")
    void getWeatherDataByTimestamp_noData_throwsException() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2024, 3, 1, 12, 0);
        String stationName = City.TARTU.getStationName();

        when(weatherDataRepository.findClosestByStationNameAndTimestamp(stationName, testTime))
                .thenReturn(Optional.empty());

        // Act & Assert
        WeatherDataNotFoundException exception = assertThrows(
                WeatherDataNotFoundException.class,
                () -> weatherService.getWeatherDataByTimestamp(City.TARTU, testTime)
        );

        assertTrue(exception.getMessage().contains(stationName));
        assertTrue(exception.getMessage().contains(testTime.toString()));
    }

    @Test
    @DisplayName("Should fetch and store weather data successfully")
    void fetchAndStoreWeatherData_success() {
        // Arrange
        WeatherResponse response = new WeatherResponse();
        List<WeatherStation> stations = new ArrayList<>();

        // Create test stations
        WeatherStation tallinnStation = new WeatherStation();
        tallinnStation.setName("Tallinn-Harku");
        tallinnStation.setAirTemperature(5.0);
        tallinnStation.setWindSpeed(4.2);
        tallinnStation.setPhenomenon("few clouds");
        tallinnStation.setWmoCode("26038");

        WeatherStation tartuStation = new WeatherStation();
        tartuStation.setName("Tartu-Tõravere");
        tartuStation.setAirTemperature(4.5);
        tartuStation.setWindSpeed(3.8);
        tartuStation.setPhenomenon("clear");
        tartuStation.setWmoCode("26242");

        WeatherStation parnuStation = new WeatherStation();
        parnuStation.setName("Pärnu");
        parnuStation.setAirTemperature(6.2);
        parnuStation.setWindSpeed(7.5);
        parnuStation.setPhenomenon("light rain");
        parnuStation.setWmoCode("41803");

        // Add all stations
        stations.add(tallinnStation);
        stations.add(tartuStation);
        stations.add(parnuStation);
        stations.add(createRandomStation("Random Station")); // This one should be filtered out

        response.setStations(stations);

        when(restTemplate.getForObject(TEST_API_URL, WeatherResponse.class)).thenReturn(response);

        // Act
        weatherService.fetchAndStoreWeatherData();

        // Assert
        verify(weatherDataRepository).saveAll(weatherDataCaptor.capture());

        List<WeatherData> savedData = weatherDataCaptor.getValue();
        assertEquals(3, savedData.size()); // Should only save the 3 monitored stations

        // Check the saved data contains correct stations
        boolean hasTallinn = false;
        boolean hasTartu = false;
        boolean hasParnu = false;

        for (WeatherData data : savedData) {
            switch (data.getStationName()) {
                case "Tallinn-Harku":
                    hasTallinn = true;
                    assertEquals(5.0, data.getAirTemperature());
                    assertEquals(4.2, data.getWindSpeed());
                    assertEquals("few clouds", data.getWeatherPhenomenon());
                    break;
                case "Tartu-Tõravere":
                    hasTartu = true;
                    assertEquals(4.5, data.getAirTemperature());
                    assertEquals(3.8, data.getWindSpeed());
                    assertEquals("clear", data.getWeatherPhenomenon());
                    break;
                case "Pärnu":
                    hasParnu = true;
                    assertEquals(6.2, data.getAirTemperature());
                    assertEquals(7.5, data.getWindSpeed());
                    assertEquals("light rain", data.getWeatherPhenomenon());
                    break;
            }
        }

        assertTrue(hasTallinn, "Tallinn data not saved");
        assertTrue(hasTartu, "Tartu data not saved");
        assertTrue(hasParnu, "Pärnu data not saved");
    }

    @Test
    @DisplayName("Should handle empty response when fetching weather data")
    void fetchAndStoreWeatherData_emptyResponse() {
        // Arrange
        when(restTemplate.getForObject(TEST_API_URL, WeatherResponse.class)).thenReturn(null);

        // Act
        weatherService.fetchAndStoreWeatherData();

        // Assert
        verify(weatherDataRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle API exception when fetching weather data")
    void fetchAndStoreWeatherData_apiException() {
        // Arrange
        when(restTemplate.getForObject(TEST_API_URL, WeatherResponse.class))
                .thenThrow(new RestClientException("API error"));

        // Act - should not throw exception
        weatherService.fetchAndStoreWeatherData();

        // Assert
        verify(weatherDataRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle empty stations list when fetching weather data")
    void fetchAndStoreWeatherData_emptyStationsList() {
        // Arrange
        WeatherResponse response = new WeatherResponse();
        response.setStations(new ArrayList<>());

        when(restTemplate.getForObject(TEST_API_URL, WeatherResponse.class)).thenReturn(response);

        // Act
        weatherService.fetchAndStoreWeatherData();

        // Assert
        verify(weatherDataRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle no monitored stations in response")
    void fetchAndStoreWeatherData_noMonitoredStations() {
        // Arrange
        WeatherResponse response = new WeatherResponse();
        List<WeatherStation> stations = new ArrayList<>();

        // Add some random stations, none of which are monitored
        stations.add(createRandomStation("Station1"));
        stations.add(createRandomStation("Station2"));

        response.setStations(stations);

        when(restTemplate.getForObject(TEST_API_URL, WeatherResponse.class)).thenReturn(response);

        // Act
        weatherService.fetchAndStoreWeatherData();

        // Assert
        verify(weatherDataRepository, never()).saveAll(anyList());
    }

    // Helper method to create a random station
    private WeatherStation createRandomStation(String name) {
        WeatherStation station = new WeatherStation();
        station.setName(name);
        station.setAirTemperature(10.0);
        station.setWindSpeed(5.0);
        station.setPhenomenon("clear");
        station.setWmoCode("12345");
        return station;
    }
}