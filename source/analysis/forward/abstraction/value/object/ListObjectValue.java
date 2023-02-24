package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.AllocatePoints;
import analysis.forward.abstraction.value.element.IntValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.ExceptionManager;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.debug.Assertions;

import java.util.ArrayList;
import java.util.Map;

public class ListObjectValue extends ComplexObjectValue<ListObjectValue> {
    public ListObjectValue(AllocatePoint allocatePoint, AllocatePointTable table) {
        super(allocatePoint, PythonTypes.list);
        setDefaultMethod("insert", (state, args, inst) -> insert(state, args, inst), table);
        setDefaultMethod("remove", (state, args, inst) -> remove(state, args, inst), table);
        setDefaultMethod("pop", (state, args, inst) -> pop(state, args, inst), table);
        setDefaultMethod("append", (state, args, inst) -> append(state, args, inst), table);
        setDefaultMethod("__add__", (state, args, inst) -> __add__(state, args, inst), table);
        setDefaultMethod("__mul__", (state, args, inst) -> __mul__(state, args, inst), table);
    }

    @Override
    public boolean union(ListObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);
    }

    @Override
    public ListObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        ListObjectValue newObject = new ListObjectValue(getAllocatePoint(), apTable);

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        return newObject;
    }

    public ListObjectValue slice(
            ForwardAbstractValue startValue, ForwardAbstractValue endValue, ForwardAbstractValue stepValue,
            ForwardAbstractValue other, AllocatePointTable apTable, ListObjectValue result) {
        // TODO: value validation.

        if (other.getAllocatePoints().isBottom()) {
            int start;
            IntValue startIntValue = startValue.getIntValue();
            if (startIntValue.isBottom() || startIntValue.isTop()) {
                start = 0;
            } else {
                start = startIntValue.getConcreteValue();
            }

            int end;
            IntValue endIntValue = endValue.getIntValue();
            if (endIntValue.isBottom() || endIntValue.isTop()) {
                end = intAccessElements.size() - 1;
            } else {
                end = endIntValue.getConcreteValue();
            }

            int step;
            IntValue stepIntValue = stepValue.getIntValue();
            if (stepIntValue.isBottom() || stepIntValue.isTop()) {
                step = 1;
            } else {
                step = stepIntValue.getConcreteValue();
            }

            if (step < 0) {
                int tmp = start; start = end; end = tmp;
            }

            // Slice from this list.
            int result_index = 0;
            for (int base_index = start; 0 <= base_index && base_index < end; base_index += step) {
                if (hasElement(base_index)) {
                    result.setElement(result_index, getElement(base_index, apTable).copy());
                    result_index += 1;
                }
            }

            result.intTopAccessedValue.union(intTopAccessedValue.copy());
        } else {
            // Slice from arg5 list and set to result list.
            // But this calculation is difficult so union to top accessed value.
            for (ObjectValue base: other.getAllocatePoints().getObjectsIterable(apTable)) {
                if (base instanceof ListObjectValue) {
                    ListObjectValue base1 = (ListObjectValue) base;
                    for (IForwardAbstractValue element: base1.getIntAccessElements().values()) {
                        result.intTopAccessedValue.union((ForwardAbstractValue) element);
                    }
                } else {
                    Assertions.UNREACHABLE();
                }
            }

            for (IForwardAbstractValue element: this.getIntAccessElements().values()) {
                result.intTopAccessedValue.union((ForwardAbstractValue) element);
            }
        }
        return result;
    }

    @Override
    public boolean isSame(ListObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable) && areElementsSame(other, apTable);
    }

    @Override
    public String toString() {
        return "List<" + super.toString() + ">";
    }



    private static ListObjectValue getSelf(ForwardState state, IForwardAbstractValue firstArg) {
        AllocatePoints allocatePoints = ((ForwardAbstractValue) firstArg).getAllocatePoints();
        assert allocatePoints.size() == 1;

        ObjectValue method = allocatePoints.getObjectsIterator(state.getAllocatePointTable()).next();
        assert method instanceof MethodSummaryObjectValue;

        ObjectValue selfObject = ((MethodSummaryObjectValue) method).getSelfObjectValue(state);
        assert selfObject instanceof ListObjectValue;

        return (ListObjectValue) selfObject;
    }

    public static IForwardAbstractValue insert(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ListObjectValue selfObject = getSelf(state, args.get(0));

        // TODO: More detail, when the directed index is not top.
        selfObject.intTopAccessedValue.union((ForwardAbstractValue) args.get(2));
        return new ForwardAbstractValue();
    }

    public static IForwardAbstractValue remove(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        return new ForwardAbstractValue();
    }

    public static IForwardAbstractValue pop(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ForwardAbstractValue result = new ForwardAbstractValue();

        // You can't find which element is selected when the list has top access abstract value.
        // So return all possible abstract value.
        ListObjectValue selfObject = getSelf(state, args.get(0));
        for (Map.Entry<Integer, IForwardAbstractValue> m: selfObject.intAccessElements.entrySet()) {
            result.union((ForwardAbstractValue) m.getValue());
        }
        result.union(selfObject.intTopAccessedValue);
        // TODO: Modify base (=self) list
        return result;
    }

    public static IForwardAbstractValue append(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ListObjectValue selfObject = getSelf(state, args.get(0));

        // TODO: More detail, when the directed index is not top.
        selfObject.intTopAccessedValue.union((ForwardAbstractValue) args.get(1));
        return new ForwardAbstractValue();
    }

    public static IForwardAbstractValue __add__(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ListObjectValue selfObject = getSelf(state, args.get(0));

        ListObjectValue newList = new ListObjectValue(
                new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable()
        );
        state.getAllocatePointTable().newAllocation(newList);

        // Copy arg0 list.
        selfObject.copyIntAccessElements(newList);

        ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
        // Copy arg1 list (arg0 list's size is unknown so arg1 list elements' 1st index is unknown
        // in new list).
        for (ObjectValue objectValue:
                arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
            if (objectValue instanceof ListObjectValue) {
                newList.getIntTopAccessedValue().union(
                        (ForwardAbstractValue)
                                ((ListObjectValue) objectValue).getElement(
                                        new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                                state.getAllocatePointTable())
                );
            } else {
                // Error
            }
        }

        return new ForwardAbstractValue(newList);
    }

    public static IForwardAbstractValue __mul__(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
        ListObjectValue selfObject = getSelf(state, args.get(0));

        ForwardAbstractValue other = (ForwardAbstractValue) args.get(1);
        // TODO: Check other don't contains string etc...

        AllocatePointTable apTable = state.getAllocatePointTable();

        ListObjectValue result = new ListObjectValue(
                new AllocatePoint(state.getCGNode(), inst), apTable
        );
        apTable.newAllocation(result);

        IntValue intValue = other.getIntValue();
        if (intValue.isBottom()) {
            Assertions.UNREACHABLE();
            ExceptionManager.illegalBinOpeException(null, intValue, null);
        } else if (intValue.isTop()) {
            for (Map.Entry<Integer, IForwardAbstractValue> m: selfObject.getIntAccessElements().entrySet()) {
                // Copy to new list.
                result.setElement(m.getKey(), m.getValue());
                // You don't know the number of copy so all elements are united to top access value.
                result.setElement(new ForwardAbstractValue(intValue), m.getValue());
            }
        } else {
            if (selfObject.getIntTopAccessedValue().isBottom()) {
                int size = intValue.getConcreteValue();
                int baseListSize = selfObject.getIntAccessElements().size();
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < baseListSize; j++) {
                        result.setElement(i * baseListSize + j, selfObject.getElement(j, apTable));
                    }
                }
            } else {
                for (int i: selfObject.getIntAccessElements().keySet()) {
                    // Copy to new list.
                    result.setElement(i, selfObject.getIntAccessElements().get(i));

                    // You don't know the number of copy so all elements are united to top access value.
                    result.getIntTopAccessedValue().union(
                            (ForwardAbstractValue) selfObject.getIntAccessElements().get(i)
                    );
                }

                result.getIntTopAccessedValue().union(selfObject.getIntTopAccessedValue());
            }
        }
        return new ForwardAbstractValue(result);
    }
}
