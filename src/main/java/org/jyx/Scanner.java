package org.jyx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jyx.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;   // 指向被扫描的词素中的第一个字符
    private int current = 0; // 指向当前正在处理的字符
    private int line = 1;    // current所在的源文件行数
    private static final Map<String, TokenType> keywords; // 字符串到保留字的映射

    // 建立字符串到保留字的映射关系
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    // 扫描出所有词素
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    // 扫描一个词素
    private void scanToken() {
        char c = advance();
        switch (c) {
            // 识别单个字符
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            // 识别操作符
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            // 识别除法和注释
            case '/':
                if (match('/')) {
                    // 不添加token
                    // 当循环处理到下个词素时，start指针被重置，注释后的内容自动被忽略
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
                break;
            // 跳过无意义的字符
            case ' ':
            case '\r':
            case '\t':
                break;
            // 更新行数
            case '\n':
                line++;
                break;
            // 处理字面量：字符串
            case '"': string(); break;
            default:
                // 处理字面量：数字
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    // 处理标识符
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    // 处理标识符
    private void identifier() {
        // 向后消费字母或数字
        while (isAlphaNumeric(peek())) advance();
        // 切割字符串
        String text = source.substring(start, current);
        // 取到该字符串对应的保留字枚举类型
        TokenType type = keywords.get(text);
        // 如果不是保留字，type为标识符类型IDENTIFIER
        if (type == null) type = IDENTIFIER;
        // 添加标识符token
        addToken(type);
    }

    // 处理数字字面量
    private void number() {
        // 向后消费整数字符
        while (isDigit(peek())) advance();
        // 处理小数部分
        if (peek() == '.' && isDigit(peekNext())) {
            // 消费一个小数点字符.
            advance();
            // 消费小数字符
            while (isDigit(peek())) advance();
        }
        // 添加数字token，转换为浮点数存储
        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    // 处理字符串字面量
    private void string() {
        // 向后消费字符串中的内容
        while (peek() != '"' && !isAtEnd()) {
            // 允许多行字符串
            if (peek() == '\n') line++;
            advance();
        }
        // 到达文件末尾，字符串未闭合，报出错误信息
        if (isAtEnd()) {
            Lox.error(line, "Unexpected end of string");
            return;
        }
        // 未到达文件末尾，消费一个闭合的引号"
        advance();
        // 添加字符串token，“”里的内容
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    // 检测当前字符是否与传入字符匹配，匹配则current向后移动一步
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    // 取出当前字符，但不移动指针
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // 取出当前字符的下一个字符，但不移动指针
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    // 判断当前字符是否为字母或下划线
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    // 判断当前字符是否为字母或下划线或数字
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // 判断当前字符是否为数字
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // 判断是否扫描到结尾
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // 取最前边的字符，并将current指针向后移动一步
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    // 处理无字面值的token，例如一些符号(){},.-+;*
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // 处理有字面值的token
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}