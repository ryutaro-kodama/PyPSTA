package analysis.forward.abstraction.value;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.element.*;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;

public class ForwardAbstractValue implements IForwardAbstractValue<ForwardAbstractValue> {
    private UndefValue undefValue;
    private NoneValue noneValue;
    private BoolValue boolValue;
    private IntValue intValue;
    private FloatValue floatValue;
    private StringValue stringValue;
    private AllocatePoints allocatePoints;

    public ForwardAbstractValue(
            UndefValue undefValue, NoneValue noneValue, BoolValue boolValue, IntValue intValue,
            FloatValue floatValue, StringValue stringValue, AllocatePoints allocatePoints
    ) {
        this.undefValue = undefValue;
        this.noneValue = noneValue;
        this.boolValue = boolValue;
        this.intValue = intValue;
        this.floatValue = floatValue;
        this.stringValue = stringValue;
        this.allocatePoints = allocatePoints;
    }

    public ForwardAbstractValue(UndefValue undefValue) {
        this(
                undefValue, new NoneValue(), new BoolValue(), new IntValue(),
                new FloatValue(), new StringValue(), new AllocatePoints()
        );
    }

    public ForwardAbstractValue(NoneValue noneValue) {
        this(
                new UndefValue(), noneValue, new BoolValue(), new IntValue(),
                new FloatValue(), new StringValue(), new AllocatePoints()
        );
    }

    public ForwardAbstractValue(BoolValue boolValue) {
        this(
                new UndefValue(), new NoneValue(), boolValue, new IntValue(),
                new FloatValue(), new StringValue(), new AllocatePoints()
        );
    }

    public ForwardAbstractValue(Boolean value) {
        this(new BoolValue(value));
    }

    public ForwardAbstractValue(IntValue intValue) {
        this(
                new UndefValue(), new NoneValue(), new BoolValue(), intValue,
                new FloatValue(), new StringValue(), new AllocatePoints()
        );
    }

    public ForwardAbstractValue(Integer value) {
        this(new IntValue(value));
    }

    public ForwardAbstractValue(FloatValue floatValue) {
        this(
                new UndefValue(), new NoneValue(), new BoolValue(), new IntValue(),
                floatValue, new StringValue(), new AllocatePoints()
        );
    }

    public ForwardAbstractValue(Float value) {
        this(new FloatValue(value));
    }

    public ForwardAbstractValue(StringValue stringValue) {
        this(
                new UndefValue(), new NoneValue(), new BoolValue(), new IntValue(),
                new FloatValue(), stringValue, new AllocatePoints()
        );
    }

    public ForwardAbstractValue(String value) {
        this(new StringValue(value));
    }

    public ForwardAbstractValue(AllocatePoints values) {
        this(
                new UndefValue(), new NoneValue(), new BoolValue(), new IntValue(),
                new FloatValue(), new StringValue(), values
        );
    }

    public ForwardAbstractValue(ObjectValue value) {
        this(new AllocatePoints(){
            {add(value.getAllocatePoint());}
        });
    }

    public ForwardAbstractValue() {
        this(
                new UndefValue(), new NoneValue(), new BoolValue(), new IntValue(),
                new FloatValue(), new StringValue(), new AllocatePoints()
        );
    }


    @Override
    public boolean union(ForwardAbstractValue other) {
        return undefValue.union(other.getUndefValue())
                | noneValue.union(other.getNoneValue())
                | boolValue.union(other.getBoolValue())
                | intValue.union(other.getIntValue())
                | floatValue.union(other.getFloatValue())
                | stringValue.union(other.getStringValue())
                | allocatePoints.union(other.getAllocatePoints());
    }

    public UndefValue getUndefValue() {
        return undefValue;
    }

    public void setUndefValue(UndefValue undefValue) {
        this.undefValue = undefValue;
    }

    public NoneValue getNoneValue() {
        return noneValue;
    }

    public void setNoneValue(NoneValue nullValue) {
        this.noneValue = nullValue;
    }

    public BoolValue getBoolValue() {
        return boolValue;
    }

