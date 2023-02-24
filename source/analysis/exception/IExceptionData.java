package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;

public interface IExceptionData {
    boolean ignore();

    void setFoundWitness(boolean b);

    boolean getFoundWitness();

    <T extends SSAInstruction> T getInst();

    ForwardState getState();

    boolean equals(Object o);

    int hashCode();
}
