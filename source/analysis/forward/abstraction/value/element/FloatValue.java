package analysis.forward.abstraction.value.element;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.lattice.ThreeHeightLattice;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.ExceptionManager;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

import java.util.Set;

public class FloatValue extends ThreeHeightLattice<Float, FloatValue> implements IForwardAbstractValueElement<FloatValue> {
    public FloatValue() {
        super();
    }

    public FloatValue(ILatticeElement latticeElement) {
        super(latticeElement);
    }

    public FloatValue(Float value) {
        super(value);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        Set<TypeReference> result = HashSetFactory.make(1);
        result.add(TypeReference.Float);
        return result;
    }

    private BoolValue illegalCompOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalCompareException(this, other);
        return new BoolValue();
    }

    /***************      Equal compare      ****************/
    public BoolValue eq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue eq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherNone.eq(this, inst, forwardState);
    }

    public BoolValue eq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return eq(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue eq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return eq(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new BoolValue(Float.compare(getConcreteValue(), otherFloat.getConcreteValue()) == 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || getConcreteValue() != null) {
            return new BoolValue(false);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue eq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherObjects.eq(this, inst, forwardState);
    }

    /***************      Not equal compare      ****************/
    public BoolValue neq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue neq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherNone.neq(this, inst, forwardState);
    }

    public BoolValue neq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return neq(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue neq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return neq(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new BoolValue(Float.compare(getConcreteValue(), otherFloat.getConcreteValue()) != 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || getConcreteValue() != null) {
            return new BoolValue(false);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue neq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherObjects.neq(this, inst, forwardState);
    }

    /***************      Less than compare      ****************/
    public BoolValue lt(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue lt(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue lt(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return lt(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue lt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return lt(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() < otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue lt(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue lt(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }

    /***************      Less than equal compare      ****************/
    public BoolValue lte(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue lte(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue lte(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return lte(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue lte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return lte(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue lte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() <= otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue lte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue lte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }

    /***************      Greater than compare      ****************/
    public BoolValue gt(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue gt(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue gt(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return gt(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue gt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return gt(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue gt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherFloat.lt(this, inst, forwardState);
    }

    public BoolValue gt(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue gt(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }

    /***************      Greater than equal compare      ****************/
    public BoolValue gte(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue gte(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue gte(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return gte(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue gte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return gte(otherInt.toFloatValue(), inst, forwardState);
    }

    public BoolValue gte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherFloat.lte(this, inst, forwardState);
    }

    public BoolValue gte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue gte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }



    private FloatValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new FloatValue();
    }

    /***************      Add calculations      ****************/
    public FloatValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return add(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return add(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue() + otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public FloatValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return sub(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return sub(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue() - otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public FloatValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mult(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mult(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue() * otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public FloatValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return div(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return div(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue() / otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public FloatValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return fdiv(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return fdiv(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            // TODO:
            return new FloatValue((float) Math.floor(getConcreteValue() / otherFloat.getConcreteValue()));
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public FloatValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mod(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mod(otherInt.toFloatValue(), inst, forwardState);
    }

    public FloatValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherFloat.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherFloat.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue() % otherFloat.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public FloatValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public FloatValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public FloatValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public FloatValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public FloatValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public FloatValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public FloatValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public FloatValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }
    /***************      Bitxor calculations      ****************/
    public FloatValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public FloatValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public FloatValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public FloatValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public FloatValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public FloatValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        return toBoolValue().not(inst, forwardState);
    }

    public FloatValue minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (isTop() || isBottom()) {
            return new FloatValue(element);
        } else {
            return new FloatValue(getConcreteValue() * -1);
        }
    }



    public BoolValue toBoolValue() {
        if (isBottom()) {
            return new BoolValue();
        } else if (isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if (getConcreteValue() != null) {
            if (Float.compare(getConcreteValue(), 0.0f) == 0) {
                return new BoolValue(false);
            } else {
                return new BoolValue(true);
            }
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    @Override
    public FloatValue copy() {
        if (isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if (isBottom()) {
            return new FloatValue(LatticeBottom.BOTTOM);
        } else {
            return new FloatValue(getConcreteValue());
        }
    }

    @Override
    public boolean isSame(FloatValue other) {
        if (isTop()) {
            return other.isTop();
        } else if (isBottom()) {
            return other.isBottom();
        } else {
            if (other.isTop() || other.isBottom()) {
                return false;
            } else {
                return getConcreteValue().compareTo(other.getConcreteValue()) == 0;
            }
        }
    }
}
