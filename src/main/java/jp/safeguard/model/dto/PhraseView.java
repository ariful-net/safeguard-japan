package jp.safeguard.model.dto;

/**
 * 画面表示用フレーズ（選択言語の文言 + 日本語原文）。
 * 日本語原文は「日本人に見せる」用途（安否メッセージの併記など）で使う。
 */
public record PhraseView(Long id, String text, String textJa) {
}
