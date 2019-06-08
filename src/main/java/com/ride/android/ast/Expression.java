package com.ride.android.ast;

import com.ride.android.types.Environment;
import com.ride.android.types.Types;

/**
 * Base class for AST expression
 */
public abstract class Expression<T extends Types.Type> {
    protected T type;

    /**
     * Infers type for this expression
     * Must implement Hindley Milner typing rules
     */
    public abstract Types.Type infer(Environment env);

    public T getType() {
        return type;
    }
}
