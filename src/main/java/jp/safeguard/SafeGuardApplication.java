package jp.safeguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * SafeGuard Japan 起動クラス。
 * 会員登録・ログインは持たない（詳細設計書 2.1）。
 */
@SpringBootApplication
@EnableCaching
public class SafeGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(SafeGuardApplication.class, args);
    }
}
