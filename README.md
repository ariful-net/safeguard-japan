# SafeGuard Japan（セーフガード・ジャパン）

留学生向けの多言語（日本語 / English / বাংলা / Tiếng Việt）防災 Web アプリ。
詳細設計書 v1.0 の「災害情報表示」「避難所検索」「安否メッセージ作成」に、
天気バー・防災の備え画面・ストーリー動画を追加している。

## 技術スタック
Java 17 / Spring Boot 3.3 / Thymeleaf / MyBatis / H2（既定）・MariaDB / Caffeine / RestClient /
素の JavaScript / Leaflet + OpenStreetMap / Service Worker

## 起動手順（かんたん: 組み込み H2）
DB の準備は不要。起動時に `db/01〜03` の SQL を組み込み H2（MariaDB 互換モード）へ読み込む。
```
./mvnw spring-boot:run
```
http://localhost:8080/ を開く。テストは `./mvnw test`
（JDK 25 では Mockito が動かないため `./mvnw test -DargLine=-Dnet.bytebuddy.experimental=true`、または JDK 17/21 を使う）。

## 公開（Render）
`Dockerfile` と `render.yaml` で Render の無料プランに置ける。
Render で「New → Blueprint」からこのリポジトリを選ぶと、`main` への push ごとに自動デプロイされる。
無料プランは 15 分アクセスがないとスリープし、次のアクセスで 30 秒〜1 分ほど起動を待つ。

## 起動手順（MariaDB を使う場合）
1. MariaDB で `db/` の SQL を番号順に実行（00 は管理者ユーザーで）
   ```
   mysql -u root -p < db/00_create_database_and_user.sql
   mysql -u root -p < db/01_schema.sql
   mysql -u root -p < db/02_data_shelters.sql
   mysql -u root -p < db/03_data_phrases.sql
   ```
2. DB パスワードを環境変数で渡して起動
   ```
   DB_USER=safeguard_app DB_PASSWORD=change-me ./mvnw spring-boot:run -Dspring-boot.run.profiles=mariadb
   ```
3. http://localhost:8080/ を開く（GPS・Service Worker は localhost か HTTPS でのみ動く）
4. テスト: `mvn test`（ShelterServiceTest / AlertServiceTest / ShelterSearchFormTest）

## 画面・API
| URL | 内容 |
|---|---|
| `/` | ホーム（ヒーロー・最新地震カード・今すぐやること・ストーリー動画・備え） |
| `/alert-info?type=earthquake\|tsunami\|flood` | 警報詳細と行動手順 |
| `/shelters` | 避難所マップ・徒歩ナビ（ルート表示・現在地追跡・到着判定） |
| `/prepare` | 備蓄カリキュレーター・チェックリスト・災害別の行動 |
| `/safety-status`, `/safety-status/sent` | 安否メッセージ作成（サーバーに送らない） |
| `/api/alert`, `/api/shelters`, `/api/shelters/{id}`, `/api/weather` | JSON API |

## 注意（提出前に必ず確認）
- **避難所データはサンプル。** 座標はおおよそ、対応災害フラグは未確認、収容人数は NULL。
  国土地理院「指定緊急避難場所データ」などの公式データに置き換えること。
- **ヒーロー写真**（阪神・淡路大震災の阪神高速）は提供された画像。著作権者と利用条件を確認し、クレジットを入れること。
  画像は 660×1000px なので大画面では少し粗い。
- 徒歩ルートは FOSSGIS の公開 OSRM サーバー（routing.openstreetmap.de）を使う。学習用途向けで、本番利用には向かない。
  取得できない場合は直線距離と方角で案内し、Google マップへのリンクも出す。
- 天気は Open-Meteo（無料・非商用）。正式な警報・注意報は気象庁。
- ストーリー動画（`static/video/evacuation-story.mp4`, 44秒）はコードで描いたアニメーション。
  字幕は `captions-*.vtt` で4言語。効果音も合成音（緊急地震速報の実際のチャイムは使っていない）。
