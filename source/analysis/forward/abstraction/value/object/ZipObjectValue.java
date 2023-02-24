package analysis.forward.abstraction.value.object;


import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

import java.util.HashSet;

public class ZipObjectValue extends ComplexObjectValue<ZipObjectValue> {
    /* The object values by which the zip function is called as real args. */
    private final HashSet<ComplexObjectValue> arg1Objects = new HashSet<>();
    private final HashSet<ComplexObjectValue> arg2Objects = new HashSet<>();

    public ZipObjectValue(AllocatePoint allocatePoint) {
        super(allocatePoint, TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.findOrCreate("Lwala/builtin/zip")));
    }

    public void addFirstArg(ComplexObjectValue complexObjectValue) {
        arg1Objects.add(complexObjectValue);
    }

    public void addSecondArg(ComplexObjectValue complexObjectValue) {
        arg2Objects.add(complexObjectValue);
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
     * Create the tuple that first element is the collected value of first argument objects,
     * and second element is the collected value of second argument objects.
     * @param cgNode the call graph node of the instruction which the get method is called.
     * @param inst the instruction which the get method is called
     * @return the result of tuple
     */
    private TupleObjectValue createResultTuple(CGNode cgNode, SSAInstruction inst) {
        IForwardAbstractValue firstArgValue = new ForwardAbstractValue();
        IForwardAbstractValue secondArgValue = new ForwardAbstractValue();

        for (ComplexObjectValue complexObjectValue: arg1Objects) {
            for (Object eachElementValue: complexObjectValue.getIntAccessElements().values()) {
                firstArgValue.union((IForwardAbstractValue) eachElementValue);
            }
            firstArgValue.union(complexObjectValue.getIntTopAccessedValue());
        }

        for (ComplexObjectValue complexObjectValue: arg2Objects) {
            for (Object eachElementValue: complexObjectValue.getIntAccessElements().values()) {
                secondArgValue.union((IForwardAbstractValue) eachElementValue);
            }
            secondArgValue.union(complexObjectValue.getIntTopAccessedValue());
        }

        TupleObjectValue resultTuple = new TupleObjectValue(new AllocatePoint(cgNode, inst));
        resultTuple.setElement(0, firstArgValue);
        resultTuple.setElement(1, secondArgValue);
        return resultTuple;
    }

    @Override
    public boolean union(ZipObjectValue other, AllocatePointTable apTable) {
        boolean hasChanged = unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);

        for (ComplexObjectValue complexObjectValue: other.arg1Objects) {
            addFirstArg(complexObjectValue);
        }
        for (ComplexObjectValue complexObjectValue: other.arg2Objects) {
            addSecondArg(complexObjectValue);
        }

        return hasChanged;
    }

    @Override
    public ZipObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        ZipObjectValue newObject = new ZipObjectValue(getAllocatePoint());

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        for (ComplexObjectValue argObject: arg1Objects) {
            newObject.addFirstArg(argObject);
        }
        for (ComplexObjectValue argObject: arg2Objects) {
            newObject.addSecondArg(argObject);
        }

        return newObject;
    }

    @Override
    public boolean isSame(ZipObjectValue other, AllocatePointTable apTable) {
        if (!areAttrsSame(other, apTable) || !areElementsSame(other, apTable)) {
            // There is at least one abstract value which is not the same.
            return false;
        }

        if (arg1Objects.size() != other.arg1Objects.size()) return false;

        for (ComplexObjectValue argObject: arg1Objects) {
            AllocatePoint ap1 = argObject.getAllocatePoint();

            for (ComplexObjectValue otherArg1Object: other.arg1Objects) {
                // From 'otherArgObjects', search object value whose allocate point is the same to 'ap'.
                if (otherArg1Object.getAllocatePoint().match(ap1)) {
                    if (!argObject.isSame(otherArg1Object, apTable)) {
                        return false;
                    }
                }
            }
        }

        if (arg2Objects.size() != other.arg2Objects.size()) return false;

        for (ComplexObjectValue argObject: arg2Objects) {
            AllocatePoint ap2 = argObject.getAllocatePoint();

            for (ComplexObjectValue otherArg2Object: other.arg2Objects) {
                // From 'otherArgObjects', search object value whose allocate point is the same to 'ap'.
                if (otherArg2Object.getAllocatePoint().match(ap2)) {
                    if (!argObject.isSame(otherArg2Object, apTable)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}