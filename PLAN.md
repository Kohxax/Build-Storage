# BuildAssist - 統合倉庫システム 実装計画

## 概要

Minecraft建築支援ツール。  
Spigot Plugin + Fabric Mod の2プロジェクト構成で、MMOスタイルの統合倉庫システムを実現する。

---

## コアコンセプト

- **仮想倉庫**: アイテムはサーバーのSQLite DBでプレイヤーUUID単位に管理。物理チェストとは無関係。
- **インベントリキーに統合**: `E`キーでバニラインベントリを開くと同時に倉庫パネルも表示。
- **クリエイティブUIそのまま**: バニラのクリエイティブインベントリと同じタブ構成・レイアウトを使用。
- **無制限容量**: スロット数制限なし。大量アイテムは `1k` / `1.2m` で省略表示。

---

## 倉庫の開き方

`E`キー（バニラのインベントリキー）を押すと、バニラインベントリと倉庫パネルが同時に開く。  
倉庫パネルの表示位置・バニラインベントリの位置は両方とも画面上で調整可能。

```
          [上]
           ↑
[左] ← [インベントリ] → [右]  ← 倉庫パネルをどの方向に出すか選べる
           ↓
          [下]
```

### 位置設定UI（設定画面）

- 倉庫パネルをインベントリの上下左右どこに出すか選択
- バニラインベントリ自体の画面上の位置も調整可能（ドラッグ or 座標入力）
- 倉庫パネル自体の位置も同様に調整可能
- 設定はローカルのconfig fileに保存（サーバー非依存）

### レイアウト例（倉庫パネルを右に設定した場合）

```
┌────────────────────┐ ┌──────────────────────────────────────┐
│  バニラインベントリ  │ │  統合倉庫  [🔍 検索...]              │
│  （通常通り）        │ │                                      │
│                    │ │ [-建築ブロック-][-装飾-][-道具-]...   │
│  [クラフト]        │ │  🪨   🧱   🪵   ░░░  ░░░  ░░░      │
│  [装備スロット]    │ │ 3420  1.2k  840  (グレーアウト)       │
│                    │ │  ...（スクロール可能）                 │
│  [インベントリ行]  │ │                                      │
└────────────────────┘ └──────────────────────────────────────┘
```

---

## UI仕様（倉庫パネル）

バニラのクリエイティブインベントリ画面をベースに倉庫パネルを構成する。

- **タブ構成**: バニラのクリエイティブタブをそのまま使用（建築ブロック / 装飾 / レッドストーン 等）
- **所持あり**: 通常表示 + 個数オーバーレイ（右下に `3420` / `1.2k` 等）
- **所持なし**: グレースケール表示、クリック不可
- **検索**: 検索バーでアイテム名フィルタ（所持なしアイテムは引き続きグレーアウト）
- **スクロール**: タブ内をスクロールして全アイテムを閲覧可能

---

## 対応するバニラチェスト操作（全操作対応）

| 操作 | 動作 |
|------|------|
| 左クリック（所持ありアイテム） | 指定スタック数をインベントリへ取り出す |
| 右クリック（所持ありアイテム） | 半スタックをインベントリへ取り出す |
| Shift + 左クリック | スタック全体をインベントリへ即時移動 |
| ダブル左クリック | カーソルのアイテムを倉庫・インベントリから集める（最大64個） |
| 左ドラッグ（複数スロット） | カーソルのアイテムを均等分配 |
| 右ドラッグ（複数スロット） | カーソルのアイテムを1個ずつ配置 |
| 数字キー (1-9) | スロットのアイテムとホットバーの対応スロットを交換 |
| Q キー | スロットのアイテムを1個ドロップ |
| Ctrl + Q | スロットのアイテムをスタックごとドロップ |
| インベントリ → 倉庫へのShift+クリック | インベントリのアイテムを倉庫に預ける |

---

## プロジェクト構成

```
mc-builder/
├── PLAN.md
├── plugin/                          # Spigot Plugin
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/main/
│       ├── java/dev/buildassist/plugin/
│       │   ├── BuildAssistPlugin.java          # メインクラス
│       │   ├── db/
│       │   │   ├── StorageDatabase.java        # SQLite接続・CRUD
│       │   │   └── StorageItem.java            # アイテムデータクラス
│       │   ├── storage/
│       │   │   ├── PlayerStorage.java          # プレイヤーごとの倉庫操作
│       │   │   └── StorageManager.java         # 全プレイヤー倉庫の管理
│       │   ├── menu/
│       │   │   ├── StorageMenu.java            # カスタムInventory
│       │   │   └── StorageMenuListener.java    # 全クリックイベント処理
│       │   └── network/
│       │       └── PluginMessaging.java        # Fabric Modとの通信
│       └── resources/
│           └── plugin.yml
│
└── mod/                             # Fabric Mod (Client-side)
    ├── build.gradle
    ├── settings.gradle
    └── src/main/
        ├── java/dev/buildassist/mod/
        │   ├── BuildAssistMod.java             # Mod初期化
        │   ├── client/
        │   │   ├── BuildAssistClient.java      # クライアント初期化
        │   │   ├── config/
        │   │   │   ├── BuildAssistConfig.java  # 設定データ（パネル位置等）
        │   │   │   └── ConfigScreen.java       # 設定UI（位置調整画面）
        │   │   ├── keybind/
        │   │   │   └── StorageKeybind.java     # インベントリキーフック
        │   │   ├── screen/
        │   │   │   ├── StoragePanel.java       # 倉庫パネル（クリエイティブUI拡張）
        │   │   │   └── StoragePanelHandler.java # スロット操作ハンドラ
        │   │   └── render/
        │   │       ├── ItemCountRenderer.java  # 1k/1m省略表示オーバーレイ
        │   │       └── GrayscaleRenderer.java  # 所持なしアイテムのグレーアウト
        │   └── network/
        │       └── ModMessaging.java           # Pluginとの通信
        └── resources/
            ├── fabric.mod.json
            ├── buildassist.mixins.json
            └── assets/buildassist/
                └── lang/ja_jp.json
```

