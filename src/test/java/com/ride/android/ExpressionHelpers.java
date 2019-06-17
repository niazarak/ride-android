package com.ride.android;

import com.ride.android.ast.Expression;
import com.ride.android.ast.Expressions;

import java.util.Arrays;
import java.util.List;

public class ExpressionHelpers {
    // expression helpers
    public static Expression lambda(String arg, Expression body) {
        return new Expressions.Lambda(list(arg), body);
    }

    public static Expression lambda(List<String> args, Expression body) {
        return new Expressions.Lambda(args, body);
    }

    public static Expressions.Application apply(Expression fun, Expression arg) {
        return new Expressions.Application(fun, list(arg));
    }

    public static Expressions.Application apply(Expression fun, List<Expression> args) {
        return new Expressions.Application(fun, args);
    }

    public static Expressions.Let let(String var, Expression varExpr, Expression body) {
        return new Expressions.Let(var, varExpr, body);
    }

    public static Expressions.LetRec letrec(String name, Expression recExpr, Expression body) {
        return new Expressions.LetRec(name, recExpr, body);
    }

    public static Expressions.IfExpr cond(Expression cond, Expression thenExpr, Expression elseExpr) {
        return new Expressions.IfExpr(cond, thenExpr, elseExpr);
    }

    public static Expressions.Int literal(int value) {
        return new Expressions.Int(value);
    }

    public static Expressions.Variable var(String y) {
        return new Expressions.Variable(y);
    }

    public static <T> List<T> list(T... args) {
        return Arrays.asList(args);
    }

}
