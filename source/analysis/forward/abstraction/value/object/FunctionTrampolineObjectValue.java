package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

import java.util.Map;

public class FunctionTrampolineObjectValue extends FunctionObjectValue {
    public FunctionTrampolineObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        super(allocatePoint, typeRef);
    }

    @Override
    public FunctionTrampolineObjectValue copy(AllocatePointTable apTable) {
        FunctionTrampolineObjectValue newObject =
                new FunctionTrampolineObjectValue(getAllocatePoint(), getTypeReference());

        for (Map.Entry<String, IForwardAbstractValue> attr2value: attributes.entrySet()) {
            String attrName = attr2value.getKey();
            if (attrName.equals("$self") || attrName.equals("$function"))
                newObject.setAttr(attrName, attr2value.getValue().copy());
            else
                // Are there other attributes?
                Assertions.UNREACHABLE();
        }
        return newObject;
    }

    @Override
    public String toString() {
        return "Tramp<" + getFullName() + ">";
    }
}