    public void setBoolValue(BoolValue boolValue) {
        this.boolValue = boolValue;
    }

    public IntValue getIntValue() {
        return intValue;
    }

    public void setIntValue(IntValue intValue) {
        this.intValue = intValue;
    }

    public FloatValue getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(FloatValue floatValue) {
        this.floatValue = floatValue;
    }

    public StringValue getStringValue() {
        return stringValue;
    }

    public void setStringValue(StringValue stringValue) {
        this.stringValue = stringValue;
    }

    public AllocatePoints getAllocatePoints() {
        return allocatePoints;
    }

    public void setAllocatePoints(AllocatePoints allocatePoints) {
        this.allocatePoints = allocatePoints;
    }

    @Override
    public ForwardAbstractValue eq(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.eq(other.getIntValue(), inst, forwardState));
            result.union(undefValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.eq(other.getStringValue(), inst, forwardState));
            result.union(undefValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.eq(other.getIntValue(), inst, forwardState));
            result.union(noneValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.eq(other.getStringValue(), inst, forwardState));
            result.union(noneValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.eq(other.getIntValue(), inst, forwardState));
            result.union(boolValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.eq(other.getStringValue(), inst, forwardState));
            result.union(boolValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(intValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(intValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(intValue.eq(other.getIntValue(), inst, forwardState));
            result.union(intValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(intValue.eq(other.getStringValue(), inst, forwardState));
            result.union(intValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.eq(other.getIntValue(), inst, forwardState));
            result.union(floatValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.eq(other.getStringValue(), inst, forwardState));
            result.union(floatValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.eq(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.eq(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.eq(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.eq(other.getIntValue(), inst, forwardState));
            result.union(stringValue.eq(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.eq(other.getStringValue(), inst, forwardState));
            result.union(stringValue.eq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.eq(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.eq(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue neq(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.neq(other.getIntValue(), inst, forwardState));
            result.union(undefValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.neq(other.getStringValue(), inst, forwardState));
            result.union(undefValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.neq(other.getIntValue(), inst, forwardState));
            result.union(noneValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.neq(other.getStringValue(), inst, forwardState));
            result.union(noneValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.neq(other.getIntValue(), inst, forwardState));
            result.union(boolValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.neq(other.getStringValue(), inst, forwardState));
            result.union(boolValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(intValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(intValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(intValue.neq(other.getIntValue(), inst, forwardState));
            result.union(intValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(intValue.neq(other.getStringValue(), inst, forwardState));
            result.union(intValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.neq(other.getIntValue(), inst, forwardState));
            result.union(floatValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.neq(other.getStringValue(), inst, forwardState));
            result.union(floatValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.neq(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.neq(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.neq(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.neq(other.getIntValue(), inst, forwardState));
            result.union(stringValue.neq(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.neq(other.getStringValue(), inst, forwardState));
            result.union(stringValue.neq(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.neq(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.neq(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue lt(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.lt(other.getIntValue(), inst, forwardState));
            result.union(undefValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.lt(other.getStringValue(), inst, forwardState));
            result.union(undefValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.lt(other.getIntValue(), inst, forwardState));
            result.union(noneValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.lt(other.getStringValue(), inst, forwardState));
            result.union(noneValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.lt(other.getIntValue(), inst, forwardState));
            result.union(boolValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.lt(other.getStringValue(), inst, forwardState));
            result.union(boolValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(intValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(intValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(intValue.lt(other.getIntValue(), inst, forwardState));
            result.union(intValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(intValue.lt(other.getStringValue(), inst, forwardState));
            result.union(intValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.lt(other.getIntValue(), inst, forwardState));
            result.union(floatValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.lt(other.getStringValue(), inst, forwardState));
            result.union(floatValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.lt(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.lt(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.lt(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.lt(other.getIntValue(), inst, forwardState));
            result.union(stringValue.lt(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.lt(other.getStringValue(), inst, forwardState));
            result.union(stringValue.lt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.lt(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.lt(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue lte(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.lte(other.getIntValue(), inst, forwardState));
            result.union(undefValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.lte(other.getStringValue(), inst, forwardState));
            result.union(undefValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.lte(other.getIntValue(), inst, forwardState));
            result.union(noneValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.lte(other.getStringValue(), inst, forwardState));
            result.union(noneValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.lte(other.getIntValue(), inst, forwardState));
            result.union(boolValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.lte(other.getStringValue(), inst, forwardState));
            result.union(boolValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(intValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(intValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(intValue.lte(other.getIntValue(), inst, forwardState));
            result.union(intValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(intValue.lte(other.getStringValue(), inst, forwardState));
            result.union(intValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.lte(other.getIntValue(), inst, forwardState));
            result.union(floatValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.lte(other.getStringValue(), inst, forwardState));
            result.union(floatValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.lte(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.lte(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.lte(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.lte(other.getIntValue(), inst, forwardState));
            result.union(stringValue.lte(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.lte(other.getStringValue(), inst, forwardState));
            result.union(stringValue.lte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.lte(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.lte(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue gt(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.gt(other.getIntValue(), inst, forwardState));
            result.union(undefValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.gt(other.getStringValue(), inst, forwardState));
            result.union(undefValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.gt(other.getIntValue(), inst, forwardState));
            result.union(noneValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.gt(other.getStringValue(), inst, forwardState));
            result.union(noneValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.gt(other.getIntValue(), inst, forwardState));
            result.union(boolValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.gt(other.getStringValue(), inst, forwardState));
            result.union(boolValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(intValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(intValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(intValue.gt(other.getIntValue(), inst, forwardState));
            result.union(intValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(intValue.gt(other.getStringValue(), inst, forwardState));
            result.union(intValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.gt(other.getIntValue(), inst, forwardState));
            result.union(floatValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.gt(other.getStringValue(), inst, forwardState));
            result.union(floatValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.gt(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.gt(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.gt(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.gt(other.getIntValue(), inst, forwardState));
            result.union(stringValue.gt(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.gt(other.getStringValue(), inst, forwardState));
            result.union(stringValue.gt(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.gt(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.gt(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue gte(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(undefValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(undefValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(undefValue.gte(other.getIntValue(), inst, forwardState));
            result.union(undefValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(undefValue.gte(other.getStringValue(), inst, forwardState));
            result.union(undefValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(noneValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(noneValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(noneValue.gte(other.getIntValue(), inst, forwardState));
            result.union(noneValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(noneValue.gte(other.getStringValue(), inst, forwardState));
            result.union(noneValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(boolValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(boolValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(boolValue.gte(other.getIntValue(), inst, forwardState));
            result.union(boolValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(boolValue.gte(other.getStringValue(), inst, forwardState));
            result.union(boolValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(intValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(intValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(intValue.gte(other.getIntValue(), inst, forwardState));
            result.union(intValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(intValue.gte(other.getStringValue(), inst, forwardState));
            result.union(intValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(floatValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(floatValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(floatValue.gte(other.getIntValue(), inst, forwardState));
            result.union(floatValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(floatValue.gte(other.getStringValue(), inst, forwardState));
            result.union(floatValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.gte(other.getUndefValue(), inst, forwardState));
            result.union(stringValue.gte(other.getNoneValue(), inst, forwardState));
            result.union(stringValue.gte(other.getBoolValue(), inst, forwardState));
            result.union(stringValue.gte(other.getIntValue(), inst, forwardState));
            result.union(stringValue.gte(other.getFloatValue(), inst, forwardState));
            result.union(stringValue.gte(other.getStringValue(), inst, forwardState));
            result.union(stringValue.gte(other.getAllocatePoints(), inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.gte(other.getUndefValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getNoneValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getBoolValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getIntValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getFloatValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getStringValue(), inst, forwardState));
            result.union(allocatePoints.gte(other.getAllocatePoints(), inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue add(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.add(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.add(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.add(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.add(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.add(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.add(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.add(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.add(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.add(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.add(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.add(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.add(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.add(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.add(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.add(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.add(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.add(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.add(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.add(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue sub(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.sub(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.sub(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.sub(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.sub(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.sub(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.sub(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.sub(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.sub(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.sub(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.sub(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.sub(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.sub(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.sub(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.sub(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.sub(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.sub(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.sub(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.sub(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.sub(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue mult(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.mult(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.mult(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.mult(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.mult(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.mult(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.mult(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.mult(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.boolValue.mult(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.boolValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.mult(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.mult(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.mult(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.mult(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.mult(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.intValue.mult(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.intValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.mult(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.mult(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.mult(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.mult(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mult(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue div(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.div(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.div(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.div(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.div(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.boolValue.div(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.boolValue.div(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.div(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.div(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.div(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.div(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.intValue.div(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.intValue.div(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.div(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.div(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.div(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.div(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.div(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.div(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.div(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue fdiv(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.fdiv(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.fdiv(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.fdiv(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.fdiv(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.fdiv(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.fdiv(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.fdiv(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.fdiv(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.fdiv(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.fdiv(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.fdiv(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.fdiv(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.fdiv(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.fdiv(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.fdiv(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.fdiv(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.fdiv(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.fdiv(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.fdiv(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue mod(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.mod(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.mod(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.mod(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.mod(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.mod(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.mod(other.getIntValue(), inst, forwardState));
            floatValue.union(this.boolValue.mod(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.mod(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.mod(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.mod(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.mod(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.mod(other.getIntValue(), inst, forwardState));
            floatValue.union(this.intValue.mod(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.mod(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.mod(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.mod(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.mod(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.mod(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.mod(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue bitor(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.bitor(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.bitor(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.bitor(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitor(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.bitor(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.bitor(other.getIntValue(), inst, forwardState));
            intValue.union(this.boolValue.bitor(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitor(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.bitor(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getIntValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.bitor(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.bitor(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.bitor(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitor(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue bitand(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.bitand(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.bitand(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.bitand(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitand(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.bitand(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.bitand(other.getIntValue(), inst, forwardState));
            intValue.union(this.boolValue.bitand(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitand(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.bitand(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getIntValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.bitand(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.bitand(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitand(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.bitand(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitand(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue bitxor(ForwardAbstractValue other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        UndefValue undefValue = new UndefValue();
        NoneValue noneValue = new NoneValue();
        BoolValue boolValue = new BoolValue();
        IntValue intValue = new IntValue();
        FloatValue floatValue = new FloatValue();
        StringValue stringValue = new StringValue();
        AllocatePoints allocatePoints = new AllocatePoints();

        if (!this.undefValue.isBottom()) {
            undefValue.union(this.undefValue.bitxor(other.getUndefValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getNoneValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getBoolValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getIntValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getFloatValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getStringValue(), inst, forwardState));
            undefValue.union(this.undefValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.noneValue.isBottom()) {
            noneValue.union(this.noneValue.bitxor(other.getUndefValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getNoneValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getBoolValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getIntValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getFloatValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getStringValue(), inst, forwardState));
            noneValue.union(this.noneValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.boolValue.isBottom()) {
            boolValue.union(this.boolValue.bitxor(other.getUndefValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitxor(other.getNoneValue(), inst, forwardState));
            intValue.union(this.boolValue.bitxor(other.getBoolValue(), inst, forwardState));
            intValue.union(this.boolValue.bitxor(other.getIntValue(), inst, forwardState));
            intValue.union(this.boolValue.bitxor(other.getFloatValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitxor(other.getStringValue(), inst, forwardState));
            boolValue.union(this.boolValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.intValue.isBottom()) {
            intValue.union(this.intValue.bitxor(other.getUndefValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getNoneValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getBoolValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getIntValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getFloatValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getStringValue(), inst, forwardState));
            intValue.union(this.intValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.floatValue.isBottom()) {
            floatValue.union(this.floatValue.bitxor(other.getUndefValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getNoneValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getBoolValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getIntValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getFloatValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getStringValue(), inst, forwardState));
            floatValue.union(this.floatValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.stringValue.isBottom()) {
            stringValue.union(this.stringValue.bitxor(other.getUndefValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getNoneValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getBoolValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getIntValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getFloatValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getStringValue(), inst, forwardState));
            stringValue.union(this.stringValue.bitxor(other.getAllocatePoints(), inst, forwardState));
        }
        if (!this.allocatePoints.isBottom()) {
            allocatePoints.union(this.allocatePoints.bitxor(other.getUndefValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getNoneValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getBoolValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getIntValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getFloatValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getStringValue(), inst, forwardState));
            allocatePoints.union(this.allocatePoints.bitxor(other.getAllocatePoints(), inst, forwardState));
        }

        return new ForwardAbstractValue(
                undefValue, noneValue, boolValue, intValue, floatValue, stringValue, allocatePoints
        );
    }

    @Override
    public ForwardAbstractValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        BoolValue result = new BoolValue();
        if (!undefValue.isBottom()) {
            result.union(undefValue.not(inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.union(noneValue.not(inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.union(boolValue.not(inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.union(intValue.not(inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.union(floatValue.not(inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.union(stringValue.not(inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.union(allocatePoints.not(inst, forwardState));
        }
        return new ForwardAbstractValue(result);
    }

    @Override
    public ForwardAbstractValue minus(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        ForwardAbstractValue result = new ForwardAbstractValue();
        if (!undefValue.isBottom()) {
            result.getUndefValue().union(undefValue.minus(inst, forwardState));
        }
        if (!noneValue.isBottom()) {
            result.getNoneValue().union(noneValue.minus(inst, forwardState));
        }
        if (!boolValue.isBottom()) {
            result.getIntValue().union(boolValue.minus(inst, forwardState));
        }
        if (!intValue.isBottom()) {
            result.getIntValue().union(intValue.minus(inst, forwardState));
        }
        if (!floatValue.isBottom()) {
            result.getFloatValue().union(floatValue.minus(inst, forwardState));
        }
        if (!stringValue.isBottom()) {
            result.getStringValue().union(stringValue.minus(inst, forwardState));
        }
        if (!allocatePoints.isBottom()) {
            result.getAllocatePoints().union(allocatePoints.minus(inst, forwardState));
        }
        return result;
    }

    @Override
    public boolean isBottom() {
        return undefValue.isBottom() && noneValue.isBottom() && boolValue.isBottom() && intValue.isBottom()
                    && floatValue.isBottom() && stringValue.isBottom() && (allocatePoints.size() == 0);
    }

    @Override
    public ForwardAbstractValue copy() {
        return new ForwardAbstractValue(
                undefValue.copy(), noneValue.copy(), boolValue.copy(),
                intValue.copy(), floatValue.copy(), stringValue.copy(), allocatePoints.copy()
        );
    }

    @Override
    public boolean isSame(ForwardAbstractValue other) {
        return undefValue.isSame(other.getUndefValue())
                && noneValue.isSame(other.getNoneValue())
                && boolValue.isSame(other.getBoolValue())
                && intValue.isSame(other.getIntValue())
                && floatValue.isSame(other.getFloatValue())
                && stringValue.isSame(other.getStringValue())
                && allocatePoints.isSame(other.getAllocatePoints());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("(").append(undefValue)
         .append(", ").append(noneValue)
         .append(", ").append(boolValue)
         .append(", ").append(intValue)
         .append(", ").append(floatValue)
         .append(", ").append(stringValue)
         .append(", {");
        if (allocatePoints.size() > 0) {
            allocatePoints.forEach(v -> s.append(v).append(", "));
            s.replace(s.length() - 2, s.length(), "");
        }
        s.append("})");
        return s.toString();
    }

}
