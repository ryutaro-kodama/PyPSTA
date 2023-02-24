package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;

import java.util.Objects;

public class AssertExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final AstAssertInstruction inst;
    private final ForwardState state;

    public AssertExceptionData(AstAssertInstruction inst, ForwardState state) {
        this.foundWitness = true;
        this.inst = inst;
        this.state = state;
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

    @Override
    public AstAssertInstruction getInst() {
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
        AssertExceptionData that = (AssertExceptionData) o;
        return Objects.equals(inst, that.inst) && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inst, state);
    }
}
