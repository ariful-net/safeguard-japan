-- =========================================================
-- SafeGuard Japan  テーブル定義（MariaDB）
-- テーブル間の外部キーは持たない（詳細設計書 8.2）
-- =========================================================
USE safeguard;

DROP TABLE IF EXISTS shelters;
CREATE TABLE shelters (
  id            INT            NOT NULL AUTO_INCREMENT COMMENT '避難所ID',
  name_ja       VARCHAR(100)   NOT NULL                COMMENT '避難所名(日)',
  name_en       VARCHAR(100)       NULL                COMMENT '避難所名(英)',
  address       VARCHAR(255)   NOT NULL                COMMENT '住所',
  latitude      DECIMAL(10,7)  NOT NULL                COMMENT '緯度',
  longitude     DECIMAL(10,7)  NOT NULL                COMMENT '経度',
  is_earthquake TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '地震対応',
  is_tsunami    TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '津波対応',
  is_flood      TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '洪水対応',
  capacity      INT                NULL                COMMENT '収容可能人数',
  PRIMARY KEY (id),
  INDEX idx_shelters_lat_lon (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='避難所';

DROP TABLE IF EXISTS phrases;
CREATE TABLE phrases (
  id          INT          NOT NULL AUTO_INCREMENT COMMENT 'フレーズ識別ID',
  category    VARCHAR(50)  NOT NULL DEFAULT 'General'
              COMMENT 'ActionStep.earthquake / ActionStep.tsunami / ActionStep.flood / Safety / Medical / Food / General',
  text_ja     TEXT         NOT NULL COMMENT '日本語テキスト',
  text_en     TEXT         NOT NULL COMMENT '英語テキスト',
  text_bn     TEXT         NOT NULL COMMENT 'ベンガル語テキスト',
  text_vi     TEXT         NOT NULL COMMENT 'ベトナム語テキスト',
  sort_order  INT          NOT NULL DEFAULT 0 COMMENT '表示順（同一カテゴリ内・昇順）',
  PRIMARY KEY (id),
  INDEX idx_phrases_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='防災フレーズ';
