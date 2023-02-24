package analysis.forward.abstraction.value.object;

import analysis.forward.BuiltinFunctionSummaries;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePointDefaultMethod;
import analysis.forward.abstraction.value.element.IntValue;
import analysis.forward.abstraction.value.element.StringValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.ExceptionManager;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;

import java.util.*;

public abstract class ComplexObjectValue<T extends ComplexObjectValue<T>> extends ObjectValue<T> {
    protected HashMap<Integer, IForwardAbstractValue> intAccessElements = new HashMap<>();

    protected ForwardAbstractValue intTopAccessedValue = new ForwardAbstractValue();

    public ComplexObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        super(allocatePoint, typeRef);
    }

    /**
     * Set the default method to attributes.
     * @param methodName the name of the method
     * @param methodSummary the function object which is summary of the method
     * @param table the caller's allocation table
     */
    protected void setDefaultMethod(String methodName,
                                    BuiltinFunctionSummaries.Summary methodSummary,
                                    AllocatePointTable table) {
        // Create the method's type reference.
        TypeReference methodType = TypeReference.findOrCreate(
                PythonTypes.pythonLoader, getFullName() + "/" + methodName
        );

        MethodSummaryObjectValue valuesObjVal = new MethodSummaryObjectValue(
                new AllocatePointDefaultMethod(getAllocatePoint(), methodName),
                methodType,
                getAllocatePoint(),
                methodSummary
        );
        table.newAllocation(valuesObjVal);
        setAttr(methodName, new ForwardAbstractValue(valuesObjVal));
    }

    public HashMap<Integer, IForwardAbstractValue> getIntAccessElements() {
        return intAccessElements;
    }

    public ForwardAbstractValue getIntTopAccessedValue() {
        return intTopAccessedValue;
    }

    public boolean hasElement(Integer index) {
        return intAccessElements.containsKey(index);
    }

    public IForwardAbstractValue getElement(Integer index, AllocatePointTable apTable) {
        return intAccessElements.get(index);
    }

    /**
     * Accessed by abstract value is element access.
     * @param abstractIndex the abstract value of index
     * @param apTable the allocation point table where this method is called
     * @return the abstract value of the element
     */
    public IForwardAbstractValue getElement(IForwardAbstractValue abstractIndex, AllocatePointTable apTable) {
        IForwardAbstractValue result = new ForwardAbstractValue();

        IntValue intValue = ((ForwardAbstractValue) abstractIndex).getIntValue();
        StringValue stringValue = ((ForwardAbstractValue) abstractIndex).getStringValue();

        if (intValue.isBottom() && stringValue.isBottom()) {
            // There is no corresponding elements or attributes.
            ExceptionManager.bottomException(intValue);
            ExceptionManager.bottomException(stringValue);
            return result;
        }

        Integer index = intValue.getConcreteValue();
        if (intValue.isBottom()) {
        } else if (intValue.isTop()) {
            // If index variable is top, there is a possibility of element exception.
            ExceptionManager.elementException(this, intValue.getLatticeElement());

            result.union(intTopAccessedValue);
            for (IForwardAbstractValue value: intAccessElements.values()) {
                result.union(value);
            }
        } else if (index < 0) {
            // If index variable is negative value, there is 2 cases.
            if (intTopAccessedValue.isBottom()) {
                // One is when int top accessed value is bottom, return the element from
                // corresponding to the index from tail.
                result.union(getElement(index + intAccessElements.size(), apTable));
            } else {
                // The other is when not bottom, return all elements. Because you don't know
                // the size of the container you also don't know what number is from tail.
                // But there is no possibility of raising element exception.

                result.union(intTopAccessedValue);
                for (IForwardAbstractValue value: intAccessElements.values()) {
                    result.union(value);
                }
            }
        } else {
            // Index variable is concrete value.

            if (intAccessElements.containsKey(index)) {
                // There is an element corresponding to the index.
                result.union(intAccessElements.get(index));
            } else {
                // There isn't an element corresponding to the index.
                ExceptionManager.elementException(this, intValue.getLatticeElement());
            }

            // Unite int top accessed value.
            result.union(intTopAccessedValue);
        }

        if (!stringValue.isBottom()) {
            result.union(getAttr(abstractIndex, apTable));
        }

        return result;
    }

    public void setElement(Integer index, IForwardAbstractValue value) {
        intAccessElements.put(index, value);
    }

    /**
     * If attribute access is done by abstract value, you think the access is element access.
     * So you admit only int access.
     * @param abstractIndex
     * @param value
     */
    public void setElement(IForwardAbstractValue abstractIndex, IForwardAbstractValue value) {
        IntValue intValue = ((ForwardAbstractValue) abstractIndex).getIntValue();
        if (intValue.isTop()) {
            intTopAccessedValue.union((ForwardAbstractValue) value);
        } else if (intValue.isBottom()) {
            ExceptionManager.bottomException(intValue);
        } else {
            setElement(intValue.getConcreteValue(), value.copy());
        }
    }

    @Override
    public IForwardAbstractValue getKeys() {
        if (!intTopAccessedValue.isBottom()) {
            // If there is some abstract value which key is not obvious, we can't decide what key is.
            // So return int top element abstract value.
            return new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
        } else {
            Set<Integer> keys = intAccessElements.keySet();
            if (keys.isEmpty()) {
                return new ForwardAbstractValue();
            } else if (keys.size() == 1) {
                return new ForwardAbstractValue((Integer) keys.toArray()[0]);
            } else {
                return new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
            }
        }
    }

    protected boolean unionIntAccessElements(ComplexObjectValue other, AllocatePointTable apTable) {
        boolean hasChanged = false;

        for (Object m: other.intAccessElements.entrySet()) {
            Map.Entry<Integer, IForwardAbstractValue> index2Value = (Map.Entry<Integer, IForwardAbstractValue>) m;
            Integer index = index2Value.getKey();
            if (intAccessElements.containsKey(index)) {
                // When you unite attributes, you must unite only this object's attributes, not do
                // for parent class, and so on. So here not using 'hasElement' method or 'getElement'
                // method.
                hasChanged = intAccessElements.get(index).union(index2Value.getValue()) || hasChanged;
            } else {
                setElement(index, index2Value.getValue());
                hasChanged = true;
            }
        }

        hasChanged = intTopAccessedValue.union(other.intTopAccessedValue) || hasChanged;
        return hasChanged;
    }

    @Override
    public boolean isCallable(AllocatePointTable table) {
        return true;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    protected ComplexObjectValue copyIntAccessElements(ComplexObjectValue target) {
        for (Map.Entry<Integer, IForwardAbstractValue> m: intAccessElements.entrySet()) {
            target.setElement(m.getKey(), m.getValue().copy());
        }
        target.intTopAccessedValue.union(intTopAccessedValue.copy());
        return target;
    }

    public IntValue size() {
        if (!intTopAccessedValue.isBottom())
            return new IntValue(LatticeTop.TOP);
        else
            return new IntValue(intAccessElements.size());
    }

    /**
     * Check abstract values of this object's elements are the same to one of the other.
     * Don't check that the object values which are pointed from elements are the same.
     * @param other other object value
     * @param apTable the allocation point table where this method is called
     * @return whether other elements are the same to this
     */
    protected boolean areElementsSame(ComplexObjectValue other, AllocatePointTable apTable) {
        // Is number of int access elements same?
        if (intAccessElements.size() != other.intAccessElements.size()) return false;

        for (Map.Entry<Integer, IForwardAbstractValue> m: intAccessElements.entrySet()) {
            Integer index = m.getKey();
            IForwardAbstractValue value = m.getValue();
            if (other.hasElement(index)) {
                if (!value.isSame(other.getElement(index, apTable))) {
                    // The abstract values of the elements are not the same.
                    return false;
                }
            } else {
                // The other object doesn't have the element.
                return false;
            }
        }
        return intTopAccessedValue.isSame(other.intTopAccessedValue);
    }

    @Override
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        super.collectRelatedAPs(apTable, result);

        for (IForwardAbstractValue elementValue : intAccessElements.values()) {
            // Get allocate points of abstract values which int access elements point to.
            for (AllocatePoint elementAp: ((ForwardAbstractValue) elementValue).getAllocatePoints()) {
                if (!result.contains(elementAp)) {
                    apTable.get(elementAp).collectRelatedAPs(apTable, result);
                }
            }
        }
        for (AllocatePoint elementAp: intTopAccessedValue.getAllocatePoints()) {
            if (!result.contains(elementAp)) {
                apTable.get(elementAp).collectRelatedAPs(apTable, result);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), intAccessElements, intTopAccessedValue);
    }
}
