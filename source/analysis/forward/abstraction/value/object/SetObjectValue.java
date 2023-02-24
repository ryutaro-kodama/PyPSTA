package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.AllocatePoints;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;

import java.util.ArrayList;

public class SetObjectValue extends ComplexObjectValue<SetObjectValue> {
    public SetObjectValue(AllocatePoint allocatePoint, AllocatePointTable table) {
        super(allocatePoint, PythonTypes.set);
        setDefaultMethod("add", (state, args, inst) -> add(state, args, inst), table);
        setDefaultMethod("clear", (state, args, inst) -> clear(state, args, inst), table);
    }

    @Override
    public boolean union(SetObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);
    }

    @Override
    public SetObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        SetObjectValue newObject = new SetObjectValue(getAllocatePoint(), apTable);

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(SetObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable) && areElementsSame(other, apTable);
    }

    @Override
    public String toString() {
        return "Set<" + super.toString() + ">";
    }



    private static SetObjectValue getSelf(ForwardState state, IForwardAbstractValue firstArg) {
        AllocatePoints allocatePoints = ((ForwardAbstractValue) firstArg).getAllocatePoints();
        assert allocatePoints.size() == 1;

        ObjectValue method = allocatePoints.getObjectsIterator(state.getAllocatePointTable()).next();
        assert method instanceof MethodSummaryObjectValue;

        ObjectValue selfObject = ((MethodSummaryObjectValue) method).getSelfObjectValue(state);
        assert selfObject instanceof SetObjectValue;

        return (SetObjectValue) selfObject;
    }

    public static IForwardAbstractValue add(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        SetObjectValue selfObject = getSelf(state, args.get(0));
        selfObject.intTopAccessedValue.union((ForwardAbstractValue) args.get(1));
        return new ForwardAbstractValue();
    }

    public static IForwardAbstractValue clear(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        SetObjectValue selfObject = getSelf(state, args.get(0));
        selfObject.intAccessElements.clear();
        selfObject.intTopAccessedValue = new ForwardAbstractValue();
        return new ForwardAbstractValue();
    }
}
