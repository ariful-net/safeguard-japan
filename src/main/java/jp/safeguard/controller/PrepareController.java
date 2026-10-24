package jp.safeguard.controller;

import jp.safeguard.util.SupportedLang;
import jp.safeguard.service.PhraseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Locale;

/**
 * 防災の備え画面（備蓄・非常持ち出し・救急用品・災害別の行動）。
 * ※ 詳細設計書 v1.0 に未記載の追加画面。設計書の 6.1 に追記が必要。
 */
@Controller
public class PrepareController {

    private final PhraseService phraseService;

    public PrepareController(PhraseService phraseService) {
        this.phraseService = phraseService;
    }

    @GetMapping("/prepare")
    public String index(Locale locale, Model model) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        model.addAttribute("food", phraseService.findByCategory(PhraseService.FOOD, lang));
        model.addAttribute("general", phraseService.findByCategory(PhraseService.GENERAL, lang));
        model.addAttribute("medical", phraseService.findByCategory(PhraseService.MEDICAL, lang));
        model.addAttribute("stepsEarthquake", phraseService.actionSteps("earthquake", lang));
        model.addAttribute("stepsTsunami", phraseService.actionSteps("tsunami", lang));
        model.addAttribute("stepsFlood", phraseService.actionSteps("flood", lang));
        return "prepare";
    }
}
