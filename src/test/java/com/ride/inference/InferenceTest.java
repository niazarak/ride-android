package com.ride.inference;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.ride.inference.Types.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class InferenceTest {
    private Environment environment;

    @Before
    public void setUp() {
        environment = new Environment();
    }

    @Test
    public void testIdentity() {
        // given
        Expressions.Expression e = new Expressions.EAbstraction(
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
        Expressions.Expression e = new Expressions.EApplication(
                new Expressions.EAbstraction("x", var("x")),
                new Expressions.EAbstraction("y", var("y"))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
    }

    @Test(expected = Expressions.InferException.class)
    public void testUnboundVar() {
        // given
        Expressions.Expression e = var("x");

        // when
        e.infer(environment);

        // then fail
    }

    @Test
    public void testVar() {
        // given
        Expressions.Expression e = var("x");
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
        Expressions.Expression e = apply(
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
        Expressions.Expression e = lambda("y",
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
        environment.define("+", func(args(integer(), integer()), integer())); // [int x int] -> int
        Expressions.Expression e = lambda(boundVars("y", "x"),
                apply(var("+"), applyArgs(var("x"), var("y")))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TFunction.class));
        assertEquals(result, func(args(integer(), integer()), integer()));
    }

    @Test
    public void testApplicationOfNaryPlus2() {
        // given
        environment.define("+", func(args(integer(), integer()), integer())); // [int x int] -> int
        Expressions.Expression e = apply(
                lambda(
                        boundVars("y", "x"),
                        apply(var("+"), applyArgs(var("x"), var("y")))
                ),
                applyArgs(literal(2), literal(2))
        );

        // when
        Types.Type result = e.infer(environment).expose(environment);

        // then
        assertThat(result, instanceOf(Types.TLiteral.class));
        assertEquals(result, integer());
    }


    // expression helpers
    private Expressions.Expression lambda(String arg, Expressions.Expression body) {
        return new Expressions.EAbstraction(arg, body);
    }

    private Expressions.Expression lambda(List<String> args, Expressions.Expression body) {
        return new Expressions.EAbstraction(args, body);
    }

    private List<String> boundVars(String... vars) {
        return Arrays.asList(vars);
    }

    private Expressions.EApplication apply(Expressions.Expression fun, Expressions.Expression arg) {
        return new Expressions.EApplication(fun, arg);
    }

    private Expressions.EApplication apply(Expressions.Expression fun, List<Expressions.Expression> args) {
        return new Expressions.EApplication(fun, args);
    }

    private List<Expressions.Expression> applyArgs(Expressions.Expression... expressions) {
        return Arrays.asList(expressions);
    }

    private Expressions.ELiteral literal(int value) {
        return new Expressions.ELiteral(value);
    }

    private Expressions.EVariable var(String y) {
        return new Expressions.EVariable(y);
    }

}
