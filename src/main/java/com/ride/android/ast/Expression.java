package com.ride.android.ast;

import com.ride.inference.Environment;
import com.ride.inference.Types;

public abstract class Expression<T extends Types.Type> {
    protected T type;

    abstract Types.Type infer(Environment env);

    public T getType() {
        return type;
    }
}
