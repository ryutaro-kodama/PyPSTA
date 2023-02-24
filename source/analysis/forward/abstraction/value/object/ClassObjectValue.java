package analysis.forward.abstraction.value.object;

import analysis.forward.ExceptionManager;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.StringValue;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;

import java.util.Collection;

public class ClassObjectValue extends CallableObjectValue<ClassObjectValue> {
    public static ClassObjectValue objectClass = new ClassObjectValue(
            new AllocatePoint(null, null) {
                @Override
                public boolean match(AllocatePoint other) {
                    return false;
                }

                @Override
                public boolean equals(Object o) {
                    return false;
                }

                @Override
                public int hashCode() {
                    return -1;
                }

                @Override
                public String toString() {
                    return "Python default object class";
                }
            },
            PythonTypes.object,
            null
    );

    private final AllocatePoint parentAP;

    public ClassObjectValue(AllocatePoint allocatePoint, TypeReference typeRef, AllocatePoint parentAP) {
        super(allocatePoint, typeRef);
        this.parentAP = parentAP;
        setAttr("__name__", new ForwardAbstractValue(new StringValue(typeRef.getName().getClassName().toString())));
    }

    public AllocatePoint getParentAP() {
        return parentAP;
    }

    @Override
    public boolean hasAttr(String fieldName, AllocatePointTable apTable) {
        if (parentAP != null) {
            return super.hasAttr(fieldName, apTable)
                    || apTable.get(parentAP).hasAttr(fieldName, apTable);
        } else {
            return super.hasAttr(fieldName, apTable);
        }
    }

    @Override
    public IForwardAbstractValue getAttr(String fieldName, AllocatePointTable apTable) {
        if (super.hasAttr(fieldName, apTable)) {
            return super.getAttr(fieldName, apTable);
        } else {
            if (parentAP != null) {
                return apTable.get(parentAP).getAttr(fieldName, apTable);
            } else {
                ExceptionManager.attributeException(this, fieldName);
                return new ForwardAbstractValue();
            }
        }
    }

    @Override
    public boolean union(ClassObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable);
    }

    @Override
    public ClassObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        ClassObjectValue newObject = new ClassObjectValue(getAllocatePoint(), getTypeReference(), parentAP);

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(ClassObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable);
    }

    @Override
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        super.collectRelatedAPs(apTable, result);

        if (parentAP != null && !result.contains(parentAP)) {
            apTable.get(parentAP).collectRelatedAPs(apTable, result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Class<" + super.toString() + ">";
    }
}
