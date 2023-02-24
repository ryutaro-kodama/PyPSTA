package util.operator;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;
import state.IAbstractValue;

public enum CompareOperator implements IPythonOperator {
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    IS,
    ISN,
    IN,
    NIN;

    @Override
    public IAbstractValue calc(
            IAbstractValue val1, IAbstractValue val2, SSAInstruction inst, ForwardState forwardState) {
        IAbstractValue result;
        SSABinaryOpInstruction inst1 = (SSABinaryOpInstruction) inst;
        switch (this) {
            case EQ:  result = val1.eq(val2, inst1, forwardState);  break;
            case NEQ: result = val1.neq(val2, inst1, forwardState); break;
            case LT:  result = val1.lt(val2, inst1, forwardState);  break;
            case LTE: result = val1.lte(val2, inst1, forwardState); break;
            case GT:  result = val1.gt(val2, inst1, forwardState);  break;
            case GTE: result = val1.gte(val2, inst1, forwardState); break;
//            case IS:  result = val1.is(val2, inst1, forwardState);  break;
//            case ISN: result = val1.isn(val2, inst1, forwardState); break;
//            case IN:  result = val1.in(val2, inst1, forwardState);  break;
//            case NIN: result = val1.nin(val2, inst1, forwardState); break;
            default:
                Assertions.UNREACHABLE();
                result = null;
        }
        return result;
    }
}

