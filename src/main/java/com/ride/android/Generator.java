package com.ride.android;

import com.android.dx.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {
    // for test purposes
    public static void main(String[] args) throws IOException {
        // final String input = "(/ (+ 9 6) (% 7 4))";
        // final String input = "(< 5 10)";
        // final String input = "(== 1 (> 1 2))";
        // final String input = "(xor #t 2)";
//        final String input = "(+ 12 (if (> 5 10) 1 0))";
        final String input = "(+ 12 (if (> 5 10) 1 0))(+ 2 2)(+ 2 (if (> 5 10) 1 0))";
        FileOutputStream dexResult = new FileOutputStream("classes.dex");

        byte[] program = generate(Parser.parse(Parser.tokenize(input)));

        dexResult.write(program);
        dexResult.flush();
        dexResult.close();
    }

    static byte[] generate(final List<Parser.Node> nodes) {
        Module module = new Module();
        for (Parser.Node node : nodes) {
            LocalWrapper target = module.getOrCreateLocal(0);
            Expr expr = generateExpression(module, node, target);
            if (expr.type == Exception.class) {
                throw new RuntimeException("Compilation failed");
            }

            TypeId<System> systemType = TypeId.get(System.class);
            TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);
            FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
            MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                    TypeId.VOID, "println", TypeId.INT);

            LocalWrapper systemOutLocal = module.getOrCreateLocal(target.getPos() + 1, printStreamType);
            module.sget(systemOutField, systemOutLocal);
            module.invokeVirtual(printlnMethod, systemOutLocal, target);
        }
        return module.compile();
    }

    private static Expr generateExpression(final Module module, final Parser.Node node, final LocalWrapper target) {
        if (node instanceof Parser.NumberNode) {
            return generateNumber(module, (Parser.NumberNode) node, target);
        } else if (node instanceof Parser.BooleanNode) {
            return generateBoolean(module, (Parser.BooleanNode) node, target);
        } else if (node instanceof Parser.ListNode) {
            return generateListExpression(module, (Parser.ListNode) node, target);
        } else {
            throw new RuntimeException("Top level symbols not supported");
        }
    }

    static class Expr {
        private final Object type;

        public Expr(final Object type) {
            this.type = type;
        }
    }

    interface ExprGenerator {
        Expr run(final Module module, final List<Parser.Node> nodes, final LocalWrapper target);
    }

    private static Map<String, ExprGenerator> exprGenerators = new HashMap<>();

    static {
        exprGenerators.put("+", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.add(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("-", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.subtract(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("*", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.multiply(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("/", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.divide(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("%", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.remainder(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("if", (module, nodes, target) -> {
            Label thenLabel = new Label();
            Label afterLabel = new Label();
            LocalWrapper ifResult = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprIf = generateExpression(module, nodes.get(1), ifResult);
            if (exprIf.type != Boolean.class) {
                return new Expr(Exception.class);
            }
            // if
            module.compareZ(thenLabel, ifResult);

            // else
            LocalWrapper elseResult = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprElse = generateExpression(module, nodes.get(2), elseResult);
            module.move(target, elseResult);
            module.jump(afterLabel);

            // then
            module.markLabel(thenLabel);
            LocalWrapper thenResult = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprThen = generateExpression(module, nodes.get(3), thenResult);
            module.move(target, thenResult);

            if (exprElse.type != Integer.class || !exprElse.type.equals(exprThen.type)) {
                return new Expr(Exception.class);
            }
            // after
            module.markLabel(afterLabel);
            return new Expr(Integer.class);
        });
        exprGenerators.put("!=", (module, nodes, target) -> {
            Comparison comparison = Comparison.NE;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("==", (module, nodes, target) -> {
            Comparison comparison = Comparison.EQ;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put(">=", (module, nodes, target) -> {
            Comparison comparison = Comparison.GE;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put(">", (module, nodes, target) -> {
            Comparison comparison = Comparison.GT;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("<=", (module, nodes, target) -> {
            Comparison comparison = Comparison.LE;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("<", (module, nodes, target) -> {
            Comparison comparison = Comparison.LT;
            return generateComparison(module, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("and", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.and(target, a, b);
            return new Expr(Boolean.class);
        });
        exprGenerators.put("or", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.or(target, a, b);
            return new Expr(Boolean.class);
        });
        exprGenerators.put("xor", (module, nodes, target) -> {
            LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(module, nodes.get(1), a);
            LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(module, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            module.xor(target, a, b);
            return new Expr(Boolean.class);
        });

    }

    private static Expr generateComparison(Module module, LocalWrapper target, Comparison comparison, Parser.Node nodeA, Parser.Node nodeB) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();
        LocalWrapper a = module.getOrCreateLocal(target.getPos() + 1);
        Expr exprA = generateExpression(module, nodeA, a);
        LocalWrapper b = module.getOrCreateLocal(target.getPos() + 2);
        Expr exprB = generateExpression(module, nodeB, b);
        if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
            return new Expr(Exception.class);
        }
        // if
        module.compare(comparison, thenLabel, a, b);

        // else
        module.load(target, 0);
        module.jump(afterLabel);

        // then
        module.markLabel(thenLabel);
        module.load(target, 1);

        // after
        module.markLabel(afterLabel);
        return new Expr(Boolean.class);
    }

    private static Expr generateListExpression(final Module module, final Parser.ListNode node, final LocalWrapper target) {
        if (!(node.getChild(0) instanceof Parser.SymbolNode)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Parser.SymbolNode func = (Parser.SymbolNode) node.getChild(0);
        String symbol = func.symbol;
        ExprGenerator exprGenerator = exprGenerators.get(symbol);
        if (exprGenerator != null) {
            return exprGenerator.run(module, node.getChildren(), target);
        } else {
            throw new RuntimeException("Unknown symbol \"" + func.symbol + "\"");
        }
    }

    private static Expr generateNumber(final Module module, final Parser.NumberNode node, final LocalWrapper target) {
        module.load(target, node.number);
        return new Expr(Integer.class);
    }

    private static Expr generateBoolean(final Module module, final Parser.BooleanNode node, final LocalWrapper target) {
        module.load(target, node.value ? 1 : 0);
        return new Expr(Boolean.class);
    }
}
