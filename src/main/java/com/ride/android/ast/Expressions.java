package com.ride.android.ast;

import com.ride.inference.Environment;
import com.ride.inference.Types;

import java.util.ArrayList;
import java.util.List;

import static com.ride.inference.Types.bool;
import static com.ride.inference.Types.integer;

public class Expressions {
    public static class Application extends Expression {
        private final List<Expression> args;
        public final Expression function;

        public Application(Expression function, List<Expression> args) {
            this.function = function;
            this.args = args;
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
                    "} : " + type;
        }

        @Override
        Types.Type infer(Environment env) {
            Types.Type functionType = function.infer(env);
            List<Types.Type> argsType = new ArrayList<>();
            for (Expression arg : args) {
                argsType.add(arg.infer(env));
            }
            Types.Type resultType = env.newvar();
            Types.TFunction unifiedFunctionType = new Types.TFunction(argsType, resultType);
            if (env.unify(functionType, unifiedFunctionType)) {
                type = resultType.expose(env);
                return resultType;
            } else
                throw new RuntimeException(unifiedFunctionType + " and " + functionType + " cannot be unified");
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
                    "} : " + type;
        }

        @Override
        Types.Type infer(Environment environment) {
            environment.push();
            List<Types.Type> argsTypes = new ArrayList<>();
            for (String arg : args) {
                Types.Type argType = environment.newvar();
                environment.define(arg, argType);
                argsTypes.add(argType);
            }
            Types.TFunction resultFunctionType = new Types.TFunction(argsTypes, body.infer(environment));
            type = resultFunctionType.expose(environment);
            environment.pop();
            environment.define(name, type);
            return resultFunctionType;
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
                    "} : " + type;
        }

        @Override
        Types.Type infer(Environment environment) {
            environment.push();
            List<Types.Type> argsTypes = new ArrayList<>();
            for (String arg : args) {
                Types.Type argType = environment.newvar();
                environment.define(arg, argType);
                argsTypes.add(argType);
            }
            Types.TFunction resultFunctionType = new Types.TFunction(argsTypes, body.infer(environment));
            type = resultFunctionType.expose(environment);
            environment.pop();
            return resultFunctionType;
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
                    "} : " + type;
        }

        @Override
        Types.Type infer(Environment env) {
            Types.Type conditionType = condition.infer(env);
            if (!conditionType.expose(env).equals(bool())) {
                throw new RuntimeException("Condition must be boolean");
            }
            Types.Type ifType = ifBranch.infer(env);
            Types.Type elseType = elseBranch.infer(env);
            if (!ifType.expose(env).equals(elseType.expose(env))) {
                throw new RuntimeException("Condition branches must have same types");
            }
            type = ifType.expose(env);
            return ifType;
        }
    }

    public static class Int extends Expression {
        public final int number;

        public Int(int number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "Int{" + number + "} : " + type;
        }

        @Override
        Types.Type infer(Environment env) {
            Types.Type resultType = integer();
            type = resultType;
            return resultType;
        }
    }

    public static class Bool extends Expression {
        public final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Bool{" + value + "} : " + type;
        }

        @Override
        Types.Type infer(Environment env) {
            Types.Type resultType = bool();
            type = resultType;
            return resultType;
        }
    }

    public static class Variable extends Expression {
        public final String name;

        public Variable(String symbol) {
            this.name = symbol;
        }

        @Override
        public String toString() {
            return "Var{" + name + "} : " + type;
        }

        @Override
        Types.Type infer(Environment environment) {
            Types.Type varType = environment.lookup(name);
            if (varType == null) {
                throw new RuntimeException(name + " is not found in environment");
            }
            type = varType.expose(environment);
            return varType;
        }
    }
}
