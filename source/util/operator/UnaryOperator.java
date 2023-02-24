package util.operator;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.util.debug.Assertions;
import state.IAbstractValue;

public enum UnaryOperator implements IPythonOperator {
    NOT,
    MINUS;

    @Override
    public IAbstractValue calc(
            IAbstractValue val1, IAbstractValue val2, SSAInstruction inst, ForwardState forwardState) {
        assert val2 == null;
        SSAUnaryOpInstruction inst1 = (SSAUnaryOpInstruction) inst;

        IAbstractValue result;
        switch (this) {
            case NOT: result = val1.not(inst1, forwardState); break;
            case MINUS: result = val1.minus(inst1, forwardState); break;
            default:
                Assertions.UNREACHABLE();
                result = null;
        }

        return result;
    }
}
