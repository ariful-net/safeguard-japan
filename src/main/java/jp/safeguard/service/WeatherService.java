package jp.safeguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import jp.safeguard.client.WeatherClient;
import jp.safeguard.model.dto.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 天気サービス（画面最上部の「これから12時間」バー用）。
 * 座標は約1km単位に丸めた値だけを受け取り、ログにも残さない。
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    static final int HOURS = 12;

    private final WeatherClient weatherClient;

    public WeatherService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    /** 座標を小数2桁に丸める（キャッシュ効率とプライバシーのため）。 */
    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    @Cacheable(cacheNames = "weather", key = "#p0 + ',' + #p1")
    public WeatherInfo getForecast(double roundedLat, double roundedLon) {
        try {
            JsonNode root = weatherClient.fetchForecast(roundedLat, roundedLon);
            return parse(root, LocalDateTime.now(JST));
        } catch (RuntimeException e) {
            log.warn("Weather fetch failed: {}", e.getClass().getSimpleName());
            return WeatherInfo.offline();
        }
    }

    WeatherInfo parse(JsonNode root, LocalDateTime now) {
        if (root == null) {
            return WeatherInfo.offline();
        }
        JsonNode cur = root.path("current");
        WeatherInfo.Hour current = new WeatherInfo.Hour(
                cur.path("time").asText(null),
                num(cur.path("temperature_2m")),
                intOrNull(cur.path("weather_code")),
                null,
                num(cur.path("wind_speed_10m")));

        JsonNode h = root.path("hourly");
        JsonNode times = h.path("time");
        LocalDateTime from = now.truncatedTo(ChronoUnit.HOURS);
        List<WeatherInfo.Hour> hours = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();

        for (int i = 0; i < times.size() && hours.size() < HOURS; i++) {
            LocalDateTime t = LocalDateTime.parse(times.get(i).asText());
            if (t.isBefore(from)) {
                continue;
            }
            WeatherInfo.Hour hour = new WeatherInfo.Hour(
                    times.get(i).asText(),
                    num(h.path("temperature_2m").path(i)),
                    intOrNull(h.path("weather_code").path(i)),
                    intOrNull(h.path("precipitation_probability").path(i)),
                    num(h.path("wind_speed_10m").path(i)));
            hours.add(hour);
            collectWarnings(hour, warnings);
        }
        return new WeatherInfo("OK", current, hours, List.copyOf(warnings),
                OffsetDateTime.now(JST).truncatedTo(ChronoUnit.MINUTES).toString());
    }

    /** 注意喚起の判定（目安。気象庁の警報・注意報の代わりではない）。 */
    static void collectWarnings(WeatherInfo.Hour h, Set<String> out) {
        if (h.code() != null && h.code() >= 95) {
            out.add("thunder");
        }
        if (h.precipProb() != null && h.precipProb() >= 70
                || h.code() != null && (h.code() == 65 || h.code() == 82)) {
            out.add("heavyRain");
        }
        if (h.windMs() != null && h.windMs() >= 10.0) {
            out.add("strongWind");
        }
        if (h.tempC() != null && h.tempC() >= 31.0) {
            out.add("heat");
        }
        if (h.tempC() != null && h.tempC() <= 0.0) {
            out.add("cold");
        }
    }

    private static Double num(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asDouble();
    }

    private static Integer intOrNull(JsonNode n) {
        return (n == null || n.isMissingNode() || n.isNull()) ? null : n.asInt();
    }
}
