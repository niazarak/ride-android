package com.ride.android.codegen;

import com.android.dx.MethodId;
import com.android.dx.TypeId;

public class LambdaCode implements Translatable {
    private final TypeId lambdaType;
    public final FunctionCode applyCode, constructorCode;

    public LambdaCode(TypeId superType, TypeId lambdaType, FunctionCode applyCode, FunctionCode constructorCode) {
        this.lambdaType = lambdaType;
        this.applyCode = applyCode;
        this.constructorCode = constructorCode;

        constructorCode.invokeConstructorSuper(superType.getConstructor(), constructorCode.getThis(lambdaType));
    }

    @Override
    public void compile() {
        constructorCode.returnVoid();
        constructorCode.compile();
        applyCode.compile();
    }

    public TypeId getLambdaType() {
        return lambdaType;
    }

    public MethodId getConstructorMethod() {
        return constructorCode.getMethodId();
    }
}
