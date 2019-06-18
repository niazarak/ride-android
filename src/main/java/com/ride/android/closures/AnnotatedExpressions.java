package com.ride.android.closures;

public class AnnExpressions {
    static class AnnotatedExpr<A, B> {}
    static abstract class AnnotatedLiteral<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedInt<A,B> extends AnnotatedLiteral<A,B> {}
    static class AnnotatedBool<A,B> extends AnnotatedLiteral<A,B> {}
    static class AnnotatedVariable<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedLambda<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedApplication<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedIfElse<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedDefine<A,B> extends AnnotatedExpr<A,B> {}
    static class AnnotatedLet<A,B> extends AnnotatedExpr<A,B> {}

}
