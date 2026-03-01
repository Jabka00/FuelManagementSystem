package org.example.fuelmanagement.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.example.fuelmanagement.model.RouteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleMapsService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleMapsService.class);

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final Pattern STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");

    public GoogleMapsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Properties config = loadConfig();
        this.apiKey = getEnvOrProperty(config, "GOOGLE_MAPS_API_KEY", "google.maps.api.key", "");
        this.baseUrl = getEnvOrProperty(config, "GOOGLE_MAPS_API_URL", "google.maps.api.url", "https://maps.googleapis.com/maps/api");

        if (apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.warn(" Google Maps API ключ не настроен! Пожалуйста, добавьте ваш API ключ в .env файл");
        } else {
            logger.info(" Google Maps Service инициализирован");
        }
    }

    private static String getEnvOrProperty(Properties config, String envKey, String propertyKey, String defaultValue) {
        String envValue = dotenv.get(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return config.getProperty(propertyKey, defaultValue);
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            logger.error("Ошибка загрузки конфигурации: ", e);
        }
        return props;
    }

    public CompletableFuture<RouteInfo> calculateRouteAsync(String startAddress, String endAddress) {
        return CompletableFuture.supplyAsync(() -> calculateRoute(startAddress, endAddress));
    }

    public CompletableFuture<RouteInfo> calculateRouteWithWaypointsAsync(String startAddress, String endAddress, java.util.List<String> waypoints) {
        return CompletableFuture.supplyAsync(() -> calculateRouteWithWaypoints(startAddress, endAddress, waypoints));
    }

    public RouteInfo calculateRouteWithWaypoints(String startAddress, String endAddress, java.util.List<String> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return calculateRoute(startAddress, endAddress);
        }

        if (apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.warn("API ключ не настроен, возвращаем пустой результат");
            return createEmptyRoute("API ключ не настроен");
        }

        if (startAddress == null || startAddress.trim().isEmpty() ||
                endAddress == null || endAddress.trim().isEmpty()) {
            logger.warn("Пустые адреса");
            return createEmptyRoute("Пустые адреса");
        }

        try {
            logger.info(" Расчет маршрута с {} проміжними точками: {} → {} → {}", 
                waypoints.size(), startAddress, waypoints.size(), endAddress);

            String url = buildDirectionsUrlWithWaypoints(startAddress, endAddress, waypoints);
            logger.debug("API URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Ошибка API запроса: HTTP {}", response.statusCode());
                return createEmptyRoute("HTTP ошибка: " + response.statusCode());
            }

            return parseDirectionsResponse(response.body());

        } catch (Exception e) {
            logger.error("Ошибка расчета маршрута с проміжними точками: ", e);
            return createEmptyRoute("Ошибка: " + e.getMessage());
        }
    }

    public RouteInfo calculateRoute(String startAddress, String endAddress) {
        if (apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.warn("API ключ не настроен, возвращаем пустой результат");
            return createEmptyRoute("API ключ не настроен");
        }

        if (startAddress == null || startAddress.trim().isEmpty() ||
                endAddress == null || endAddress.trim().isEmpty()) {
            logger.warn("Пустые адреса для расчета маршрута");
            return createEmptyRoute("Пустые адреса");
        }

        try {
            logger.info(" Расчет маршрута: {} → {}", startAddress, endAddress);

            String url = buildDirectionsUrl(startAddress, endAddress);
            logger.debug("API URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Ошибка API запроса: HTTP {}", response.statusCode());
                return createEmptyRoute("HTTP ошибка: " + response.statusCode());
            }

            return parseDirectionsResponse(response.body());

        } catch (Exception e) {
            logger.error("Ошибка расчета маршрута: ", e);
            return createEmptyRoute("Ошибка: " + e.getMessage());
        }
    }

    private String buildDirectionsUrl(String origin, String destination) {
        try {
            String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
            String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8);

            return String.format("%s/directions/json?origin=%s&destination=%s&key=%s&language=uk&region=ua&units=metric",
                    baseUrl, encodedOrigin, encodedDestination, apiKey);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка кодирования URL", e);
        }
    }

    private String buildDirectionsUrlWithWaypoints(String origin, String destination, java.util.List<String> waypoints) {
        try {
            String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
            String encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8);

            StringBuilder waypointsParam = new StringBuilder();
            if (waypoints != null && !waypoints.isEmpty()) {
                waypointsParam.append("&waypoints=");
                for (int i = 0; i < waypoints.size(); i++) {
                    if (i > 0) {
                        waypointsParam.append("%7C"); 
                    }
                    waypointsParam.append(URLEncoder.encode(waypoints.get(i), StandardCharsets.UTF_8));
                }
            }

            return String.format("%s/directions/json?origin=%s&destination=%s%s&key=%s&language=uk&region=ua&units=metric",
                    baseUrl, encodedOrigin, encodedDestination, waypointsParam.toString(), apiKey);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка кодирования URL", e);
        }
    }

    private RouteInfo parseDirectionsResponse(String jsonResponse) {
        try {
            logger.debug("Парсинг ответа Google Maps API");

            Matcher statusMatcher = STATUS_PATTERN.matcher(jsonResponse);
            if (statusMatcher.find()) {
                String status = statusMatcher.group(1);
                if (!"OK".equals(status)) {
                    logger.error("Google Maps API вернул статус: {}", status);
                    return createEmptyRoute("API статус: " + status);
                }
            }

            Pattern legInfoPattern = Pattern.compile(
                "\"distance\"\\s*:\\s*\\{[^}]*?\"value\"\\s*:\\s*(\\d+)[^}]*?\\}" + 
                "\\s*,\\s*" +                                                          
                "\"duration\"\\s*:\\s*\\{[^}]*?\"value\"\\s*:\\s*(\\d+)[^}]*?\\}" + 
                "\\s*,\\s*" +                                                          
                "\"end_address\"",                                                    
                Pattern.DOTALL
            );
            Matcher legMatcher = legInfoPattern.matcher(jsonResponse);
            BigDecimal totalDistanceKm = BigDecimal.ZERO;
            long totalDistanceMeters = 0;
            long totalDurationSeconds = 0;
            int legCount = 0;
            logger.debug(" Пошук legs в JSON відповіді...");
            while (legMatcher.find()) {
                long distanceMeters = Long.parseLong(legMatcher.group(1));
                long durationSeconds = Long.parseLong(legMatcher.group(2));
                totalDistanceMeters += distanceMeters;
                totalDurationSeconds += durationSeconds;
                legCount++;
                BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
                logger.info("   Leg {}: {} м ({} км)", legCount, distanceMeters, distanceKm);
            }
            if (legCount == 0) {
                logger.warn(" Не знайдено жодного leg! Перевірте формат відповіді API");
                logger.debug("Фрагмент відповіді: {}", jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
            }
            if (totalDistanceMeters > 0) {
                totalDistanceKm = BigDecimal.valueOf(totalDistanceMeters)
                        .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            }
            Duration estimatedDuration = totalDurationSeconds > 0 
                ? Duration.ofSeconds(totalDurationSeconds) 
                : Duration.ZERO;

            RouteInfo routeInfo = new RouteInfo(totalDistanceKm, estimatedDuration);

            BigDecimal cityPercent;
            BigDecimal highwayPercent;
            double avgSpeedKmH = 0;
            if (totalDurationSeconds > 0 && totalDistanceKm.compareTo(BigDecimal.ZERO) > 0) {
                avgSpeedKmH = totalDistanceKm.doubleValue() / (totalDurationSeconds / 3600.0);
            }
            logger.debug("Середня швидкість маршруту: {} км/год", String.format("%.1f", avgSpeedKmH));
            if (totalDistanceKm.compareTo(BigDecimal.valueOf(10)) <= 0) {
                cityPercent = BigDecimal.valueOf(0.90);
                highwayPercent = BigDecimal.valueOf(0.10);
                logger.debug("Короткий маршрут: 90% місто, 10% траса");
            } else if (totalDistanceKm.compareTo(BigDecimal.valueOf(30)) <= 0) {
                if (avgSpeedKmH >= 50) {
                    cityPercent = BigDecimal.valueOf(0.40);
                    highwayPercent = BigDecimal.valueOf(0.60);
                    logger.debug("Середній маршрут з високою швидкістю: 40% місто, 60% траса");
                } else {
                    cityPercent = BigDecimal.valueOf(0.70);
                    highwayPercent = BigDecimal.valueOf(0.30);
                    logger.debug("Середній маршрут з низькою швидкістю: 70% місто, 30% траса");
                }
            } else if (totalDistanceKm.compareTo(BigDecimal.valueOf(100)) <= 0) {
                if (avgSpeedKmH >= 70) {
                    cityPercent = BigDecimal.valueOf(0.20);
                    highwayPercent = BigDecimal.valueOf(0.80);
                    logger.debug("Довгий маршрут з дуже високою швидкістю: 20% місто, 80% траса");
                } else if (avgSpeedKmH >= 50) {
                    cityPercent = BigDecimal.valueOf(0.30);
                    highwayPercent = BigDecimal.valueOf(0.70);
                    logger.debug("Довгий маршрут з високою швидкістю: 30% місто, 70% траса");
                } else {
                    cityPercent = BigDecimal.valueOf(0.50);
                    highwayPercent = BigDecimal.valueOf(0.50);
                    logger.debug("Довгий маршрут з середньою швидкістю: 50% місто, 50% траса");
                }
            } else {
                if (avgSpeedKmH >= 80) {
                    cityPercent = BigDecimal.valueOf(0.10);
                    highwayPercent = BigDecimal.valueOf(0.90);
                    logger.debug("Дуже довгий маршрут з високою швидкістю: 10% місто, 90% траса");
                } else if (avgSpeedKmH >= 60) {
                    cityPercent = BigDecimal.valueOf(0.20);
                    highwayPercent = BigDecimal.valueOf(0.80);
                    logger.debug("Дуже довгий маршрут з середньою швидкістю: 20% місто, 80% траса");
                } else {
                    cityPercent = BigDecimal.valueOf(0.30);
                    highwayPercent = BigDecimal.valueOf(0.70);
                    logger.debug("Дуже довгий маршрут з низькою швидкістю: 30% місто, 70% траса");
                }
            }
            routeInfo.setCityDistance(totalDistanceKm.multiply(cityPercent).setScale(2, RoundingMode.HALF_UP));
            routeInfo.setHighwayDistance(totalDistanceKm.multiply(highwayPercent).setScale(2, RoundingMode.HALF_UP));
            logger.info(" Розподіл маршруту: Місто {} км ({}%), Траса {} км ({}%)",
                    routeInfo.getCityDistance(), 
                    cityPercent.multiply(BigDecimal.valueOf(100)).intValue(),
                    routeInfo.getHighwayDistance(),
                    highwayPercent.multiply(BigDecimal.valueOf(100)).intValue());

            routeInfo.setRouteDescription(String.format("Дистанция: %s, Время: %s",
                    routeInfo.getFormattedDistance(), routeInfo.getFormattedDuration()));

            if (legCount > 1) {
                logger.info(" Маршрут з {} сегментами (legs) розраховано: {} км за {}", legCount, totalDistanceKm, routeInfo.getFormattedDuration());
            } else {
                logger.info(" Маршрут розраховано: {} км за {}", totalDistanceKm, routeInfo.getFormattedDuration());
            }

            return routeInfo;

        } catch (Exception e) {
            logger.error("Ошибка парсинга ответа Google Maps: ", e);
            logger.debug("Ответ API: {}", jsonResponse.substring(0, Math.min(jsonResponse.length(), 500)));
            return createEmptyRoute("Ошибка парсинга: " + e.getMessage());
        }
    }

    private RouteInfo createEmptyRoute(String error) {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setRouteDescription("Не удалось рассчитать: " + error);
        return routeInfo;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty() && !"YOUR_API_KEY_HERE".equals(apiKey);
    }

    public String getConfigurationStatus() {
        if (!isConfigured()) {
            return " Google Maps API не настроен";
        }
        return " Google Maps API готов к использованию";
    }
    public boolean validateAddress(String address) {
        if (!isConfigured()) {
            logger.warn("Google Maps API не настроен, пропускаем валидацию адреса");
            return true; 
        }
        if (address == null || address.trim().isEmpty()) {
            logger.warn("Пуста адреса для валідації");
            return false;
        }
        try {
            String trimmedAddress = address.trim();
            String encodedAddress = URLEncoder.encode(trimmedAddress, StandardCharsets.UTF_8);
            String url = String.format(
                "%s/geocode/json?address=%s&components=country:UA&language=uk&region=ua&key=%s",
                baseUrl, encodedAddress, apiKey);
            logger.debug(" Валідація адреси: '{}'", trimmedAddress);
            logger.debug("📡 URL запиту: {}", url.replace(apiKey, "***"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() != 200) {
                logger.error(" Помилка валідації адреси: HTTP {}", response.statusCode());
                logger.debug("Відповідь сервера: {}", responseBody);
                return true; 
            }
            logger.debug("📨 Відповідь API (перші 500 символів): {}", 
                responseBody.substring(0, Math.min(500, responseBody.length())));
            Matcher statusMatcher = STATUS_PATTERN.matcher(responseBody);
            if (statusMatcher.find()) {
                String status = statusMatcher.group(1);
                logger.info(" Статус API: {}", status);
                switch (status) {
                    case "OK":
                        logger.info(" Адреса знайдена та валідна: '{}'", trimmedAddress);
                        Pattern formattedAddressPattern = Pattern.compile("\"formatted_address\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher addressMatcher = formattedAddressPattern.matcher(responseBody);
                        if (addressMatcher.find()) {
                            String formattedAddress = addressMatcher.group(1);
                            logger.info(" Знайдена адреса: '{}'", formattedAddress);
                        }
                        return true;
                    case "ZERO_RESULTS":
                        logger.warn(" Адреса не знайдена (ZERO_RESULTS): '{}'", trimmedAddress);
                        logger.info(" Підказка: спробуйте ввести адресу в форматі 'вулиця, будинок, місто' або 'місто, вулиця, будинок'");
                        return false;
                    case "OVER_QUERY_LIMIT":
                        logger.error(" Перевищено ліміт запитів Google Maps API");
                        logger.info(" Спробуйте пізніше або перевірте квоти API ключа");
                        return true; 
                    case "REQUEST_DENIED":
                        logger.error(" Запит відхилено Google Maps API");
                        logger.error(" Можливі причини:");
                        logger.error("   1. API ключ неправильний або відсутній");
                        logger.error("   2. Geocoding API не увімкнено в Google Cloud Console");
                        logger.error("   3. Обмеження по IP адресі або реферрерам");
                        Pattern errorMessagePattern = Pattern.compile("\"error_message\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher errorMatcher = errorMessagePattern.matcher(responseBody);
                        if (errorMatcher.find()) {
                            String errorMessage = errorMatcher.group(1);
                            logger.error("   Повідомлення від API: {}", errorMessage);
                        }
                        return true; 
                    case "INVALID_REQUEST":
                        logger.warn(" Невалідний запит для адреси: '{}'", trimmedAddress);
                        logger.info(" Перевірте формат адреси");
                        return false;
                    case "UNKNOWN_ERROR":
                        logger.error(" Невідома помилка від Google Maps API");
                        logger.info(" Спробуйте повторити запит пізніше");
                        return true; 
                    default:
                        logger.warn(" Невідомий статус API: {} для адреси '{}'", status, trimmedAddress);
                        logger.debug("Повна відповідь: {}", responseBody);
                        return true; 
                }
            }
            logger.warn(" Не вдалося розпарсити відповідь API для валідації адреси");
            logger.debug("Повна відповідь: {}", responseBody);
            return true; 
        } catch (Exception e) {
            logger.error(" Помилка під час валідації адреси '{}': ", address, e);
            return true; 
        }
    }
    public CompletableFuture<Boolean> validateAddressAsync(String address) {
        return CompletableFuture.supplyAsync(() -> validateAddress(address));
    }
    public String generateGoogleMapsUrl(String startAddress, String endAddress, java.util.List<String> waypoints, boolean isRoundTrip) {
        try {
            StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/");
            if (startAddress != null && !startAddress.trim().isEmpty()) {
                url.append(URLEncoder.encode(startAddress.trim(), StandardCharsets.UTF_8));
            }
            if (waypoints != null && !waypoints.isEmpty()) {
                for (String waypoint : waypoints) {
                    if (waypoint != null && !waypoint.trim().isEmpty()) {
                        url.append("/").append(URLEncoder.encode(waypoint.trim(), StandardCharsets.UTF_8));
                    }
                }
            }
            if (endAddress != null && !endAddress.trim().isEmpty()) {
                url.append("/").append(URLEncoder.encode(endAddress.trim(), StandardCharsets.UTF_8));
            }
            if (isRoundTrip && startAddress != null && !startAddress.trim().isEmpty()) {
                url.append("/").append(URLEncoder.encode(startAddress.trim(), StandardCharsets.UTF_8));
            }
            url.append("?hl=uk&gl=ua");
            String generatedUrl = url.toString();
            logger.info(" Згенеровано посилання Google Maps: {}", generatedUrl);
            return generatedUrl;
        } catch (Exception e) {
            logger.error(" Помилка генерації посилання Google Maps: ", e);
            return "https://www.google.com/maps";
        }
    }
    public String generateGoogleMapsUrl(String startAddress, String endAddress) {
        return generateGoogleMapsUrl(startAddress, endAddress, null, false);
    }
    public String generateGoogleMapsUrl(String startAddress, String endAddress, java.util.List<String> waypoints) {
        return generateGoogleMapsUrl(startAddress, endAddress, waypoints, false);
    }
}