package util.operator;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;
import state.IAbstractValue;

public interface IPythonOperator {
    // TODO: Override `CAstOperator`.
    IAbstractValue calc(
            IAbstractValue val1, IAbstractValue val2,
            SSAInstruction inst, ForwardState forwardState);
}
