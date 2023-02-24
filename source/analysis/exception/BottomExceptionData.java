package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.Objects;

public class BottomExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final SSAInstruction inst;
    private final ForwardState state;

    public BottomExceptionData(SSAInstruction inst, ForwardState state) {
        this.foundWitness = true;
        this.inst = inst;
        this.state = state;
    }

    @Override
    public boolean ignore() {
        // Do manual check whether to the variable is bottom. (There is a possibility that
        // the variable which was judged as bottom became not bottom, because of order of
        // basic block to analyze.)
        foundWitness = false;

        for (int i = 0; i < inst.getNumberOfUses(); i++) {
            if (state.getValue(inst.getUse(0)).isBottom()) {
                // If there is bottom value, there is a witness
                foundWitness = true;
            }
        }

        for (int i = 0; i < inst.getNumberOfDefs(); i++) {
            if (state.getValue(inst.getDef(0)).isBottom()) {
                // If there is bottom value, there is a witness
                foundWitness = true;
            }
        }

        // Don't do backward analysis.
        return true;
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
    public SSAInstruction getInst() {
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
        BottomExceptionData that = (BottomExceptionData) o;
        return Objects.equals(inst, that.inst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inst);
    }
}
