package com.ride.android;

import com.android.dx.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class Module {
    private List<LocalWrapper> locals = new ArrayList<>();

    private List<Instructions.Instruction> instructions = new ArrayList<>();

    LocalWrapper getOrCreateLocal(final int finalPos) {
        if (locals.size() == finalPos) {
            LocalWrapper local = new LocalWrapper<>(finalPos);
            locals.add(local);
            return local;
        } else if (finalPos > locals.size()) {
            throw new RuntimeException("Somehow registers do not increment");
        }
        if (locals.get(finalPos) == null) {
            locals.set(finalPos, new LocalWrapper<>(finalPos));
        }
        return locals.get(finalPos);
    }

    void move(LocalWrapper dest, LocalWrapper target) {
        instructions.add(code -> code.move(dest.getRealLocal(), target.getRealLocal()));
    }

    void add(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.ADD, dest, a, b));
    }

    void subtract(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.SUBTRACT, dest, a, b));
    }

    void multiply(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.MULTIPLY, dest, a, b));
    }

    void divide(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.DIVIDE, dest, a, b));
    }

    void remainder(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.REMAINDER, dest, a, b));
    }

    void and(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.AND, dest, a, b));
    }

    void or(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.OR, dest, a, b));
    }

    void xor(LocalWrapper dest, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.BinaryInstruction(BinaryOp.XOR, dest, a, b));
    }

    void compare(Comparison comparison, Label trueLabel, LocalWrapper a, LocalWrapper b) {
        instructions.add(new Instructions.CompareInstruction(comparison, trueLabel, a, b));
    }

    void compareZ(Label trueLabel, LocalWrapper a) {
        instructions.add(new Instructions.CompareZInstruction(Comparison.EQ, trueLabel, a));
    }

    void jump(Label label) {
        instructions.add(new Instructions.JumpInstruction(label));
    }

    void markLabel(Label label) {
        instructions.add((code -> code.mark(label)));
    }

    void load(LocalWrapper dest, int constant) {
        instructions.add(new Instructions.LoadInstruction(dest, constant));
    }

    TypeId<System> systemType = TypeId.get(System.class);
    TypeId<PrintStream> printStreamType = TypeId.get(PrintStream.class);

    byte[] compile() {
        DexMaker maker = new DexMaker();
        TypeId<?> mainClassType = TypeId.get("LMain;");
        maker.declare(mainClassType, "Main.compiled", Modifier.PUBLIC, TypeId.OBJECT);

        MethodId mainMethodType = mainClassType.getMethod(TypeId.VOID, "main", TypeId.get(String[].class));
        Code code = maker.declare(mainMethodType, Modifier.STATIC | Modifier.PUBLIC);

        Local<PrintStream> localSystemOut = code.newLocal(printStreamType);

        // System.out.println("Locals to compile:" + locals);
        for (LocalWrapper local : locals) {
            local.generate(code);
        }
        // System.out.println("Insns to compile:" + instructions);
        for (Instructions.Instruction instruction : instructions) {
            instruction.generate(code);
        }

        FieldId<System, PrintStream> systemOutField = systemType.getField(printStreamType, "out");
        code.sget(systemOutField, localSystemOut);
        MethodId<PrintStream, Void> printlnMethod = printStreamType.getMethod(
                TypeId.VOID, "println", TypeId.INT);
        code.invokeVirtual(printlnMethod, null, localSystemOut, locals.get(0).getRealLocal());

        code.returnVoid();

        return maker.generate();
    }

}
