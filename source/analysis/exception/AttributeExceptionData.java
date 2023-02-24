package analysis.exception;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AttributeExceptionData implements IExceptionData {
    private boolean foundWitness;

    private final ObjectValue object;
    private final String field;

    private final SSAInstruction inst;

    private final ForwardState state;

    public AttributeExceptionData(ObjectValue object,
                                  String field,
                                  SSAInstruction inst,
                                  ForwardState currentState) {
        this.foundWitness = true;
        this.object = object;
        assert field != null;
        this.field = field;
        this.inst = inst;
        this.state = currentState;
    }

    @Override
    public boolean ignore() {
        // Dictionary's attribute exception is key error, so don't backward analysis.
        if (getObjectTypes().contains(PythonTypes.dict)) {
            return true;
        }

        if (state.getAllocatePointTable().get(object.getAllocatePoint())
                .hasAttr(field, state.getAllocatePointTable())) {
            // If the object corresponding to target allocate point has the field,
            // this exception is fake.
            foundWitness = false;
            return true;
        }
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

    public int getObjectId() {
        return inst.getUse(0);
    }

    public Set<TypeReference> getObjectTypes() {
        return ((ForwardAbstractValue) state.getValue(getObjectId()))
                        .getAllocatePoints()
                        .stream()
                        .map(ap -> state.getAllocatePointTable().get(ap))
                        .filter(o -> !o.hasAttr(field, state.getAllocatePointTable()))
                        .map(o -> o.getTypeReference())
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeExceptionData that = (AttributeExceptionData) o;
        return Objects.equals(object, that.object) && Objects.equals(field, that.field) && Objects.equals(inst, that.inst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, field, inst);
    }
}
