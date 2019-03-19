package com.ride.inference;

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
        final Type arg, res;

        public TFunction(Type arg, Type res) {
            this.arg = arg;
            this.res = res;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TFunction tFunction = (TFunction) o;
            return Objects.equals(arg, tFunction.arg) &&
                    Objects.equals(res, tFunction.res);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arg, res);
        }

        @Override
        public String toString() {
            return "(" + arg + " => " + res + ')';
        }

        @Override
        Type expose(Environment env) {
            return new TFunction(arg.expose(env), res.expose(env));
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
