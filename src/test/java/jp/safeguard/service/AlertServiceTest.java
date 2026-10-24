package jp.safeguard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.safeguard.client.JmaClient;
import jp.safeguard.model.dto.AlertInfo;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AlertService の単体テスト（キャッシュは Spring 外なので効かない = 毎回 JmaClient を呼ぶ）。
 */
class AlertServiceTest {

    private final JmaClient client = mock(JmaClient.class);
    private final AlertService service = new AlertService(client, new AlertTermTable());

    private static JsonNode quake(String maxi) throws Exception {
        return new ObjectMapper().readTree("""
                {"ttl":"震源・震度情報","en_ttl":"Earthquake and Seismic Intensity Information",
                 "anm":"大阪府北部","en_anm":"Northern Osaka Prefecture",
                 "maxi":"%s","mag":"6.1","at":"2018-06-18T07:58:00+09:00"}
                """.formatted(maxi));
    }

    @Test
    void 震度と地域を選択言語に変換する() throws Exception {
        when(client.fetchLatestQuake()).thenReturn(Optional.of(quake("6-")));

        AlertInfo ja = service.getLatestAlert("ja");
        AlertInfo en = service.getLatestAlert("en");

        assertThat(ja.intensityText()).isEqualTo("震度6弱");
        assertThat(ja.area()).isEqualTo("大阪府北部");
        assertThat(ja.level()).isEqualTo("danger");
        assertThat(en.intensityText()).isEqualTo("Shindo 6 Lower");
        assertThat(en.area()).isEqualTo("Northern Osaka Prefecture");
        assertThat(en.stale()).isFalse();
    }

    @Test
    void 震度3は注意レベル() throws Exception {
        when(client.fetchLatestQuake()).thenReturn(Optional.of(quake("3")));

        assertThat(service.getLatestAlert("ja").level()).isEqualTo("caution");
    }

    @Test
    void 不正な言語コードはjaになる() throws Exception {
        when(client.fetchLatestQuake()).thenReturn(Optional.of(quake("4")));

        assertThat(service.getLatestAlert("xx").intensityText()).isEqualTo("震度4");
    }

    @Test
    void タイムアウト時は直前の成功結果を返す() throws Exception {
        when(client.fetchLatestQuake())
                .thenReturn(Optional.of(quake("5+")))
                .thenThrow(new ResourceAccessException("timeout"));

        service.getLatestAlert("ja");
        AlertInfo second = service.getLatestAlert("ja");

        assertThat(second.status()).isEqualTo("OK");
        assertThat(second.intensityText()).isEqualTo("震度5強");
        assertThat(second.stale()).isTrue();
    }

    @Test
    void 成功結果が無いままタイムアウトならオフライン() {
        when(client.fetchLatestQuake()).thenThrow(new ResourceAccessException("timeout"));

        assertThat(service.getLatestAlert("ja").status()).isEqualTo("OFFLINE");
    }

    @Test
    void 気象庁データが空ならNONE() {
        when(client.fetchLatestQuake()).thenReturn(Optional.empty());

        assertThat(service.getLatestAlert("ja").status()).isEqualTo("NONE");
    }
}
