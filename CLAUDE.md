# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

mfm.ktは、MFM（Misskey Flavored Markdown）のKotlin Multiplatform実装パーサーライブラリです。
[mfm.js](https://github.com/misskey-dev/mfm.js/)を参考にして実装されています。

## アーキテクチャ

### 2段階パース処理
MFMテキストの解析は2つの段階で実行されます：

1. **字句解析（Token Parser）**: `MfmTokenParser`
   - 入力テキストを意味のあるトークンに分解
   - パーサーコンビネータパターンを使用（`or`, `and`, `many`）
   - 正規表現ベースのトークン抽出

2. **構文解析（Syntax Parser）**: `MfmSyntaxParser`
   - トークンからMfmNodeツリーを構築
   - 状態機械（ParseState enum）による解析
   - ネストレベル制限（深さ10まで）
   - Optionによる機能の有効/無効切り替え

### 主要コンポーネント

- **MfmNode**: 構文解析木のノード定義（sealed class）
- **TokenType**: 字句解析で認識するトークンの種類
- **Token**: 字句解析結果を表すデータクラス

## プロジェクト構造

```
mfm_kt/                           # メインライブラリモジュール（KMP）
├── src/
│   ├── commonMain/kotlin/        # 共通コード（全プラットフォーム共有）
│   │   └── jp/takke/mfm_kt/
│   │       ├── syntax_parser/    # 構文解析
│   │       └── token_parser/     # 字句解析
│   ├── commonTest/kotlin/        # 共通テスト
│   ├── androidMain/              # Android固有コード
│   └── jvmMain/                  # JVM固有コード

sample_activity/                  # サンプルAndroidアプリ
```

### サポートプラットフォーム

- **Android** (androidTarget)
- **JVM** (jvm)
- **iOS** (iosX64, iosArm64, iosSimulatorArm64)

## 開発コマンド

### ビルド
```bash
./gradlew build
```

### テスト実行
```bash
# 全プラットフォームのテスト
./gradlew test

# JVMテストのみ
./gradlew jvmTest

# Androidテストのみ
./gradlew :mfm_kt:testDebugUnitTest
```

### 単一テスト実行
```bash
./gradlew :mfm_kt:jvmTest --tests "jp.takke.mfm_kt.MfmSyntaxParserTest"
./gradlew :mfm_kt:jvmTest --tests "jp.takke.mfm_kt.MfmTokenParserTest"
```

### Mavenパブリッシュ（ローカル）
```bash
./gradlew publishToMavenLocal
```

### サンプルアプリビルド
```bash
./gradlew :sample_activity:build
```

## 日本語サポート

Unicode文字クラスを定義してCJK（中日韓）文字に対応：
- ひらがな・カタカナ・漢字のサポート
- 日本語の記号や句読点の適切な処理
