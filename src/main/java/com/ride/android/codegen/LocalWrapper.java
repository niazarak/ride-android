package com.ride.android.codegen;

/**
 * Wraps locals
 * See {@link DeferredLocal} and {@link ParamLocal}
 */
public interface LocalWrapper<T> {
    /**
     * Returns the wrapped local
     * Must be used during low level compilation
     */
    com.android.dx.Local<T> getRealLocal();
}
