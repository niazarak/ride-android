package com.ride.android.ast;

import com.ride.android.parser.SExpressions;

import java.util.ArrayList;
import java.util.List;

public class Ast {

    public static List<Expression> ast(List<SExpressions.SExpression> nodes) {
        List<Expression> expressions = new ArrayList<>();
        for (SExpressions.SExpression node : nodes) {
            expressions.add(transform(node, 0));
        }
        System.out.println("Transformed expresions: " + expressions.toString());
        return expressions;
    }

    private static Expression transform(SExpressions.SExpression expr, int depth) {
        if (expr instanceof SExpressions.Boolean) {
            return new Expressions.Bool(((SExpressions.Boolean) expr).value);
        } else if (expr instanceof SExpressions.Integer) {
            return new Expressions.Int(((SExpressions.Integer) expr).value);
        } else if (expr instanceof SExpressions.Symbol) {
            return new Expressions.Variable(((SExpressions.Symbol) expr).name);
        }
        SExpressions.ListSExpr listExpr = (SExpressions.ListSExpr) expr;
        if (listExpr.getAll().isEmpty()) {
            throw new RuntimeException("Empty list expression");
        }

        // check first expr
        SExpressions.SExpression firstExpr = listExpr.get(0);
        if (firstExpr instanceof SExpressions.Symbol) {
            SExpressions.Symbol firstVar = (SExpressions.Symbol) firstExpr;
            switch (firstVar.name) {
                case "define": {
                    return transformToDefine(listExpr, depth);
                }
                case "lambda": {
                    return transformToLambda(listExpr, depth);
                }
                case "if": {
                    return transformToIf(listExpr, depth);
                }
                default: {
                    return transformToApplication(listExpr, depth);
                }
            }
        } else if (firstExpr instanceof SExpressions.ListSExpr) {
            return transformToApplication(listExpr, depth);
        }
        throw new RuntimeException("Atoms are not callable");
    }

    private static Expression transformToApplication(SExpressions.ListSExpr listExpr, int depth) {
        List<Expression> args = new ArrayList<>();
        for (int i = 1; i < listExpr.getAll().size(); i++) {
            args.add(transform(listExpr.get(i), depth + 1));
        }
        return new Expressions.Application(transform(listExpr.get(0), depth + 1), args);
    }

    private static Expressions.IfExpr transformToIf(SExpressions.ListSExpr expr, int depth) {
        if (expr.getAll().size() != 4) {
            throw new RuntimeException("Incorrect condition: should have 3 children");
        }

        return new Expressions.IfExpr(
                transform(expr.get(1), depth + 1),
                transform(expr.get(2), depth + 1),
                transform(expr.get(3), depth + 1)
        );
    }

    private static Expressions.Definition transformToDefine(SExpressions.ListSExpr expr, int depth) {
        if (depth != 0) {
            throw new RuntimeException("Incorrect definition: should be top-level declaration");
        }
        if (expr.getAll().size() != 3) {
            throw new RuntimeException("Incorrect definition: should have 3 children");
        }
        if (!(expr.get(1) instanceof SExpressions.ListSExpr)) {
            throw new RuntimeException("Incorrect definition: should specify contract in list");
        }
        SExpressions.ListSExpr contract = (SExpressions.ListSExpr) expr.get(1);
        if (!(contract.get(0) instanceof SExpressions.Symbol)) {
            throw new RuntimeException("Incorrect definition: function name should be a symbol");
        }
        String name = ((SExpressions.Symbol) contract.get(0)).name;
        List<String> args = new ArrayList<>();
        for (int i = 1; i < contract.getAll().size(); i++) {
            if (!(contract.get(i) instanceof SExpressions.Symbol)) {
                throw new RuntimeException("Incorrect definition: function arg should be a symbol");
            }
            SExpressions.Symbol arg = (SExpressions.Symbol) contract.get(i);
            args.add(arg.name);
        }
        return new Expressions.Definition(name, args, transform(expr.get(2), depth + 1));
    }

    private static Expressions.Lambda transformToLambda(SExpressions.ListSExpr expr, int depth) {
        if (expr.getAll().size() != 3) {
            throw new RuntimeException("Incorrect lambda: should have 3 children");
        }
        if (!(expr.get(1) instanceof SExpressions.ListSExpr)) {
            throw new RuntimeException("Incorrect lambda: should specify contract in list");
        }
        SExpressions.ListSExpr contract = (SExpressions.ListSExpr) expr.get(1);
        List<String> args = new ArrayList<>();
        for (int i = 0; i < contract.getAll().size(); i++) {
            if (!(contract.get(i) instanceof SExpressions.Symbol)) {
                throw new RuntimeException("Incorrect lambda: function arg should be a symbol");
            }
            SExpressions.Symbol arg = (SExpressions.Symbol) contract.get(i);
            args.add(arg.name);
        }
        return new Expressions.Lambda(args, transform(expr.get(2), depth + 1));
    }
}
