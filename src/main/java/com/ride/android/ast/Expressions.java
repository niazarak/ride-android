package com.ride.android.ast;

import java.util.List;

public class Expressions {
    public static class Application extends Expression {
        private final List<Expression> args;
        private Expression function;

        public Application(Expression function, List<Expression> args) {
            this.function = function;
            this.args = args;
        }

        public Expression getFunction() {
            return function;
        }

        public Expression getArg(int i) {
            return args.get(i);
        }

        public List<Expression> getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return "App{" +
                    "function=" + function +
                    ", args=" + args +
                    '}';
        }
    }

    public static class Definition extends Expression {
        private final String name;
        private final List<String> args;
        private final Expression body;

        public Definition(String name, List<String> args, Expression body) {
            this.name = name;
            this.args = args;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Def{" +
                    "name='" + name + '\'' +
                    ", args=" + args +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Lambda extends Expression {
        public final List<String> args;
        public final Expression body;

        public Lambda(List<String> args, Expression body) {
            this.args = args;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Lambda{" +
                    "args=" + args +
                    ", body=" + body +
                    '}';
        }
    }

    public static class IfExpr extends Expression {
        public final Expression condition, ifBranch, elseBranch;

        public IfExpr(Expression condition, Expression ifBranch, Expression elseBranch) {
            this.condition = condition;
            this.ifBranch = ifBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public String toString() {
            return "IfExpr{" +
                    "condition=" + condition +
                    ", ifBranch=" + ifBranch +
                    ", elseBranch=" + elseBranch +
                    '}';
        }
    }

    public static class Int extends Expression {
        public final int number;

        public Int(int number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "Int{" + number + '}';
        }
    }

    public static class Bool extends Expression {
        public final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Bool{" + value + '}';
        }
    }

    public static class Variable extends Expression {
        public final String name;

        public Variable(String symbol) {
            this.name = symbol;
        }

        @Override
        public String toString() {
            return "Var{" + name + '}';
        }
    }
}
