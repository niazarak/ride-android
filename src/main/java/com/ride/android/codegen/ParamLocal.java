package com.ride.android.codegen;

import com.android.dx.Local;

/**
 * Wraps function arguments - locals that are already "generated"
 */
class ParamLocal<T> implements LocalWrapper<T> {
    private com.android.dx.Local<T> realLocal;

    ParamLocal(Local<T> realLocal) {
        this.realLocal = realLocal;
    }

    @Override
    public Local<T> getRealLocal() {
        return realLocal;
    }
}
