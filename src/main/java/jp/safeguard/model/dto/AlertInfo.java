package jp.safeguard.model.dto;

/**
 * 警報表示データ（画面表示用の震度・地域・発表時刻）。
 *
 * @param status        OK / OFFLINE / NONE
 * @param level         danger（震度5弱以上）/ caution（震度3〜4）/ info / none
 * @param title         情報の種類（言語変換済み）
 * @param area          震源地域（言語変換済み）
 * @param intensity     震度の生値（"5-", "6+" など）
 * @param intensityText 震度の表示文言（言語変換済み）
 * @param magnitude     マグニチュード（不明なら null）
 * @param reportedAt    発表時刻 ISO-8601
 * @param stale         true なら取得失敗で直前の成功結果を表示している
 */
public record AlertInfo(
        String status,
        String level,
        String title,
        String area,
        String intensity,
        String intensityText,
        String magnitude,
        String reportedAt,
        boolean stale) {

    public static AlertInfo offline() {
        return new AlertInfo("OFFLINE", "none", null, null, null, null, null, null, true);
    }

    public static AlertInfo none() {
        return new AlertInfo("NONE", "none", null, null, null, null, null, null, false);
    }

    public AlertInfo asStale() {
        return new AlertInfo(status, level, title, area, intensity, intensityText, magnitude, reportedAt, true);
    }
}
