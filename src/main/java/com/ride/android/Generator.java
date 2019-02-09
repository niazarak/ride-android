package com.ride.android;

import com.android.dx.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class Generator {
    // for test purposes
    public static void main(String[] args) throws IOException {
        // final String input = "(/ (+ 9 6) (% 7 4))";
        // final String input = "(< 5 10)";
        // final String input = "(== 1 (> 1 2))";
        // final String input = "(xor #t 2)";
        // final String input = "(+ 12 (if (> 5 10) 1 0))";
        // final String input = "(define (funA a) (+ a #t)) (define (funB b) (- b (funA 2))) (funB 2)";
        final String input = "(+ 12 (if (> 5 10) 1 0))(+ 2 2)(+ 2 (if (> 5 10) 1 0))";
        FileOutputStream dexResult = new FileOutputStream("classes.dex");

        byte[] program = generate(Parser.parse(Parser.tokenize(input)));

        dexResult.write(program);
        dexResult.flush();
        dexResult.close();
    }

    static class Environment {
        private LinkedList<Map<String, EnvironmentEntry>> frames = new LinkedList<>();

        public Environment() {
            Map<String, EnvironmentEntry> baseEnvironment = new HashMap<String, EnvironmentEntry>() {{
            }};
            frames.add(baseEnvironment);
        }

        public void push() {
            frames.add(new HashMap<>());
        }

        public void add(String name, EnvironmentEntry entry) {
            frames.getLast().put(name, entry);
        }

        public void pop() {
            frames.removeLast();
        }

        public EnvironmentEntry lookup(String symbol) {
            ListIterator<Map<String, EnvironmentEntry>> iterator = frames.listIterator(frames.size());
            while (iterator.hasPrevious()) {
                Map<String, EnvironmentEntry> frame = iterator.previous();
                if (frame.get(symbol) != null) {
                    return frame.get(symbol);
                }
            }
            return null;
        }
    }

    static byte[] generate(final List<Parser.Node> nodes) {
        Environment environment = new Environment();

        Module megaModule = new Module();

        FunctionCode mainFunctionCode = megaModule.makeMain();

        for (Parser.Node node : nodes) {
            Parser.Node firstChild = ((Parser.ListNode) node).getChild(0);
            if (firstChild instanceof Parser.SymbolNode && ((Parser.SymbolNode) firstChild).symbol.equals("define")) {
                Parser.ListNode definition = (Parser.ListNode) ((Parser.ListNode) node).getChild(1);

                // get name and args
                Parser.SymbolNode name = (Parser.SymbolNode) definition.getChild(0);
                int paramsCount = definition.getChildren().size() - 1;
                TypeId[] params = new TypeId[paramsCount];
                Arrays.fill(params, TypeId.INT);
                FunctionCode functionCode = megaModule.make(TypeId.INT, name.symbol, params);

                // register params
                environment.push();
                for (int i = 0; i < paramsCount; i++) {
                    environment.add(((Parser.SymbolNode) definition.getChild(i + 1)).symbol, new VarEntry(functionCode.getParam(i)));
                }

                // launch func body
                LocalWrapper target = functionCode.getOrCreateLocal(0);
                Expr funcBodyExpr = generateExpression(functionCode, ((Parser.ListNode) node).getChild(2), target, environment);
                if (funcBodyExpr.type == Type.EXCEPTION) {
                    throw new RuntimeException("Compilation failed");
                }
                functionCode.returnValue(target);

                // register function
                environment.pop();
                List<Type> exprParams = new ArrayList<>();
                Collections.fill(exprParams, Type.INTEGER);
                environment.add(name.symbol, new FunctionEntry(new TypeFunction(Type.INTEGER, exprParams),
                        functionCode.getMethodId()));
            } else {
                LocalWrapper target = mainFunctionCode.getOrCreateLocal(0);
                Expr expr = generateExpression(mainFunctionCode, node, target, environment);
                if (expr.type == Type.EXCEPTION) {
                    throw new RuntimeException("Compilation failed");
                }

                TypeId<System> systemType = TypeId.get(System.class);
                TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);
                FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
                MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                        TypeId.VOID, "println", TypeId.INT);

                LocalWrapper systemOutLocal = mainFunctionCode.getOrCreateLocal(target.getPos() + 1, printStreamType);
                mainFunctionCode.sget(systemOutField, systemOutLocal);
                mainFunctionCode.invokeVirtual(printlnMethod, systemOutLocal, target);
            }
        }

        mainFunctionCode.returnVoid();

        return megaModule.compile();
    }

    private static Expr generateExpression(final FunctionCode functionCode, final Parser.Node node,
                                           final LocalWrapper target, final Environment environment) {
        if (node instanceof Parser.NumberNode) {
            return generateNumber(functionCode, (Parser.NumberNode) node, target);
        } else if (node instanceof Parser.BooleanNode) {
            return generateBoolean(functionCode, (Parser.BooleanNode) node, target);
        } else if (node instanceof Parser.ListNode) {
            return generateListExpression(functionCode, (Parser.ListNode) node, target, environment);
        } else if (node instanceof Parser.SymbolNode) {
            return generateVarExpression(functionCode, (Parser.SymbolNode) node, target, environment);
        } else {
            throw new RuntimeException("Top level symbols not supported");
        }
    }

    interface Type {
        TypeInteger INTEGER = new TypeInteger();
        TypeException EXCEPTION = new TypeException();
        TypeBoolean BOOLEAN = new TypeBoolean();
    }

    static class TypeInteger implements Type {
    }

    static class TypeException implements Type {
    }

    static class TypeBoolean implements Type {
    }

    static class TypeFunction implements Type {
        final List<? extends Type> inputTypes;

        final Type returnType;

        public TypeFunction(Type returnType, List<? extends Type> inputTypes) {
            this.returnType = returnType;
            this.inputTypes = inputTypes;
        }
    }

    static class Expr {
        private final Type type;

        public Expr(final Type type) {
            this.type = type;
        }
    }

    interface EnvironmentEntry {
        Type getType();
    }

    static class FunctionEntry implements EnvironmentEntry {
        private final TypeFunction type;
        private final MethodId methodId;

        FunctionEntry(TypeFunction type, MethodId methodId) {
            this.type = type;
            this.methodId = methodId;
        }

        @Override
        public Type getType() {
            return type;
        }

        public MethodId getMethodId() {
            return methodId;
        }
    }

    static class VarEntry implements EnvironmentEntry {
        private final TypeInteger type;
        private final FunctionCode.VarWrapper varWrapper;

        VarEntry(FunctionCode.VarWrapper varWrapper) {
            this.varWrapper = varWrapper;
            type = Type.INTEGER;
        }

        @Override
        public Type getType() {
            return type;
        }
    }


    interface ExprGenerator {
        Expr run(final FunctionCode functionCode, final List<Parser.Node> nodes,
                 final LocalWrapper target, final Environment environment);
    }

    private static Map<String, ExprGenerator> exprGenerators = new HashMap<>();

    static {
        exprGenerators.put("+", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.add(target, a, b);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("-", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.subtract(target, a, b);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("*", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.multiply(target, a, b);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("/", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.divide(target, a, b);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("%", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.remainder(target, a, b);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("if", (functionCode, nodes, target, environment) -> {
            Label thenLabel = new Label();
            Label afterLabel = new Label();
            LocalWrapper ifResult = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprIf = generateExpression(functionCode, nodes.get(1), ifResult, environment);
            if (exprIf.type != Type.BOOLEAN) {
                return new Expr(Type.EXCEPTION);
            }
            // if
            functionCode.compareZ(thenLabel, ifResult);

            // else
            LocalWrapper elseResult = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprElse = generateExpression(functionCode, nodes.get(2), elseResult, environment);
            functionCode.move(target, elseResult);
            functionCode.jump(afterLabel);

            // then
            functionCode.markLabel(thenLabel);
            LocalWrapper thenResult = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprThen = generateExpression(functionCode, nodes.get(3), thenResult, environment);
            functionCode.move(target, thenResult);

            if (exprElse.type != Type.INTEGER || !exprElse.type.equals(exprThen.type)) {
                return new Expr(Type.EXCEPTION);
            }
            // after
            functionCode.markLabel(afterLabel);
            return new Expr(Type.INTEGER);
        });
        exprGenerators.put("!=", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.NE;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put("==", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.EQ;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put(">=", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.GE;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put(">", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.GT;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put("<=", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.LE;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put("<", (functionCode, nodes, target, environment) -> {
            Comparison comparison = Comparison.LT;
            return generateComparison(functionCode, target, comparison, nodes.get(1), nodes.get(2), environment);
        });
        exprGenerators.put("and", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.BOOLEAN || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.and(target, a, b);
            return new Expr(Type.BOOLEAN);
        });
        exprGenerators.put("or", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.BOOLEAN || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.or(target, a, b);
            return new Expr(Type.BOOLEAN);
        });
        exprGenerators.put("xor", (functionCode, nodes, target, environment) -> {
            LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
            Expr exprA = generateExpression(functionCode, nodes.get(1), a, environment);
            LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
            Expr exprB = generateExpression(functionCode, nodes.get(2), b, environment);
            if (exprA.type != Type.BOOLEAN || !exprA.type.equals(exprB.type)) {
                return new Expr(Type.EXCEPTION);
            }
            functionCode.xor(target, a, b);
            return new Expr(Type.BOOLEAN);
        });
    }

    private static Expr generateComparison(FunctionCode functionCode, LocalWrapper target, Comparison comparison,
                                           Parser.Node nodeA, Parser.Node nodeB, Environment environment) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();
        LocalWrapper a = functionCode.getOrCreateLocal(target.getPos() + 1);
        Expr exprA = generateExpression(functionCode, nodeA, a, environment);
        LocalWrapper b = functionCode.getOrCreateLocal(target.getPos() + 2);
        Expr exprB = generateExpression(functionCode, nodeB, b, environment);
        if (exprA.type != Type.INTEGER || !exprA.type.equals(exprB.type)) {
            return new Expr(Type.EXCEPTION);
        }
        // if
        functionCode.compare(comparison, thenLabel, a, b);

        // else
        functionCode.load(target, 0);
        functionCode.jump(afterLabel);

        // then
        functionCode.markLabel(thenLabel);
        functionCode.load(target, 1);

        // after
        functionCode.markLabel(afterLabel);
        return new Expr(Type.BOOLEAN);
    }

    private static Expr generateListExpression(final FunctionCode functionCode, final Parser.ListNode node,
                                               final LocalWrapper target, final Environment environment) {
        if (!(node.getChild(0) instanceof Parser.SymbolNode)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Parser.SymbolNode func = (Parser.SymbolNode) node.getChild(0);
        String symbol = func.symbol;
        // check if function call
        EnvironmentEntry lookedUpEntry = environment.lookup(symbol);
        if (lookedUpEntry != null && lookedUpEntry.getType() instanceof TypeFunction) {
            FunctionEntry functionEntry = (FunctionEntry) lookedUpEntry;
            int argsCount = ((FunctionEntry) lookedUpEntry).getMethodId().getParameters().size();
            LocalWrapper[] args = new LocalWrapper[argsCount];
            for (int i = 0; i < argsCount; i++) {
                LocalWrapper argLocalWrapper = functionCode.getOrCreateLocal(target.getPos() + i + 1);
                Expr argExpr = generateExpression(functionCode, node.getChild(i + 1), argLocalWrapper, environment);
                if (argExpr.type != Type.INTEGER) {
                    return new Expr(Type.EXCEPTION);
                }
                args[i] = argLocalWrapper;
            }
            functionCode.call(functionEntry.getMethodId(), target, args);
            return new Expr(Type.INTEGER);
        } else if (lookedUpEntry != null) {
            // this should never happen
            throw new RuntimeException("Symbol is not callable \"" + func.symbol + "\"");
        } else {
            // else do smth
            ExprGenerator exprGenerator = exprGenerators.get(symbol);
            if (exprGenerator != null) {
                return exprGenerator.run(functionCode, node.getChildren(), target, environment);
            } else {
                throw new RuntimeException("Unknown symbol \"" + func.symbol + "\"");
            }
        }
    }

    private static Expr generateVarExpression(final FunctionCode functionCode, final Parser.SymbolNode node,
                                              final LocalWrapper target, final Environment environment) {
        final EnvironmentEntry lookedUpEntry = environment.lookup(node.symbol);
        if (lookedUpEntry == null) {
            System.out.println("Unknown entry \"" + node.symbol + "\"");
            return new Expr(Type.EXCEPTION);
        } else if (lookedUpEntry.getType() instanceof TypeInteger) {
            VarEntry varEntry = (VarEntry) lookedUpEntry;
            functionCode.move(target, varEntry.varWrapper);
            return new Expr(Type.INTEGER);
        } else {
            // todo: support functions as vars
            System.out.println("Function as var not supported \"" + node.symbol + "\"");
            return new Expr(Type.EXCEPTION);
        }
    }

    private static Expr generateNumber(final FunctionCode functionCode, final Parser.NumberNode node, final LocalWrapper target) {
        functionCode.load(target, node.number);
        return new Expr(Type.INTEGER);
    }

    private static Expr generateBoolean(final FunctionCode functionCode, final Parser.BooleanNode node, final LocalWrapper target) {
        functionCode.load(target, node.value ? 1 : 0);
        return new Expr(Type.BOOLEAN);
    }
}
