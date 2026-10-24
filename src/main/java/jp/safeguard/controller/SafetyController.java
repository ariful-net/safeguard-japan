package jp.safeguard.controller;

import jp.safeguard.util.SupportedLang;
import jp.safeguard.service.PhraseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Locale;

/**
 * 安否確認コントローラー。
 * メッセージはブラウザ内で組み立てて OS の共有機能で送る。サーバーには送信しない（詳細設計書 9.3）。
 */
@Controller
public class SafetyController {

    private final PhraseService phraseService;

    public SafetyController(PhraseService phraseService) {
        this.phraseService = phraseService;
    }

    @GetMapping("/safety-status")
    public String form(Locale locale, Model model) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        model.addAttribute("templates", phraseService.findByCategory(PhraseService.SAFETY, lang));
        return "safety/form";
    }

    @GetMapping("/safety-status/sent")
    public String sent() {
        return "safety/sent";
    }
}
