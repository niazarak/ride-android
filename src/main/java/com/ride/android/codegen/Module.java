package com.ride.android.codegen;

import com.android.dx.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This is module
 * Module contains definitions and plain expressions
 * <p>
 * It is designed in this way:
 * <p>
 * There is a Main class - it is the entry point of program (contains main method)
 * <p>
 * When function is defined, new lambda class is generated and it is instantiated into a static field of Main
 * So calling a defined function means
 * - MOV function lambda field into local
 * - CALL apply method on that local (no cast needed, because there is an interface for each lambda)
 * <p>
 * When usual expression occurs, it is just generated into the main method
 */
class Module {
    public static final TypeId FUNCTION_TYPE_0 = TypeId.get("LFunction0;");
    public static final MethodId APPLY_TYPE_0 = FUNCTION_TYPE_0.getMethod(TypeId.OBJECT, "apply");
    public static final TypeId FUNCTION_TYPE_1 = TypeId.get("LFunction1;");
    public static final MethodId APPLY_TYPE_1 = FUNCTION_TYPE_1.getMethod(TypeId.OBJECT, "apply", TypeId.OBJECT);
    public static final TypeId FUNCTION_TYPE_2 = TypeId.get("LFunction2;");
    public static final MethodId APPLY_TYPE_2 = FUNCTION_TYPE_2.getMethod(TypeId.OBJECT, "apply", TypeId.OBJECT, TypeId.OBJECT);
    public static final TypeId EXCEPTION_TYPE = TypeId.get(RuntimeException.class);
    private static final TypeId MAIN_CLASS_TYPE = TypeId.get("LMain;");

    public static final TypeId<Integer> BOXED_INT = TypeId.get(Integer.class);
    public static final TypeId<Boolean> BOXED_BOOLEAN = TypeId.get(Boolean.class);
    public static final MethodId<Boolean, Boolean> METHOD_BOOLEAN_VALUE =
            Module.BOXED_BOOLEAN.getMethod(TypeId.BOOLEAN, "booleanValue");
    public static final MethodId<Boolean, Boolean> METHOD_BOOLEAN_VALUE_OF =
            Module.BOXED_BOOLEAN.getMethod(Module.BOXED_BOOLEAN, "valueOf", TypeId.BOOLEAN);
    public static final MethodId<Integer, Integer> METHOD_INT_VALUE_OF =
            Module.BOXED_INT.getMethod(Module.BOXED_INT, "valueOf", TypeId.INT);

    private final List<Translatable> funcs = new ArrayList<>();
    private final DexMaker maker;
    private final FunctionCode clinit;
    private int generatedLambdas = 0;
    private int generatedDefinitions = 0;

    public Module() {
        maker = new DexMaker();
        maker.declare(MAIN_CLASS_TYPE, "Main.compiled", Modifier.PUBLIC, TypeId.OBJECT);

        maker.declare(FUNCTION_TYPE_0, "Function.compiled", Modifier.PUBLIC | Modifier.ABSTRACT, TypeId.OBJECT);
        declareClass(APPLY_TYPE_0);
        maker.declare(FUNCTION_TYPE_1, "Function.compiled", Modifier.PUBLIC | Modifier.ABSTRACT, TypeId.OBJECT);
        declareClass(APPLY_TYPE_1);
        maker.declare(FUNCTION_TYPE_2, "Function.compiled", Modifier.PUBLIC | Modifier.ABSTRACT, TypeId.OBJECT);
        declareClass(APPLY_TYPE_2);

        MethodId staticInitializer = MAIN_CLASS_TYPE.getStaticInitializer();
        Code clinitCode = maker.declare(staticInitializer, Modifier.STATIC);
        clinit = new FunctionCode(clinitCode, staticInitializer);
        funcs.add(clinit);
    }

    private void declareClass(MethodId applyType1) {
        Code apply1code = maker.declare(applyType1, Modifier.PUBLIC);
        Local exceptionLocal = apply1code.newLocal(EXCEPTION_TYPE);
        apply1code.newInstance(exceptionLocal, EXCEPTION_TYPE.getConstructor());
        apply1code.throwValue(exceptionLocal);
    }

    /**
     * Declares static functions
     */
    public FunctionCode make(TypeId returnType, String name, TypeId... parameters) {
        // make function typeId in Main class
        MethodId methodType = MAIN_CLASS_TYPE.getMethod(returnType, name, parameters);

        // declare it
        Code code = maker.declare(methodType, Modifier.STATIC | Modifier.PUBLIC);

        // wrap it into our delegate
        FunctionCode functionCode = new FunctionCode(code, methodType);
        funcs.add(functionCode);
        return functionCode;
    }

    static class ModuleDefinition {
        final LambdaCode lambdaCode;
        final FieldId definitionField;

        ModuleDefinition(LambdaCode lambdaCode, FieldId definitionField) {
            this.lambdaCode = lambdaCode;
            this.definitionField = definitionField;
        }
    }

    /**
     * Declares new lambda, instantiates it into static field with the name
     * <p>
     * Returns Code delegate (with which you do stuff in the apply method)
     */
    ModuleDefinition makeDefine(String name, TypeId returnType, TypeId... parameters) {
        LambdaCode lambdaCode = makeLambda(returnType, parameters);

        FieldId functionField = MAIN_CLASS_TYPE.getField(lambdaCode.getLambdaType(), name);
        maker.declare(functionField, Modifier.PUBLIC | Modifier.STATIC, null);
        LocalWrapper lambdaLocal = clinit.getOrCreateLocal(generatedDefinitions++, lambdaCode.getLambdaType());
        clinit.newInstance(lambdaCode.getConstructorMethod(), lambdaLocal);
        clinit.sput(functionField, lambdaLocal);
        return new ModuleDefinition(lambdaCode, functionField);
    }

    /**
     * Declares lambda with apply method and costructor
     */
    LambdaCode makeLambda(TypeId returnType, TypeId... parameters) {
        final int argCount = parameters.length;
        TypeId superType;
        switch (argCount) {
            case 0:
                superType = FUNCTION_TYPE_0;
                break;
            case 1:
                superType = FUNCTION_TYPE_1;
                break;
            case 2:
                superType = FUNCTION_TYPE_2;
                break;
            default:
                throw new RuntimeException("Only 1 and 2 arg lambdas are supported");
        }

        // declare a subclass of Function
        String lambdaName = "LLambda" + generatedLambdas++ + ";";
        TypeId lambdaType = TypeId.get(lambdaName);
        maker.declare(lambdaType, lambdaName + ".compiled", Modifier.PUBLIC, superType);

        // declare constructor
        MethodId lambdaConstructorType = lambdaType.getConstructor();
        Code lambdaConstructorCode = maker.declare(lambdaConstructorType, Modifier.PUBLIC);

        // declare "apply" method
        MethodId applyMethodType = lambdaType.getMethod(TypeId.OBJECT, "apply", parameters);
        Code applyMethodCode = maker.declare(applyMethodType, Modifier.PUBLIC);

        LambdaCode lambdaCode = new LambdaCode(superType, lambdaType,
                new FunctionCode(applyMethodCode, applyMethodType),
                new FunctionCode(lambdaConstructorCode, lambdaConstructorType)
        );
        funcs.add(lambdaCode);
        return lambdaCode;
    }

    public FunctionCode makeMain() {
        return make(TypeId.VOID, "main", TypeId.get(String[].class));
    }

    byte[] compile() {
        clinit.returnVoid();

        for (Translatable func : funcs) {
            func.compile();
        }

        return maker.generate();
    }
}
