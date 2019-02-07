package com.ride.android;

import com.android.dx.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
        final String input = "(+ 12 (if (> 5 10) 1 0))";
        FileOutputStream dexResult = new FileOutputStream("classes.dex");

        byte[] program = generate(Parser.parse(Parser.tokenize(input)));

        dexResult.write(program);
        dexResult.flush();
        dexResult.close();
    }

    static class Local<T> {
        private com.android.dx.Local<T> realLocal;
        private TypeId<T> typeId;
        private int pos;

        public Local(int pos) {
            this.pos = pos;
            this.typeId = (TypeId<T>) TypeId.INT;
        }

        public com.android.dx.Local<T> getRealLocal() {
            if (realLocal == null) {
                throw new RuntimeException("Accessing locals before generating them is forbidden");
            }
            return realLocal;
        }

        public void generate(Code code) {
            realLocal = code.newLocal(typeId);
        }
    }

    static class Document {
        private List<Local> locals = new ArrayList<>();

        private List<Instructions.Instruction> instructions = new ArrayList<>();

        Local getOrCreateLocal(final int finalPos) {
            if (locals.size() == finalPos) {
                Local local = new Local<>(finalPos);
                locals.add(local);
                return local;
            } else if (finalPos > locals.size()) {
                throw new RuntimeException("Somehow registers do not increment");
            }
            if (locals.get(finalPos) == null) {
                locals.set(finalPos, new Local<>(finalPos));
            }
            return locals.get(finalPos);
        }

        void move(Local dest, Local target) {
            instructions.add(code -> code.move(dest.getRealLocal(), target.getRealLocal()));
        }

        void add(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.ADD, dest, a, b));
        }

        void subtract(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.SUBTRACT, dest, a, b));
        }

        void multiply(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.MULTIPLY, dest, a, b));
        }

        void divide(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.DIVIDE, dest, a, b));
        }

        void remainder(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.REMAINDER, dest, a, b));
        }

        void and(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.AND, dest, a, b));
        }

        void or(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.OR, dest, a, b));
        }

        void xor(Local dest, Local a, Local b) {
            instructions.add(new Instructions.BinaryInstruction(BinaryOp.XOR, dest, a, b));
        }

        void compare(Comparison comparison, Label trueLabel, Local a, Local b) {
            instructions.add(new Instructions.CompareInstruction(comparison, trueLabel, a, b));
        }

        void compareZ(Label trueLabel, Local a) {
            instructions.add(new Instructions.CompareZInstruction(Comparison.EQ, trueLabel, a));
        }

        void jump(Label label) {
            instructions.add(new Instructions.JumpInstruction(label));
        }

        void markLabel(Label label) {
            instructions.add((code -> code.mark(label)));
        }

        void load(Local dest, int constant) {
            instructions.add(new Instructions.LoadInstruction(dest, constant));
        }

        TypeId<System> systemType = TypeId.get(System.class);
        TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);

        byte[] compile() {
            DexMaker maker = new DexMaker();
            TypeId<?> mainClassType = TypeId.get("LMain;");
            maker.declare(mainClassType, "Main.compiled", Modifier.PUBLIC, TypeId.OBJECT);

            MethodId mainMethodType = mainClassType.getMethod(TypeId.VOID, "main", TypeId.get(String[].class));
            Code code = maker.declare(mainMethodType, Modifier.STATIC | Modifier.PUBLIC);

            com.android.dx.Local<PrintStream> localSystemOut = code.newLocal(printStreamType);

            // System.out.println("Locals to compile:" + locals);
            for (Local local : locals) {
                local.generate(code);
            }
            // System.out.println("Insns to compile:" + instructions);
            for (Instructions.Instruction instruction : instructions) {
                instruction.generate(code);
            }

            FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
            code.sget(systemOutField, localSystemOut);
            MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                    TypeId.VOID, "println", TypeId.INT);
            code.invokeVirtual(printlnMethod, null, localSystemOut, locals.get(0).getRealLocal());

            code.returnVoid();

            return maker.generate();
        }
    }

    static byte[] generate(final Parser.Node node) {
        Document document = new Document();
        Local target = document.getOrCreateLocal(0);
        Expr expr = generateExpression(document, node, target);
        if (expr.type == Exception.class) {
            throw new RuntimeException("Compilation failed");
        } else {
            return document.compile();
        }
    }

    private static Expr generateExpression(final Document document, final Parser.Node node, final Local target) {
        if (node instanceof Parser.NumberNode) {
            return generateNumber(document, (Parser.NumberNode) node, target);
        } else if (node instanceof Parser.BooleanNode) {
            return generateBoolean(document, (Parser.BooleanNode) node, target);
        } else if (node instanceof Parser.ListNode) {
            return generateListExpression(document, (Parser.ListNode) node, target);
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
        Expr run(final Document document, final List<Parser.Node> nodes, final Local target);
    }

    private static Map<String, ExprGenerator> exprGenerators = new HashMap<>();

    static {
        exprGenerators.put("+", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.add(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("-", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.subtract(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("*", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.multiply(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("/", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.divide(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("%", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.remainder(target, a, b);
            return new Expr(Integer.class);
        });
        exprGenerators.put("if", (document, nodes, target) -> {
            Label thenLabel = new Label();
            Label afterLabel = new Label();
            Local ifResult = document.getOrCreateLocal(target.pos + 1);
            Expr exprIf = generateExpression(document, nodes.get(1), ifResult);
            if (exprIf.type != Boolean.class) {
                return new Expr(Exception.class);
            }
            // if
            document.compareZ(thenLabel, ifResult);

            // else
            Local elseResult = document.getOrCreateLocal(target.pos + 1);
            Expr exprElse = generateExpression(document, nodes.get(2), elseResult);
            document.move(target, elseResult);
            document.jump(afterLabel);

            // then
            document.markLabel(thenLabel);
            Local thenResult = document.getOrCreateLocal(target.pos + 1);
            Expr exprThen = generateExpression(document, nodes.get(3), thenResult);
            document.move(target, thenResult);

            if (exprElse.type != Integer.class || !exprElse.type.equals(exprThen.type)) {
                return new Expr(Exception.class);
            }
            // after
            document.markLabel(afterLabel);
            return new Expr(Integer.class);
        });
        exprGenerators.put("!=", (document, nodes, target) -> {
            Comparison comparison = Comparison.NE;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("==", (document, nodes, target) -> {
            Comparison comparison = Comparison.EQ;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put(">=", (document, nodes, target) -> {
            Comparison comparison = Comparison.GE;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put(">", (document, nodes, target) -> {
            Comparison comparison = Comparison.GT;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("<=", (document, nodes, target) -> {
            Comparison comparison = Comparison.LE;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("<", (document, nodes, target) -> {
            Comparison comparison = Comparison.LT;
            return generateComparison(document, target, comparison, nodes.get(1), nodes.get(2));
        });
        exprGenerators.put("and", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.and(target, a, b);
            return new Expr(Boolean.class);
        });
        exprGenerators.put("or", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.or(target, a, b);
            return new Expr(Boolean.class);
        });
        exprGenerators.put("xor", (document, nodes, target) -> {
            Local a = document.getOrCreateLocal(target.pos + 1);
            Expr exprA = generateExpression(document, nodes.get(1), a);
            Local b = document.getOrCreateLocal(target.pos + 2);
            Expr exprB = generateExpression(document, nodes.get(2), b);
            if (exprA.type != Boolean.class || !exprA.type.equals(exprB.type)) {
                return new Expr(Exception.class);
            }
            document.xor(target, a, b);
            return new Expr(Boolean.class);
        });

    }

    private static Expr generateComparison(Document document, Local target, Comparison comparison, Parser.Node nodeA, Parser.Node nodeB) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();
        Local a = document.getOrCreateLocal(target.pos + 1);
        Expr exprA = generateExpression(document, nodeA, a);
        Local b = document.getOrCreateLocal(target.pos + 2);
        Expr exprB = generateExpression(document, nodeB, b);
        if (exprA.type != Integer.class || !exprA.type.equals(exprB.type)) {
            return new Expr(Exception.class);
        }
        // if
        document.compare(comparison, thenLabel, a, b);

        // else
        document.load(target, 0);
        document.jump(afterLabel);

        // then
        document.markLabel(thenLabel);
        document.load(target, 1);

        // after
        document.markLabel(afterLabel);
        return new Expr(Boolean.class);
    }

    private static Expr generateListExpression(final Document document, final Parser.ListNode node, final Local target) {
        if (!(node.getChild(0) instanceof Parser.SymbolNode)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Parser.SymbolNode func = (Parser.SymbolNode) node.getChild(0);
        String symbol = func.symbol;
        ExprGenerator exprGenerator = exprGenerators.get(symbol);
        if (exprGenerator != null) {
            return exprGenerator.run(document, node.getChildren(), target);
        } else {
            throw new RuntimeException("Unknown symbol \"" + func.symbol + "\"");
        }
    }

    private static Expr generateNumber(final Document document, final Parser.NumberNode node, final Local target) {
        document.load(target, node.number);
        return new Expr(Integer.class);
    }

    private static Expr generateBoolean(final Document document, final Parser.BooleanNode node, final Local target) {
        document.load(target, node.value ? 1 : 0);
        return new Expr(Boolean.class);
    }
}
