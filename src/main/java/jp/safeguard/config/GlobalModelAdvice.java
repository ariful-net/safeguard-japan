package jp.safeguard.config;

import jp.safeguard.util.SupportedLang;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 全画面共通のモデル属性（現在の言語コード・言語一覧）。
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAdvice {

    @ModelAttribute("lang")
    public String lang() {
        return SupportedLang.normalize(LocaleContextHolder.getLocale().getLanguage());
    }

    @ModelAttribute("langs")
    public List<String> langs() {
        return SupportedLang.CODES;
    }
}
