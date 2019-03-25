package com.ride.android.ast;

import com.ride.inference.Environment;
import com.ride.inference.Types;

/**
 * Base class for AST expression
 */
public abstract class Expression<T extends Types.Type> {
    protected T type;

    /**
     * Infers type for this expression
     * Must implement Hindley Milner typing rules
     */
    abstract Types.Type infer(Environment env);

    public T getType() {
        return type;
    }
}
