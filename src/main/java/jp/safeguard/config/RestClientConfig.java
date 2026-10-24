package jp.safeguard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 外部連携用 RestClient。
 * 災害時に相手側が遅くなっても画面が固まらないよう、タイムアウトは3秒（詳細設計書 9.2）。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient externalRestClient(@Value("${app.http.timeout-ms:3000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "SafeGuardJapan/1.0 (school project)")
                .build();
    }
}
