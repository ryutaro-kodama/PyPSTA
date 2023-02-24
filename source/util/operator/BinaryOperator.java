package util.operator;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;
import state.IAbstractValue;

public enum BinaryOperator implements IPythonOperator {
    ADD,
    SUB,
    MULT,
    DIV,
    FDIV,
    MOD,
    POW,
    LSHIFT,
    RSHIFT,
    BITOR,
    BITXOR,
    BITAND,
    MATMULT;

    @Override
    public IAbstractValue calc(
            IAbstractValue val1, IAbstractValue val2, SSAInstruction inst, ForwardState forwardState) {
        SSABinaryOpInstruction inst1 = (SSABinaryOpInstruction) inst;
        IAbstractValue resultAbstractValue;
        switch (this) {
            case ADD:     resultAbstractValue = val1.add(val2, inst1, forwardState);     break;
            case SUB:     resultAbstractValue = val1.sub(val2, inst1, forwardState);     break;
            case MULT:    resultAbstractValue = val1.mult(val2, inst1, forwardState);    break;
            case DIV:     resultAbstractValue = val1.div(val2, inst1, forwardState);     break;
            case FDIV:    resultAbstractValue = val1.fdiv(val2, inst1, forwardState);    break;
            case MOD:     resultAbstractValue = val1.mod(val2, inst1, forwardState);     break;
//            case POW:     resultAbstractValue = val1.pow(val2, inst1, forwardState);     break;
//            case LSHIFT:  resultAbstractValue = val1.lshift(val2, inst1, forwardState);  break;
//            case RSHIFT:  resultAbstractValue = val1.rshift(val2, inst1, forwardState);  break;
            case BITOR:   resultAbstractValue = val1.bitor(val2, inst1, forwardState);   break;
            case BITAND:  resultAbstractValue = val1.bitand(val2, inst1, forwardState);  break;
            case BITXOR:  resultAbstractValue = val1.bitxor(val2, inst1, forwardState);  break;
//            case MATMULT: resultAbstractValue = val1.matmult(val2, inst1, forwardState); break;
            default:
                Assertions.UNREACHABLE();
                resultAbstractValue = null;
        }

        return resultAbstractValue;
    }
}
