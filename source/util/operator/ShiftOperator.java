package util.operator;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.element.IntValue;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;
import state.IAbstractValue;

public enum ShiftOperator implements IPythonOperator {
    SHL;

    @Override
    public IAbstractValue calc(IAbstractValue val1,
                               IAbstractValue val2,
                               SSAInstruction inst,
                               ForwardState forwardState) {
        IAbstractValue result;
        SSABinaryOpInstruction inst1 = (SSABinaryOpInstruction) inst;
        switch (this) {
            case SHL:
                result = new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
                break;
            default:
                Assertions.UNREACHABLE();
                result = null;
        }
        return result;
    }
}

