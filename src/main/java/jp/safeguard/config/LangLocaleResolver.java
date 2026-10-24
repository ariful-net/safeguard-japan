package jp.safeguard.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.safeguard.util.SupportedLang;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * 表示言語の決定。
 * 優先順位: ?lang= パラメータ → Cookie(sg_lang) → ja。
 * 言語の正本はブラウザの LocalStorage（JS が Cookie にも同じ値を書く）。
 * サーバーはユーザー情報を保持しない。
 */
public class LangLocaleResolver implements LocaleResolver {

    public static final String PARAM = "lang";
    public static final String COOKIE = "sg_lang";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String param = request.getParameter(PARAM);
        if (param != null && !param.isBlank()) {
            return SupportedLang.toLocale(param);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE.equals(c.getName())) {
                    return SupportedLang.toLocale(c.getValue());
                }
            }
        }
        return SupportedLang.toLocale(SupportedLang.DEFAULT);
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // 言語はクライアント側で保存するため、サーバーからは書き込まない。
    }
}
