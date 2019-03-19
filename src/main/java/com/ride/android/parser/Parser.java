/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.ride.android.parser;

import java.util.List;

public class Parser {

    static class ParseResult {
        final SExpressions.ListSExpr node;
        final int offset;

        ParseResult(SExpressions.ListSExpr expr, int offset) {
            this.node = expr;
            this.offset = offset;
        }
    }

    private static ParseResult parse(List<Tokenizer.Token> tokens, int offset, int depth) {
        SExpressions.ListSExpr list = new SExpressions.ListSExpr();
        int i = offset;
        while (i < tokens.size() && tokens.get(i).getType() != Tokenizer.TokenType.PAREN_CLOSE) {
            Tokenizer.Token<?> t = tokens.get(i);
            if (t.getType() == Tokenizer.TokenType.PAREN_OPEN) {
                ParseResult childResult = parse(tokens, i + 1, depth + 1);
                i = childResult.offset;
                list.add(childResult.node);
            } else if (t.getType() == Tokenizer.TokenType.NUMBER) {
                Tokenizer.Token<Integer> numberToken = t.as(Tokenizer.TokenType.NUMBER);
                SExpressions.Integer childNode = new SExpressions.Integer(numberToken.getValue());
                list.add(childNode);
            } else if (t.getType() == Tokenizer.TokenType.BOOLEAN) {
                Tokenizer.Token<Boolean> booleanToken = t.as(Tokenizer.TokenType.BOOLEAN);
                SExpressions.Boolean childNode = new SExpressions.Boolean(booleanToken.getValue());
                list.add(childNode);
            } else if (t.getType() == Tokenizer.TokenType.SYMBOL) {
                Tokenizer.Token<String> symbolToken = t.as(Tokenizer.TokenType.SYMBOL);
                String symbol = symbolToken.getValue();

                SExpressions.Symbol childNode = new SExpressions.Symbol(symbol);
                list.add(childNode);

            } else {
                throw new RuntimeException("Unknown token: " + t.toString());
            }
            i++;
        }
        return new ParseResult(list, i);
    }

    static public List<SExpressions.SExpression> parse(List<Tokenizer.Token> tokens) {
        tokens.add(0, Tokenizer.Token.makeToken(Tokenizer.TokenType.PAREN_OPEN, "("));
        tokens.add(Tokenizer.Token.makeToken(Tokenizer.TokenType.PAREN_CLOSE, ")"));
        ParseResult parseResult = parse(tokens, 0, 0);
        if (parseResult.offset != tokens.size()) {
            throw new RuntimeException("Invalid syntax");
        }
        List<SExpressions.SExpression> nodes = ((SExpressions.ListSExpr) parseResult.node.get(0)).getAll();
        System.out.println("Parsed node: " + nodes.toString());
        return nodes;
    }


    // for testing purposes
    public static void main(String[] args) {
        final String input = "((+) (is 2))(define (a) (if (== a 2) 2 3))";
        parse(Tokenizer.tokenize(input));
    }
}