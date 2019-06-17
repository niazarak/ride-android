package com.ride.android.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Tokenizer {

    /**
     * Main method
     * Input is program string
     * Output is tokenized program
     */
    public static List<Token> tokenize(final String input) {
        // System.out.println("Initial input: " + input);
        // insert spaces & split
        final String[] rawTokens = input.replace("(", " ( ")
                .replace(")", " ) ")
                .split(" ");
        // System.out.println("Converted input: " + Arrays.toString(rawTokens));


        // parse into tokens
        final List<Token> tokens = new LinkedList<>();
        for (final String rawToken : rawTokens) {
            if (rawToken == null || rawToken.isEmpty()) {
                continue;
            }
            if (isNumber(rawToken)) {
                tokens.add(Token.makeToken(TokenType.NUMBER, Integer.parseInt(rawToken)));
            } else if (isParenOpen(rawToken)) {
                tokens.add(Token.makeToken(TokenType.PAREN_OPEN, rawToken));
            } else if (isParenClose(rawToken)) {
                tokens.add(Token.makeToken(TokenType.PAREN_CLOSE, rawToken));
            } else if (isBoolean(rawToken)) {
                tokens.add(Token.makeToken(TokenType.BOOLEAN, parseBoolean(rawToken)));
            } else {
                tokens.add(Token.makeToken(TokenType.SYMBOL, rawToken));
            }
        }

        // System.out.println("Tokens: " + tokens);

        return tokens;
    }

    static boolean isNumber(final String rawToken) {
        return Pattern.compile("\\d+").matcher(rawToken).matches();
    }

    static boolean isBoolean(final String rawToken) {
        return Pattern.compile("#[ft]").matcher(rawToken).matches();
    }

    static boolean isParenOpen(final String rawToken) {
        return rawToken.equals("(");
    }

    static boolean isParenClose(final String rawToken) {
        return rawToken.equals(")");
    }

    static boolean parseBoolean(final String rawToken) {
        switch (rawToken) {
            case "#t":
                return true;
            case "#f":
                return false;
            default:
                throw new IllegalArgumentException("boolean value should be either '#t' or '#f'");
        }
    }

    static class TokenType<TVALUE> {
        private final String title;

        public TokenType(final String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "#" + title;
        }

        public static final TokenType<String> PAREN_OPEN = new TokenType<String>("PAREN_OPEN") {
        };
        public static final TokenType<String> PAREN_CLOSE = new TokenType<String>("PAREN_CLOSE") {
        };
        public static final TokenType<Integer> NUMBER = new TokenType<Integer>("NUMBER") {
        };
        public static final TokenType<String> SYMBOL = new TokenType<String>("SYMBOL") {
        };
        public static final TokenType<Boolean> BOOLEAN = new TokenType<Boolean>("BOOLEAN") {
        };
    }

    public static abstract class Token<R> {
        abstract TokenType<R> getType();

        abstract R getValue();

        public <SR> Token<SR> as(TokenType<SR> type) {
            return (Token<SR>) this;
        }

        @Override
        public String toString() {
            TokenType<R> type = getType();
            R value = getValue();
            return "Token{" + type + ", " + value + " }";
        }

        static <SR> Token<SR> makeToken(final TokenType<SR> type, final SR value) {
            return new Token<SR>() {
                @Override
                public TokenType<SR> getType() {
                    return type;
                }

                @Override
                public SR getValue() {
                    return value;
                }
            };
        }
    }
}
