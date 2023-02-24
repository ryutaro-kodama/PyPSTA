package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;

import java.util.HashSet;

public class EnumerateObjectValue extends ComplexObjectValue<EnumerateObjectValue> {
    /* The object values by which the enumerate function is called as real args. */
    private final HashSet<ComplexObjectValue> argObjects = new HashSet<>();

    public EnumerateObjectValue(AllocatePoint allocatePoint) {
        super(allocatePoint, PythonTypes.enumerate);
    }

    public void add(ComplexObjectValue complexObjectValue) {
        argObjects.add(complexObjectValue);
    }

    @Override
    public IForwardAbstractValue getElement(Integer index, AllocatePointTable apTable) {
        Assertions.UNREACHABLE();
        return null;
    }

    public IForwardAbstractValue getElement(ForwardState forwardState, SSAInstruction inst) {
        TupleObjectValue resultTuple = createResultTuple(forwardState.getCGNode(), inst);
        forwardState.getAllocatePointTable().newAllocation(resultTuple);
        return new ForwardAbstractValue(resultTuple);
    }

    @Override
    public IForwardAbstractValue getKeys() {
        Assertions.UNREACHABLE();
        return null;
    }

    public IForwardAbstractValue getKeys(ForwardState forwardState, SSAInstruction inst) {
        TupleObjectValue resultTuple = createResultTuple(forwardState.getCGNode(), inst);
        forwardState.getAllocatePointTable().newAllocation(resultTuple);
        return new ForwardAbstractValue(resultTuple);
    }

    /**
     * Create the tuple that first element is the collected key of argument objects, and second element is
     * the collected value of argument objects.
     * @param cgNode the call graph node of the instruction which the get method is called.
     * @param inst the instruction which the get method is called
     * @return the result of tuple
     */
    private TupleObjectValue createResultTuple(CGNode cgNode, SSAInstruction inst) {
        IForwardAbstractValue keys = new ForwardAbstractValue();
        IForwardAbstractValue values = new ForwardAbstractValue();
        for (ComplexObjectValue complexObjectValue: argObjects) {
            keys.union(complexObjectValue.getKeys());

            for (Object eachElementValue: complexObjectValue.getIntAccessElements().values()) {
                values.union((IForwardAbstractValue) eachElementValue);
            }
            values.union(complexObjectValue.getIntTopAccessedValue());
        }

        TupleObjectValue resultTuple = new TupleObjectValue(new AllocatePoint(cgNode, inst));
        resultTuple.setElement(0, keys);
        resultTuple.setElement(1, values);
        return resultTuple;
    }

    @Override
    public boolean union(EnumerateObjectValue other, AllocatePointTable apTable) {
        boolean hasChanged = unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);

        for (ComplexObjectValue complexObjectValue: other.argObjects) {
            add(complexObjectValue);
        }

        return hasChanged;
    }

    @Override
    public EnumerateObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        EnumerateObjectValue newObject = new EnumerateObjectValue(getAllocatePoint());

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        for (ComplexObjectValue argObject: argObjects) {
            newObject.add(argObject);
        }

        return newObject;
    }

    @Override
    public boolean isSame(EnumerateObjectValue other, AllocatePointTable apTable) {
        if (!areAttrsSame(other, apTable) || !areElementsSame(other, apTable)) {
            // There is at least one abstract value which is not the same.
            return false;
        }

        if (argObjects.size() != other.argObjects.size()) return false;

        for (ComplexObjectValue argObject: argObjects) {
            AllocatePoint ap = argObject.getAllocatePoint();

            for (ComplexObjectValue otherArgObject: other.argObjects) {
                // From 'otherArgObjects', search object value whose allocate point is the same to 'ap'.
                if (otherArgObject.getAllocatePoint().match(ap)) {
                    if (!argObject.isSame(otherArgObject, apTable)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean equals(Object o) {
        // TODO:
        return false;
    }

}