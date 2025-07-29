package com.litevar.agent.openai.util;

import java.util.regex.Pattern;

public class JsonStringUtils {
    private static final int CODE_SPACE = 0x20; // " "
    private static final int CODE_NEWLINE = 0x0A; // "\n"
    private static final int CODE_TAB = 0x09; // "\t"
    private static final int CODE_RETURN = 0x0D; // "\r"
    private static final int CODE_NON_BREAKING_SPACE = 0xA0;
    private static final int CODE_EN_QUAD = 0x2000;
    private static final int CODE_HAIR_SPACE = 0x200A;
    private static final int CODE_NARROW_NO_BREAK_SPACE = 0x202F;
    private static final int CODE_MEDIUM_MATHEMATICAL_SPACE = 0x205F;
    private static final int CODE_IDEOGRAPHIC_SPACE = 0x3000;

    private static final Pattern REGEX_URL_START = Pattern.compile("^(http|https|ftp|mailto|file|data|irc)://$");
    private static final Pattern REGEX_URL_CHAR = Pattern.compile("^[A-Za-z0-9\\-._~:/?#@!$&'()*+;=]$");
    private static final Pattern REGEX_START_OF_VALUE = Pattern.compile("^[\\[{\\w-]$");

    public static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isValidStringCharacter(char c) {
        return c >= '\u0020';
    }

    public static boolean isDelimiter(char c) {
        return ",:[]/{}()\n+".indexOf(c) >= 0;
    }

    public static boolean isFunctionNameCharStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    public static boolean isFunctionNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                c == '_' || c == '$' || (c >= '0' && c <= '9');
    }

    public static boolean isUnquotedStringDelimiter(char c) {
        return ",[]/{}\n+".indexOf(c) >= 0;
    }

    public static boolean isStartOfValue(char c) {
        return isQuote(c) || REGEX_START_OF_VALUE.matcher(String.valueOf(c)).matches();
    }

    public static boolean isControlCharacter(char c) {
        return c == '\n' || c == '\r' || c == '\t' || c == '\b' || c == '\f';
    }

    public static boolean isWhitespace(String text, int index) {
        if (index >= text.length()) return false;
        int code = text.charAt(index);
        return code == CODE_SPACE || code == CODE_NEWLINE || code == CODE_TAB || code == CODE_RETURN;
    }

    public static boolean isWhitespaceExceptNewline(String text, int index) {
        if (index >= text.length()) return false;
        int code = text.charAt(index);
        return code == CODE_SPACE || code == CODE_TAB || code == CODE_RETURN;
    }

    public static boolean isSpecialWhitespace(String text, int index) {
        if (index >= text.length()) return false;
        int code = text.charAt(index);
        return code == CODE_NON_BREAKING_SPACE ||
                (code >= CODE_EN_QUAD && code <= CODE_HAIR_SPACE) ||
                code == CODE_NARROW_NO_BREAK_SPACE ||
                code == CODE_MEDIUM_MATHEMATICAL_SPACE ||
                code == CODE_IDEOGRAPHIC_SPACE;
    }

    public static boolean isQuote(char c) {
        return isDoubleQuoteLike(c) || isSingleQuoteLike(c);
    }

    public static boolean isDoubleQuoteLike(char c) {
        return c == '"' || c == '\u201c' || c == '\u201d';
    }

    public static boolean isDoubleQuote(char c) {
        return c == '"';
    }

    public static boolean isSingleQuoteLike(char c) {
        return c == '\'' || c == '\u2018' || c == '\u2019' || c == '\u0060' || c == '\u00b4';
    }

    public static boolean isSingleQuote(char c) {
        return c == '\'';
    }

    public static String stripLastOccurrence(String text, String textToStrip, boolean stripRemainingText) {
        int index = text.lastIndexOf(textToStrip);
        if (index != -1) {
            return text.substring(0, index) +
                    (stripRemainingText ? "" : text.substring(index + textToStrip.length()));
        }
        return text;
    }

    public static String stripLastOccurrence(String text, String textToStrip) {
        return stripLastOccurrence(text, textToStrip, false);
    }

    public static String insertBeforeLastWhitespace(String text, String textToInsert) {
        int index = text.length();

        if (index == 0 || !isWhitespace(text, index - 1)) {
            return text + textToInsert;
        }

        while (index > 0 && isWhitespace(text, index - 1)) {
            index--;
        }

        return text.substring(0, index) + textToInsert + text.substring(index);
    }

    public static String removeAtIndex(String text, int start, int count) {
        return text.substring(0, start) + text.substring(start + count);
    }

    public static boolean endsWithCommaOrNewline(String text) {
        return Pattern.compile("[,\\n][ \\t\\r]*$").matcher(text).find();
    }

    public static boolean urlStartMatches(String text) {
        return REGEX_URL_START.matcher(text).matches();
    }

    public static boolean urlCharMatches(String text) {
        return REGEX_URL_CHAR.matcher(text).matches();
    }
}