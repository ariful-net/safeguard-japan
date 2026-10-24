package jp.safeguard.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 気象庁の用語 → 多言語用語の固定変換表（詳細設計書 5.2 AlertService）。
 * 機械翻訳は使わない。防災用語は誤訳が命に関わるため、ここで固定する。
 */
@Component
public class AlertTermTable {

    /** 震度の並び順（小さい順）。レベル判定に使う。 */
    private static final List<String> INTENSITY_ORDER = List.of("1", "2", "3", "4", "5-", "5+", "6-", "6+", "7");

    private static final Map<String, Map<String, String>> TITLES = Map.of(
            "震度速報", Map.of(
                    "en", "Seismic Intensity Information",
                    "bn", "ভূকম্পন তীব্রতার দ্রুত তথ্য",
                    "vi", "Thông tin nhanh về cường độ rung"),
            "震源に関する情報", Map.of(
                    "en", "Earthquake Epicenter Information",
                    "bn", "ভূমিকম্পের উৎসস্থল সম্পর্কিত তথ্য",
                    "vi", "Thông tin về tâm chấn"),
            "震源・震度情報", Map.of(
                    "en", "Earthquake and Seismic Intensity Information",
                    "bn", "ভূমিকম্পের উৎস ও তীব্রতার তথ্য",
                    "vi", "Thông tin tâm chấn và cường độ rung"),
            "遠地地震に関する情報", Map.of(
                    "en", "Distant Earthquake Information",
                    "bn", "দূরবর্তী ভূমিকম্পের তথ্য",
                    "vi", "Thông tin động đất ở xa"),
            "長周期地震動に関する観測情報", Map.of(
                    "en", "Long-period Ground Motion Information",
                    "bn", "দীর্ঘ-পর্যায়ের ভূকম্পন পর্যবেক্ষণ তথ্য",
                    "vi", "Thông tin dao động nền chu kỳ dài"));

    /** 震度の表示文言。 */
    public String intensityText(String maxi, String lang) {
        if (maxi == null || maxi.isBlank()) {
            return switch (lang) {
                case "en" -> "Intensity: not reported";
                case "bn" -> "তীব্রতা: জানানো হয়নি";
                case "vi" -> "Cường độ: chưa công bố";
                default -> "震度：発表なし";
            };
        }
        String num = maxi.substring(0, 1);
        boolean lower = maxi.endsWith("-");
        boolean upper = maxi.endsWith("+");
        return switch (lang) {
            case "en" -> "Shindo " + num + (lower ? " Lower" : upper ? " Upper" : "");
            case "bn" -> "শিন্দো " + toBengaliDigits(num) + (lower ? " (নিম্ন)" : upper ? " (উচ্চ)" : "");
            case "vi" -> "Shindo " + num + (lower ? " yếu" : upper ? " mạnh" : "");
            default -> "震度" + num + (lower ? "弱" : upper ? "強" : "");
        };
    }

    /** 情報の種類（タイトル）。変換表に無いものは英語版→日本語版の順に使う。 */
    public String title(String ttlJa, String ttlEn, String lang) {
        if ("ja".equals(lang)) {
            return ttlJa;
        }
        Map<String, String> row = TITLES.get(ttlJa);
        if (row != null && row.containsKey(lang)) {
            return row.get(lang);
        }
        return (ttlEn != null && !ttlEn.isBlank()) ? ttlEn : ttlJa;
    }

    /**
     * 警戒レベル。震度5弱以上 = danger、震度3〜4 = caution、それ以外 = info。
     */
    public String level(String maxi) {
        int idx = INTENSITY_ORDER.indexOf(maxi == null ? "" : maxi);
        if (idx < 0) {
            return "info";
        }
        if (idx >= INTENSITY_ORDER.indexOf("5-")) {
            return "danger";
        }
        if (idx >= INTENSITY_ORDER.indexOf("3")) {
            return "caution";
        }
        return "info";
    }

    private static String toBengaliDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(Character.isDigit(c) ? (char) ('০' + (c - '0')) : c);
        }
        return sb.toString();
    }
}
