# CxrlSample

Rokid 公式 [CXR-L SDK サンプル `CXRLSample`](https://custom.rokid.com/prod/rokid_web/84feb39f8ef141b0ad0326f902ab881f/pc/us/index.html?documentId=d7a98730be0f416fb7816ae04bc2c26d) の **グローバル版 Hi Rokid 対応デモ**。本家は中国版 Hi Rokid (`com.rokid.sprite.aiapp`) にハードコードされていてグローバル版環境では動かないため、グローバル版 (`com.rokid.sprite.global.aiapp`) でも本家と同じ書き味で動かせるよう一式整え直したもの。

スマホ側 (`phone/`) で CustomView / CustomApp / Audio / Photo / CustomCmd の全機能を試せる。グラス側 (`glass/`) は CustomApp シナリオでスマホからアップロードされる対象アプリで、Rokid 公式グラス側サンプル [`sSDKSampleforCXR`](https://custom.rokid.com/prod/rokid_web/84feb39f8ef141b0ad0326f902ab881f/pc/us/index.html?documentId=d7a98730be0f416fb7816ae04bc2c26d) の表示文字列を日本語化したフォーク。

## このリポジトリと依存リポジトリ

```
┌──────────────────────────────────────────┐
│ CxrlSample  ← このリポジトリ
│   phone/  : スマホ側デモアプリ (CXRLSample 改)
│   glass/  : グラス側 APK (sSDKSampleforCXR 改、CustomApp で投入)
└────┬───────────────────────────┬─────────┘
     │ ① depends on              │ ② Caps シリアライザ / グラス側 SDK
     │ (Gradle composite build)  │ (Rokid maven)
     ▼                            ▼
   CxrGlobal              com.rokid.cxr:client-l (phone)
   (Hi Rokid global       (glass は本家 SDK をそのまま利用)
    対応の薄いラッパー)
```

| 役割 | リポジトリ / 依存 | 説明 |
|---|---|---|
| ① ライブラリ | [TakanariShimbo/CxrGlobal](https://github.com/TakanariShimbo/CxrGlobal) | グローバル版 Hi Rokid 対応の CXR-L 薄いラッパー。`phone/` から Gradle composite build (`includeBuild("../../CxrGlobal")`) で取り込む |
| 本体 | **CxrlSample** (このリポ) | スマホ + グラスのサンプル一式 |
| ② Caps (phone) | `com.rokid.cxr:client-l:1.0.1` (Rokid maven) | Wire 互換のため本家 SDK の Caps シリアライザだけ借用 |

## 動作要件

| カテゴリ | 必要条件 | 動作確認済み |
|---|---|---|
| スマホ | Android (minSdk 31 / compileSdk 36) | Google Pixel 8 / Android 16 (SDK 36) |
| グラス | ペアリング済みであること | Rokid Glasses / YodaOS SPRITE 1.18.007-20260427-150201 |
| Hi Rokid アプリ | グローバル版 (`com.rokid.sprite.global.aiapp`) インストール済み | G1.5.9.0408 (versionCode 10050009) |

## セットアップ

### 1. 隣接配置で 2 リポジトリを clone

CxrGlobal は Gradle composite build (`includeBuild("../../CxrGlobal")`) で参照するので **同じ親ディレクトリに並べて** clone する:

```bash
cd ~/AndroidStudioProjects
git clone https://github.com/TakanariShimbo/CxrGlobal.git
git clone https://github.com/TakanariShimbo/CxrlSample.git
# → CxrGlobal / CxrlSample が並ぶ
```

### 2. SDK パスを設定

`phone/local.properties` と `glass/local.properties` のそれぞれに:

```properties
sdk.dir=/path/to/Android/Sdk
```

### 3. JDK は Android Studio バンドル JBR を使う

```bash
export JAVA_HOME=/opt/android-studio/jbr
export PATH=$JAVA_HOME/bin:$PATH
```

### 4. グラス用 APK をビルドしてスマホに配置 (CustomApp シナリオ用)

```bash
cd CxrlSample/glass
./gradlew assembleDebug

# スマホの所定パスに cxrL.apk として置く
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/DCIM/Rokid/cxrL.apk
```

### 5. スマホアプリをビルド & 実機に投入

```bash
cd ../phone
./gradlew installDebug
```

## 使い方

ホーム画面の流れ: グローバル版 Hi Rokid 検出 → 認可フロー → token 取得 → 各機能ページ遷移。

- **CustomViewType**: `customViewOpen` / `customViewUpdate` / `customViewClose` の動作確認
- **CustomAppType**: グラス側アプリの `appUploadAndInstall` / `appStart` / `appStop` / `appUninstall` / `appIsInstalled` を試せる (上記ステップ 4 で配置した `cxrL.apk` がインストールされる)
- **AudioUsage / PhotoUsage / CustomCmd**: 上 2 シナリオから入って共通の link を再利用

## phone (スマホ側) の元サンプルからの主な改変点

公式 `client-l:1.0.1` SDK は中国版 Hi Rokid 固定。本デモはそれを **CxrGlobal ラッパー** で迂回している:

- 依存差し替え: `com.rokid.cxr:client-l` の `CXRLink` / `AuthorizationHelper` 直叩き → **`com.example.cxrglobal:lib`** 経由
- import 書き換え: `com.rokid.cxr.link.*` / `com.rokid.sprite.aiapp.externalapp.auth.*` → `com.example.cxrglobal.*`
- API 形状の差を吸収:
  - `AuthorizationHelper.INSTANCE.foo()` → `AuthorizationHelper.foo()` (Kotlin object 化)
  - `parseAuthorizationResult` の戻り値が non-null 化、`onAudioReceived` / `onImageReceived` / `onCustomCmdResult` の引数も non-null に
- `MainActivity` でアプリマーケットに飛ばす対象 package を中国版 → グローバル版に変更
- `AndroidManifest.xml` に `<queries>` で `com.rokid.sprite.global.aiapp` を可視化 (Android 11+ 必須)
- 本家 `client-l:1.0.1` は `Caps` シリアライザ (`com.rokid.cxr.Caps`) を借りる目的でのみ依存に残置 (グラス側 APK との payload wire 互換のため)
- リネーム (CxrGlobal / HelloToggleCxrl と同じ `com.example.*` 流儀に揃えた):
  - applicationId / namespace / Kotlin パッケージ: `com.rokid.cxrlsample` → `com.example.cxrlsample.host`
  - アプリ表示名 (`app_name`): `CXRLSample` → `CxrlSample Host`
  - アプリ内ヘッダー (`screen_title_main`): `CXRLSample` → `CxrlSample Host`
  - Compose Theme: `Theme.CXRLSample` / `CXRLSampleTheme` → `Theme.CxrlSampleHost` / `CxrlSampleHostTheme`
  - Application クラス: `CXRLSampleApplication` → `CxrlSampleHostApplication`
  - CustomApp で起動するグラス側パッケージ参照 (`CONSTANT.APP_PACKAGE_NAME`): `com.rokid.cxrswithcxrl` → `com.example.cxrlsample.client` (glass 側 applicationId に追従)
- バグ修正: CustomApp 画面で接続成立後にインストール状態を問い合わせていなかったため、グラス側に APK が残っていても画面再オープン毎に「未インストール」UI が出て再インストールを促していた。`CustomAppTypeViewModel` の接続フラグセッターで未接続→接続の遷移時に `checkApkInstalled()` (`cxrLink.appIsInstalled`) を1回呼ぶよう修正

## glass (グラス側) の元サンプルからの改変点

- `MainActivity.kt` 内 Compose の中国語表示文字列 2 箇所を日本語に差し替え (SDK 連携ロジックには手を入れていない):

  | 元 (中国語) | 改変 (日本語) |
  |---|---|
  | 这里是主页\n可以在这个页面测试自定义指令\n点击任意按键将向手机端发送键值信息 | ここはメインページです\nこの画面でカスタムコマンドをテストできます\n任意のキーを押すとスマホ側にキー情報を送信します |
  | 下边是来自手机端的自定义指令 | 以下はスマホ側から受信したカスタムコマンド |

- リネーム (phone と揃え、`com.example.*` 流儀に統一):
  - applicationId / namespace / Kotlin パッケージ: `com.rokid.cxrswithcxrl` → `com.example.cxrlsample.client`
  - アプリ表示名 (`app_name`): `CXRSWithCXRL` → `CxrlSample Client`
  - Compose Theme: `Theme.CXRSWithCXRL` / `CXRSWithCXRLTheme` → `Theme.CxrlSampleClient` / `CxrlSampleClientTheme`

## トラブルシューティング

- **「Hi Rokid アプリ未インストール」表示**: グローバル版 (`com.rokid.sprite.global.aiapp`) がインストールされていない、または `phone/app/src/main/AndroidManifest.xml` の `<queries>` 漏れ
- **認可後に link が繋がらない**: token 期限切れの可能性。再度認可フローを通す
- **CustomApp で `installApp failed: cannot find readable cxrL.apk`**: APK が所定パスに無い。`/sdcard/DCIM/Rokid/cxrL.apk` を確認
- **CustomCmd の send で `UnsatisfiedLinkError`**: CxrGlobal のバージョンが古い可能性。`Caps` の native 登録は CxrGlobal 側で対応済みなので、CxrGlobal を最新に更新
- **ビルド時に `Could not resolve com.example.cxrglobal:lib`**: CxrGlobal リポを並列に clone していない、または `phone/settings.gradle.kts` の `includeBuild` パスが合っていない
