package util.operator;

import client.operator.PypstaBinaryOperator;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.util.debug.Assertions;

public class PythonOperatorFactory {

    public static IPythonOperator convert(IConditionalBranchInstruction.IOperator op) {
        IConditionalBranchInstruction.Operator op1 = (IConditionalBranchInstruction.Operator) op;
        switch (op1) {
            case EQ: return CompareOperator.EQ;
            case NE: return CompareOperator.NEQ;
            case LT: return CompareOperator.LT;
            case GE: return CompareOperator.LTE;
            case GT: return CompareOperator.GT;
            case LE: return CompareOperator.GTE;
//            case : return CompareOperand.IS;
//            case : return CompareOperand.ISN;
//            case : return CompareOperand.IN;
//            case : return CompareOperand.NIN;
            default: Assertions.UNREACHABLE("Undefined operand");
        }
        return null;
    }

    public static IPythonOperator convert(IBinaryOpInstruction.IOperator op){
        if (op instanceof IBinaryOpInstruction.Operator){
            IBinaryOpInstruction.Operator op1 = (IBinaryOpInstruction.Operator) op;
            switch (op1) {
                case ADD: return BinaryOperator.ADD;
                case SUB: return BinaryOperator.SUB;
                case MUL: return BinaryOperator.MULT;
                case DIV: return BinaryOperator.DIV;
//                case : return BinaryOperand.MOD;
//                case : return BinaryOperand.POW;
//                case : return BinaryOperand.LSHIFT;
//                case : return BinaryOperand.RSHIFT;
//                case : return BinaryOperand.BITOR;
                case XOR : return BinaryOperator.BITXOR;
                case AND : return BinaryOperator.BITAND;
//                case : return BinaryOperand.MATMULT;
                case REM : return BinaryOperator.MOD;
//                case AND : return ;
                case OR : return BinaryOperator.BITOR;
//                case XOR : return ;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        } else if (op instanceof CAstBinaryOp) {
            CAstBinaryOp op1 = (CAstBinaryOp) op;
            switch (op1) {
//                case CONCAT: return ;
                case EQ: return CompareOperator.EQ;
                case NE: return CompareOperator.NEQ;
                case LT: return CompareOperator.LT;
                case GE: return CompareOperator.GTE;
                case GT: return CompareOperator.GT;
                case LE: return CompareOperator.LTE;
//                case STRICT_EQ: return ;
//                case STRICT_NE: return ;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        } else if (op instanceof IShiftInstruction.Operator) {
            IShiftInstruction.Operator op1 = (IShiftInstruction.Operator) op;
            switch (op1) {
//                case CONCAT: return ;
                case SHL: return ShiftOperator.SHL;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        } else if (op instanceof PypstaBinaryOperator) {
            PypstaBinaryOperator op1 = (PypstaBinaryOperator) op;
            switch (op1) {
                case FDIV: return BinaryOperator.FDIV;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        }
        return null;
    }

    public static UnaryOperator convert(IUnaryOpInstruction.IOperator op) {
        if (op instanceof IUnaryOpInstruction.Operator) {
            IUnaryOpInstruction.Operator op1 = (IUnaryOpInstruction.Operator) op;
            switch (op1) {
                case NEG: return UnaryOperator.NOT;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        } else if (op instanceof CAstUnaryOp) {
            CAstUnaryOp op1 = (CAstUnaryOp) op;
            switch (op1) {
                case MINUS: return UnaryOperator.MINUS;
                default: Assertions.UNREACHABLE("Undefined operand");
            }
        }
        Assertions.UNREACHABLE("Undefined operand");
        return null;
    }
}
