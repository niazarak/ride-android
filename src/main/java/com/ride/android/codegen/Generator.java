package com.ride.android.codegen;

import com.android.dx.FieldId;
import com.android.dx.Label;
import com.android.dx.MethodId;
import com.android.dx.TypeId;
import com.ride.android.ast.Ast;
import com.ride.android.ast.Expression;
import com.ride.android.ast.Expressions;
import com.ride.android.ast.TypeChecker;
import com.ride.android.parser.Parser;
import com.ride.android.parser.Tokenizer;
import com.ride.inference.Types;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.ride.inference.Types.*;

/**
 * Main generator class
 * Recursively traverses AST and generates code
 */
public class Generator {

    // for test purposes
    public static void main(String[] args) throws IOException {
        // final String input = "(/ (+ 9 6) (% 7 4))";
        // final String input = "(< 5 10)";
        // final String input = "(== 1 (> 1 2))";
        // final String input = "(xor #t #f)";
        // final String input = "(+ 12 (if (> 5 10) 1 0))";
        // final String input = "(define (funA a) (+ a 7)) (define (funB b) (- b (funA 2))) (funB 2)";
        //final String input = "(+ 12 (if (> 5 10) 1 0))(+ 2 2)(+ 2 (if (> 5 10) 1 0))";
        //final String input = "2";
        //final String input = "((lambda (x y) (* x y)) 2 2)";
        final String input = "((lambda () (* 2 2)))";

        FileOutputStream dexResult = new FileOutputStream("classes.dex");

        byte[] program = generate(TypeChecker.infer(Ast.ast(Parser.parse(Tokenizer.tokenize(input)))));

        dexResult.write(program);
        dexResult.flush();
        dexResult.close();
    }

