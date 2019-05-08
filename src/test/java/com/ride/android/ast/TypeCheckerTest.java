package com.ride.android.ast;

import com.ride.inference.Environment;
import com.ride.inference.Types;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.ride.inference.Types.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TypeCheckerTest {
    private Environment environment;

    @Before
    public void setUp() {
        environment = new Environment();
    }

    @Test
    public void testIdentity() {
        // given
        Expression e = lambda(
                "x", var("x")
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
        assertEquals(((Types.TFunction) result).args.size(), 1);
        assertEquals(((Types.TFunction) result).args.get(0),
                ((Types.TFunction) result).res);
    }

    @Test
    public void testApplicationOfAbstractions() {
        // given
        Expression e = apply(
                lambda("x", var("x")),
                lambda("y", var("y"))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
    }

    @Test(expected = RuntimeException.class)
    public void testUnboundVar() {
        // given
        Expression e = var("x");

        // when
        e.infer(environment);

        // then fail
    }

    @Test
    public void testVar() {
        // given
        Expression e = var("x");
        environment.define("x", typeVar("t"));

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TVariable.class));
    }

    @Test
    public void testPlusWithTwoNumbers() {
        // given
        environment.define("+", func(integer(), func(integer(), integer()))); // int -> int -> int
        Expression e = apply(
                apply(var("+"), literal(2)), literal(2)
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TLiteral.class));
    }

    @Test
    public void testApplicationOfPlus() {
        // given
        environment.define("+", func(integer(), func(integer(), integer()))); // int -> int -> int
        Expression e = lambda("y",
                lambda("x", apply(
                        apply(var("+"), var("x")),
                        var("y")
                ))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
        assertEquals(result, func(integer(), func(integer(), integer())));
    }

    @Test
    public void testApplicationOfNaryPlus() {
        // given
        environment.define("+", func(list(integer(), integer()), integer())); // [int x int] -> int
        Expression e = lambda(list("x", "y"),
                apply(var("+"), list(var("x"), var("y")))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
        assertEquals(result, func(list(integer(), integer()), integer()));
    }

    @Test
    public void testApplicationOfNaryPlus2() {
        // given
        environment.define("+", func(list(integer(), integer()), integer())); // [int x int] -> int
        Expression e = apply(
                lambda(
                        list("y", "x"),
                        apply(var("+"), list(var("x"), var("y")))
                ),
                list(literal(2), literal(2))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TLiteral.class));
        assertEquals(result, integer());
    }

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