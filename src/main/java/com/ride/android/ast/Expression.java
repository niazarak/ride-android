package com.ride.android.ast;

import com.ride.inference.Environment;
import com.ride.inference.Types;

public abstract class Expression {
    protected Types.Type type;

    abstract Types.Type infer(Environment env);

    public Types.Type getType() {
        return type;
    }
}
