package analysis.forward.abstraction.value.object;

import analysis.forward.ExceptionManager;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.StringValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class ObjectValue<T extends ObjectValue<T>> {
    private final AllocatePoint allocatePoint;

    private final TypeReference typeRef;

    protected final HashMap<String, IForwardAbstractValue> attributes = new HashMap<>();

    /**
     * This abstract value is used when abstract value of attribute access's member variable is top. This means that
     * we don't know what element the member is access.
     */
    public ObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        this.allocatePoint = allocatePoint;
        this.typeRef = typeRef;
    }

    public AllocatePoint getAllocatePoint() {
        return allocatePoint;
    }

    public TypeReference getTypeReference() {
        return typeRef;
    }

    public HashMap<String, IForwardAbstractValue> getAttributes() {
        return attributes;
    }

    public boolean hasAttr(String fieldName, AllocatePointTable apTable) {
        return attributes.containsKey(fieldName);
    }

    public boolean hasAttr(FieldReference fieldRef, AllocatePointTable apTable) {
        return hasAttr(fieldRef.getName().toString(), apTable);
    }

    public boolean hasAttr(IForwardAbstractValue abstractField, AllocatePointTable apTable) {
        StringValue stringValue = ((ForwardAbstractValue) abstractField).getStringValue();
        if (stringValue.isBottom()) {
            return false;
        } else if (stringValue.isTop()) {
            Assertions.UNREACHABLE(); return false;
        } else {
            return hasAttr(stringValue.getConcreteValue(), apTable);
        }
    }

    public IForwardAbstractValue getAttr(String fieldName, AllocatePointTable apTable) {
        if (attributes.containsKey(fieldName)) {
            return attributes.get(fieldName);
        } else {
            ExceptionManager.attributeException(this, fieldName);
            return new ForwardAbstractValue();
        }
    }

    public IForwardAbstractValue getAttr(FieldReference fieldRef, AllocatePointTable apTable) {
        return getAttr(fieldRef.getName().toString(), apTable);
    }

    public IForwardAbstractValue getAttr(IForwardAbstractValue abstractField, AllocatePointTable apTable) {
        StringValue stringValue = ((ForwardAbstractValue) abstractField).getStringValue();
        if (stringValue.isBottom()) {
            Assertions.UNREACHABLE(); return null;
        } else if (stringValue.isTop()) {
            Assertions.UNREACHABLE(); return null;
        } else {
            return getAttr(stringValue.getConcreteValue(), apTable);
        }
    }

    public void setAttr(String fieldName, IForwardAbstractValue value) {
        attributes.put(fieldName, value);
    }

    public void setAttr(FieldReference fieldRef, IForwardAbstractValue value) {
        setAttr(fieldRef.getName().toString(), value);
    }

    public void setAttr(IForwardAbstractValue abstractField, IForwardAbstractValue abstractValue) {
        StringValue stringValue = ((ForwardAbstractValue) abstractField).getStringValue();
        if (stringValue.isBottom()) {
            Assertions.UNREACHABLE();
        } else if (stringValue.isTop()) {
            Assertions.UNREACHABLE();
        } else {
            setAttr(stringValue.getConcreteValue(), abstractValue.copy());
        }
    }

    /**
     * Get all keys (attribute names) of this object.
     * @return the union abstract value of keys.
     */
    public IForwardAbstractValue getKeys() {
        // TODO: Whether this object has '__iter__' ?
        Assertions.UNREACHABLE();
        return null;
    }

    /**
     * Change this object's state to the result of union with other object value.
     * @param other other object value
     * @param apTable the allocation point table where this method is called
     * @return whether this value has changed
     */
    public abstract boolean union(T other, AllocatePointTable apTable);

    /**
     * Change state of this object's to the result of union of the attribute's values with other object value.
     * @param other other object value
     * @param apTable the allocation point table where this method is called
     * @return whether this value has changed
     */
    protected boolean unionAttrs(ObjectValue other, AllocatePointTable apTable) {
        boolean hasChanged = false;
        for (Object m: other.attributes.entrySet()) {
            Map.Entry<String, IForwardAbstractValue> attr2value = (Map.Entry<String, IForwardAbstractValue>) m;
            String attrName = attr2value.getKey();
            if (attributes.containsKey(attrName)) {
                // When you unite attributes, you must unite only this object's attributes, not do
                // for parent class, and so on. So here not using 'hasattr' method or 'getattr' method.
                hasChanged = attributes.get(attrName).union(attr2value.getValue()) || hasChanged;
            } else {
                setAttr(attrName, attr2value.getValue().copy());
                hasChanged = true;
            }
        }

        return hasChanged;
    }

    public boolean isCallable(AllocatePointTable apTable) {
        return hasAttr("__call__", apTable);
    }

    public boolean isComplex() {
        return false;
    }

    public abstract T copy(AllocatePointTable apTable);

    protected ObjectValue copyAttrs(ObjectValue target) {
        for (Map.Entry<String, IForwardAbstractValue> m: attributes.entrySet()) {
            target.attributes.put(m.getKey(), m.getValue().copy());
        }
        return target;
    }

    /**
     * Check that this object value and other object value is the same, which means that
     * whether all abstract values of attributes (and elements) are the same. Don't check
     * that the object values which are pointed from attribute (and element) abstract values
     * are the same.
     * @param other other object value
     * @param apTable the allocation point table where this method is called
     * @return whether other is the same to this
     */
    public abstract boolean isSame(T other, AllocatePointTable apTable);

    /**
     * Check abstract values of this object's attribute are the same to one of the other.
     * Don't check that the object values which are pointed from attributes are the same.
     * @param other other object value
     * @param apTable the allocation point table where this method is called
     * @return whether other attribute are the same to this
     */
    protected boolean areAttrsSame(ObjectValue other, AllocatePointTable apTable) {
        // Is number of attribute same?
        if (attributes.size() != other.attributes.size()) return false;

        for (Map.Entry<String, IForwardAbstractValue> m: attributes.entrySet()) {
            String attrName = m.getKey();
            IForwardAbstractValue value = m.getValue();
            if (other.hasAttr(attrName, apTable)) {
                if (!value.isSame(other.getAttr(attrName, apTable))) {
                    // The abstract values of the attribute are not the same.
                    return false;
                }
            } else {
                // The other object doesn't have the attribute.
                return false;
            }
        }
        return true;
    }

    /**
     * Collect allocation points, which are need to compose this object value.
     * @param apTable the allocation point table where this method is called
     * @param result the collection of result
     * @return the collection of related allocate points
     */
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        result.add(getAllocatePoint());
        for (IForwardAbstractValue attrValue : attributes.values()) {
            // Get allocate points of abstract values which attributes point to.
            for (AllocatePoint attrAp: ((ForwardAbstractValue) attrValue).getAllocatePoints()) {
                if (!result.contains(attrAp)) {
                    apTable.get(attrAp).collectRelatedAPs(apTable, result);
                }
            }
        }
        return result;
    }

    public String getFullName() {
        return getTypeReference().getName().toString();
    }

    public String getObjectName() {
        return getTypeReference().getName().getClassName().toString();
    }

    @Override
    public String toString() {
        return getTypeReference().getName().toString();
    }
}
