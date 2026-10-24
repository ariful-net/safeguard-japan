package jp.safeguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import jp.safeguard.client.JmaClient;
import jp.safeguard.model.dto.AlertInfo;
import jp.safeguard.util.SupportedLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 警報サービス。
 * 処理順（詳細設計書 9.2）:
 * Cache にあれば返す → 無ければ JmaClient が取得（3秒）→ 用語変換 → 返却
 * → 失敗時は直前の成功結果、それも無ければ OFFLINE。
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final JmaClient jmaClient;
    private final AlertTermTable terms;

    /** 直前に取得に成功した気象庁データ（言語変換前）。 */
    private final AtomicReference<JsonNode> lastSuccess = new AtomicReference<>();

    public AlertService(JmaClient jmaClient, AlertTermTable terms) {
        this.jmaClient = jmaClient;
        this.terms = terms;
    }

    @Cacheable(cacheNames = "alerts", key = "T(jp.safeguard.util.SupportedLang).normalize(#p0)")
    public AlertInfo getLatestAlert(String lang) {
        String l = SupportedLang.normalize(lang);
        try {
            Optional<JsonNode> latest = jmaClient.fetchLatestQuake();
            if (latest.isEmpty()) {
                return AlertInfo.none();
            }
            lastSuccess.set(latest.get());
            return convert(latest.get(), l, false);
        } catch (RuntimeException e) {
            // 通信失敗・タイムアウト・JSON不正。詳細は残すが個人情報（位置）は扱っていない。
            log.warn("JMA fetch failed: {}", e.getClass().getSimpleName());
            JsonNode prev = lastSuccess.get();
            return prev != null ? convert(prev, l, true) : AlertInfo.offline();
        }
    }

    AlertInfo convert(JsonNode q, String lang, boolean stale) {
        String maxi = text(q, "maxi");
        String area = "ja".equals(lang) ? text(q, "anm") : firstNonBlank(text(q, "en_anm"), text(q, "anm"));
        String mag = text(q, "mag");
        if (mag != null && !mag.matches("^[0-9]+(\\.[0-9]+)?$")) {
            mag = null; // "Ｍ不明" などは表示しない
        }
        return new AlertInfo(
                "OK",
                terms.level(maxi),
                terms.title(text(q, "ttl"), text(q, "en_ttl"), lang),
                area,
                maxi,
                terms.intensityText(maxi, lang),
                mag,
                firstNonBlank(text(q, "at"), text(q, "rdt")),
                stale);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
