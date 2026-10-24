package jp.safeguard.util;

import java.util.List;
import java.util.Locale;

/**
 * 対応言語。不正値は ja に置換する（エラーにしない / 詳細設計書 7章）。
 */
public final class SupportedLang {

    public static final String DEFAULT = "ja";
    public static final List<String> CODES = List.of("ja", "en", "bn", "vi");

    private SupportedLang() {
    }

    /** 言語コードを正規化する。null・空・未対応はすべて ja。 */
    public static String normalize(String code) {
        if (code == null) {
            return DEFAULT;
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        if (c.length() > 2) {
            c = c.substring(0, 2); // "en-US" -> "en"
        }
        return CODES.contains(c) ? c : DEFAULT;
    }

    public static Locale toLocale(String code) {
        return Locale.forLanguageTag(normalize(code));
    }
}
