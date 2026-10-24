package jp.safeguard.util;

/**
 * 2点間距離（ハバーサイン式）と検索用の緯度経度範囲を計算する。
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000d;
    /** 緯度1度あたりの距離（km）の近似値 */
    private static final double KM_PER_DEGREE_LAT = 111.32d;

    private GeoUtils() {
    }

    /** 2点間の距離をメートルで返す。 */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 半径 km を緯度差（度）に変換する。 */
    public static double latDelta(double radiusKm) {
        return radiusKm / KM_PER_DEGREE_LAT;
    }

    /** 半径 km を経度差（度）に変換する。高緯度で 0 除算にならないよう下限を設ける。 */
    public static double lonDelta(double radiusKm, double lat) {
        double cos = Math.max(Math.cos(Math.toRadians(lat)), 0.01);
        return radiusKm / (KM_PER_DEGREE_LAT * cos);
    }
}
