package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.IntValue;
import analysis.forward.abstraction.value.element.StringValue;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;

import java.util.Collection;

public class IteratorObjectValue extends ObjectValue<IteratorObjectValue> {
    private final AllocatePoint baseAP;

    public IteratorObjectValue(AllocatePoint allocatePoint, AllocatePoint baseAP) {
        super(allocatePoint, TypeReference.find(PythonTypes.pythonLoader, "Liter"));
        this.baseAP = baseAP;
    }

    public AllocatePoint getBaseAP() {
        return baseAP;
    }

    public IForwardAbstractValue next(IForwardAbstractValue defaultValue, ForwardState state) {
        IForwardAbstractValue result = new ForwardAbstractValue();
        result.union(defaultValue);

        ObjectValue baseObj = state.getAllocatePointTable().get(baseAP);
        assert baseObj instanceof ComplexObjectValue;
        ComplexObjectValue baseObj1 = (ComplexObjectValue) baseObj;

        result.union(baseObj1.getElement(new ForwardAbstractValue(new IntValue(LatticeTop.TOP)), state.getAllocatePointTable()));
        if (baseObj1 instanceof DictObjectValue) {
            result.union(baseObj1.getAttr(new ForwardAbstractValue(new StringValue(LatticeTop.TOP)), state.getAllocatePointTable()));
        }
        return result;
    }

    @Override
    public boolean union(IteratorObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable);
    }

    @Override
    public IteratorObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        IteratorObjectValue newObject =
                new IteratorObjectValue(getAllocatePoint(), baseAP);

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(IteratorObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable);
    }

    @Override
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        super.collectRelatedAPs(apTable, result);
        result.add(baseAP);
        return result;
    }

    @Override
    public String toString() {
        return "Iter<" + super.toString() + ">";
    }
}