---

## 技術スタック

| 項目 | 選択 |
|------|------|
| Minecraft | 1.21.x |
| サーバー | Paper（Spigot互換） |
| Plugin API | Spigot API |
| Mod loader | Fabric |
| Mod API | Fabric API |
| DB | SQLite (xerial/sqlite-jdbc) |
| 通信 | Plugin Messaging Channel (`buildassist:main`) |
| 設定保存 | JSON config file（クライアントローカル） |
| ビルドツール | Gradle (両プロジェクト) |

---

## DBスキーマ

```sql
CREATE TABLE IF NOT EXISTS player_storage (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT    NOT NULL,
    item_key    TEXT    NOT NULL,  -- 例: "minecraft:stone"
    nbt_data    TEXT,              -- JSON形式のNBT（エンチャント等）
    count       INTEGER NOT NULL DEFAULT 0,
    UNIQUE(player_uuid, item_key, nbt_data)
);
```

---

## Plugin Messaging 通信仕様

チャンネル名: `buildassist:main`

| 方向 | パケットID | 内容 |
|------|-----------|------|
| Mod → Plugin | `open_storage` | インベントリを開いたタイミングで倉庫データをリクエスト |
| Plugin → Mod | `storage_contents` | アイテムリスト全件（item_key + count のJSON） |
| Plugin → Mod | `storage_update` | 差分更新（操作後） |

※ Modはバニラのクリエイティブタブ全アイテムリストをクライアント側で取得し、
　 倉庫の所持データと突き合わせて表示を切り替える。
　 実際のアイテム増減はPluginのInventoryClickEventで処理。

---

## Mixin対象

| Mixin対象クラス | 目的 |
|----------------|------|
| `InventoryScreen` | Eキーでインベントリが開いたとき倉庫パネルを同時描画 |
| `GameRenderer` or Screen event | パネルのオフセット描画制御 |

---

## 設定ファイル仕様（config/buildassist.json）

```json
{
  "panel_side": "RIGHT",          // UP / DOWN / LEFT / RIGHT
  "inventory_offset_x": 0,        // バニラインベントリのX位置オフセット
  "inventory_offset_y": 0,        // バニラインベントリのY位置オフセット
  "panel_offset_x": 0,            // 倉庫パネルのX位置オフセット（side基準からの微調整）
  "panel_offset_y": 0             // 倉庫パネルのY位置オフセット
}
```

---

## コミットスタイル

### プレフィックス

| プレフィックス | 用途 |
|--------------|------|
| `feat:` | 新機能の追加 |
| `enhance:` | 既存機能の改善・拡張 |
| `fix:` | バグ修正 |
| `chore:` | ビルド設定・依存関係等の雑務 |
| `docs:` | ドキュメント・コメントのみの変更 |

### ルール

- **タスク単位でコミット**: PLAN.mdの `[ ]` 1項目 = 1コミットを基本とする
- **まとめコミット禁止**: 複数タスクを1コミットにまとめない
- **コミットメッセージ例**:
  ```
  feat: add StorageDatabase with SQLite CRUD operations
  feat: implement PlayerStorage per-UUID item management
  enhance: add count abbreviation (1k/1m) to ItemCountRenderer
  fix: correct shift-click handling when storage is full
  chore: init plugin Gradle project with Spigot API dependency
  ```

---

## 実装フェーズ

### Phase 1: Plugin - データ基盤
- [x] Gradleプロジェクト初期化
- [x] SQLite接続・スキーマ作成
- [x] アイテムのCRUD操作（StorageDatabase）
- [x] PlayerStorage（UUID単位の倉庫操作）

### Phase 2: Plugin - 操作処理と通信
- [x] StorageMenu（カスタムInventory）
- [x] StorageMenuListener（全バニラ操作のイベント処理）
- [x] Plugin Messaging（Modからのリクエスト受付・レスポンス）

### Phase 3: Mod - 通信とインベントリフック
- [x] Gradleプロジェクト初期化
- [x] Plugin Messagingの送受信
- [x] `InventoryScreen` Mixinでインベントリ開閉フック
- [x] StoragePanelHandler（スロット定義）

### Phase 4: Mod - UI描画
- [x] クリエイティブUIをベースにStoragePanel実装
- [x] GrayscaleRenderer（所持なしアイテムのグレーアウト）
- [x] ItemCountRenderer（所持数の右下オーバーレイ、1k/1m省略）
- [x] 検索バー実装

### Phase 5: Mod - 位置調整システム
- [x] BuildAssistConfig（設定の読み書き）
- [x] ConfigScreen（設定UI、パネル位置をリアルタイムプレビューで調整）
- [x] panel_side（上下左右）の切り替えと自動オフセット計算
- [x] バニラインベントリ・倉庫パネル両方の位置をドラッグ調整

### Phase 6: 統合テスト・調整
- [x] Plugin / Mod 両プロジェクトのビルド確認（BUILD SUCCESSFUL）
- [x] Mixin method descriptor の警告修正
- [ ] 実機での全バニラ操作の動作確認（要サーバー環境）
- [ ] 大量アイテム時のパフォーマンス確認
- [ ] エッジケース対応（NBT付きアイテム、スタック上限等）
- [ ] 画面解像度・UIスケール違いでの表示確認
