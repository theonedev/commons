package io.onedev.commons.jsymbol.golang;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple Go scanner/lexer that tokenizes Go source code.
 * Based on Go's own scanner implementation.
 */
public class GoScanner {
    
    public enum TokenType {
        EOF,
        IDENT,
        INT,
        FLOAT,
        IMAG,
        CHAR,
        STRING,
        RAW_STRING,
        
        // Operators and delimiters
        LPAREN, RPAREN,     // ( )
        LBRACK, RBRACK,     // [ ]
        LBRACE, RBRACE,     // { }
        COMMA,              // ,
        SEMICOLON,          // ;
        COLON,              // :
        DOT,                // .
        ELLIPSIS,           // ...
        STAR,               // *
        ASSIGN,             // =
        ARROW,              // <-
        
        // Keywords
        PACKAGE,
        IMPORT,
        TYPE,
        STRUCT,
        INTERFACE,
        FUNC,
        VAR,
        CONST,
        MAP,
        CHAN,
        
        // Other
        NEWLINE,
        OTHER
    }
    
    public static class Token {
        public final TokenType type;
        public final String value;
        public final int line;
        public final int column;
        public final int offset;
        
        public Token(TokenType type, String value, int line, int column, int offset) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
            this.offset = offset;
        }
        
        @Override
        public String toString() {
            return type + "(" + value + ")@" + line + ":" + column;
        }
    }
    
    private final String source;
    private int pos;
    private int line;
    private int column;
    
    public GoScanner(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 0;
        this.column = 0;
    }
    
    public List<Token> scanAll() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = scan()).type != TokenType.EOF) {
            tokens.add(token);
        }
        tokens.add(token); // Add EOF
        return tokens;
    }
    
    public Token scan() {
        skipWhitespaceAndComments();
        
        if (pos >= source.length()) {
            return new Token(TokenType.EOF, "", line, column, pos);
        }
        
        int startLine = line;
        int startColumn = column;
        int startPos = pos;
        char ch = source.charAt(pos);
        
        // Newline (important for Go's implicit semicolons)
        if (ch == '\n') {
            pos++;
            int oldLine = line;
            line++;
            column = 0;
            return new Token(TokenType.NEWLINE, "\n", oldLine, startColumn, startPos);
        }
        
        // Identifiers and keywords
        if (isLetter(ch)) {
            return scanIdentifier(startLine, startColumn, startPos);
        }
        
        // Numbers
        if (isDigit(ch)) {
            return scanNumber(startLine, startColumn, startPos);
        }
        
        // Strings
        if (ch == '"') {
            return scanString(startLine, startColumn, startPos);
        }
        
        // Raw strings
        if (ch == '`') {
            return scanRawString(startLine, startColumn, startPos);
        }
        
        // Runes
        if (ch == '\'') {
            return scanRune(startLine, startColumn, startPos);
        }
        
        // Operators and delimiters
        pos++;
        column++;
        
        switch (ch) {
            case '(': return new Token(TokenType.LPAREN, "(", startLine, startColumn, startPos);
            case ')': return new Token(TokenType.RPAREN, ")", startLine, startColumn, startPos);
            case '[': return new Token(TokenType.LBRACK, "[", startLine, startColumn, startPos);
            case ']': return new Token(TokenType.RBRACK, "]", startLine, startColumn, startPos);
            case '{': return new Token(TokenType.LBRACE, "{", startLine, startColumn, startPos);
            case '}': return new Token(TokenType.RBRACE, "}", startLine, startColumn, startPos);
            case ',': return new Token(TokenType.COMMA, ",", startLine, startColumn, startPos);
            case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startColumn, startPos);
            case ':': return new Token(TokenType.COLON, ":", startLine, startColumn, startPos);
            case '*': return new Token(TokenType.STAR, "*", startLine, startColumn, startPos);
            case '=': return new Token(TokenType.ASSIGN, "=", startLine, startColumn, startPos);
            case '.':
                if (pos + 1 < source.length() && source.charAt(pos) == '.' && source.charAt(pos + 1) == '.') {
                    pos += 2;
                    column += 2;
                    return new Token(TokenType.ELLIPSIS, "...", startLine, startColumn, startPos);
                }
                return new Token(TokenType.DOT, ".", startLine, startColumn, startPos);
            case '<':
                if (pos < source.length() && source.charAt(pos) == '-') {
                    pos++;
                    column++;
                    return new Token(TokenType.ARROW, "<-", startLine, startColumn, startPos);
                }
                return new Token(TokenType.OTHER, "<", startLine, startColumn, startPos);
            default:
                return new Token(TokenType.OTHER, String.valueOf(ch), startLine, startColumn, startPos);
        }
    }
    
    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char ch = source.charAt(pos);
            
            if (ch == ' ' || ch == '\t' || ch == '\r') {
                pos++;
                column++;
            } else if (ch == '/' && pos + 1 < source.length()) {
                char next = source.charAt(pos + 1);
                if (next == '/') {
                    // Line comment
                    pos += 2;
                    column += 2;
                    while (pos < source.length() && source.charAt(pos) != '\n') {
                        pos++;
                        column++;
                    }
                } else if (next == '*') {
                    // Block comment
                    pos += 2;
                    column += 2;
                    while (pos + 1 < source.length()) {
                        if (source.charAt(pos) == '\n') {
                            line++;
                            column = 0;
                        } else if (source.charAt(pos) == '*' && source.charAt(pos + 1) == '/') {
                            pos += 2;
                            column += 2;
                            break;
                        }
                        pos++;
                        column++;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }
    
    private Token scanIdentifier(int startLine, int startColumn, int startPos) {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && isLetterOrDigit(source.charAt(pos))) {
            sb.append(source.charAt(pos));
            pos++;
            column++;
        }
        String value = sb.toString();
        TokenType type = getKeywordType(value);
        return new Token(type, value, startLine, startColumn, startPos);
    }
    
    private Token scanNumber(int startLine, int startColumn, int startPos) {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false;
        boolean isImag = false;
        
        // Handle hex, octal, binary
        if (source.charAt(pos) == '0' && pos + 1 < source.length()) {
            char next = Character.toLowerCase(source.charAt(pos + 1));
            if (next == 'x' || next == 'o' || next == 'b') {
                sb.append(source.charAt(pos));
                sb.append(source.charAt(pos + 1));
                pos += 2;
                column += 2;
            }
        }
        
        while (pos < source.length()) {
            char ch = source.charAt(pos);
            if (isDigit(ch) || ch == '_' || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
                sb.append(ch);
                pos++;
                column++;
            } else if (ch == '.') {
                if (pos + 1 < source.length() && isDigit(source.charAt(pos + 1))) {
                    isFloat = true;
                    sb.append(ch);
                    pos++;
                    column++;
                } else {
                    break;
                }
            } else if (ch == 'e' || ch == 'E') {
                isFloat = true;
                sb.append(ch);
                pos++;
                column++;
                if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                    sb.append(source.charAt(pos));
                    pos++;
                    column++;
                }
            } else if (ch == 'i') {
                isImag = true;
                sb.append(ch);
                pos++;
                column++;
                break;
            } else {
                break;
            }
        }
        
        TokenType type = isImag ? TokenType.IMAG : (isFloat ? TokenType.FLOAT : TokenType.INT);
        return new Token(type, sb.toString(), startLine, startColumn, startPos);
    }
    
    private Token scanString(int startLine, int startColumn, int startPos) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        pos++;
        column++;
        
        while (pos < source.length()) {
            char ch = source.charAt(pos);
            if (ch == '"') {
                sb.append(ch);
                pos++;
                column++;
                break;
            } else if (ch == '\\' && pos + 1 < source.length()) {
                sb.append(ch);
                sb.append(source.charAt(pos + 1));
                pos += 2;
                column += 2;
            } else if (ch == '\n') {
                // Unterminated string
                break;
            } else {
                sb.append(ch);
                pos++;
                column++;
            }
        }
        
        return new Token(TokenType.STRING, sb.toString(), startLine, startColumn, startPos);
    }
    
    private Token scanRawString(int startLine, int startColumn, int startPos) {
        StringBuilder sb = new StringBuilder();
        sb.append('`');
        pos++;
        column++;
        
        while (pos < source.length()) {
            char ch = source.charAt(pos);
            if (ch == '`') {
                sb.append(ch);
                pos++;
                column++;
                break;
            } else if (ch == '\n') {
                sb.append(ch);
                pos++;
                line++;
                column = 0;
            } else {
                sb.append(ch);
                pos++;
                column++;
            }
        }
        
        return new Token(TokenType.RAW_STRING, sb.toString(), startLine, startColumn, startPos);
    }
    
    private Token scanRune(int startLine, int startColumn, int startPos) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        pos++;
        column++;
        
        while (pos < source.length()) {
            char ch = source.charAt(pos);
            if (ch == '\'') {
                sb.append(ch);
                pos++;
                column++;
                break;
            } else if (ch == '\\' && pos + 1 < source.length()) {
                sb.append(ch);
                sb.append(source.charAt(pos + 1));
                pos += 2;
                column += 2;
            } else if (ch == '\n') {
                break;
            } else {
                sb.append(ch);
                pos++;
                column++;
            }
        }
        
        return new Token(TokenType.CHAR, sb.toString(), startLine, startColumn, startPos);
    }
    
    private boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }
    
    private boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }
    
    private boolean isLetterOrDigit(char ch) {
        return isLetter(ch) || isDigit(ch);
    }
    
    private TokenType getKeywordType(String value) {
        switch (value) {
            case "package": return TokenType.PACKAGE;
            case "import": return TokenType.IMPORT;
            case "type": return TokenType.TYPE;
            case "struct": return TokenType.STRUCT;
            case "interface": return TokenType.INTERFACE;
            case "func": return TokenType.FUNC;
            case "var": return TokenType.VAR;
            case "const": return TokenType.CONST;
            case "map": return TokenType.MAP;
            case "chan": return TokenType.CHAN;
            default: return TokenType.IDENT;
        }
    }
}
