package com.alphabot.utils;

public class TextProcessingUtils {

    /**
     * Safely escapes characters required by Telegram's MarkdownV2
     * while preserving asterisks (*) for bold formatting.
     * LLMs often produce **bold** which we map to *bold* first.
     */
    public static String escapeForMarkdownV2(String text) {
        if (text == null)
            return "";

        // 1. Convert LLM **bold** to Telegram *bold*
        String processed = text.replaceAll("\\*\\*", "*");

        // 2. Escape all mandatory MarkdownV2 characters EXCEPT * (asterisk)
        // Mandatory escaped chars: _ * [ ] ( ) ~ ` > # + - = | { } . !
        return processed
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    /**
     * Cleans up common bad HTML tags found in RSS feeds
     */
    public static String sanitizeHtmlForRss(String rawXml) {
        if (rawXml == null)
            return "";
        return rawXml
                .replace("</br>", "") // Invalid closing br tag
                .replace("<br>", " "); // Unclosed br tag — convert to space
    }

    /**
     * Fixes unescaped ampersands in URLs inside XML
     */
    public static String fixUnescapedAmpersands(String rawXml) {
        if (rawXml == null)
            return "";
        return rawXml.replaceAll("&(?!amp;|lt;|gt;|quot;|apos;|#)", "&amp;");
    }
}
