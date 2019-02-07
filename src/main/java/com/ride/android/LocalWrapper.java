package com.ride.android;

import com.android.dx.Code;
import com.android.dx.TypeId;

public class LocalWrapper<T> {
    private com.android.dx.Local<T> realLocal;
    private TypeId<T> typeId;
    private int pos;

    public LocalWrapper(int pos) {
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

    public int getPos() {
        return pos;
    }
}
