package com.ride.android.codegen;

import com.android.dx.Comparison;
import com.android.dx.Label;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

/**
 * Contains builtins for our language, nothing unusual
 * <p>
 * Worth mentioning - all functions here are implemented with lambdas, so most of the code is boxing/unboxing routine
 */
public class Builtins {

    interface OperationDelegate {
        void apply(FunctionCode functionCode, LocalWrapper target, LocalWrapper... args);
    }

    private static void initArithmeticOp(final CodegenEnvironment baseEnvironment,
                                         final Module module,
                                         final String realName,
                                         final String alias,
                                         final OperationDelegate delegate) {
        TypeId numberType = TypeId.get(Number.class);
        MethodId intValueMethod = numberType.getMethod(TypeId.INT, "intValue");

        Module.ModuleDefinition moduleDefinition = module.makeDefine(realName, TypeId.OBJECT, TypeId.OBJECT, TypeId.OBJECT);
        FunctionCode applyCode = moduleDefinition.lambdaCode.applyCode;
        // params
        ParamLocal param = applyCode.getParam(0, TypeId.OBJECT);
        ParamLocal param2 = applyCode.getParam(1, TypeId.OBJECT);

        // locals
        LocalWrapper resultObj = applyCode.getOrCreateLocal(0, TypeId.OBJECT);
        LocalWrapper castedParam = applyCode.getOrCreateLocal(1, numberType);
        LocalWrapper castedParam2 = applyCode.getOrCreateLocal(2, numberType);
        LocalWrapper result = applyCode.getOrCreateLocal(3, TypeId.INT);
        LocalWrapper arg1 = applyCode.getOrCreateLocal(4, TypeId.INT);
        LocalWrapper arg2 = applyCode.getOrCreateLocal(5, TypeId.INT);

        applyCode.cast(castedParam, param);
        applyCode.cast(castedParam2, param2);
        applyCode.invokeVirtual(intValueMethod, arg1, castedParam);
        applyCode.invokeVirtual(intValueMethod, arg2, castedParam2);
        delegate.apply(applyCode, result, arg1, arg2);
        applyCode.call(Module.METHOD_INT_VALUE_OF, resultObj, result);
        applyCode.returnValue(resultObj);

        baseEnvironment.add(alias, new Generator.DefinitionEntry(moduleDefinition.definitionField));
    }

    private static void initComparisonOp(final CodegenEnvironment baseEnvironment,
                                         final Module module,
                                         final String realName,
                                         final String alias,
                                         final OperationDelegate delegate) {
        TypeId numberType = TypeId.get(Number.class);
        MethodId intValueMethod = numberType.getMethod(TypeId.INT, "intValue");

        Module.ModuleDefinition moduleDefinition = module.makeDefine(realName, TypeId.OBJECT, TypeId.OBJECT, TypeId.OBJECT);
        FunctionCode applyCode = moduleDefinition.lambdaCode.applyCode;
        // params
        ParamLocal param = applyCode.getParam(0, TypeId.OBJECT);
        ParamLocal param2 = applyCode.getParam(1, TypeId.OBJECT);

        // locals
        LocalWrapper resultObj = applyCode.getOrCreateLocal(0, TypeId.OBJECT);
        LocalWrapper castedParam = applyCode.getOrCreateLocal(1, numberType);
        LocalWrapper castedParam2 = applyCode.getOrCreateLocal(2, numberType);
        LocalWrapper result = applyCode.getOrCreateLocal(3, TypeId.BOOLEAN);
        LocalWrapper arg1 = applyCode.getOrCreateLocal(4, TypeId.INT);
        LocalWrapper arg2 = applyCode.getOrCreateLocal(5, TypeId.INT);

        applyCode.cast(castedParam, param);
        applyCode.cast(castedParam2, param2);
        applyCode.invokeVirtual(intValueMethod, arg1, castedParam);
        applyCode.invokeVirtual(intValueMethod, arg2, castedParam2);
        delegate.apply(applyCode, result, arg1, arg2);
        applyCode.call(Module.METHOD_BOOLEAN_VALUE_OF, resultObj, result);
        applyCode.returnValue(resultObj);

        baseEnvironment.add(alias, new Generator.DefinitionEntry(moduleDefinition.definitionField));
    }

