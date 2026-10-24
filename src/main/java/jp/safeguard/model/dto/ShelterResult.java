package jp.safeguard.model.dto;

/**
 * 避難所検索結果（距離・徒歩分数付き）。JSON でクライアントに返す。
 */
public record ShelterResult(
        Long id,
        String name,
        String nameJa,
        String address,
        double lat,
        double lon,
        int distanceM,
        int walkMinutes,
        Integer capacity,
        boolean earthquake,
        boolean tsunami,
        boolean flood) {
}
