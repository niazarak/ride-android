package com.ride.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Expressions {

    static abstract class Expression {
        abstract Types.Type infer(Environment environment);
    }

    static class EVariable extends Expression {
        String name;

        public EVariable(String name) {
            this.name = name;
        }

        @Override
        Types.Type infer(Environment environment) {
            Types.Type varType = environment.lookup(name);
            if (varType == null) {
                throw new InferException(name + " is not found in environment");
            }
            return varType;
        }
    }

    static class EApplication extends Expression {
        final Expression function;
        final List<Expression> args;

        public EApplication(Expression function, Expression arg) {
            this.function = function;
            this.args = Collections.singletonList(arg);
        }

        public EApplication(Expression function, List<Expression> args) {
            this.function = function;
            this.args = new ArrayList<>(args);
        }

        @Override
        Types.Type infer(Environment environment) {
            Types.Type functionType = function.infer(environment);
            List<Types.Type> argsType = new ArrayList<>();
            for (Expression arg : args) {
                argsType.add(arg.infer(environment));
            }
            Types.Type resultType = environment.newvar();
            Types.TFunction unifiedFunctionType = new Types.TFunction(argsType, resultType);
            if (environment.unify(functionType, unifiedFunctionType))
                return resultType;
            else
                throw new InferException(unifiedFunctionType + " and " + functionType + " cannot be unified");
        }
    }

    static class ELiteral extends Expression {
        private Integer value;
        private Boolean boolValue;

        public ELiteral(int value) {
            this.value = value;
        }

        public ELiteral(boolean value) {
            this.boolValue = value;
        }

        @Override
        Types.Type infer(Environment environment) {
            return value != null ? Types.TLiteral.TInt : Types.TLiteral.TBool;
        }
    }

    static class EAbstraction extends Expression {
        List<String> args;
        Expression body;

        public EAbstraction(String arg, Expression body) {
            this.args = Collections.singletonList(arg);
            this.body = body;
        }

        public EAbstraction(List<String> args, Expression body) {
            this.args = new ArrayList<>(args);
            this.body = body;
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
            environment.pop();
            return resultFunctionType;
        }
    }

    static class InferException extends RuntimeException {
        public InferException(String message) {
            super(message);
        }
    }
}
