package jp.safeguard.controller;

import jp.safeguard.model.dto.WeatherInfo;
import jp.safeguard.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 画面最上部の天気バー用 API。
 * ※ 詳細設計書 v1.0 に未記載の追加 API。設計書の 6.1 に追記が必要。
 */
@RestController
public class WeatherController {

    private final WeatherService weatherService;
    private final double defaultLat;
    private final double defaultLon;

    public WeatherController(WeatherService weatherService,
                             @Value("${app.default-location.lat}") double defaultLat,
                             @Value("${app.default-location.lon}") double defaultLon) {
        this.weatherService = weatherService;
        this.defaultLat = defaultLat;
        this.defaultLon = defaultLon;
    }

    /** 範囲外・未指定の座標はデフォルト位置（大阪駅）で検索する。 */
    @GetMapping("/api/weather")
    public WeatherInfo weather(@RequestParam(name = "lat", required = false) Double lat,
                               @RequestParam(name = "lon", required = false) Double lon) {
        boolean valid = lat != null && lon != null
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
        double la = valid ? lat : defaultLat;
        double lo = valid ? lon : defaultLon;
        return weatherService.getForecast(WeatherService.round2(la), WeatherService.round2(lo));
    }
}
