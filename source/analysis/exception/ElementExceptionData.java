package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.abstraction.value.object.ComplexObjectValue;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.ssa.SSAInstruction;

import java.util.Objects;

public class ElementExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final ObjectValue object;
    private final ILatticeElement index;

    private final SSAInstruction inst;
    private final ForwardState state;

    public ElementExceptionData(ObjectValue object, ILatticeElement index, SSAInstruction inst, ForwardState state) {
        this.foundWitness = true;
        this.object = object;
        this.index = index;
        this.inst = inst;
        this.state = state;
    }

    @Override
    public boolean ignore() {
        if (index == LatticeTop.TOP || index == LatticeBottom.BOTTOM) {
            // If the index variable is top or bottom, you can't create initial query. So don't do backward analysis.
            return true;
        }

        for (ObjectValue objectValue:
                ((ForwardAbstractValue) state.getValue(inst.getUse(0)))
                        .getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
            if (objectValue instanceof ComplexObjectValue
                    && !((ComplexObjectValue) objectValue).getIntTopAccessedValue().isBottom()) {
                // Although the index variable has concrete value, if the container has int top
                // accessed value, you can't statically know the element whose index is the
                // concrete index value.
                return true;
            }
        }
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
        ElementExceptionData that = (ElementExceptionData) o;
        return index == that.index && Objects.equals(object, that.object) && Objects.equals(inst, that.inst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, index, inst);
    }
}
