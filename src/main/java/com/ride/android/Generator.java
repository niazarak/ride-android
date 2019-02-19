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
        // final String input = "(xor #t #f)";
        // final String input = "(+ 12 (if (> 5 10) 1 0))";
        // final String input = "(define (funA a) (+ a 7)) (define (funB b) (- b (funA 2))) (funB 2)";
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
            Map<String, EnvironmentEntry> baseEnvironment = new HashMap<>();
            baseEnvironment.put("+", new OperatorEntry(new TypeFunction(Type.INTEGER, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.add(target, args[0], args[1]);
                    return new Expr(Type.INTEGER);
                }
            });
            baseEnvironment.put("-", new OperatorEntry(new TypeFunction(Type.INTEGER, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.subtract(target, args[0], args[1]);
                    return new Expr(Type.INTEGER);
                }
            });
            baseEnvironment.put("*", new OperatorEntry(new TypeFunction(Type.INTEGER, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.multiply(target, args[0], args[1]);
                    return new Expr(Type.INTEGER);
                }
            });
            baseEnvironment.put("/", new OperatorEntry(new TypeFunction(Type.INTEGER, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.divide(target, args[0], args[1]);
                    return new Expr(Type.INTEGER);
                }
            });
            baseEnvironment.put("%", new OperatorEntry(new TypeFunction(Type.INTEGER, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.remainder(target, args[0], args[1]);
                    return new Expr(Type.INTEGER);
                }
            });
            baseEnvironment.put("!=", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.NE;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put("==", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.EQ;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put(">=", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.GE;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put(">", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.GT;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put("<=", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.LE;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put("<", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.INTEGER, Type.INTEGER))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    Comparison comparison = Comparison.LT;
                    return generateComparison(functionCode, target, comparison, args[0], args[1]);
                }
            });
            baseEnvironment.put("and", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.BOOLEAN, Type.BOOLEAN))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.and(target, args[0], args[1]);
                    return new Expr(Type.BOOLEAN);
                }
            });
            baseEnvironment.put("or", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.BOOLEAN, Type.BOOLEAN))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.or(target, args[0], args[1]);
                    return new Expr(Type.BOOLEAN);
                }
            });
            baseEnvironment.put("xor", new OperatorEntry(new TypeFunction(Type.BOOLEAN, Arrays.asList(Type.BOOLEAN, Type.BOOLEAN))) {
                @Override
                public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                    functionCode.xor(target, args[0], args[1]);
                    return new Expr(Type.BOOLEAN);
                }
            });
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
                generateFunction(environment, megaModule,
                        (Parser.ListNode) ((Parser.ListNode) node).getChild(1),
                        ((Parser.ListNode) node).getChild(2));
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

    private static void generateFunction(final Environment environment,
                                         final Module megaModule,
                                         final Parser.ListNode definition,
                                         final Parser.Node body) {
        // get name and args
        Parser.SymbolNode name = (Parser.SymbolNode) definition.getChild(0);
        int paramsCount = definition.getChildren().size() - 1;
        TypeId[] params = new TypeId[paramsCount];
        Arrays.fill(params, TypeId.INT);
        FunctionCode functionCode = megaModule.make(TypeId.INT, name.symbol, params);

        // register params
        environment.push();
        for (int i = 0; i < paramsCount; i++) {
            environment.add(((Parser.SymbolNode) definition.getChild(i + 1)).symbol,
                    new VarEntry(functionCode.getParam(i)));
        }

        // launch func body
        LocalWrapper target = functionCode.getOrCreateLocal(0);
        Expr funcBodyExpr = generateExpression(functionCode, body, target, environment);
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
    }

    private static Expr generateExpression(final FunctionCode functionCode,
                                           final Parser.Node node,
                                           final LocalWrapper target,
                                           final Environment environment) {
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

    interface ApplicableEnvironmentEntry extends EnvironmentEntry {
        Expr apply(final FunctionCode functionCode, LocalWrapper target, LocalWrapper... args);
    }

    static class FunctionEntry implements ApplicableEnvironmentEntry {
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

        @Override
        public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
            functionCode.call(methodId, target, args);
            return new Expr(Type.INTEGER);
        }
    }

    static abstract class OperatorEntry implements ApplicableEnvironmentEntry {
        private final TypeFunction type;

        OperatorEntry(TypeFunction type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
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

    private static Expr generateComparison(FunctionCode functionCode, LocalWrapper target,
                                           Comparison comparison, LocalWrapper a, LocalWrapper b) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();
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

    public static Expr generateIf(final FunctionCode functionCode,
                                  final List<Parser.Node> nodes,
                                  final LocalWrapper target,
                                  final Environment environment) {
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
    }


    private static Expr generateListExpression(final FunctionCode functionCode,
                                               final Parser.ListNode node,
                                               final LocalWrapper target,
                                               final Environment environment) {
        if (!(node.getChild(0) instanceof Parser.SymbolNode)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Parser.SymbolNode func = (Parser.SymbolNode) node.getChild(0);
        String symbol = func.symbol;

        if (symbol.equals("if")) {
            return generateIf(functionCode, node.getChildren(), target, environment);
        }

        // check if function call
        EnvironmentEntry lookedUpEntry = environment.lookup(symbol);
        if (lookedUpEntry != null && lookedUpEntry.getType() instanceof TypeFunction) {
            ApplicableEnvironmentEntry functionEntry = (ApplicableEnvironmentEntry) lookedUpEntry;
            TypeFunction functionEntryType = (TypeFunction) functionEntry.getType();
            int argsCount = functionEntryType.inputTypes.size();
            LocalWrapper[] args = new LocalWrapper[argsCount];
            for (int i = 0; i < argsCount; i++) {
                LocalWrapper argLocalWrapper = functionCode.getOrCreateLocal(target.getPos() + i + 1);
                Expr argExpr = generateExpression(functionCode, node.getChild(i + 1), argLocalWrapper, environment);
                if (argExpr.type != functionEntryType.inputTypes.get(i)) {
                    return new Expr(Type.EXCEPTION);
                }
                args[i] = argLocalWrapper;
            }
            return functionEntry.apply(functionCode, target, args);
        } else if (lookedUpEntry != null) {
            // this should never happen
            throw new RuntimeException("Symbol is not callable \"" + func.symbol + "\"");
        } else {
            throw new RuntimeException("Unknown symbol \"" + func.symbol + "\"");
        }
    }

    private static Expr generateVarExpression(final FunctionCode functionCode,
                                              final Parser.SymbolNode node,
                                              final LocalWrapper target,
                                              final Environment environment) {
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

    private static Expr generateNumber(final FunctionCode functionCode,
                                       final Parser.NumberNode node,
                                       final LocalWrapper target) {
        functionCode.load(target, node.number);
        return new Expr(Type.INTEGER);
    }

    private static Expr generateBoolean(final FunctionCode functionCode,
                                        final Parser.BooleanNode node,
                                        final LocalWrapper target) {
        functionCode.load(target, node.value ? 1 : 0);
        return new Expr(Type.BOOLEAN);
    }
}
