package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.Objects;

public class NoneExceptionData<T extends SSAInstruction> implements IExceptionData {
    private boolean foundWitness = true;

    private final int varId;
    private final T inst;
    private final ForwardState state;

    public NoneExceptionData(int varId, T currentInst, ForwardState currentState) {
        this.varId = varId;
        this.inst = currentInst;
        this.state = currentState;
    }

    @Override
    public boolean ignore() {
        return false;
    }

    @Override
    public void setFoundWitness(boolean b) {
        foundWitness = b;
    }

    @Override
    public boolean getFoundWitness() {
        return foundWitness;
    }

    public int getVarId() {
        return varId;
    }

    @Override
    public T getInst() {
        return inst;
    }

    @Override
    public ForwardState getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoneExceptionData<?> that = (NoneExceptionData<?>) o;
        return varId == that.varId && Objects.equals(inst, that.inst) && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varId, inst, state);
    }
}
