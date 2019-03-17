package com.ride.inference;

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
        Expression function, arg;

        public EApplication(Expression function, Expression arg) {
            this.function = function;
            this.arg = arg;
        }

        @Override
        Types.Type infer(Environment environment) {
            Types.Type functionType = function.infer(environment);
            Types.Type argType = arg.infer(environment);

            Types.Type resultType = environment.newvar();
            Types.TFunction unifiedFunctionType = new Types.TFunction(argType, resultType);
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
        String arg;
        Expression body;

        public EAbstraction(String arg, Expression body) {
            this.arg = arg;
            this.body = body;
        }

        @Override
        Types.Type infer(Environment environment) {
            environment.push();
            Types.Type argType = environment.newvar();
            environment.define(arg, argType);
            Types.TFunction resultFunctionType = new Types.TFunction(argType, body.infer(environment));
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
