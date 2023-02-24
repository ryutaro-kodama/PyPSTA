package analysis.forward.abstraction.value.object;

import analysis.forward.ExceptionManager;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.AllocatePoints;
import analysis.forward.abstraction.value.element.IntValue;
import analysis.forward.abstraction.value.element.NoneValue;
import analysis.forward.abstraction.value.element.StringValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;

public class DictObjectValue extends ComplexObjectValue<DictObjectValue> {
    public static final TypeReference pypstaIndexOfDictKeys
            = TypeReference.findOrCreate(PythonTypes.pythonLoader, "pypstaIndexOfDictKeys");

    private final HashMap<String, IForwardAbstractValue> stringAccessElements = new HashMap<>();

    /**
     * This abstract value is used when abstract value of attribute access's member variable is top. This means that
     * we don't know what element the member is access.
     */
    private final ForwardAbstractValue stringTopAccessedValue = new ForwardAbstractValue();

    private static final Set<String> defaultMethodNames = new HashSet<String>() {{
        add("values"); add("get"); add("items"); add("setdefault");
    }};

    public DictObjectValue(AllocatePoint allocatePoint, AllocatePointTable table) {
        super(allocatePoint, PythonTypes.dict);
        setDefaultMethod("values", (state, args, inst) -> values(state, args, inst), table);
        setDefaultMethod("get", (state, args, inst) -> get(state, args, inst), table);
        setDefaultMethod("setdefault", (state, args, inst) -> setdefault(state, args, inst), table);
        setDefaultMethod("items", (state, args, inst) -> items(state, args, inst), table);
    }

    public HashMap<String, IForwardAbstractValue> getStringAccessElements() {
        return stringAccessElements;
    }

    public ForwardAbstractValue getStringTopAccessedValue() {
        return stringTopAccessedValue;
    }

    @Override
    public boolean hasAttr(String fieldName, AllocatePointTable apTable) {
        return stringAccessElements.containsKey(fieldName);
    }

    @Override
    public boolean hasAttr(FieldReference fieldRef, AllocatePointTable apTable) {
        return hasAttr(fieldRef.getName().toString(), apTable);
    }

    public IForwardAbstractValue getElement(ForwardState state, IForwardAbstractValue abstractKey) {
        if (((ForwardAbstractValue) abstractKey).getAllocatePoints().isBottom()) {
            return getElement(abstractKey, state.getAllocatePointTable());
        } else {
            IForwardAbstractValue result = new ForwardAbstractValue();
            for (ObjectValue objectValue : ((ForwardAbstractValue) abstractKey).getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                if (objectValue.getClass() == IndexOfDictKeysObjectValue.class) {
                    if (!intTopAccessedValue.isBottom() || intAccessElements.size()!=0) {
                        result.union(new ForwardAbstractValue(new IntValue(LatticeTop.TOP)));
                    }
                    if (!stringTopAccessedValue.isBottom() || stringAccessElements.size()!=0) {
                        result.union(new ForwardAbstractValue(new StringValue(LatticeTop.TOP)));
                    }
                }
            }

            return result;
        }
    }

    @Override
    public IForwardAbstractValue getElement(IForwardAbstractValue abstractKey, AllocatePointTable apTable) {
        IForwardAbstractValue result = new ForwardAbstractValue();

        IntValue intValue = ((ForwardAbstractValue) abstractKey).getIntValue();
        StringValue stringValue = ((ForwardAbstractValue) abstractKey).getStringValue();

        // There is no corresponding elements or attributes.
        if (intValue.isBottom() && stringValue.isBottom()) {
            ExceptionManager.bottomException(intValue);
            ExceptionManager.bottomException(stringValue);
        }

        if (!intValue.isBottom())
            result.union(super.getElement(abstractKey, apTable));

        if (stringValue.isBottom()) {
        } else if (stringValue.isTop()) {
            // If index variable is top, there is a possibility of element exception.
            ExceptionManager.attributeException(this, "PYPSTATOP");

            result.union(stringTopAccessedValue);
            for (Map.Entry<String, IForwardAbstractValue> m: stringAccessElements.entrySet()) {
                String key = m.getKey();
                if (defaultMethodNames.contains(key))
                    // We assert that method access may be using concrete string value.
                    // So result doesn't contain the abstract value of method.
                    continue;
                result.union(m.getValue());
            }
        } else {
            // Index key is concrete value.
            String key = stringValue.getConcreteValue();

            if (stringAccessElements.containsKey(key)) {
                // There is an element corresponding to the index.
                result.union(stringAccessElements.get(key));
            } else {
                // There isn't an element corresponding to the index.
                ExceptionManager.attributeException(this, key);
            }

            // Unite string top accessed value.
            result.union(stringTopAccessedValue);
        }

        return result;
    }

