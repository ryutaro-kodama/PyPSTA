package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public abstract class CallableObjectValue<T extends CallableObjectValue<T>> extends ObjectValue<T> {
    public CallableObjectValue(AllocatePoint allocatePoint, TypeReference typeRef) {
        super(allocatePoint, typeRef);
    }

    @Override
    public boolean isCallable(AllocatePointTable table) {
        return true;
    }

    public MethodReference getMethodReference() {
        return MethodReference.findOrCreate(
                PythonLanguage.Python,
                getTypeReference(),
                AstMethodReference.fnAtomStr,  // "do"
                "()LRoot"
                // TODO: Descriptor's literal can not be general, refer to
                //  {@link com.ibm.wala.cast.python.client.ir.PythonCAstToIRTranslator@doPrimitive}
        );
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
