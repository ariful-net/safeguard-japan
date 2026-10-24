package jp.safeguard.controller;

import jp.safeguard.util.SupportedLang;
import jakarta.validation.Valid;
import jp.safeguard.model.dto.ShelterResult;
import jp.safeguard.model.entity.Shelter;
import jp.safeguard.model.form.ShelterSearchForm;
import jp.safeguard.service.ShelterService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * 避難所コントローラー。避難所マップ画面と周辺避難所検索 API。
 * Controller は Service だけに依存する（詳細設計書 5.3）。
 */
@Controller
public class ShelterController {

    private final ShelterService shelterService;
    private final MessageSource messageSource;

    public ShelterController(ShelterService shelterService, MessageSource messageSource) {
        this.shelterService = shelterService;
        this.messageSource = messageSource;
    }

    @GetMapping("/shelters")
    public String index() {
        return "shelters/map";
    }

    /**
     * 検証エラーがあれば 400 とエラーメッセージ、なければ距離順の結果を返す。
     * 位置情報はこのリクエストの間だけ使い、保存もログ出力もしない。
     */
    @GetMapping("/api/shelters")
    @ResponseBody
    public ResponseEntity<?> search(@Valid @ModelAttribute("searchForm") ShelterSearchForm form,
                                    BindingResult result,
                                    Locale locale) {
        String lang = SupportedLang.normalize(locale.getLanguage());
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(fe -> toMessage(fe, locale))
                    .distinct()
                    .toList();
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }
        List<ShelterResult> shelters = shelterService.findNearby(
                form.getLat(), form.getLon(), form.radiusOrDefault(), form.typeOrNull(), lang);
        return ResponseEntity.ok(Map.of("count", shelters.size(), "shelters", shelters));
    }

    @GetMapping("/api/shelters/{id}")
    @ResponseBody
    public Shelter detail(@PathVariable("id") Long id) {
        return shelterService.findById(id);
    }

    /**
     * 型変換エラー（lat=abc など）は Spring の技術的な英文になるため、
     * 項目ごとの業務メッセージに置き換える。
     */
    private String toMessage(FieldError fe, Locale locale) {
        if (!fe.isBindingFailure()) {
            return fe.getDefaultMessage();
        }
        String key = switch (fe.getField()) {
            case "radius" -> "validation.radius";
            case "type" -> "validation.type";
            default -> "validation.location";
        };
        return messageSource.getMessage(key, null, locale);
    }
}
