package jp.safeguard.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * 気象庁の公開 JSON（地震情報一覧）への HTTP アクセス。
 * 業務ロジック（用語変換・キャッシュ）は持たない。外部連携専用。
 */
@Component
public class JmaClient {

    private final RestClient restClient;
    private final String quakeListUrl;

    public JmaClient(RestClient externalRestClient,
                     @Value("${app.jma.quake-list-url}") String quakeListUrl) {
        this.restClient = externalRestClient;
        this.quakeListUrl = quakeListUrl;
    }

    /**
     * 最新の地震情報1件を返す。一覧が空なら Optional.empty()。
     *
     * @throws RestClientException 通信失敗・タイムアウト（3秒）時
     */
    public Optional<JsonNode> fetchLatestQuake() {
        JsonNode list = restClient.get()
                .uri(quakeListUrl)
                .retrieve()
                .body(JsonNode.class);
        if (list == null || !list.isArray() || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }
}
