43. [解決済み] 2スタック以上をピッカー（右クリックメニュー）から取り出した後、サバイバルインベントリ内のアイテムを拾うために左クリックを2回押す必要がある。

    【根本原因】
    shift=true の sendWithdraw 送信後、サーバーのスロット更新パケットが届く前にユーザーがクリックすると
    HandledScreen のクライアント予測が空振りするレースコンディション。

    【解決策】
    shift=true の sendWithdraw 送信直後に withdrawLockUntil = currentTimeMillis() + 300 をセットし、
    300ms 間パネル外へのマウスクリックを StoragePanel.mouseClicked で消費（return true）してブロックする。
    shift+click（StoragePanel）・ピッカー確定ボタン・ピッカーEnterキーの3ルートすべてに対応。
    QuantityPickerOverlay に wasLastShift() を追加し、StoragePanel がピッカー close 後にシフト判定を取得できるようにした。

48. [解決済み] サーバー側にプラグインが存在しないときはクリエイティブインベントリを表示しないようにする（シングルプレイでも同様）

    【解決策】
    BuildAssistClient に pluginDetected フラグを追加。
    storage_contents パケット受信時（ModMessaging → BuildAssistClient.onStorageContentsReceived）に true をセットし、
    パネルを遅延生成する。disconnect 時にリセット。
    プラグイン未導入のサーバーでは storage_contents が来ないのでパネルは表示されない。

49. [解決済み] ゲームバランス調整 — 建築ブロック系のみ許可

    【解決策】
    TOOLS / COMBAT / FOOD_AND_DRINK / INGREDIENTS タブを initTabs() から削除。
    BLOCKED_TABS 定数（上記4グループ）を StoragePanel に追加し、
    - UNCATEGORIZED タブ: blocked に属するアイテムを除外（タブ削除で落ちてきたアイテムが表示されないよう）
    - SEARCH タブ: blocked に属するアイテムを除外
    - mouseReleased のデポジット処理: isDepositBlocked() で blocked アイテムのストックを拒否
