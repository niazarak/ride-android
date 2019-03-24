package com.ride.android.codegen;

import com.android.dx.Code;
import com.android.dx.TypeId;

/**
 * Wraps a local that has to be generated
 * <p>
 * Why do we need it? Because all locals must be generated before any instruction.
 * (Generation of local means calling {@link Code#newLocal(TypeId)})
 * So we add a layer of indirection - we can operate on this object while generating expressions.
 */
public class DeferredLocal<T> implements LocalWrapper<T> {
    private com.android.dx.Local<T> realLocal;
    private TypeId<T> typeId;
    private int pos;

    public DeferredLocal(int pos, TypeId<T> typeId) {
        this.typeId = typeId;
        this.pos = pos;
    }

    @Override
    public com.android.dx.Local<T> getRealLocal() {
        if (realLocal == null) {
            throw new RuntimeException("Accessing locals before generating them is forbidden");
        }
        return realLocal;
    }

    public void generate(Code code) {
        realLocal = code.newLocal(typeId);
    }

    public int getPos() {
        return pos;
    }
}
