package com.ride.android;

import com.android.dx.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionCode {
    private final List<Map<TypeId, LocalWrapper>> locals = new ArrayList<>();
    private final List<Instructions.Instruction> instructions = new ArrayList<>();
    private final Code code;
    private final MethodId methodId;

    public FunctionCode(final Code code, final MethodId methodId) {
        this.methodId = methodId;
        this.code = code;
    }

    LocalWrapper getOrCreateLocal(final int finalPos) {
        return getOrCreateLocal(finalPos, TypeId.INT);
    }

    LocalWrapper getOrCreateLocal(final int finalPos, final TypeId<?> typeId) {
        if (finalPos > locals.size()) {
            throw new RuntimeException("Somehow registers do not increment");
        } else if (locals.size() == finalPos) {
            LocalWrapper local = new LocalWrapper<>(finalPos, typeId);
            Map<TypeId, LocalWrapper> localsMap = new HashMap<>();
            localsMap.put(typeId, local);
            locals.add(localsMap);
            return local;
        }
        if (locals.get(finalPos).get(typeId) == null) {
            locals.get(finalPos).put(typeId, new LocalWrapper<>(finalPos, typeId));
        }
        return locals.get(finalPos).get(typeId);
    }

    public MethodId getMethodId() {
        return methodId;
    }

    static class VarWrapper<T> {
        private com.android.dx.Local<T> realLocal;

        VarWrapper(Local<T> realLocal) {
            this.realLocal = realLocal;
        }

        public com.android.dx.Local<T> getRealLocal() {
            return realLocal;
        }
    }

    VarWrapper getParam(int index) {
        return new VarWrapper<>(code.getParameter(index, TypeId.INT));
    }

    void move(LocalWrapper dest, LocalWrapper target) {
        instructions.add(code -> code.move(dest.getRealLocal(), target.getRealLocal()));
    }

    void move(LocalWrapper dest, VarWrapper target) {
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

    void sget(FieldId field, LocalWrapper dest) {
        instructions.add(code -> code.sget(field, dest.getRealLocal()));
    }

    void invokeVirtual(MethodId method, LocalWrapper instance, LocalWrapper... args) {
        instructions.add(code -> {
            Local[] realArgs = new Local[args.length];
            for (int i = 0, argsLength = args.length; i < argsLength; i++) {
                realArgs[i] = args[i].getRealLocal();
            }
            code.invokeVirtual(method, null, instance.getRealLocal(), realArgs);
        });
    }

    void call(MethodId method, LocalWrapper target, LocalWrapper... args) {
        instructions.add(code -> {
            Local[] realArgs = new Local[args.length];
            for (int i = 0, argsLength = args.length; i < argsLength; i++) {
                realArgs[i] = args[i].getRealLocal();
            }
            code.invokeStatic(method, target.getRealLocal(), realArgs);
        });
    }

    void returnVoid() {
        instructions.add(Code::returnVoid);
    }

    void returnValue(LocalWrapper result) {
        instructions.add(code -> code.returnValue(result.getRealLocal()));
    }

    void compile() {
        // System.out.println("Locals to compile:" + locals);
        for (Map<TypeId, LocalWrapper> localsMap : locals) {
            for (LocalWrapper local : localsMap.values()) {
                local.generate(code);
            }
        }
        // System.out.println("Insns to compile:" + instructions);
        for (Instructions.Instruction instruction : instructions) {
            instruction.generate(code);
        }
    }
}

