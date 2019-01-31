package com.ride.android;

import com.android.dx.BinaryOp;
import com.android.dx.Code;
import com.android.dx.Comparison;
import com.android.dx.Label;

public class Instructions {
    interface Instruction {
        void generate(Code code);
    }

    static class BinaryInstruction implements Instruction {
        final BinaryOp op;
        final Generator.Local dest, a, b;

        BinaryInstruction(BinaryOp op, Generator.Local dest, Generator.Local a, Generator.Local b) {
            this.op = op;
            this.dest = dest;
            this.a = a;
            this.b = b;
        }

        @Override
        public void generate(Code code) {
            code.op(op, dest.getRealLocal(), a.getRealLocal(), b.getRealLocal());
        }
    }

    static class LoadInstruction implements Instruction {
        final Generator.Local dest;
        final int constant;

        LoadInstruction(Generator.Local dest, int constant) {
            this.dest = dest;
            this.constant = constant;
        }

        @Override
        public void generate(Code code) {
            code.loadConstant(dest.getRealLocal(), constant);
        }
    }

    static class CompareInstruction implements Instruction {
        final Label label;
        final Generator.Local a, b;
        final Comparison comparison;

        CompareInstruction(Comparison comparison, Label trueLabel, Generator.Local a, Generator.Local b) {
            this.label = trueLabel;
            this.a = a;
            this.b = b;
            this.comparison = comparison;
        }

        @Override
        public void generate(Code code) {
            code.compare(comparison, label, a.getRealLocal(), b.getRealLocal());
        }
    }

    static class CompareZInstruction implements Instruction {
        final Generator.Local a;
        final Comparison comparison;
        final Label label;

        CompareZInstruction(Comparison comparison, Label trueLabel, Generator.Local a) {
            this.label = trueLabel;
            this.a = a;
            this.comparison = comparison;
        }

        @Override
        public void generate(Code code) {
            code.compareZ(comparison, label, a.getRealLocal());
        }
    }

    static class JumpInstruction implements Instruction {
        final Label label;

        JumpInstruction(Label label) {
            this.label = label;
        }

        @Override
        public void generate(Code code) {
            code.jump(label);
        }
    }
}
