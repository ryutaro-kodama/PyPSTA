package analysis.forward.abstraction.value.element;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.object.MethodSummaryObjectValue;
import analysis.forward.abstraction.value.object.ObjectValue;
import analysis.forward.ExceptionManager;
import analysis.forward.fixpoint.ForwardCallManager;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;

public class AllocatePoints extends HashSet<AllocatePoint> implements IForwardAbstractValueElement<AllocatePoints> {
    @Override
    public Set<TypeReference> getTypes(ForwardState forwardState) {
        return null;
    }

    public Iterator<ObjectValue> getObjectsIterator(AllocatePointTable allocatePointTable) {
        Iterator<AllocatePoint> iter = iterator();
        return new Iterator<ObjectValue>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public ObjectValue next() {
                AllocatePoint allocatePoint = iter.next();
                if (!allocatePointTable.containsKey(allocatePoint))
                    Assertions.UNREACHABLE();
                return allocatePointTable.get(allocatePoint);
            }
        };
    }

    public Iterable<ObjectValue> getObjectsIterable(AllocatePointTable allocatePointTable) {
        return Iterator2Iterable.make(getObjectsIterator(allocatePointTable));
    }

    /**
     * Add all other's allocate points to this.
     * @param other
     * @return has changed
     */
    public boolean union(AllocatePoints other) {
        return addAll(other);
    }

    private BoolValue illegalCompOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalCompareException(this, other);
        return new BoolValue();
    }

    private BoolValue commonCompOperation(IForwardAbstractValueElement other,
                                          String funcName,
                                          SSABinaryOpInstruction inst,
                                          ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (other.isBottom()) return result;

        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr(funcName, forwardState.getAllocatePointTable())) {
                // Call special method.
                IForwardAbstractValue retVal = ForwardCallManager.callSpecialFunction(
                        inst, objectValue, forwardState, funcName
                );
                result.union(((ForwardAbstractValue) retVal).getBoolValue());
            } else {
                ExceptionManager.notDefinedCompare(objectValue, other, funcName);
            }
        }
        return result;
    }

    public BoolValue eq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue eq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherNone.eq(this, inst, forwardState);
    }

    public BoolValue eq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__eq__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherBool, "__eq__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue eq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__eq__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherInt, "__eq__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__eq__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherFloat, "__eq__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__eq__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherString, "__eq__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue eq(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__eq__", inst, forwardState);
    }

    public BoolValue neq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue neq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherNone.neq(this, inst, forwardState);
    }

    public BoolValue neq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__ne__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherBool, "__ne__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue neq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__ne__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherInt, "__ne__", inst, forwardState);
            } else {
                result.union(new BoolValue(true));
            }
        }
        return result;
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__ne__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherFloat, "__ne__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        for (ObjectValue objectValue: getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr("__ne__", forwardState.getAllocatePointTable())) {
                return commonCompOperation(otherString, "__ne__", inst, forwardState);
            } else {
                result.union(new BoolValue(false));
            }
        }
        return result;
    }

    public BoolValue neq(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__ne__", inst, forwardState);
    }

    public BoolValue lt(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue lt(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue lt(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherBool, "__lt__", inst, forwardState);
    }

    public BoolValue lt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherInt, "__lt__", inst, forwardState);
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherFloat, "__lt__", inst, forwardState);
    }

    public BoolValue lt(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherString, "__lt__", inst, forwardState);
    }

    public BoolValue lt(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__lt__", inst, forwardState);
    }

    public BoolValue lte(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue lte(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue lte(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherBool, "__le__", inst, forwardState);
    }

    public BoolValue lte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherInt, "__le__", inst, forwardState);
    }

    public BoolValue lte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherFloat, "__le__", inst, forwardState);
    }

    public BoolValue lte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherString, "__le__", inst, forwardState);
    }

    public BoolValue lte(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__le__", inst, forwardState);
    }

    public BoolValue gt(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue gt(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue gt(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherBool, "__gt__", inst, forwardState);
    }

    public BoolValue gt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherInt, "__gt__", inst, forwardState);
    }

    public BoolValue gt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherFloat, "__gt__", inst, forwardState);
    }

    public BoolValue gt(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherString, "__gt__", inst, forwardState);
    }

    public BoolValue gt(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__gt__", inst, forwardState);
    }

    public BoolValue gte(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue gte(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue gte(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherBool, "__ge__", inst, forwardState);
    }

    public BoolValue gte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherInt, "__ge__", inst, forwardState);
    }

    public BoolValue gte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherFloat, "__ge__", inst, forwardState);
    }

    public BoolValue gte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherString, "__ge__", inst, forwardState);
    }

    public BoolValue gte(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonCompOperation(otherAllocatePoints, "__ge__", inst, forwardState);
    }



    private AllocatePoints illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new AllocatePoints();
    }

    private AllocatePoints commonBinOperation(
            IForwardAbstractValue other,
            String funcName,
            SSABinaryOpInstruction inst,
            ForwardState forwardState) {
        AllocatePoints result = new AllocatePoints();
        if (other.isBottom())
            return result;

        AllocatePointTable apTable = forwardState.getAllocatePointTable();
        for (ObjectValue objectValue: getObjectsIterable(apTable)) {
            // Check whether special method is defined in the other.
            if (objectValue.hasAttr(funcName, forwardState.getAllocatePointTable())) {
                IForwardAbstractValue methodContainedValue = objectValue.getAttr(funcName, apTable);
                for (ObjectValue method:
                        ((ForwardAbstractValue) methodContainedValue).getAllocatePoints()
                                .getObjectsIterable(apTable)) {
                    if (method instanceof MethodSummaryObjectValue) {
                        ArrayList<IForwardAbstractValue> arg = new ArrayList<IForwardAbstractValue>(){{
                            add(new ForwardAbstractValue(method));
                            add(other);
                        }};

                        // Call special method.
                        IForwardAbstractValue retVal =
                                ((MethodSummaryObjectValue) method).call(forwardState, arg, inst);
                        if (retVal != null)
                            result.addAll(((ForwardAbstractValue) retVal).getAllocatePoints());
                    } else {
                        // Call special method.
                        IForwardAbstractValue retVal = ForwardCallManager.callSpecialFunction(
                                inst, objectValue, forwardState, funcName
                        );
                        result.union(((ForwardAbstractValue) retVal).getAllocatePoints());
                    }
                }
            }
        }
        return result;
    }

    public AllocatePoints add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__add__", inst, forwardState);
    }

    public AllocatePoints add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__add__", inst, forwardState);
    }

    public AllocatePoints add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__add__", inst, forwardState);
    }

    public AllocatePoints add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__add__", inst, forwardState);
    }

    public AllocatePoints add(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__add__", inst, forwardState);
    }

    public AllocatePoints sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__sub__", inst, forwardState);
    }

    public AllocatePoints sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__sub__", inst, forwardState);
    }

    public AllocatePoints sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__sub__", inst, forwardState);
    }

    public AllocatePoints sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__sub__", inst, forwardState);
    }

    public AllocatePoints sub(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__sub__", inst, forwardState);
    }

    public AllocatePoints mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__mul__", inst, forwardState);
    }

    public AllocatePoints mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__mul__", inst, forwardState);
    }

    public AllocatePoints mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__mul__", inst, forwardState);
    }

    public AllocatePoints mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__mul__", inst, forwardState);
    }

    public AllocatePoints mult(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__mul__", inst, forwardState);
    }

    public AllocatePoints div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__div__", inst, forwardState);
    }

    public AllocatePoints div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__div__", inst, forwardState);
    }

    public AllocatePoints div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__div__", inst, forwardState);
    }

    public AllocatePoints div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__div__", inst, forwardState);
    }

    public AllocatePoints div(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__div__", inst, forwardState);
    }

    public AllocatePoints fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__fdiv__", inst, forwardState);
    }

    public AllocatePoints fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__fdiv__", inst, forwardState);
    }

    public AllocatePoints fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__fdiv__", inst, forwardState);
    }

    public AllocatePoints fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__fdiv__", inst, forwardState);
    }

    public AllocatePoints fdiv(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__fdiv__", inst, forwardState);
    }

    public AllocatePoints mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__mod__", inst, forwardState);
    }

    public AllocatePoints mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__mod__", inst, forwardState);
    }

    public AllocatePoints mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__mod__", inst, forwardState);
    }

    public AllocatePoints mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__mod__", inst, forwardState);
    }

    public AllocatePoints mod(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__mod__", inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public AllocatePoints bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__bitor__", inst, forwardState);
    }

    public AllocatePoints bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__bitor__", inst, forwardState);
    }

    public AllocatePoints bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__bitor__", inst, forwardState);
    }

    public AllocatePoints bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__bitor__", inst, forwardState);
    }

    public AllocatePoints bitor(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__bitor__", inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public AllocatePoints bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__bitand__", inst, forwardState);
    }

    public AllocatePoints bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__bitand__", inst, forwardState);
    }

    public AllocatePoints bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__bitand__", inst, forwardState);
    }

    public AllocatePoints bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__bitand__", inst, forwardState);
    }

    public AllocatePoints bitand(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__bitand__", inst, forwardState);
    }

    /***************      Bitxor calculations      ****************/
    public AllocatePoints bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public AllocatePoints bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public AllocatePoints bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherBool), "__bitxor__", inst, forwardState);
    }

    public AllocatePoints bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherInt), "__bitxor__", inst, forwardState);
    }

    public AllocatePoints bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherFloat), "__bitxor__", inst, forwardState);
    }

    public AllocatePoints bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherString), "__bitxor__", inst, forwardState);
    }

    public AllocatePoints bitxor(AllocatePoints otherAllocatePoints, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return commonBinOperation(new ForwardAbstractValue(otherAllocatePoints), "__bitxor__", inst, forwardState);
    }

    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom())
            return new BoolValue();
        else
            // `not <OBJECT>` returns `False`.
            return new BoolValue(false);
    }

    public AllocatePoints minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new AllocatePoints();
    }



    @Override
    public boolean isBottom() {
        return size() == 0;
    }

    public AllocatePoint get(int i) {
        return Iterator2List.toList(iterator()).get(i);
    }

    public AllocatePoints copy() {
        AllocatePoints newAllocatePoints = new AllocatePoints();
        newAllocatePoints.addAll(this);
        return newAllocatePoints;
    }

    @Override
    public boolean isSame(AllocatePoints other) {
        AllocatePoints interSection = copy();
        interSection.retainAll(other);
        return interSection.size() == size();
    }
}
