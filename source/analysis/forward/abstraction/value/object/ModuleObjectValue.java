package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.types.TypeReference;

public class ModuleObjectValue extends CallableObjectValue<ModuleObjectValue> {
    public ModuleObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        super(allocatePoint, typeRef);
    }

    @Override
    public boolean union(ModuleObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable);
    }

    @Override
    public ModuleObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        ModuleObjectValue newObject = new ModuleObjectValue(getAllocatePoint(), getTypeReference());

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(ModuleObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable);
    }

    @Override
    public String toString() {
        return "Module<" + getTypeReference().getName() + ">";
    }
}