    @Override
    public IForwardAbstractValue getAttr(String fieldName, AllocatePointTable apTable) {
        return stringAccessElements.get(fieldName);
    }

    /**
     * On Ariadne SSA, dictionary access of string literal key is also represented by 'fieldRef'.
     * @param fieldRef the key of accessed element
     * @param apTable the state where this method is called
     * @return the value of accessed element
     */
    @Override
    public IForwardAbstractValue getAttr(FieldReference fieldRef, AllocatePointTable apTable) {
        return getAttr(fieldRef.getName().toString(), apTable);
    }

    @Override
    public void setElement(IForwardAbstractValue abstractKey, IForwardAbstractValue value) {
        IntValue intValue = ((ForwardAbstractValue) abstractKey).getIntValue();
        StringValue stringValue = ((ForwardAbstractValue) abstractKey).getStringValue();

        if (intValue.isBottom() && stringValue.isBottom()) {
            ExceptionManager.bottomException(intValue);
            ExceptionManager.bottomException(stringValue);
        } else {
            if (!intValue.isBottom())
                super.setElement(abstractKey, value);

            if (stringValue.isTop()) {
                stringTopAccessedValue.union((ForwardAbstractValue) value);
            } else if (stringValue.isBottom()) {
            } else {
                setAttr(stringValue.getConcreteValue(), value);
            }
        }
    }

    @Override
    public void setAttr(String fieldName, IForwardAbstractValue value) {
        stringAccessElements.put(fieldName, value);
    }

    /**
     * On Ariadne SSA, dictionary access of string literal key is also represented by 'fieldRef'.
     * @param fieldRef the key of accessed element
     * @param value the value of accessed element
     */
    @Override
    public void setAttr(FieldReference fieldRef, IForwardAbstractValue value) {
        setAttr(fieldRef.getName().toString(), value);
    }

    @Override
    public IForwardAbstractValue getKeys() {
        Assertions.UNREACHABLE();
        return null;
    }

    public IForwardAbstractValue getKeys(ForwardState forwardState, SSAInstruction inst) {
        IndexOfDictKeysObjectValue resultObject = new IndexOfDictKeysObjectValue(
                new AllocatePoint(forwardState.getCGNode(), inst), pypstaIndexOfDictKeys, this
        );
        forwardState.getAllocatePointTable().newAllocation(resultObject);
        return new ForwardAbstractValue(resultObject);
    }

    private class IndexOfDictKeysObjectValue extends ObjectValue<IndexOfDictKeysObjectValue> {
        private final DictObjectValue dict;

        public IndexOfDictKeysObjectValue(AllocatePoint allocatePoint, TypeReference typeRef, DictObjectValue dict) {
            super(allocatePoint, typeRef);
            this.dict = dict;
        }

        @Override
        public boolean union(IndexOfDictKeysObjectValue other, AllocatePointTable apTable) {
            return false;
        }

        @Override
        public IndexOfDictKeysObjectValue copy(AllocatePointTable apTable) {
            return new IndexOfDictKeysObjectValue(
                    getAllocatePoint(), getTypeReference(), dict
            );
        }

        @Override
        public boolean isSame(IndexOfDictKeysObjectValue other, AllocatePointTable apTable) {
            return dict.isSame(other.dict, apTable);
        }
    }

