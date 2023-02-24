package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.Objects;

public class TmpExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final SSAInstruction inst;
    private final ForwardState state;

    public TmpExceptionData(SSAInstruction inst, ForwardState currentState) {
        this.foundWitness = true;
        this.inst = inst;
        this.state = currentState;
    }

    @Override
    public boolean ignore() {
        return true;
    }

    @Override
    public void setFoundWitness(boolean b) {
        this.foundWitness = b;
    }

    @Override
    public boolean getFoundWitness() {
        return foundWitness;
    }

    @Override
    public SSAInstruction getInst() {
        return inst;
    }

    @Override
    public ForwardState getState() {
        return state;
    }

    public int getObjectId() {
        return inst.getUse(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmpExceptionData that = (TmpExceptionData) o;
        return Objects.equals(inst, that.inst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inst);
    }
}
