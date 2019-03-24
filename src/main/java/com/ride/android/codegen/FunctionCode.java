package com.ride.android.codegen;

import com.android.dx.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents code of the declared function
 * <p>
 * It accumulates all instructions and locals, that must be generated.
 * And also wraps {@link Code} instance for declared function
 * One of the main reasons this exists - locals need to be generated before instructions.
 * That's why they are stored separately.
 */
public class FunctionCode implements Translatable {
    private final List<Map<TypeId, DeferredLocal>> locals = new ArrayList<>();
    private final List<Instructions.Instruction> instructions = new ArrayList<>();
    private final Code code;
    private final MethodId methodId;

    public FunctionCode(final Code code, final MethodId methodId) {
        this.methodId = methodId;
        this.code = code;
    }

    DeferredLocal getOrCreateLocal(final int finalPos, final TypeId<?> typeId) {
        if (finalPos > locals.size()) {
            throw new RuntimeException("Somehow registers do not increment");
        } else if (locals.size() == finalPos) {
            DeferredLocal local = new DeferredLocal<>(finalPos, typeId);
            Map<TypeId, DeferredLocal> localsMap = new HashMap<>();
            localsMap.put(typeId, local);
            locals.add(localsMap);
            return local;
        }
        if (locals.get(finalPos).get(typeId) == null) {
            locals.get(finalPos).put(typeId, new DeferredLocal<>(finalPos, typeId));
        }
        return locals.get(finalPos).get(typeId);
    }

    public MethodId getMethodId() {
        return methodId;
    }

    ParamLocal getParam(int index, TypeId typeId) {
        return new ParamLocal<>(code.getParameter(index, typeId));
    }

    ParamLocal getThis(TypeId typeId) {
        return new ParamLocal(code.getThis(typeId));
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

    <T> void load(LocalWrapper<T> dest, T constant) {
        instructions.add(new Instructions.LoadInstruction(dest, constant));
    }

    void sget(FieldId field, LocalWrapper dest) {
        instructions.add(code -> code.sget(field, dest.getRealLocal()));
    }

    void sput(FieldId field, LocalWrapper src) {
        instructions.add(code -> code.sput(field, src.getRealLocal()));
    }

    void invokeVirtual(MethodId method, LocalWrapper target, LocalWrapper instance, DeferredLocal... args) {
        instructions.add(code -> {
            Local[] realArgs = new Local[args.length];
            for (int i = 0, argsLength = args.length; i < argsLength; i++) {
                realArgs[i] = args[i].getRealLocal();
            }

            Local realLocal = null;
            if (target != null) {
                realLocal = target.getRealLocal();
            }
            code.invokeVirtual(method, realLocal, instance.getRealLocal(), realArgs);
        });
    }

    void invokeConstructorSuper(MethodId method, ParamLocal var) {
        instructions.add(code -> code.invokeDirect(method, null, var.getRealLocal()));
    }

    void newInstance(MethodId constructorMethod, LocalWrapper target, LocalWrapper... args) {
        instructions.add(code -> {
            Local[] realArgs = new Local[args.length];
            for (int i = 0, argsLength = args.length; i < argsLength; i++) {
                realArgs[i] = args[i].getRealLocal();
            }
            code.newInstance(target.getRealLocal(), constructorMethod, realArgs);
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

    void cast(LocalWrapper target, LocalWrapper src) {
        instructions.add(code -> {
            code.cast(target.getRealLocal(), src.getRealLocal());
        });
    }

    void returnVoid() {
        instructions.add(Code::returnVoid);
    }

    void returnValue(LocalWrapper result) {
        instructions.add(code -> code.returnValue(result.getRealLocal()));
    }

    @Override
    public void compile() {
        // System.out.println("Locals to compile:" + locals);
        for (Map<TypeId, DeferredLocal> localsMap : locals) {
            for (DeferredLocal local : localsMap.values()) {
                local.generate(code);
            }
        }
        // System.out.println("Insns to compile:" + instructions);
        for (Instructions.Instruction instruction : instructions) {
            instruction.generate(code);
        }
    }
}

