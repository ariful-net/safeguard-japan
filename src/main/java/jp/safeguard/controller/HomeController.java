package jp.safeguard.controller;

import jp.safeguard.util.SupportedLang;
import jp.safeguard.service.AlertService;
import jp.safeguard.service.PhraseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Locale;

/**
 * ホーム画面（警報カード・主要ボタン・ストーリー動画）。
 */
@Controller
public class HomeController {

    private final AlertService alertService;
    private final PhraseService phraseService;

    public HomeController(AlertService alertService, PhraseService phraseService) {
        this.alertService = alertService;
        this.phraseService = phraseService;
    }

    @GetMapping("/")
    public String index(Locale locale, Model model) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        model.addAttribute("alert", alertService.getLatestAlert(lang));
        model.addAttribute("steps", phraseService.actionSteps("earthquake", lang));
        return "home";
    }

    /** オフライン時に Service Worker が返す画面 */
    @GetMapping("/offline")
    public String offline() {
        return "offline";
    }
}
