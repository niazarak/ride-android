package com.ride.android.ast;

import com.ride.android.types.Environment;
import com.ride.android.types.Types;

import java.util.ArrayList;
import java.util.List;

import static com.ride.android.types.Types.bool;
import static com.ride.android.types.Types.integer;

/**
 * Expressions that are supported in the language
 */
public class Expressions {
    public static class Application extends Expression<Types.Type> {
        private final List<Expression> args;
        public final Expression<Types.TFunction> function;

        public Application(Expression<Types.TFunction> function, List<Expression> args) {
            this.function = function;
            this.args = args;
        }

        public Expression getArg(int i) {
            return args.get(i);
        }

        public List<Expression> getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return "App{" +
                    "function=" + function +
                    ", args=" + args +
                    "} : " + type;
        }

        @Override
        public Types.Type infer(Environment env) {
            Types.Type functionType = function.infer(env);
            List<Types.Type> argsType = new ArrayList<>();
            for (Expression arg : args) {
                argsType.add(arg.infer(env));
            }
            Types.Type resultType = env.newvar();
            Types.TFunction unifiedFunctionType = new Types.TFunction(argsType, resultType);
            if (env.unify(functionType, unifiedFunctionType)) {
                type = resultType.expose(env);
                return resultType;
            } else
                throw new RuntimeException(unifiedFunctionType + " and " + functionType + " cannot be unified");
        }
    }

    public static class Definition extends Expression<Types.TFunction> {
        public final String name;
        public final Expression body;

        private final List<String> args;

        public Definition(String name, List<String> args, Expression body) {
            this.name = name;
            this.args = args;
            this.body = body;
        }

        public String getArg(int i) {
            return args.get(i);
        }

        public List<String> getArgs() {
            return args;
        }

        @Override
        public Types.Type infer(Environment environment) {
            environment.push();
            List<Types.Type> argsTypes = new ArrayList<>();
            for (String arg : args) {
                Types.Type argType = environment.newvar();
                environment.define(arg, argType);
                argsTypes.add(argType);
            }
            Types.TFunction resultFunctionType = new Types.TFunction(argsTypes, body.infer(environment));
            type = (Types.TFunction) resultFunctionType.expose(environment);
            environment.pop();
            environment.define(name, type);
            return resultFunctionType;
        }

        @Override
        public String toString() {
            return "Def{" +
                    "name='" + name + '\'' +
                    ", args=" + args +
                    ", body=" + body +
                    "} : " + type;
        }
    }

    public static class Lambda extends Expression<Types.TFunction> {
        public final List<String> args;
        public final Expression body;

        public Lambda(List<String> args, Expression body) {
            this.args = args;
            this.body = body;
        }

        public String getArg(int i) {
            return args.get(i);
        }

        @Override
        public String toString() {
            return "Lambda{" +
                    "args=" + args +
                    ", body=" + body +
                    "} : " + type;
        }

        @Override
        public Types.Type infer(Environment environment) {
            environment.push();
            List<Types.Type> argsTypes = new ArrayList<>();
            for (String arg : args) {
                Types.Type argType = environment.newvar();
                environment.define(arg, argType);
                argsTypes.add(argType);
            }
            Types.TFunction resultFunctionType = new Types.TFunction(argsTypes, body.infer(environment));
            type = (Types.TFunction) resultFunctionType.expose(environment);
            environment.pop();
            return resultFunctionType;
        }
    }

    public static class Let extends Expression {
        public final String var;
        public final Expression varExpr;
        public final Expression body;

        public Let(String var, Expression varExpr, Expression body) {
            this.var = var;
            this.varExpr = varExpr;
            this.body = body;
        }

        @Override
        public Types.Type infer(Environment env) {
            Types.Type varExprType = varExpr.infer(env);
            return env.scoped(scopedEnv -> {
                scopedEnv.define(var, scopedEnv.generalize(varExprType));
                Types.Type infer = body.infer(scopedEnv);
                type = infer.expose(scopedEnv);
                return infer;
            });
        }

        @Override
        public String toString() {
            return "Let{" +
                    "var='" + var + '\'' +
                    ", varExpr=" + varExpr +
                    ", body=" + body +
                    "} : " + type;
        }
    }

    public static class LetRec extends Expression {
        public final String var;
        public final Expression varExpr;
        public final Expression body;

        public LetRec(String var, Expression varExpr, Expression body) {
            this.var = var;
            this.varExpr = varExpr;
            this.body = body;
        }

        @Override
        public Types.Type infer(Environment env) {
            return env.scoped(scopedEnv -> {
                Types.Type varTmpType = scopedEnv.newvar();
                scopedEnv.define(var, varTmpType);

                Types.Type varType = varExpr.infer(scopedEnv);
                if (scopedEnv.unify(varTmpType, varType)) {
                    scopedEnv.define(var, scopedEnv.generalize(varType));
                    Types.Type bodyType = body.infer(scopedEnv);

                    // ADDITIONAL PASS SO REC VAR GETS THE RIGHT TYPE
                    varExpr.infer(scopedEnv);

                    type = bodyType.expose(scopedEnv);
                    return bodyType;
                } else {
                    throw new RuntimeException(varTmpType + " and " + varType + " cannot be unified");
                }
            });
        }

        @Override
        public String toString() {
            return "LetRec{" +
                    "var='" + var + '\'' +
                    ", varExpr=" + varExpr +
                    ", body=" + body +
                    "} : " + type;
        }
    }

    public static class IfExpr extends Expression {
        public final Expression condition, ifBranch, elseBranch;

        public IfExpr(Expression condition, Expression ifBranch, Expression elseBranch) {
            this.condition = condition;
            this.ifBranch = ifBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public String toString() {
            return "IfExpr{" +
                    "condition=" + condition +
                    ", ifBranch=" + ifBranch +
                    ", elseBranch=" + elseBranch +
                    "} : " + type;
        }

        @Override
        public Types.Type infer(Environment env) {
            Types.Type conditionType = condition.infer(env);
            if (!conditionType.expose(env).equals(bool())) {
                throw new RuntimeException("Condition must be boolean");
            }
            Types.Type ifType = ifBranch.infer(env);
            Types.Type elseType = elseBranch.infer(env);
            if (!ifType.expose(env).equals(elseType.expose(env))) {
                throw new RuntimeException("Condition branches must have same types");
            }
            type = ifType.expose(env);
            return ifType;
        }
    }

    public static abstract class Literal extends Expression<Types.TLiteral> {
    }

    public static class Int extends Literal {
        public final int number;

        public Int(int number) {
            this.number = number;
        }

        @Override
        public String toString() {
            return "Int{" + number + "} : " + type;
        }

        @Override
        public Types.Type infer(Environment env) {
            return type = integer(); // assignment is expression
        }
    }

    public static class Bool extends Literal {
        public final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Bool{" + value + "} : " + type;
        }

        @Override
        public Types.Type infer(Environment env) {
            return type = bool(); // assignment is expression
        }
    }

    public static class Variable extends Expression {
        public final String name;

        public Variable(String symbol) {
            this.name = symbol;
        }

        @Override
        public String toString() {
            return "Var{" + name + "} : " + type;
        }

        @Override
        public Types.Type infer(Environment environment) {
            Types.TScheme varType = environment.lookup(name);
            if (varType == null) {
                throw new RuntimeException(name + " is not found in environment");
            }
            type = varType.instantiate(environment);
            return type;
        }
    }
}
