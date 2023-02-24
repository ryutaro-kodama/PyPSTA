package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.BuiltinFunctionSummaries;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

import java.util.ArrayList;

public class MethodSummaryObjectValue extends FunctionObjectValue implements BuiltinFunctionSummaries.Summary {
    private final AllocatePoint self;
    private final BuiltinFunctionSummaries.Summary method;

    public MethodSummaryObjectValue(AllocatePoint allocatePoint, TypeReference typeRef, AllocatePoint self, BuiltinFunctionSummaries.Summary method) {
        super(allocatePoint, typeRef);
        this.self = self;
        this.method = method;
    }

    public ObjectValue getSelfObjectValue(ForwardState state) {
        AllocatePointTable table = state.getAllocatePointTable();
        if (!table.containsKey(self))
            Assertions.UNREACHABLE();
        return table.get(self);
    }

    /**
     * Call the function object which is the summary.
     * @param state the caller's state
     * @param args the real arguments
     * @param inst the call node
     * @return the return value
     */
    @Override
    public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        return method.call(state, args, inst);
    }

    @Override
    public MethodSummaryObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        MethodSummaryObjectValue newObject = new MethodSummaryObjectValue(
                getAllocatePoint(), getTypeReference(), self, method
        );

        // Copy attributes.
        copyAttrs(newObject);

        return newObject;
    }
}
