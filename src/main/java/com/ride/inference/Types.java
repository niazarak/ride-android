package com.ride.inference;

import java.util.*;

public class Types {

    public static class TScheme<T extends Type> {
        public final T type;
        public final List<Type> freeVars;

        TScheme(T type, ArrayList<Type> freeVars) {
            this.type = type;
            this.freeVars = freeVars;
        }

        public Type instantiate(final Environment environment) {
            Map<Type, Type> mappings = new HashMap<>();
            for (Type freeVar : freeVars) {
                mappings.put(freeVar, environment.newvar());
            }
            return type.genericCopy(environment, mappings);
        }
    }

    public static abstract class Type {
        public abstract Type expose(Environment env);

        public abstract Set<Type> freeVariables();

        public abstract Type genericCopy(Environment env, Map<Type, Type> mapping);
    }

    public static class TVariable extends Type {
        final String name;

        public TVariable(String name) {
            this.name = name;
        }

        @Override
        public Type expose(Environment env) {
            return env.expose(this);
        }

        @Override
        public Set<Type> freeVariables() {
            HashSet<Type> ftv = new HashSet<>();
            ftv.add(this);
            return ftv;
        }

        @Override
        public Type genericCopy(Environment env, Map<Type, Type> mapping) {
            if (mapping.containsKey(this)) {
                return mapping.get(this);
            }
            return this;
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
        public String toString() {
            return name;
        }
    }

    public static class TFunction extends Type {
        public final List<Type> args;
        public final Type res;

        public TFunction(Type arg, Type res) {
            this.args = Collections.singletonList(arg);
            this.res = res;
        }

        public TFunction(List<Type> args, Type res) {
            this.args = args;
            this.res = res;
        }

        public Type getArg(int i) {
            return args.get(i);
        }

        @Override
        public Type expose(Environment env) {
            return new TFunction(exposeArgs(env), res.expose(env));
        }

        @Override
        public Set<Type> freeVariables() {
            Set<Type> ftvUnion = res.freeVariables();
            for (Type arg : args) {
                ftvUnion.addAll(arg.freeVariables());
            }
            return ftvUnion;
        }

        @Override
        public Type genericCopy(Environment env, Map<Type, Type> mapping) {
            return new TFunction(copyArgs(env, mapping), res.genericCopy(env, mapping));
        }

        private List<Type> exposeArgs(Environment env) {
            List<Type> exposedArgs = new ArrayList<>();
            for (Type arg : args) {
                exposedArgs.add(arg.expose(env));
            }
            return exposedArgs;
        }

        private List<Type> copyArgs(Environment env, Map<Type, Type> mappings) {
            List<Type> copiedArgs = new ArrayList<>();
            for (Type arg : args) {
                copiedArgs.add(arg.genericCopy(env, mappings));
            }
            return copiedArgs;
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
    }

    public static class TLiteral extends Type {
        public static final TLiteral TInt = new TLiteral("Int");
        public static final TLiteral TBool = new TLiteral("Bool");

        private final String name;

        TLiteral(String name) {
            this.name = name;
        }

        @Override
        public Type expose(Environment env) {
            return this;
        }

        @Override
        public Set<Type> freeVariables() {
            return new HashSet<>();
        }

        @Override
        public Type genericCopy(Environment env, Map<Type, Type> mapping) {
            return this;
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
    }


    public static TFunction func(Type arg, Type res) {
        return new TFunction(arg, res);
    }

    public static TFunction func(List<Type> args, Type res) {
        return new TFunction(args, res);
    }

    public static Type typeVar(String name) {
        return new TVariable(name);
    }

    public static List<Type> args(Type... types) {
        return Arrays.asList(types);
    }

    public static TLiteral integer() {
        return TLiteral.TInt;
    }

    public static TLiteral bool() {
        return TLiteral.TBool;
    }
}
