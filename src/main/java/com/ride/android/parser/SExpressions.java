package com.ride.android.parser;

import java.util.ArrayList;
import java.util.List;

public class SExpressions {
    public static class SExpression {
    }

    public static class ListSExpr extends SExpression {
        List<SExpression> exprs = new ArrayList<>();

        public void add(SExpression sexpr) {
            exprs.add(sexpr);
        }

        public SExpression get(int i) {
            return exprs.get(i);
        }

        public List<SExpression> getAll() {
            return exprs;
        }
    }

    public static class Symbol extends SExpression {
        public final String name;

        Symbol(String name) {
            this.name = name;
        }
    }

    public static class Boolean extends SExpression {
        public final boolean value;

        Boolean(boolean value) {
            this.value = value;
        }
    }

    public static class Integer extends SExpression {
        public final int value;

        Integer(int value) {
            this.value = value;
        }
    }

}
