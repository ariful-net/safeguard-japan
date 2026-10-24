package jp.safeguard.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 天気予報 API（Open-Meteo, APIキー不要）への HTTP アクセス。
 * 座標は呼び出し側で小数2桁（約1km）に丸めてから渡すこと。
 */
@Component
public class WeatherClient {

    private final RestClient restClient;
    private final String baseUrl;

    public WeatherClient(RestClient externalRestClient,
                         @Value("${app.weather.base-url}") String baseUrl) {
        this.restClient = externalRestClient;
        this.baseUrl = baseUrl;
    }

    public JsonNode fetchForecast(double lat, double lon) {
        return restClient.get()
                .uri(baseUrl + "?latitude={lat}&longitude={lon}"
                                + "&current=temperature_2m,weather_code,wind_speed_10m"
                                + "&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m"
                                + "&forecast_days=2&timezone={tz}&wind_speed_unit=ms",
                        lat, lon, "Asia/Tokyo")
                .retrieve()
                .body(JsonNode.class);
    }
}
