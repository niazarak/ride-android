package com.ride.android.codegen;

import com.android.dx.*;
import com.ride.android.Environment;
import com.ride.android.ast.Expression;
import com.ride.android.ast.Expressions;
import com.ride.android.ast.Parser;
import com.ride.android.ast.Tokenizer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import static com.ride.inference.Types.*;

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

        byte[] program = generate(Parser.parse(Tokenizer.tokenize(input)));

        dexResult.write(program);
        dexResult.flush();
        dexResult.close();
    }

    public static byte[] generate(final List<Expression> nodes) {
        Environment environment = new Environment();
        initBuiltins(environment);

        Module megaModule = new Module();

        FunctionCode mainFunctionCode = megaModule.makeMain();

        for (Expression node : nodes) {
            Expression functionExpression = ((Expressions.Application) node).getFunction();
            if (functionExpression instanceof Expressions.Variable && ((Expressions.Variable) functionExpression).name.equals("define")) {
                generateFunction(environment, megaModule,
                        (Expressions.Application) ((Expressions.Application) node).getArg(0),
                        ((Expressions.Application) node).getArg(1));
            } else {
                LocalWrapper target = mainFunctionCode.getOrCreateLocal(0);
                Expr expr = generateExpression(mainFunctionCode, node, target, environment);

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
                                         final Expressions.Application definition,
                                         final Expression body) {
        // get name and args
        Expressions.Variable functionVarExpression = (Expressions.Variable) definition.getFunction();
        int argsCount = definition.getArgs().size() - 1;
        TypeId[] params = new TypeId[argsCount];
        Arrays.fill(params, TypeId.INT);
        FunctionCode functionCode = megaModule.make(TypeId.INT, functionVarExpression.name, params);

        // register args
        environment.push();
        for (int i = 0; i < argsCount; i++) {
            environment.add(((Expressions.Variable) definition.getArg(i)).name,
                    new VarEntry(functionCode.getParam(i)));
        }

        // launch func body
        LocalWrapper target = functionCode.getOrCreateLocal(0);
        Expr funcBodyExpr = generateExpression(functionCode, body, target, environment);
        functionCode.returnValue(target);

        // register function
        environment.pop();
        Type[] exprParams = new Type[argsCount];
        Arrays.fill(exprParams, integer());
        environment.add(functionVarExpression.name, new FunctionEntry(func(Arrays.asList(exprParams), integer()),
                functionCode.getMethodId()));
    }

    private static Expr generateExpression(final FunctionCode functionCode,
                                           final Expression expression,
                                           final LocalWrapper target,
                                           final Environment environment) {
        if (expression instanceof Expressions.Int) {
            return generateNumber(functionCode, (Expressions.Int) expression, target);
        } else if (expression instanceof Expressions.Bool) {
            return generateBoolean(functionCode, (Expressions.Bool) expression, target);
        } else if (expression instanceof Expressions.Application) {
            return generateApplication(functionCode, (Expressions.Application) expression, target, environment);
        } else if (expression instanceof Expressions.IfExpr) {
            return generateIf(functionCode, (Expressions.IfExpr) expression, target, environment);
        } else if (expression instanceof Expressions.Variable) {
            return generateVarExpression(functionCode, (Expressions.Variable) expression, target, environment);
        } else {
            throw new RuntimeException("Top level symbols not supported");
        }
    }

    static class Expr {
        private final Type type;

        public Expr(final Type type) {
            this.type = type;
        }
    }

    public interface EnvironmentEntry {
        Type getType();
    }

    interface ApplicableEnvironmentEntry extends EnvironmentEntry {
        Expr apply(final FunctionCode functionCode, LocalWrapper target, LocalWrapper... args);
    }

    static class FunctionEntry implements ApplicableEnvironmentEntry {
        private final TFunction type;
        private final MethodId methodId;

        FunctionEntry(TFunction type, MethodId methodId) {
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
            return new Expr(integer());
        }
    }

    static abstract class OperatorEntry implements ApplicableEnvironmentEntry {
        private final TFunction type;

        OperatorEntry(TFunction type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    static class VarEntry implements EnvironmentEntry {
        private final Type type;
        private final FunctionCode.VarWrapper varWrapper;

        VarEntry(FunctionCode.VarWrapper varWrapper) {
            this.varWrapper = varWrapper;
            type = integer();
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
        return new Expr(bool());
    }

    public static Expr generateIf(final FunctionCode functionCode,
                                  final Expressions.IfExpr expr,
                                  final LocalWrapper target,
                                  final Environment environment) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();

        LocalWrapper ifResult = functionCode.getOrCreateLocal(target.getPos() + 1);
        Expr exprIf = generateExpression(functionCode, expr.condition, ifResult, environment);
        if (exprIf.type != bool()) {
            throw new RuntimeException("Condition should return boolean");
        }
        // if
        functionCode.compareZ(thenLabel, ifResult);

        // else
        LocalWrapper elseResult = functionCode.getOrCreateLocal(target.getPos() + 1);
        Expr exprElse = generateExpression(functionCode, expr.ifBranch, elseResult, environment);
        functionCode.move(target, elseResult);
        functionCode.jump(afterLabel);

        // then
        functionCode.markLabel(thenLabel);
        LocalWrapper thenResult = functionCode.getOrCreateLocal(target.getPos() + 1);
        Expr exprThen = generateExpression(functionCode, expr.elseBranch, thenResult, environment);
        functionCode.move(target, thenResult);

        if (exprElse.type != integer() || !exprElse.type.equals(exprThen.type)) {
            throw new RuntimeException("Branches types should match");
        }

        // after
        functionCode.markLabel(afterLabel);
        return new Expr(integer());
    }


    private static Expr generateApplication(final FunctionCode functionCode,
                                            final Expressions.Application application,
                                            final LocalWrapper target,
                                            final Environment environment) {
        if (!(application.getFunction() instanceof Expressions.Variable)) {
            throw new RuntimeException("Functions as expressions not supported");
        }
        Expressions.Variable functionVarExpression = (Expressions.Variable) application.getFunction();

        EnvironmentEntry lookedUpEntry = environment.lookup(functionVarExpression.name);
        if (lookedUpEntry != null && lookedUpEntry.getType() instanceof TFunction) {
            // lookup entry and its type
            ApplicableEnvironmentEntry functionEntry = (ApplicableEnvironmentEntry) lookedUpEntry;
            TFunction functionEntryType = (TFunction) functionEntry.getType();

            // eval args and put into locals
            int argsCount = functionEntryType.args.size();
            LocalWrapper[] args = new LocalWrapper[argsCount];
            for (int i = 0; i < argsCount; i++) {
                LocalWrapper argLocalWrapper = functionCode.getOrCreateLocal(target.getPos() + i + 1);
                Expr argExpr = generateExpression(functionCode, application.getArg(i), argLocalWrapper, environment);
                args[i] = argLocalWrapper;
            }

            // generate call
            return functionEntry.apply(functionCode, target, args);
        } else if (lookedUpEntry != null) {
            // this should never happen
            throw new RuntimeException("Symbol is not callable \"" + functionVarExpression.name + "\"");
        } else {
            throw new RuntimeException("Unknown name \"" + functionVarExpression.name + "\"");
        }
    }

    private static Expr generateVarExpression(final FunctionCode functionCode,
                                              final Expressions.Variable expr,
                                              final LocalWrapper target,
                                              final Environment environment) {
        final EnvironmentEntry lookedUpEntry = environment.lookup(expr.name);
        if (lookedUpEntry == null) {
            throw new RuntimeException("Unknown entry \"" + expr.name + "\"");
        } else if (lookedUpEntry.getType() instanceof TLiteral) {
            VarEntry varEntry = (VarEntry) lookedUpEntry;
            functionCode.move(target, varEntry.varWrapper);
            return new Expr(integer());
        } else {
            // todo: support functions as vars
            throw new RuntimeException("Function as name not supported \"" + expr.name + "\"");
        }
    }

    private static Expr generateNumber(final FunctionCode functionCode,
                                       final Expressions.Int expr,
                                       final LocalWrapper target) {
        functionCode.load(target, expr.number);
        return new Expr(integer());
    }

    private static Expr generateBoolean(final FunctionCode functionCode,
                                        final Expressions.Bool expr,
                                        final LocalWrapper target) {
        functionCode.load(target, expr.value ? 1 : 0);
        return new Expr(bool());
    }

    private static void initBuiltins(final Environment baseEnvironment) {
        baseEnvironment.add("+", new OperatorEntry(func(args(integer(), integer()), integer())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.add(target, args[0], args[1]);
                return new Expr(integer());
            }
        });
        baseEnvironment.add("-", new OperatorEntry(func(args(integer(), integer()), integer())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.subtract(target, args[0], args[1]);
                return new Expr(integer());
            }
        });
        baseEnvironment.add("*", new OperatorEntry(func(args(integer(), integer()), integer())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.multiply(target, args[0], args[1]);
                return new Expr(integer());
            }
        });
        baseEnvironment.add("/", new OperatorEntry(func(args(integer(), integer()), integer())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.divide(target, args[0], args[1]);
                return new Expr(integer());
            }
        });
        baseEnvironment.add("%", new OperatorEntry(func(args(integer(), integer()), integer())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.remainder(target, args[0], args[1]);
                return new Expr(integer());
            }
        });
        baseEnvironment.add("!=", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.NE;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add("==", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.EQ;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add(">=", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.GE;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add(">", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.GT;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add("<=", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.LE;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add("<", new OperatorEntry(func(args(integer(), integer()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                Comparison comparison = Comparison.LT;
                return generateComparison(functionCode, target, comparison, args[0], args[1]);
            }
        });
        baseEnvironment.add("and", new OperatorEntry(func(args(bool(), bool()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.and(target, args[0], args[1]);
                return new Expr(bool());
            }
        });
        baseEnvironment.add("or", new OperatorEntry(func(args(bool(), bool()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.or(target, args[0], args[1]);
                return new Expr(bool());
            }
        });
        baseEnvironment.add("xor", new OperatorEntry(func(args(bool(), bool()), bool())) {
            @Override
            public Expr apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args) {
                functionCode.xor(target, args[0], args[1]);
                return new Expr(bool());
            }
        });
    }
}