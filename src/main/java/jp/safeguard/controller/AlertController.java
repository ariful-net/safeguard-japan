package jp.safeguard.controller;

import jp.safeguard.util.SupportedLang;
import jp.safeguard.model.dto.AlertInfo;
import jp.safeguard.service.AlertService;
import jp.safeguard.service.PhraseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Locale;

/**
 * 警報コントローラー。警報詳細画面と、カード自動更新用 JSON。
 */
@Controller
public class AlertController {

    private final AlertService alertService;
    private final PhraseService phraseService;

    public AlertController(AlertService alertService, PhraseService phraseService) {
        this.alertService = alertService;
        this.phraseService = phraseService;
    }

    @GetMapping("/alert-info")
    public String detail(Locale locale,
                         @RequestParam(name = "type", required = false) String type,
                         Model model) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        String t = PhraseService.normalizeType(type);
        model.addAttribute("alert", alertService.getLatestAlert(lang));
        model.addAttribute("type", t);
        model.addAttribute("steps", phraseService.actionSteps(t, lang));
        return "alert/detail";
    }

    @GetMapping("/api/alert")
    @ResponseBody
    public AlertInfo latest(Locale locale) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        return alertService.getLatestAlert(lang);
    }
}
