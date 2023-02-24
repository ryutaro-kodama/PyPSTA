package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

import java.util.Collection;

public class InstanceObjectValue extends ObjectValue<InstanceObjectValue> {
    // TODO: Delete this static field.
    public static IClassHierarchy cha;

    private final AllocatePoint baseClassAllocatePoint;

    public InstanceObjectValue(AllocatePoint allocatePoint, ClassObjectValue baseClass) {
        this(allocatePoint,
                TypeReference.findOrCreate(
                        PythonTypes.pythonLoader, "object_instance"),
                baseClass.getAllocatePoint());
    }

    public InstanceObjectValue(AllocatePoint allocatePoint,
                               TypeReference instanceType,
                               ClassObjectValue baseClass) {
        this(allocatePoint, instanceType, baseClass.getAllocatePoint());
    }

    private InstanceObjectValue(AllocatePoint allocatePoint, TypeReference typeRef, AllocatePoint baseClassAllocatePoint) {
        super(allocatePoint, typeRef);
        this.baseClassAllocatePoint = baseClassAllocatePoint;
    }

    public AllocatePoint getBaseClassAP() {
        return baseClassAllocatePoint;
    }

    @Override
    public boolean hasAttr(String fieldName, AllocatePointTable apTable) {
        return super.hasAttr(fieldName, apTable)
                || apTable.get(baseClassAllocatePoint).hasAttr(fieldName, apTable);
    }

    /**
     * If you get attribute from base classes, the result may be the class's method. If so,
     * you must use trampoline method to convert `val.attr()` to `class.(val, attr)`. Therefore,
     * we decide that just when you access attributes, we return the trampoline method.
     * @param fieldName the field name
     * @param apTable the allocation point table where this method is called
     * @return the attribute's abstract value (, which may be trampoline method).
     */
    @Override
    public IForwardAbstractValue getAttr(String fieldName, AllocatePointTable apTable) {
        if (super.hasAttr(fieldName, apTable)) {
            return super.getAttr(fieldName, apTable);
        } else {
            ForwardAbstractValue parentAttrVal
                    = (ForwardAbstractValue) apTable.get(baseClassAllocatePoint).getAttr(fieldName, apTable);

            ForwardAbstractValue result = parentAttrVal.copy();
            result.getAllocatePoints().clear();

            for (ObjectValue objectValue: parentAttrVal.getAllocatePoints().getObjectsIterable(apTable)) {
                if (objectValue instanceof FunctionObjectValue) {
                    String clsName = objectValue.getTypeReference().getName().getPackage().toString();
                    TypeReference clsType = TypeReference.find(PythonTypes.pythonLoader, "L" +clsName);
                    IClass cls = cha.lookupClass(clsType);
                    if (cha.isSubclassOf(cls, cha.lookupClass(PythonTypes.object))) {
                        // We assume that if function's package is subclass of 'PythonType.object',
                        // the package is class, and the function is the class's method.

                        // Create trampoline class and type.
                        TypeReference trampType = PythonInstanceMethodTrampoline.findOrCreate(
                                objectValue.getTypeReference(), cha
                        );

                        // Create trampoline function object.
                        FunctionTrampolineObjectValue trampFunc = new FunctionTrampolineObjectValue(
                                new TrampAllocatePoint(
                                        objectValue.getAllocatePoint(), trampType, getAllocatePoint()),
                                trampType);
                        apTable.newAllocation(trampFunc);

                        // Set 'self' instance object and real function to the trampoline function.
                        trampFunc.setAttr("$self", new ForwardAbstractValue(this));
                        trampFunc.setAttr("$function", new ForwardAbstractValue(objectValue));

                        result.getAllocatePoints().add(trampFunc.getAllocatePoint());
                        continue;
                    }
                }
                result.getAllocatePoints().add(objectValue.getAllocatePoint());
            }
            return result;
        }
    }

    private class TrampAllocatePoint extends AllocatePoint {
        private final TypeReference trampType;
        private final AllocatePoint instanceAP;

        public TrampAllocatePoint(AllocatePoint funcAP, TypeReference trampType, AllocatePoint instanceAP) {
            super(funcAP.getCGNode(), funcAP.getInstruction());
            this.trampType = trampType;
            this.instanceAP = instanceAP;
        }

        @Override
        public boolean match(AllocatePoint other) {
            return equals(other) ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrampAllocatePoint that = (TrampAllocatePoint) o;
            return trampType.equals(that.trampType) && instanceAP.equals(that.instanceAP)
                    && super.equals(that);
        }

        @Override
        public int hashCode() {
            return super.hashCode() + instanceAP.hashCode() + trampType.hashCode();
        }

        @Override
        public String toString() {
            return super.toString() + "-tramp";
        }
    }

    @Override
    public boolean union(InstanceObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable);
    }

    @Override
    public InstanceObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        InstanceObjectValue newObject = new InstanceObjectValue(getAllocatePoint(), getTypeReference(), baseClassAllocatePoint);

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(InstanceObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable);
    }

    @Override
    public Collection<AllocatePoint> collectRelatedAPs(AllocatePointTable apTable,
                                                       Collection<AllocatePoint> result) {
        super.collectRelatedAPs(apTable, result);

        if (!result.contains(baseClassAllocatePoint)) {
            apTable.get(baseClassAllocatePoint).collectRelatedAPs(apTable, result);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Instance<" + baseClassAllocatePoint + '>';
    }
}
