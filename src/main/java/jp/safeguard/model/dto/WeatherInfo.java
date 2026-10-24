package jp.safeguard.model.dto;

import java.util.List;

/**
 * 画面最上部の天気バー用データ。
 *
 * @param status   OK / OFFLINE
 * @param current  現在の天気
 * @param hourly   今後12時間
 * @param warnings 注意喚起キー（heavyRain / strongWind / heat / cold）
 * @param updatedAt 取得時刻 ISO-8601
 */
public record WeatherInfo(
        String status,
        Hour current,
        List<Hour> hourly,
        List<String> warnings,
        String updatedAt) {

    /**
     * @param time        ISO-8601 (Asia/Tokyo)
     * @param tempC       気温
     * @param code        WMO 天気コード
     * @param precipProb  降水確率 %（current では null）
     * @param windMs      風速 m/s
     */
    public record Hour(String time, Double tempC, Integer code, Integer precipProb, Double windMs) {
    }

    public static WeatherInfo offline() {
        return new WeatherInfo("OFFLINE", null, List.of(), List.of(), null);
    }
}
