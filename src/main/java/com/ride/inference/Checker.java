package com.ride.inference;

public class Checker {
    public static void main(String[] args) {
        Expressions.Expression e = new Expressions.EAbstraction(
                "x", new Expressions.EVariable("x")
        );
        Environment env = new Environment();
        Types.Type inferResult = e.infer(env);
        reportResult(inferResult, env);

        e = new Expressions.EApplication(
                new Expressions.EAbstraction("x", new Expressions.EVariable("x")),
                new Expressions.EAbstraction("y", new Expressions.EVariable("y"))
        );
        env = new Environment();
        inferResult = e.infer(env);
        reportResult(inferResult, env);

        e = new Expressions.EVariable("x");
        env = new Environment();
        env.define("x", new Types.TVariable("t"));
        inferResult = e.infer(env);
        reportResult(inferResult, env);


        e = new Expressions.EApplication(
                new Expressions.EApplication(new Expressions.EVariable("+"), new Expressions.ELiteral(2)),
                new Expressions.ELiteral(2)
        );
        env = new Environment();
        env.define("+", new Types.TFunction(Types.TLiteral.TInt,
                new Types.TFunction(Types.TLiteral.TInt,
                        Types.TLiteral.TInt)));
        inferResult = e.infer(env);
        reportResult(inferResult, env);

        e = new Expressions.EAbstraction("x", new Expressions.ELiteral(2));
        env = new Environment();
        inferResult = e.infer(env);
        reportResult(inferResult, env);
    }

    private static void reportResult(Types.Type inferResult, Environment env) {
        System.out.println("Result type: " + inferResult + ", exposed: " + env.expose(inferResult));
    }
}
