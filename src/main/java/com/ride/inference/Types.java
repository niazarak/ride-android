package com.ride.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Types {
    static abstract class Type {
        abstract Type expose(Environment env);
    }

    static class TVariable extends Type {
        final String name;

        @Override
        public String toString() {
            return name;
        }

        public TVariable(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TVariable tVariable = (TVariable) o;
            return Objects.equals(name, tVariable.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        Type expose(Environment env) {
            return env.expose(this);
        }
    }

    static class TFunction extends Type {
        final List<Type> args;
        final Type res;

        public TFunction(Type arg, Type res) {
            this.args = Collections.singletonList(arg);
            this.res = res;
        }

        public TFunction(List<Type> args, Type res) {
            this.args = args;
            this.res = res;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TFunction tFunction = (TFunction) o;
            return Objects.equals(args, tFunction.args) &&
                    Objects.equals(res, tFunction.res);
        }

        @Override
        public int hashCode() {
            return Objects.hash(args, res);
        }

        @Override
        public String toString() {
            return "(" + args + " => " + res + ')';
        }

        @Override
        Type expose(Environment env) {
            return new TFunction(exposeArgs(env), res.expose(env));
        }

        private List<Type> exposeArgs(Environment env) {
            List<Type> exposedArgs = new ArrayList<>();
            for (Type arg : args) {
                exposedArgs.add(arg.expose(env));
            }
            return exposedArgs;
        }
    }

    static class TLiteral extends Type {
        public static final TLiteral TInt = new TLiteral("Int");
        public static final TLiteral TBool = new TLiteral("Bool");

        private final String name;

        TLiteral(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TLiteral tLiteral = (TLiteral) o;
            return Objects.equals(name, tLiteral.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        Type expose(Environment env) {
            return this;
        }
    }
}
