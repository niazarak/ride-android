package com.ride.android.types;

import com.ride.android.ast.Ast;
import com.ride.android.ast.Expression;
import com.ride.android.parser.Parser;
import com.ride.android.parser.Tokenizer;

import java.util.List;

import static com.ride.android.types.Types.*;

/**
 * Infers types for each expression in AST.
 * After inference, each expression is assigned its type.
 */
public class TypeChecker {
    /**
     * Main method
     * Input is undecorated AST
     * Output is Decorated AST
     */
    public static List<Expression> infer(List<Expression> expressions) {
        Environment env = makeEnvironment();
        for (Expression expression : expressions) {
            expression.infer(env);
        }
        System.out.println("Decorated expressions: " + expressions);
        return expressions;
    }

    private static Environment makeEnvironment() {
        Environment environment = new Environment();
        environment.define("+", func(args(integer(), integer()), integer()));
        environment.define("-", func(args(integer(), integer()), integer()));
        environment.define("*", func(args(integer(), integer()), integer()));
        environment.define("/", func(args(integer(), integer()), integer()));
        environment.define("%", func(args(integer(), integer()), integer()));
        environment.define("!=", func(args(integer(), integer()), bool()));
        environment.define("==", func(args(integer(), integer()), bool()));
        environment.define(">=", func(args(integer(), integer()), bool()));
        environment.define(">", func(args(integer(), integer()), bool()));
        environment.define("<=", func(args(integer(), integer()), bool()));
        environment.define("<", func(args(integer(), integer()), bool()));
        environment.define("and", func(args(bool(), bool()), bool()));
        environment.define("or", func(args(bool(), bool()), bool()));
        environment.define("xor", func(args(bool(), bool()), bool()));
        return environment;
    }

    // for test purposes
    public static void main(String[] args) {
        // final String input = "(xor #t #f)";
        // final String input = "(+ 12 (if (> 5 10) 1 0))";
        // final String input = "(define (funA a) (+ a 7)) (define (funB b) (- b (funA 2))) (funB 2)";
        // final String input = "(+ 12 (if (> 5 10) 1 0))(+ 2 2)(+ 2 (if (> 5 10) 1 0))";
        final String input = "((lambda (a) (+ a 7)) 2)";

        List<Expression> ast = Ast.ast(Parser.parse(Tokenizer.tokenize(input)));
        System.out.println("Ast: ");
        for (Expression expression : ast) {
            System.out.println(expression);
        }
        List<Expression> decoratedAst = infer(ast);
        System.out.println();
        System.out.println("DecoratedAst: ");
        for (Expression expression : decoratedAst) {
            System.out.println(expression);
        }
    }
}
