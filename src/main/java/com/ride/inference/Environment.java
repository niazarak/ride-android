package com.ride.inference;

import java.util.*;

public final class Environment {
    private final LinkedList<Map<String, Types.Type>> env = new LinkedList<>();
    private final Map<Types.Type, Types.Type> constraints = new HashMap<>();

    private int i = 0;

    public Environment() {
        push();
    }

    public void push() {
        env.add(new HashMap<>());
    }

    public void pop() {
        env.removeLast();
    }

    public Types.Type lookup(String name) {
        ListIterator<Map<String, Types.Type>> envIterator = env.listIterator(env.size());
        while (envIterator.hasPrevious()) {
            Map<String, Types.Type> localScope = envIterator.previous();
            if (localScope.containsKey(name)) {
                return localScope.get(name);
            }
        }
        return null;
    }

    public Types.Type expose(Types.Type type) {
        Types.Type res = type;
        while (constraints.containsKey(res)) {
            res = constraints.get(res);
        }
        return res;
    }

    public boolean unify(Types.Type a, Types.Type b) {
        // if both literals
        if (a instanceof Types.TLiteral && b instanceof Types.TLiteral) {
            return a == b;
        }

        // if a == var || b == var -> bind variables
        if (a instanceof Types.TVariable) {
            return bind((Types.TVariable) a, b);
        } else if (b instanceof Types.TVariable) {
            return bind((Types.TVariable) b, a);
        }

        // if both functions
        if (a instanceof Types.TFunction && b instanceof Types.TFunction) {
            Types.TFunction aFunc = (Types.TFunction) a;
            Types.TFunction bFunc = (Types.TFunction) b;
            return unify(aFunc.args, bFunc.args) && unify(aFunc.res, bFunc.res);
        }

        return false;
    }

    private boolean unify(List<Types.Type> a, List<Types.Type> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int j = 0; j < a.size(); j++) {
            boolean unificationResult = unify(a.get(j), b.get(j));
            if (!unificationResult) {
                return false;
            }
        }
        return true;
    }

    private boolean bind(Types.TVariable var, Types.Type t) {
        if (constraints.containsKey(var)) {
            return unify(constraints.get(var), t);
        } else {
            constraints.put(var, t);
            return true;
        }
    }

    public void define(String name, Types.Type type) {
        env.getLast().put(name, type);
    }

    public Types.TVariable newvar() {
        return new Types.TVariable(String.valueOf((char) ((i++ % 26) + 97)));
    }
}
