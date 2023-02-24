package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.types.TypeReference;

public class FunctionObjectValue extends CallableObjectValue<FunctionObjectValue> {
    public FunctionObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        super(allocatePoint, typeRef);
    }

    @Override
    public boolean union(FunctionObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable);
    }

    @Override
    public FunctionObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        FunctionObjectValue newObject = new FunctionObjectValue(getAllocatePoint(), getTypeReference());

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(FunctionObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable);
    }

    @Override
    public String toString() {
        return "Func<" + super.toString() + ">";
    }
}
