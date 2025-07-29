package com.litevar.agent.openai.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class JsonRepair {
    private static final Map<Character, String> CONTROL_CHARACTERS = new HashMap<>();
    private static final Map<Character, String> ESCAPE_CHARACTERS = new HashMap<>();

    static {
        CONTROL_CHARACTERS.put('\b', "\\b");
        CONTROL_CHARACTERS.put('\f', "\\f");
        CONTROL_CHARACTERS.put('\n', "\\n");
        CONTROL_CHARACTERS.put('\r', "\\r");
        CONTROL_CHARACTERS.put('\t', "\\t");

        ESCAPE_CHARACTERS.put('"', "\"");
        ESCAPE_CHARACTERS.put('\\', "\\");
        ESCAPE_CHARACTERS.put('/', "/");
        ESCAPE_CHARACTERS.put('b', "\b");
        ESCAPE_CHARACTERS.put('f', "\f");
        ESCAPE_CHARACTERS.put('n', "\n");
        ESCAPE_CHARACTERS.put('r', "\r");
        ESCAPE_CHARACTERS.put('t', "\t");
    }

    private String text;
    private int i; // current index in text
    private StringBuilder output; // generated output

    /**
     * Repair a string containing an invalid JSON document.
     * For example changes JavaScript notation into JSON notation.
     *
     * @param text the invalid JSON string to repair
     * @return the repaired JSON string
     * @throws JSONRepairError if the JSON cannot be repaired
     */
    public static String jsonrepair(String text) {
        return new JsonRepair().repair(text);
    }

    private String repair(String text) {
        this.text = text;
        this.i = 0;
        this.output = new StringBuilder();

        parseMarkdownCodeBlock();

        boolean processed = parseValue();
        if (!processed) {
            throw new JSONRepairError("Unexpected end of json string", i);
        }

        parseMarkdownCodeBlock();

        boolean processedComma = parseCharacter(',');
        if (processedComma) {
            parseWhitespaceAndSkipComments();
        }

        if (i < text.length() && JsonStringUtils.isStartOfValue(text.charAt(i)) &&
                JsonStringUtils.endsWithCommaOrNewline(output.toString())) {
            if (!processedComma) {
                output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), ","));
            }
            parseNewlineDelimitedJSON();
        } else if (processedComma) {
            output = new StringBuilder(JsonStringUtils.stripLastOccurrence(output.toString(), ","));
        }

        // repair redundant end quotes
        while (i < text.length() && (text.charAt(i) == '}' || text.charAt(i) == ']')) {
            i++;
            parseWhitespaceAndSkipComments();
        }

        if (i >= text.length()) {
            return output.toString();
        }

        throw new JSONRepairError("Unexpected character", i);
    }

    private boolean parseValue() {
        parseWhitespaceAndSkipComments();
        boolean processed = parseObject() || parseArray() || parseString() ||
                parseNumber() || parseKeywords() || parseUnquotedString(false) ||
                parseRegex();
        parseWhitespaceAndSkipComments();
        return processed;
    }

    private boolean parseWhitespaceAndSkipComments(boolean skipNewline) {
        int start = i;
        boolean changed = parseWhitespace(skipNewline);
        do {
            changed = parseComment();
            if (changed) {
                changed = parseWhitespace(skipNewline);
            }
        } while (changed);
        return i > start;
    }

    private boolean parseWhitespaceAndSkipComments() {
        return parseWhitespaceAndSkipComments(true);
    }

    private boolean parseWhitespace(boolean skipNewline) {
        StringBuilder whitespace = new StringBuilder();

        while (i < text.length()) {
            if (skipNewline ? JsonStringUtils.isWhitespace(text, i) : JsonStringUtils.isWhitespaceExceptNewline(text, i)) {
                whitespace.append(text.charAt(i));
                i++;
            } else if (JsonStringUtils.isSpecialWhitespace(text, i)) {
                whitespace.append(' ');
                i++;
            } else {
                break;
            }
        }

        if (!whitespace.isEmpty()) {
            output.append(whitespace);
            return true;
        }
        return false;
    }

    private boolean parseComment() {
        if (i < text.length() - 1 && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
            while (i < text.length() && !atEndOfBlockComment(text, i)) {
                i++;
            }
            i += 2;
            return true;
        }

        if (i < text.length() - 1 && text.charAt(i) == '/' && text.charAt(i + 1) == '/') {
            while (i < text.length() && text.charAt(i) != '\n') {
                i++;
            }
            return true;
        }

        return false;
    }

    private boolean parseMarkdownCodeBlock() {
        if (i < text.length() - 2 && text.startsWith("```", i)) {
            i += 3;

            if (i < text.length() && JsonStringUtils.isFunctionNameCharStart(text.charAt(i))) {
                while (i < text.length() && JsonStringUtils.isFunctionNameChar(text.charAt(i))) {
                    i++;
                }
            }

            parseWhitespaceAndSkipComments();
            return true;
        }
        return false;
    }

    private boolean parseCharacter(char ch) {
        if (i < text.length() && text.charAt(i) == ch) {
            output.append(text.charAt(i));
            i++;
            return true;
        }
        return false;
    }

    private boolean skipCharacter(char ch) {
        if (i < text.length() && text.charAt(i) == ch) {
            i++;
            return true;
        }
        return false;
    }

    private boolean skipEscapeCharacter() {
        return skipCharacter('\\');
    }

    private boolean skipEllipsis() {
        parseWhitespaceAndSkipComments();

        if (i < text.length() - 2 && text.charAt(i) == '.' &&
                text.charAt(i + 1) == '.' && text.charAt(i + 2) == '.') {
            i += 3;
            parseWhitespaceAndSkipComments();
            skipCharacter(',');
            return true;
        }
        return false;
    }

    private boolean parseObject() {
        if (i < text.length() && text.charAt(i) == '{') {
            output.append('{');
            i++;
            parseWhitespaceAndSkipComments();

            if (skipCharacter(',')) {
                parseWhitespaceAndSkipComments();
            }

            boolean initial = true;
            while (i < text.length() && text.charAt(i) != '}') {
                boolean processedComma;
                if (!initial) {
                    processedComma = parseCharacter(',');
                    if (!processedComma) {
                        output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), ","));
                    }
                    parseWhitespaceAndSkipComments();
                } else {
                    processedComma = true;
                    initial = false;
                }

                skipEllipsis();

                boolean processedKey = parseString() || parseUnquotedString(true);
                if (!processedKey) {
                    if (i >= text.length() || text.charAt(i) == '}' || text.charAt(i) == '{' ||
                            text.charAt(i) == ']' || text.charAt(i) == '[') {
                        output = new StringBuilder(JsonStringUtils.stripLastOccurrence(output.toString(), ","));
                    } else {
                        throw new JSONRepairError("Object key expected", i);
                    }
                    break;
                }

                parseWhitespaceAndSkipComments();
                boolean processedColon = parseCharacter(':');
                boolean truncatedText = i >= text.length();
                if (!processedColon) {
                    if ((i < text.length() && JsonStringUtils.isStartOfValue(text.charAt(i))) || truncatedText) {
                        output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), ":"));
                    } else {
                        throw new JSONRepairError("Colon expected", i);
                    }
                }

                boolean processedValue = parseValue();
                if (!processedValue) {
                    if (processedColon || truncatedText) {
                        output.append("null");
                    } else {
                        throw new JSONRepairError("Colon expected", i);
                    }
                }
            }

            if (i < text.length() && text.charAt(i) == '}') {
                output.append('}');
                i++;
            } else {
                output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), "}"));
            }

            return true;
        }
        return false;
    }

    private boolean parseArray() {
        if (i < text.length() && text.charAt(i) == '[') {
            output.append('[');
            i++;
            parseWhitespaceAndSkipComments();

            if (skipCharacter(',')) {
                parseWhitespaceAndSkipComments();
            }

            boolean initial = true;
            while (i < text.length() && text.charAt(i) != ']') {
                if (!initial) {
                    boolean processedComma = parseCharacter(',');
                    if (!processedComma) {
                        output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), ","));
                    }
                } else {
                    initial = false;
                }

                skipEllipsis();

                boolean processedValue = parseValue();
                if (!processedValue) {
                    output = new StringBuilder(JsonStringUtils.stripLastOccurrence(output.toString(), ","));
                    break;
                }
            }

            if (i < text.length() && text.charAt(i) == ']') {
                output.append(']');
                i++;
            } else {
                output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), "]"));
            }

            return true;
        }
        return false;
    }

    private void parseNewlineDelimitedJSON() {
        boolean initial = true;
        boolean processedValue = true;

        while (processedValue) {
            if (!initial) {
                boolean processedComma = parseCharacter(',');
                if (!processedComma) {
                    output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), ","));
                }
            } else {
                initial = false;
            }
            processedValue = parseValue();
        }

        output = new StringBuilder(JsonStringUtils.stripLastOccurrence(output.toString(), ","));

        output = new StringBuilder("[\n" + output.toString() + "\n]");
    }

    private boolean parseString() {
        return parseString(false, -1);
    }

    private boolean parseString(boolean stopAtDelimiter, int stopAtIndex) {
        boolean skipEscapeChars = i < text.length() && text.charAt(i) == '\\';
        if (skipEscapeChars) {
            i++;
        }

        if (i < text.length() && JsonStringUtils.isQuote(text.charAt(i))) {
            Function<Character, Boolean> isEndQuote = JsonStringUtils.isDoubleQuote(text.charAt(i)) ?
                    JsonStringUtils::isDoubleQuote :
                    JsonStringUtils.isSingleQuote(text.charAt(i)) ?
                            JsonStringUtils::isSingleQuote :
                            JsonStringUtils.isSingleQuoteLike(text.charAt(i)) ?
                                    JsonStringUtils::isSingleQuoteLike :
                                    JsonStringUtils::isDoubleQuoteLike;

            int iBefore = i;
            int oBefore = output.length();

            StringBuilder str = new StringBuilder("\"");
            i++;

            while (true) {
                if (i >= text.length()) {
                    int iPrev = prevNonWhitespaceIndex(i - 1);
                    if (!stopAtDelimiter && iPrev >= 0 && iPrev < text.length() &&
                            JsonStringUtils.isDelimiter(text.charAt(iPrev))) {
                        i = iBefore;
                        output.setLength(oBefore);
                        return parseString(true, -1);
                    }

                    str = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(str.toString(), "\""));
                    output.append(str);
                    return true;
                } else if (i == stopAtIndex) {
                    str = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(str.toString(), "\""));
                    output.append(str);
                    return true;
                } else if (isEndQuote.apply(text.charAt(i))) {
                    int iQuote = i;
                    int oQuote = str.length();
                    str.append('"');
                    i++;
                    output.append(str);

                    parseWhitespaceAndSkipComments(false);

                    if (stopAtDelimiter || i >= text.length() ||
                            JsonStringUtils.isDelimiter(text.charAt(i)) ||
                            JsonStringUtils.isQuote(text.charAt(i)) ||
                            JsonStringUtils.isDigit(text.charAt(i))) {
                        parseConcatenatedString();
                        return true;
                    }

                    int iPrevChar = prevNonWhitespaceIndex(iQuote - 1);
                    char prevChar = iPrevChar >= 0 ? text.charAt(iPrevChar) : '\0';

                    if (prevChar == ',') {
                        i = iBefore;
                        output.setLength(oBefore);
                        return parseString(false, iPrevChar);
                    }

                    if (JsonStringUtils.isDelimiter(prevChar)) {
                        i = iBefore;
                        output.setLength(oBefore);
                        return parseString(true, -1);
                    }

                    output.setLength(oBefore);
                    i = iQuote + 1;
                    str = new StringBuilder(str.substring(0, oQuote) + "\\" + str.substring(oQuote));
                } else if (stopAtDelimiter && JsonStringUtils.isUnquotedStringDelimiter(text.charAt(i))) {
                    if (i > 0 && text.charAt(i - 1) == ':' &&
                            i + 2 < text.length() &&
                            JsonStringUtils.urlStartMatches(text.substring(iBefore + 1, i + 2))) {
                        while (i < text.length() && JsonStringUtils.urlCharMatches(String.valueOf(text.charAt(i)))) {
                            str.append(text.charAt(i));
                            i++;
                        }
                    }

                    str = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(str.toString(), "\""));
                    output.append(str);
                    parseConcatenatedString();
                    return true;
                } else if (text.charAt(i) == '\\') {
                    if (i + 1 < text.length()) {
                        char ch = text.charAt(i + 1);
                        String escapeChar = ESCAPE_CHARACTERS.get(ch);
                        if (escapeChar != null) {
                            str.append(text, i, i + 2);
                            i += 2;
                        } else if (ch == 'u') {
                            int j = 2;
                            while (j < 6 && i + j < text.length() && JsonStringUtils.isHex(text.charAt(i + j))) {
                                j++;
                            }

                            if (j == 6) {
                                str.append(text, i, i + 6);
                                i += 6;
                            } else if (i + j >= text.length()) {
                                i = text.length();
                            } else {
                                throw new JSONRepairError("Invalid Unicode character", i);
                            }
                        } else {
                            str.append(ch);
                            i += 2;
                        }
                    } else {
                        i++;
                    }
                } else {
                    char ch = text.charAt(i);

                    if (ch == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                        str.append("\\").append(ch);
                        i++;
                    } else if (JsonStringUtils.isControlCharacter(ch)) {
                        str.append(CONTROL_CHARACTERS.get(ch));
                        i++;
                    } else {
                        if (!JsonStringUtils.isValidStringCharacter(ch)) {
                            throw new JSONRepairError("Invalid character:" + ch, i);
                        }
                        str.append(ch);
                        i++;
                    }
                }

                if (skipEscapeChars) {
                    skipEscapeCharacter();
                }
            }
        }
        return false;
    }

    private boolean parseConcatenatedString() {
        boolean processed = false;

        parseWhitespaceAndSkipComments();
        while (i < text.length() && text.charAt(i) == '+') {
            processed = true;
            i++;
            parseWhitespaceAndSkipComments();

            output = new StringBuilder(JsonStringUtils.stripLastOccurrence(output.toString(), "\"", true));
            int start = output.length();
            boolean parsedStr = parseString();
            if (parsedStr) {
                output = new StringBuilder(JsonStringUtils.removeAtIndex(output.toString(), start, 1));
            } else {
                output = new StringBuilder(JsonStringUtils.insertBeforeLastWhitespace(output.toString(), "\""));
            }
        }

        return processed;
    }

    private boolean parseNumber() {
        int start = i;
        if (i < text.length() && text.charAt(i) == '-') {
            i++;
            if (atEndOfNumber()) {
                repairNumberEndingWithNumericSymbol(start);
                return true;
            }
            if (i >= text.length() || !JsonStringUtils.isDigit(text.charAt(i))) {
                i = start;
                return false;
            }
        }

        while (i < text.length() && JsonStringUtils.isDigit(text.charAt(i))) {
            i++;
        }

        if (i < text.length() && text.charAt(i) == '.') {
            i++;
            if (atEndOfNumber()) {
                repairNumberEndingWithNumericSymbol(start);
                return true;
            }
            if (i >= text.length() || !JsonStringUtils.isDigit(text.charAt(i))) {
                i = start;
                return false;
            }
            while (i < text.length() && JsonStringUtils.isDigit(text.charAt(i))) {
                i++;
            }
        }

        if (i < text.length() && (text.charAt(i) == 'e' || text.charAt(i) == 'E')) {
            i++;
            if (i < text.length() && (text.charAt(i) == '-' || text.charAt(i) == '+')) {
                i++;
            }
            if (atEndOfNumber()) {
                repairNumberEndingWithNumericSymbol(start);
                return true;
            }
            if (i >= text.length() || !JsonStringUtils.isDigit(text.charAt(i))) {
                i = start;
                return false;
            }
            while (i < text.length() && JsonStringUtils.isDigit(text.charAt(i))) {
                i++;
            }
        }

        if (!atEndOfNumber()) {
            i = start;
            return false;
        }

        if (i > start) {
            String num = text.substring(start, i);
            boolean hasInvalidLeadingZero = Pattern.compile("^0\\d").matcher(num).find();

            if (hasInvalidLeadingZero) {
                output.append("\"").append(num).append("\"");
            } else {
                output.append(num);
            }
            return true;
        }

        return false;
    }

    private boolean parseKeywords() {
        return parseKeyword("true", "true") ||
                parseKeyword("false", "false") ||
                parseKeyword("null", "null") ||
                parseKeyword("True", "true") ||
                parseKeyword("False", "false") ||
                parseKeyword("None", "null");
    }

    private boolean parseKeyword(String name, String value) {
        if (i + name.length() <= text.length() &&
                text.startsWith(name, i)) {
            output.append(value);
            i += name.length();
            return true;
        }
        return false;
    }

    private boolean parseUnquotedString(boolean isKey) {
        int start = i;

        if (i < text.length() && JsonStringUtils.isFunctionNameCharStart(text.charAt(i))) {
            while (i < text.length() && JsonStringUtils.isFunctionNameChar(text.charAt(i))) {
                i++;
            }

            int j = i;
            while (j < text.length() && JsonStringUtils.isWhitespace(text, j)) {
                j++;
            }

            if (j < text.length() && text.charAt(j) == '(') {
                i = j + 1;
                parseValue();
                if (i < text.length() && text.charAt(i) == ')') {
                    i++;
                    if (i < text.length() && text.charAt(i) == ';') {
                        i++;
                    }
                }
                return true;
            }
        }

        while (i < text.length() &&
                !JsonStringUtils.isUnquotedStringDelimiter(text.charAt(i)) &&
                !JsonStringUtils.isQuote(text.charAt(i)) &&
                (!isKey || text.charAt(i) != ':')) {
            i++;
        }

        if (i > 0 && i < text.length() && text.charAt(i - 1) == ':' &&
                i + 2 < text.length() &&
                JsonStringUtils.urlStartMatches(text.substring(start, i + 2))) {
            while (i < text.length() && JsonStringUtils.urlCharMatches(String.valueOf(text.charAt(i)))) {
                i++;
            }
        }

        if (i > start) {
            while (i > 0 && JsonStringUtils.isWhitespace(text, i - 1)) {
                i--;
            }

            String symbol = text.substring(start, i);
            if ("undefined".equals(symbol)) {
                output.append("null");
            } else {
                output.append("\"").append(symbol.replace("\"", "\\\"")).append("\"");
            }

            if (i < text.length() && text.charAt(i) == '"') {
                i++;
            }

            return true;
        }

        return false;
    }

    private boolean parseRegex() {
        if (text.charAt(i) == '/') {
            int start = i;
            i++;

            while (i < text.length() && (text.charAt(i) != '/' || text.charAt(i - 1) == '\\')) {
                i++;
            }
            i++;

            output.append("\"").append(text, start, i).append("\"");
            return true;
        }
        return false;
    }

    private int prevNonWhitespaceIndex(int start) {
        int prev = start;
        while (prev > 0 && JsonStringUtils.isWhitespace(text, prev)) {
            prev--;
        }
        return prev;
    }

    private boolean atEndOfNumber() {
        return i >= text.length() || JsonStringUtils.isDelimiter(text.charAt(i)) || JsonStringUtils.isWhitespace(text, i);
    }

    private void repairNumberEndingWithNumericSymbol(int start) {
        // repair numbers cut off at the end
        output.append(text, start, i).append("0");
    }

    private boolean atEndOfBlockComment(String text, int i) {
        return text.charAt(i) == '*' && text.charAt(i + 1) == '/';
    }
}