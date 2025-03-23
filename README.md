# Delivery Fee Calculator

This application offer a sub-functionality to calculate delivery fees for food couriers based on regional base fees, vehicle types, and weather conditions. It fetches real-time weather data from the Estonian Environment Agency and applies business rules to determine the appropriate delivery fee.

## Features

- **Weather Data Import**: Automatically imports weather data from the Estonian Environment Agency at configurable intervals
- **Fee Calculation**: Calculates delivery fees based on:
    - City (Tallinn, Tartu, Pärnu)
    - Vehicle type (Car, Scooter, Bike)
    - Current weather conditions (temperature, wind speed, weather phenomena)
- **REST API**: Simple REST interface for calculating fees
- **Historical Data**: Supports calculating fees based on historical weather data 
- **OpenAPI Documentation**: Interactive API documentation using Swagger UI

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6+

### Installation

1. Clone the repository
   ```
   git clone https://github.com/fujitsu/delivery-fee-calculator.git
   cd delivery-fee-calculator
   ```

2. Build the project
   ```
   ./mvnw clean package
   ```

3. Run the application
   ```
   ./mvnw spring-boot:run
   ```

The application will be available at `http://localhost:8080`

## API Documentation

The API documentation is available through Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

### Key Endpoints

- **Calculate Delivery Fee**: `GET /api/delivery-fee/{city}/{vehicleType}`
- **Calculate Historical Fee**: `GET /api/delivery-fee/{city}/{vehicleType}/at?datetime={datetime}`
- **View Weather Data**: `GET /api/weather`

### Example Requests

**Calculate current delivery fee for a car in Tallinn:**
```
GET /api/delivery-fee/TALLINN/CAR
```

**Calculate current delivery fee for a bike in Tartu:**
```
GET /api/delivery-fee/TARTU/BIKE
```

**Calculate historical delivery fee (using ISO-8601 datetime format):**
```
GET /api/delivery-fee/TALLINN/SCOOTER/at?datetime=2024-03-15T12:00:00
```

**Important note about historical requests:** The system will find the most recent weather record that is before or equal to the requested time. So if you request `2024-03-15T12:30:00` and there's only weather data at `2024-03-15T12:00:00`, the system will use that earlier record for the calculation. It is also important to note that because the database is not live, it may not be populated enough to satisfy the historical request being made.

## Database

The application uses an H2 in-memory database for storing weather data:

- Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/weatherdb`
- Username: `sa`
- Password: `password`

## Business Rules

### Regional Base Fee (RBF)

|              | Car   | Scooter | Bike  |
|--------------|-------|---------|-------|
| **Tallinn**  | 4.00€ | 3.50€   | 3.00€ |
| **Tartu**    | 3.50€ | 3.00€   | 2.50€ |
| **Pärnu**    | 3.00€ | 2.50€   | 2.00€ |

### Extra Fees Based on Weather Conditions

#### Air Temperature (ATEF)
- Applies to: Scooter, Bike
- Temperature < -10°C: +1.00€
- Temperature between -10°C and 0°C: +0.50€

#### Wind Speed (WSEF)
- Applies to: Bike
- Wind speed between 10 m/s and 20 m/s: +0.50€
- Wind speed > 20 m/s: Usage forbidden

#### Weather Phenomenon (WPEF)
- Applies to: Scooter, Bike
- Snow or sleet: +1.00€
- Rain: +0.50€
- Glaze, hail, thunder: Usage forbidden

## Configuration

Key configuration properties in `application.properties`:

```properties
# Weather API URL
weather.api.url=https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php

# Cron expression for weather data fetch (every hour at 15 minutes past the hour)
weather.fetch.cron=0 15 * * * ?
```

## Testing

Run the tests with:
```
./mvnw test
```

The project includes:
- Unit tests for services and controllers
- Integration tests for the REST API

## Project Structure

```
src/
├── main/
│   ├── java/com/fujitsu/deliveryfeecalculator/
│   │   ├── config/           # Configuration classes
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Data transfer objects
│   │   ├── exception/        # Custom exceptions
│   │   ├── model/            # Domain model classes
│   │   │   ├── entity/       # JPA entities
│   │   │   ├── enums/        # Enumeration types
│   │   │   └── weather/      # Weather data response models
│   │   ├── repository/       # Data access layer
│   │   └── service/          # Business logic
│   └── resources/
│       └── application.properties # Application configuration
└── test/
    └── java/com/fujitsu/deliveryfeecalculator/
        ├── controller/       # Controller tests
        └── service/          # Service tests
```

## Future Enhancements

- CRUD operations for managing business rules through the API (bonus task 1)
- Caching for weather data to reduce external API calls
- Metrics and monitoring for the weather data fetching job
- Support for additional cities and vehicle types
- 
 © 2025 