    @Override
    public boolean union(DictObjectValue other, AllocatePointTable apTable) {
        boolean hasChanged = unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);

        for (Map.Entry<String, IForwardAbstractValue> m: other.stringAccessElements.entrySet()) {
            String key = m.getKey();
            if (stringAccessElements.containsKey(key)) {
                // When you unite attributes, you must unite only this object's attributes, not do
                // for parent class, and so on. So here not using 'hasattr' method or 'getattr' method.
                hasChanged = stringAccessElements.get(key).union(m.getValue()) || hasChanged;
            } else {
                setAttr(key, m.getValue());
                hasChanged = true;
            }
        }

        hasChanged = stringTopAccessedValue.union(other.stringTopAccessedValue) || hasChanged;
        return hasChanged;
    }

    @Override
    public IntValue size() {
        return new IntValue(super.size().getConcreteValue() + stringAccessElements.size());
    }

    @Override
    public DictObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        DictObjectValue newObject = new DictObjectValue(getAllocatePoint(), apTable);

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        // Copy string key elements.
        for (Map.Entry<String, IForwardAbstractValue> m: stringAccessElements.entrySet()) {
            newObject.setAttr(m.getKey(), m.getValue().copy());
        }
        newObject.stringTopAccessedValue.union(stringTopAccessedValue.copy());

        return newObject;
    }

    @Override
    public boolean isSame(DictObjectValue other, AllocatePointTable apTable) {
        if (!areAttrsSame(other, apTable) || !areElementsSame(other, apTable)) {
            // There is at least one abstract value which is not the same.
            return false;
        }

        for (Map.Entry<String, IForwardAbstractValue> m: stringAccessElements.entrySet()) {
            String key = m.getKey();
            IForwardAbstractValue value = m.getValue();
            if (other.hasAttr(key, apTable)) {
                if (!value.isSame(other.getAttr(key, apTable))) {
                    // The abstract values of the attribute are not the same.
                    return false;
                }
            } else {
                // The other object doesn't have the attribute.
                return false;
            }
        }

        return stringTopAccessedValue.isSame(other.stringTopAccessedValue);
    }

    @Override
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        super.collectRelatedAPs(apTable, result);

        for (IForwardAbstractValue strAccessValue : stringAccessElements.values()) {
            // Get allocate points of abstract values which string access elements point to.
            for (AllocatePoint strAccessAp:
                    ((ForwardAbstractValue) strAccessValue).getAllocatePoints()) {
                if (!result.contains(strAccessAp)) {
                    apTable.get(strAccessAp).collectRelatedAPs(apTable, result);
                }
            }
        }
        for (AllocatePoint strTopAccessAp: stringTopAccessedValue.getAllocatePoints()) {
            if (!result.contains(strTopAccessAp)) {
                apTable.get(strTopAccessAp).collectRelatedAPs(apTable, result);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Dict<" + super.toString() + ">";
    }



    private static DictObjectValue getSelf(ForwardState state, IForwardAbstractValue firstArg) {
        AllocatePoints allocatePoints = ((ForwardAbstractValue) firstArg).getAllocatePoints();
        assert allocatePoints.size() == 1;

        ObjectValue method = allocatePoints.getObjectsIterator(state.getAllocatePointTable()).next();
        assert method instanceof MethodSummaryObjectValue;

        ObjectValue selfObject = ((MethodSummaryObjectValue) method).getSelfObjectValue(state);
        assert selfObject instanceof DictObjectValue;

        return (DictObjectValue) selfObject;
    }

    public static IForwardAbstractValue values(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        DictObjectValue selfObject = getSelf(state, args.get(0));

        AllocatePointTable callerAPTable = state.getAllocatePointTable();
        ListObjectValue result = new ListObjectValue(
                new AllocatePoint(state.getCGNode(), inst), callerAPTable
        );
        callerAPTable.newAllocation(result);

        ForwardAbstractValue element = new ForwardAbstractValue();
        element.union(selfObject.getStringTopAccessedValue());
        for (String key: selfObject.getStringAccessElements().keySet()) {
            if (!defaultMethodNames.contains(key))
                element.union((ForwardAbstractValue) selfObject.getStringAccessElements().get(key));
        }
        element.union(selfObject.getIntTopAccessedValue());
        for (Integer key: selfObject.getIntAccessElements().keySet()) {
            element.union((ForwardAbstractValue) selfObject.getIntAccessElements().get(key));
        }

        result.setElement(
                new ForwardAbstractValue(new IntValue(LatticeTop.TOP)), element
        );

        return new ForwardAbstractValue(result);
    }

    public static IForwardAbstractValue get(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        DictObjectValue self = getSelf(state, args.get(0));
        ForwardAbstractValue attrName = (ForwardAbstractValue) args.get(1);

        IForwardAbstractValue result = null;
        if (self.hasAttr(attrName, state.getAllocatePointTable())) {
            result = self.getElement(attrName, state.getAllocatePointTable());
        } else if (args.size() < 3) {
            result = new ForwardAbstractValue(new NoneValue(LatticeTop.TOP));
        } else {
            result = args.get(2);
        }
        return result;
    }

    public static IForwardAbstractValue setdefault(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ForwardAbstractValue attrName = (ForwardAbstractValue) args.get(1);
        ForwardAbstractValue defaultValue = (ForwardAbstractValue) args.get(2);

        DictObjectValue self = getSelf(state, args.get(0));
        if (!self.stringAccessElements.containsKey(attrName)) {
            self.setElement(attrName, defaultValue);
        }

        return self.getElement(attrName, state.getAllocatePointTable());
    }

    public static IForwardAbstractValue items(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        TupleObjectValue tuple = new TupleObjectValue(new AllocatePoint(state.getCGNode(), inst));
        state.getAllocatePointTable().newAllocation(tuple);

        DictObjectValue selfObject = getSelf(state, args.get(0));

        ForwardAbstractValue key = new ForwardAbstractValue();
        IForwardAbstractValue value = new ForwardAbstractValue();

        if (!selfObject.getStringTopAccessedValue().isBottom()) {
            key.setStringValue(new StringValue(LatticeTop.TOP));
        } else if (selfObject.getStringAccessElements().size() == 1) {
            String keyStr = (String) selfObject.getStringAccessElements().entrySet().toArray()[0];
            key.setStringValue(new StringValue(keyStr));
        } else if (selfObject.getStringAccessElements().size() == 0) {
        } else {
            key.setStringValue(new StringValue(LatticeTop.TOP));
        }
        value.union(
                selfObject.getElement(
                        new ForwardAbstractValue(new StringValue(LatticeTop.TOP)),
                        state.getAllocatePointTable()));

        if (!selfObject.getIntTopAccessedValue().isBottom()) {
            key.setIntValue(new IntValue(LatticeTop.TOP));
        } else if (selfObject.getIntAccessElements().size() == 1) {
            int keyInt = (int) selfObject.getIntAccessElements().entrySet().toArray()[0];
            key.setIntValue(new IntValue(keyInt));
        } else if (selfObject.getIntAccessElements().size() == 0) {
        } else {
            key.setIntValue(new IntValue(LatticeTop.TOP));
        }
        value.union(
                selfObject.getElement(
                        new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                        state.getAllocatePointTable()));

        tuple.setElement(0, key);
        tuple.setElement(1, value);

        ListObjectValue result = new ListObjectValue(
                new AllocatePoint(state.getCGNode(), inst) {
                    // In order to be different from allocate point of 'tuple', override methods.
                    @Override
                    public int hashCode() {
                        return super.hashCode() + Objects.hashCode("list");
                    }

                    @Override
                    public String toString() {
                        return super.toString() + "-list";
                    }
                }, state.getAllocatePointTable());
        state.getAllocatePointTable().newAllocation(result);
        result.setElement(
                new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                new ForwardAbstractValue(tuple)
        );

        return new ForwardAbstractValue(result);
    }
}
