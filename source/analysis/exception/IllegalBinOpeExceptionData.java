package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.element.IForwardAbstractValueElement;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.Objects;

public class IllegalBinOpeExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final IForwardAbstractValueElement val1;
    private final IForwardAbstractValueElement val2;

    private final SSABinaryOpInstruction inst;

    private final ForwardState state;

    public IllegalBinOpeExceptionData(IForwardAbstractValueElement val1,
                                      IForwardAbstractValueElement val2,
                                      SSABinaryOpInstruction inst,
                                      ForwardState currentState) {
        this.foundWitness = true;
        this.val1 = val1;
        this.val2 = val2;
        this.inst = inst;
        this.state = currentState;
    }

    @Override
    public boolean ignore() {
        // The abstract value element can't become bottom, so this exception can't be fake.
        return false;
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

    public int getVal1Id() {
        return inst.getUse(0);
    }

    public IForwardAbstractValueElement getVal1() {
        return val1;
    }

    public int getVal2Id() {
        return inst.getUse(1);
    }

    public IForwardAbstractValueElement getVal2() {
        return val2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IllegalBinOpeExceptionData that = (IllegalBinOpeExceptionData) o;
        return Objects.equals(val1, that.val1) && Objects.equals(val2, that.val2) && Objects.equals(inst, that.inst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(val1, val2, inst);
    }

    @Override
    public String toString() {
        return "IllegalBinOpe: '" + val1 + "' " + inst.getOperator() + " '" + val2 + "' (@" + state.getCGNode() + ")";
    }
}
