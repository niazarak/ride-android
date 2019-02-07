package com.ride.android;

import com.android.dx.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class Generator {
    // for test purposes
    public static void main(String[] args) throws IOException {
        // final String input = "(/ (+ 9 6) (% 7 4))";
        // final String input = "(< 5 10)";
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
        generateExpression(document, node, target);
        return document.compile();
    }

    private static void generateExpression(final Document document, final Parser.Node node, final Local target) {
        if (node instanceof Parser.NumberNode) {
            generateNumber(document, (Parser.NumberNode) node, target);
        } else if (node instanceof Parser.ListNode) {
            generateListExpression(document, (Parser.ListNode) node, target);
        } else {
            throw new RuntimeException("Top level symbols not supported");
        }
    }

    private static void generateListExpression(final Document document, final Parser.ListNode node, final Local target) {
        if (!(node.getChild(0) instanceof Parser.SymbolNode)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Parser.SymbolNode func = (Parser.SymbolNode) node.getChild(0);
        switch (func.symbol) {
            case "+": {
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
                document.add(target, a, b);
                break;
            }
            case "-": {
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
                document.subtract(target, a, b);
                break;
            }
            case "*": {
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
                document.multiply(target, a, b);
                break;
            }
            case "/": {
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
                document.divide(target, a, b);
                break;
            }
            case "%": {
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
                document.remainder(target, a, b);
                break;
            }
            case "if": {
                Label thenLabel = new Label();
                Label afterLabel = new Label();
                Local ifResult = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), ifResult);
                // if
                document.compareZ(thenLabel, ifResult);

                // else
                Local elseResult = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(2), elseResult);
                document.move(target, elseResult);
                document.jump(afterLabel);

                // then
                document.markLabel(thenLabel);
                Local thenResult = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(3), thenResult);
                document.move(target, thenResult);

                // after
                document.markLabel(afterLabel);
                break;
            }
            case "!=":
            case "==":
            case ">=":
            case ">":
            case "<=":
            case "<": {
                Comparison comparison = Comparison.LE;
                switch (func.symbol) {
                    case "!=":
                        comparison = Comparison.NE;
                        break;
                    case "==":
                        comparison = Comparison.EQ;
                        break;
                    case ">=":
                        comparison = Comparison.GE;
                        break;
                    case ">":
                        comparison = Comparison.GT;
                        break;
                    case "<=":
                        comparison = Comparison.LE;
                        break;
                    case "<":
                        comparison = Comparison.LT;
                        break;
                }
                Label thenLabel = new Label();
                Label afterLabel = new Label();
                Local a = document.getOrCreateLocal(target.pos + 1);
                generateExpression(document, node.getChild(1), a);
                Local b = document.getOrCreateLocal(target.pos + 2);
                generateExpression(document, node.getChild(2), b);
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
                break;
            }
            default: {
                throw new RuntimeException("Unknown symbol \"" + func.symbol + "\"");
            }
        }
    }

    private static void generateNumber(final Document document, final Parser.NumberNode node, final Local target) {
        document.load(target, node.number);
    }
}