    public static byte[] generate(final List<Expression> nodes) {
        CodegenEnvironment environment = new CodegenEnvironment();

        Module megaModule = new Module();

        Builtins.initBuiltins(environment, megaModule);

        FunctionCode mainFunctionCode = megaModule.makeMain();

        for (Expression node : nodes) {
            if (node instanceof Expressions.Definition) {
                generateDefinition(environment, megaModule, (Expressions.Definition) node, megaModule);
            } else {
                DeferredLocal target = mainFunctionCode.getOrCreateLocal(0, TypeId.OBJECT);
                generateExpression(mainFunctionCode, node, target, environment, megaModule);

                TypeId<System> systemType = TypeId.get(System.class);
                TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);
                FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
                MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                        TypeId.VOID, "println", TypeId.OBJECT);

                LocalWrapper systemOutLocal = mainFunctionCode.getOrCreateLocal(target.getPos() + 1, printStreamType);
                mainFunctionCode.sget(systemOutField, systemOutLocal);
                mainFunctionCode.invokeVirtual(printlnMethod, null, systemOutLocal, target);
            }
        }

        mainFunctionCode.returnVoid();

        return megaModule.compile();
    }

    private static void generateExpression(final FunctionCode functionCode,
                                           final Expression expression,
                                           final DeferredLocal target,
                                           final CodegenEnvironment environment, Module module) {
        if (expression instanceof Expressions.Int) {
            generateNumber(functionCode, (Expressions.Int) expression, target);
        } else if (expression instanceof Expressions.Bool) {
            generateBoolean(functionCode, (Expressions.Bool) expression, target);
        } else if (expression instanceof Expressions.Application) {
            generateApplication(functionCode, (Expressions.Application) expression, target, environment, module);
        } else if (expression instanceof Expressions.Lambda) {
            generateLambda((Expressions.Lambda) expression, environment, functionCode, target, module);
        } else if (expression instanceof Expressions.IfExpr) {
            generateIf(functionCode, (Expressions.IfExpr) expression, target, environment, module);
        } else if (expression instanceof Expressions.Variable) {
            generateVarExpression(functionCode, (Expressions.Variable) expression, target, environment);
        } else {
            throw new RuntimeException("Top level symbols not supported");
        }
    }

    public interface EnvironmentEntry {
    }

    static class DefinitionEntry implements EnvironmentEntry {
        private final FieldId fieldId;

        DefinitionEntry(FieldId fieldId) {
            this.fieldId = fieldId;
        }
    }

    static class NamedArgEntry implements EnvironmentEntry {
        private final ParamLocal varWrapper;

        NamedArgEntry(ParamLocal varWrapper) {
            this.varWrapper = varWrapper;
        }
    }

    private static void generateDefinition(final CodegenEnvironment environment,
                                           final Module megaModule,
                                           final Expressions.Definition definition,
                                           final Module module) {
        // make function delegate
        TypeId[] args = convertToTypeId(definition.getType().args);
        TypeId res = convertToTypeId(definition.getType().res);
        Module.ModuleDefinition moduleDefinition = megaModule.makeDefine(definition.name, res, args);

        // define args
        environment.push();
        for (int i = 0; i < args.length; i++) {
            environment.add(definition.getArg(i), new NamedArgEntry(moduleDefinition.lambdaCode.applyCode.getParam(i, args[i])));
        }

        // launch func body
        DeferredLocal castedResult = moduleDefinition.lambdaCode.applyCode.getOrCreateLocal(0, TypeId.OBJECT);
        DeferredLocal target = moduleDefinition.lambdaCode.applyCode.getOrCreateLocal(1, res);
        generateExpression(moduleDefinition.lambdaCode.applyCode, definition.body, target, environment, module);
        moduleDefinition.lambdaCode.applyCode.cast(castedResult, target);
        moduleDefinition.lambdaCode.applyCode.returnValue(castedResult);

        // register function
        environment.pop();
        environment.add(definition.name, new DefinitionEntry(moduleDefinition.definitionField));
    }

    private static void generateLambda(final Expressions.Lambda lambda,
                                       final CodegenEnvironment environment,
                                       final FunctionCode functionCode,
                                       final DeferredLocal target,
                                       final Module module) {
        // declare lambda
        TypeId[] args = convertToTypeId(lambda.getType().args);
        TypeId res = convertToTypeId(lambda.getType().res);
        LambdaCode lambdaCode = module.makeLambda(convertToTypeId(lambda.getType().res), args);

        // register args
        environment.push();
        for (int i = 0; i < args.length; i++) {
            environment.add(lambda.getArg(i), new NamedArgEntry(lambdaCode.applyCode.getParam(i, args[i])));
        }

        // generate body
        DeferredLocal lambdaApplyCastedResult = lambdaCode.applyCode.getOrCreateLocal(0, TypeId.OBJECT);
        DeferredLocal lambdaApplyResult = lambdaCode.applyCode.getOrCreateLocal(1, res);
        generateExpression(lambdaCode.applyCode, lambda.body, lambdaApplyResult, environment, module);
        lambdaCode.applyCode.cast(lambdaApplyCastedResult, lambdaApplyResult);
        lambdaCode.applyCode.returnValue(lambdaApplyCastedResult);
        environment.pop();

        // instantiate lambda object to target
        functionCode.newInstance(lambdaCode.getConstructorMethod(), target);
    }

    public static void generateIf(final FunctionCode functionCode,
                                  final Expressions.IfExpr expr,
                                  final DeferredLocal target,
                                  final CodegenEnvironment environment,
                                  final Module module) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();

        // generate if expression
        DeferredLocal ifResult = functionCode.getOrCreateLocal(target.getPos() + 1,
                convertToTypeId(expr.condition.getType()));

        // cast to boxed boolean and get primitive value
        DeferredLocal ifCastedResult = functionCode.getOrCreateLocal(target.getPos() + 2, Module.BOXED_BOOLEAN);
        DeferredLocal ifRawResult = functionCode.getOrCreateLocal(target.getPos() + 3, TypeId.BOOLEAN);
        generateExpression(functionCode, expr.condition, ifResult, environment, module);
        functionCode.cast(ifCastedResult, ifResult);
        functionCode.invokeVirtual(Module.METHOD_BOOLEAN_VALUE, ifRawResult, ifCastedResult);

        // if
        functionCode.compareZ(thenLabel, ifRawResult);

        // else
        DeferredLocal elseResult = functionCode.getOrCreateLocal(target.getPos() + 1,
                convertToTypeId(expr.ifBranch.getType()));
        generateExpression(functionCode, expr.ifBranch, elseResult, environment, module);
        functionCode.move(target, elseResult);
        functionCode.jump(afterLabel);

        // then
        functionCode.markLabel(thenLabel);
        DeferredLocal thenResult = functionCode.getOrCreateLocal(target.getPos() + 1,
                convertToTypeId(expr.elseBranch.getType()));
        generateExpression(functionCode, expr.elseBranch, thenResult, environment, module);
        functionCode.move(target, thenResult);

        // after
        functionCode.markLabel(afterLabel);
    }

    private static void generateApplication(final FunctionCode functionCode,
                                            final Expressions.Application application,
                                            final DeferredLocal target,
                                            final CodegenEnvironment environment,
                                            final Module module) {
        TypeId lambdaType = convertToTypeId(application.function.getType());
        DeferredLocal lambdaLocal = functionCode.getOrCreateLocal(target.getPos() + 1, lambdaType);
        DeferredLocal lambdaСLocal = functionCode.getOrCreateLocal(target.getPos() + 2, TypeId.OBJECT);
        generateExpression(functionCode, application.function, lambdaСLocal, environment, module);
        functionCode.cast(lambdaLocal, lambdaСLocal);

        // eval args and put into locals
        int argsCount = application.getArgs().size();
        DeferredLocal[] args = new DeferredLocal[argsCount];
        for (int i = 0; i < argsCount; i++) {
            Expression arg = application.getArg(i);
            DeferredLocal argLocalWrapper = functionCode.getOrCreateLocal(target.getPos() + i + 1,
                    convertToTypeId(application.function.getType().getArg(i)));
            generateExpression(functionCode, arg, argLocalWrapper, environment, module);
            args[i] = argLocalWrapper;
        }

        // generate call
        TypeId[] lambdaArgsTypes = convertToTypeId(application.function.getType().args);
        MethodId lambdaApplyMethod = lambdaType.getMethod(TypeId.OBJECT, "apply", lambdaArgsTypes);
        functionCode.invokeVirtual(lambdaApplyMethod, target, lambdaLocal, args);
    }

    private static void generateVarExpression(final FunctionCode functionCode,
                                              final Expressions.Variable expr,
                                              final DeferredLocal target,
                                              final CodegenEnvironment environment) {
        final EnvironmentEntry lookedUpEntry = environment.lookup(expr.name);
        if (lookedUpEntry == null) {
            throw new RuntimeException(expr.name + " is not found in scope");
        } else {
            if (lookedUpEntry instanceof NamedArgEntry) {
                NamedArgEntry namedArgEntry = (NamedArgEntry) lookedUpEntry;
                functionCode.move(target, namedArgEntry.varWrapper);
            } else {
                DefinitionEntry definitionEntry = (DefinitionEntry) lookedUpEntry;
                functionCode.sget(definitionEntry.fieldId, target);
            }
        }
    }

    private static void generateNumber(final FunctionCode functionCode,
                                       final Expressions.Int expr,
                                       final DeferredLocal target) {
        DeferredLocal result = functionCode.getOrCreateLocal(target.getPos() + 1, TypeId.INT);
        functionCode.load(result, expr.number);
        functionCode.call(Module.METHOD_INT_VALUE_OF, target, result);
    }

    private static void generateBoolean(final FunctionCode functionCode,
                                        final Expressions.Bool expr,
                                        final DeferredLocal target) {
        DeferredLocal result = functionCode.getOrCreateLocal(target.getPos() + 1, TypeId.BOOLEAN);
        functionCode.load(result, expr.value);
        functionCode.call(Module.METHOD_BOOLEAN_VALUE_OF, target, result);

    }

    private static TypeId[] convertToTypeId(List<Type> types) {
        List<TypeId> result = new ArrayList<>();
        for (Type type : types) {
            result.add(convertToTypeId(type));
        }
        return result.toArray(new TypeId[types.size()]);
    }

    private static TypeId convertToTypeId(Type type) {
        if (type instanceof TLiteral) {
            return TypeId.OBJECT;
        } else if (type instanceof TFunction) {
            int argsCount = ((TFunction) type).args.size();
            switch (argsCount) {
                case 0:
                    return Module.FUNCTION_TYPE_0;
                case 1:
                    return Module.FUNCTION_TYPE_1;
                case 2:
                    return Module.FUNCTION_TYPE_2;
                default:
                    throw new RuntimeException("Only 0, 1 and 2 arg lambdas are supported");
            }
        } else if (type instanceof Types.TVariable) {
            System.out.println("Converting type variable!");
            return TypeId.OBJECT;
        } else {
            throw new RuntimeException("Unknown type to convert to TypeId");
        }
    }
}
