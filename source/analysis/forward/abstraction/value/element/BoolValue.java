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

public class BoolValue extends ThreeHeightLattice<Boolean, BoolValue> implements IForwardAbstractValueElement<BoolValue> {
    public BoolValue() {
        super();
    }

    public BoolValue(ILatticeElement latticeElement) {
        super(latticeElement);
    }

    public BoolValue(Boolean value) {
        super(value);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        Set<TypeReference> result = HashSetFactory.make(1);
        result.add(TypeReference.Boolean);
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
        if (isBottom() || otherBool.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherBool.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherBool.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() == otherBool.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue eq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherInt.eq(this, inst, forwardState);
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherFloat.eq(this, inst, forwardState);
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherString.eq(this, inst, forwardState);
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
        if (isBottom() || otherBool.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherBool.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherBool.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() != otherBool.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue neq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherInt.neq(this, inst, forwardState);
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherFloat.neq(this, inst, forwardState);
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherString.neq(this, inst, forwardState);
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
        return toIntValue().lt(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue lt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherInt, inst, forwardState);
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherFloat, inst, forwardState);
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
        return toIntValue().lte(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue lte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherInt, inst, forwardState);
    }

    public BoolValue lte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherFloat, inst, forwardState);
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
        return toIntValue().gt(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue gt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherInt, inst, forwardState);
    }

    public BoolValue gt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherFloat, inst, forwardState);
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
        return toIntValue().gte(otherBool.toIntValue(), inst, forwardState);
    }

    public BoolValue gte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherInt, inst, forwardState);
    }

    public BoolValue gte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().gte(otherFloat, inst, forwardState);
    }

    public BoolValue gte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue gte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }



    private BoolValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new BoolValue();
    }

    /***************      Add calculations      ****************/
    public BoolValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().add(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().add(otherInt, inst, forwardState);
    }

    public FloatValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().add(otherFloat, inst, forwardState);
    }

    public BoolValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public BoolValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().sub(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().sub(otherInt, inst, forwardState);
    }

    public FloatValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().sub(otherFloat, inst, forwardState);
    }

    public BoolValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public BoolValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mult(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mult(otherInt, inst, forwardState);
    }

    public FloatValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mult(otherFloat, inst, forwardState);
    }

    public StringValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mult(otherString, inst, forwardState);
    }

    public AllocatePoints mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mult(otherObjects, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public BoolValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().div(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().div(otherInt, inst, forwardState);
    }

    public FloatValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().div(otherFloat, inst, forwardState);
    }

    public BoolValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public BoolValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().fdiv(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().fdiv(otherInt, inst, forwardState);
    }

    public FloatValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().fdiv(otherFloat, inst, forwardState);
    }

    public BoolValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public BoolValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mod(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mod(otherInt, inst, forwardState);
    }

    public FloatValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().mod(otherFloat, inst, forwardState);
    }

    public BoolValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public BoolValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitor(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitor(otherInt, inst, forwardState);
    }

    public IntValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitor(otherFloat, inst, forwardState);
    }

    public BoolValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public BoolValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitand(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitand(otherInt, inst, forwardState);
    }

    public IntValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitand(otherFloat, inst, forwardState);
    }

    public BoolValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitxor calculations      ****************/
    public BoolValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public BoolValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitxor(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitxor(otherInt, inst, forwardState);
    }

    public IntValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().bitxor(otherFloat, inst, forwardState);
    }

    public BoolValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public BoolValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom()) {
            return new BoolValue();
        } else if (isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if (getConcreteValue() != null) {
            return new BoolValue(!getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public IntValue minus(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        return toIntValue().minus(inst, forwardState);
    }



    public IntValue toIntValue() {
        if (isBottom()) {
            return new IntValue();
        } else if (isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if (getConcreteValue() == true) {
            return new IntValue(1);
        } else if (getConcreteValue() == false) {
            return new IntValue(0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    @Override
    public BoolValue copy() {
        if (isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if (isBottom()) {
            return new BoolValue(LatticeBottom.BOTTOM);
        } else {
            return new BoolValue(getConcreteValue());
        }
    }

    @Override
    public boolean isSame(BoolValue other) {
        if (isTop()) {
            return other.isTop();
        } else if (isBottom()) {
            return other.isBottom();
        } else {
            if (other.isTop() || other.isBottom()) {
                return false;
            } else {
                return getConcreteValue() == other.getConcreteValue();
            }
        }
    }
}
