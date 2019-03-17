package com.ride.android.codegen;

import com.android.dx.Code;
import com.android.dx.DexMaker;
import com.android.dx.MethodId;
import com.android.dx.TypeId;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

class Module {
    private static final TypeId<?> MAIN_CLASS_TYPE = TypeId.get("LMain;");

    private final List<FunctionCode> funcs = new ArrayList<>();
    private final DexMaker maker;

    public Module() {
        maker = new DexMaker();
    }

    public FunctionCode make(TypeId returnType, String name, TypeId... parameters) {
        MethodId methodType = MAIN_CLASS_TYPE.getMethod(returnType, name, parameters);
        Code code = maker.declare(methodType, Modifier.STATIC | Modifier.PUBLIC);
        FunctionCode functionCode = new FunctionCode(code, methodType);
        funcs.add(functionCode);
        return functionCode;
    }

    public FunctionCode makeMain() {
        return make(TypeId.VOID, "main", TypeId.get(String[].class));
    }

    byte[] compile() {
        maker.declare(MAIN_CLASS_TYPE, "Main.compiled", Modifier.PUBLIC, TypeId.OBJECT);

        for (FunctionCode func : funcs) {
            func.compile();
        }

        return maker.generate();
    }
}
