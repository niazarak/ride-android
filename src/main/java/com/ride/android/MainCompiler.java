package com.ride.android;

import com.ride.android.ast.Ast;
import com.ride.android.parser.Parser;
import com.ride.android.parser.Tokenizer;
import com.ride.android.codegen.Generator;
import picocli.CommandLine;

import java.io.*;

public class MainCompiler {

    @CommandLine.Command(name = "ride-android")
    static class CompilerOptions {
        @CommandLine.Parameters(paramLabel = "INPUT", description = "Source file")
        File input;

        @CommandLine.Option(names = {"-o"},
                description = "Output file (default: ${DEFAULT-VALUE})")
        File output = new File("classes.dex");

        @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
        boolean usageHelpRequested;
    }

    public static void main(String[] args) {
        CompilerOptions options = CommandLine.populateCommand(new CompilerOptions(), args);
        if (options.usageHelpRequested || options.input == null) {
            CommandLine.usage(new CompilerOptions(), System.out);
            return;
        }
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(options.input)));
            String line = reader.readLine();
            while (line != null) {
                result.append(line);
                line = reader.readLine();
            }

        } catch (FileNotFoundException e) {
            System.out.println("\"" + options.input.getName() + "\" not found");
            return;
        } catch (IOException e) {
            System.out.println("Unhandled error while reading input file:");
            e.printStackTrace();
            return;
        }

        try {
            if (!options.output.exists()) {
                if (!options.output.createNewFile()) {
                    System.out.println("Couldn't create output file");
                    return;
                }
            }
            FileOutputStream output = new FileOutputStream(options.output);
            compile(result.toString(), output);
            output.flush();
            output.close();
        } catch (IOException e) {
            System.out.println("Unhandled error while writing to output file:");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Unhandled error during compilation");
            e.printStackTrace();
        }
    }

    static void compile(final String input, OutputStream output) throws IOException {
        byte[] program = Generator.generate(Ast.ast(Parser.parse(Tokenizer.tokenize(input))));
        output.write(program);
    }
}
