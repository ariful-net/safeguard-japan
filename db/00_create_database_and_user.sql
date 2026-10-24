-- =========================================================
-- SafeGuard Japan  DB とアプリ用ユーザーの作成（管理者で実行）
-- アプリ用ユーザーは SELECT 権限のみ（詳細設計書 8.2 / 10.1）
-- =========================================================
CREATE DATABASE IF NOT EXISTS safeguard
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'safeguard_app'@'localhost' IDENTIFIED BY 'change-me';
GRANT SELECT ON safeguard.* TO 'safeguard_app'@'localhost';
FLUSH PRIVILEGES;