    private static void initLogicalOp(final CodegenEnvironment baseEnvironment,
                                      final Module module,
                                      final String realName,
                                      final String alias,
                                      final OperationDelegate delegate) {
        Module.ModuleDefinition moduleDefinition = module.makeDefine(realName, TypeId.OBJECT, TypeId.OBJECT, TypeId.OBJECT);
        FunctionCode applyCode = moduleDefinition.lambdaCode.applyCode;
        // params
        ParamLocal param = applyCode.getParam(0, TypeId.OBJECT);
        ParamLocal param2 = applyCode.getParam(1, TypeId.OBJECT);

        // locals
        LocalWrapper resultObj = applyCode.getOrCreateLocal(0, TypeId.OBJECT);
        LocalWrapper castedParam = applyCode.getOrCreateLocal(1, Module.BOXED_BOOLEAN);
        LocalWrapper castedParam2 = applyCode.getOrCreateLocal(2, Module.BOXED_BOOLEAN);
        LocalWrapper result = applyCode.getOrCreateLocal(3, TypeId.BOOLEAN);
        LocalWrapper arg1 = applyCode.getOrCreateLocal(4, TypeId.BOOLEAN);
        LocalWrapper arg2 = applyCode.getOrCreateLocal(5, TypeId.BOOLEAN);

        applyCode.cast(castedParam, param);
        applyCode.cast(castedParam2, param2);
        applyCode.invokeVirtual(Module.METHOD_BOOLEAN_VALUE, arg1, castedParam);
        applyCode.invokeVirtual(Module.METHOD_BOOLEAN_VALUE, arg2, castedParam2);
        delegate.apply(applyCode, result, arg1, arg2);
        applyCode.call(Module.METHOD_BOOLEAN_VALUE_OF, resultObj, result);
        applyCode.returnValue(resultObj);

        baseEnvironment.add(alias, new Generator.DefinitionEntry(moduleDefinition.definitionField));
    }

    static void initBuiltins(final CodegenEnvironment baseEnvironment, final Module module) {
        initArithmeticOp(baseEnvironment, module, "add", "+",
                (functionCode, target, args) -> functionCode.add(target, args[0], args[1]));
        initArithmeticOp(baseEnvironment, module, "subtract", "-",
                (functionCode, target, args) -> functionCode.subtract(target, args[0], args[1]));
        initArithmeticOp(baseEnvironment, module, "mul", "*",
                (functionCode, target, args) -> functionCode.multiply(target, args[0], args[1]));
        initArithmeticOp(baseEnvironment, module, "divide", "/",
                (functionCode, target, args) -> functionCode.divide(target, args[0], args[1]));
        initArithmeticOp(baseEnvironment, module, "remainder", "%",
                (functionCode, target, args) -> functionCode.remainder(target, args[0], args[1]));

        initComparisonOp(baseEnvironment, module, "ne", "!=",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.NE, args[0], args[1]));
        initComparisonOp(baseEnvironment, module, "eq", "==",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.EQ, args[0], args[1]));
        initComparisonOp(baseEnvironment, module, "ge", ">=",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.GE, args[0], args[1]));
        initComparisonOp(baseEnvironment, module, "gt", ">",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.GT, args[0], args[1]));
        initComparisonOp(baseEnvironment, module, "le", "<=",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.LE, args[0], args[1]));
        initComparisonOp(baseEnvironment, module, "lt", "<",
                (functionCode, target, args) -> generateComparison(functionCode, target, Comparison.LT, args[0], args[1]));

        initLogicalOp(baseEnvironment, module, "and", "and",
                (functionCode, target, args) -> functionCode.and(target, args[0], args[1]));
        initLogicalOp(baseEnvironment, module, "xor", "xor",
                (functionCode, target, args) -> functionCode.xor(target, args[0], args[1]));
        initLogicalOp(baseEnvironment, module, "or", "or",
                (functionCode, target, args) -> functionCode.or(target, args[0], args[1]));
    }

    private static void generateComparison(FunctionCode functionCode, LocalWrapper<Boolean> target,
                                           Comparison comparison, LocalWrapper a, LocalWrapper b) {
        Label thenLabel = new Label();
        Label afterLabel = new Label();
        // if
        functionCode.compare(comparison, thenLabel, a, b);

        // else
        functionCode.load(target, false);
        functionCode.jump(afterLabel);

        // then
        functionCode.markLabel(thenLabel);
        functionCode.load(target, true);

        // after
        functionCode.markLabel(afterLabel);
    }

}
