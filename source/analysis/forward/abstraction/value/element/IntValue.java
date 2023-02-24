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

public class IntValue extends ThreeHeightLattice<Integer, IntValue> implements IForwardAbstractValueElement<IntValue> {
    public IntValue() {
        super();
    }

    public IntValue(ILatticeElement latticeElement) {
        super(latticeElement);
    }

    public IntValue(Integer value) {
        super(value);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        Set<TypeReference> result = HashSetFactory.make(1);
        result.add(TypeReference.Int);
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
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherInt.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherInt.getConcreteValue()) == 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().eq(otherFloat, inst, forwardState);
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
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherInt.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherInt.getConcreteValue()) != 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().neq(otherFloat, inst, forwardState);
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || getConcreteValue() != null) {
            return new BoolValue(true);
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
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherInt.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() < otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().lt(otherFloat, inst, forwardState);
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
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherInt.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue() <= otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue lte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().lte(otherFloat, inst, forwardState);
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
        return otherInt.lt(this, inst, forwardState);
    }

    public BoolValue gt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().gt(otherFloat, inst, forwardState);
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
        return otherInt.lte(this, inst, forwardState);
    }

    public BoolValue gte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().gte(otherFloat, inst, forwardState);
    }

    public BoolValue gte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue gte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }



    private IntValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new IntValue();
    }

    /***************      Add calculations      ****************/
    public IntValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return add(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() + otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().add(otherFloat, inst, forwardState);
    }

    public IntValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public IntValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return sub(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() - otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().sub(otherFloat, inst, forwardState);
    }

    public IntValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public IntValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mult(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() * otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().mult(otherFloat, inst, forwardState);
    }

    public StringValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherString.mult(this, inst, forwardState);
    }

    public AllocatePoints mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return otherObjects.mult(this, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public IntValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public FloatValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return div(otherBool.toIntValue(), inst, forwardState);
    }

    public FloatValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new FloatValue();
        } else if (isTop() || otherInt.isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new FloatValue(getConcreteValue().floatValue() / otherInt.getConcreteValue().floatValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().div(otherFloat, inst, forwardState);
    }

    public IntValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public IntValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return fdiv(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(Math.floorDiv(getConcreteValue(), otherInt.getConcreteValue()));
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().fdiv(otherFloat, inst, forwardState);
    }

    public IntValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public IntValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mod(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() % otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public FloatValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return toFloatValue().mod(otherFloat, inst, forwardState);
    }

    public IntValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public IntValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return bitor(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() | otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public IntValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public IntValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public IntValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return bitand(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() & otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public IntValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public IntValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************       Bitxor calculations      ****************/
    public IntValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public IntValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public IntValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return bitxor(otherBool.toIntValue(), inst, forwardState);
    }

    public IntValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new IntValue();
        } else if (isTop() || otherInt.isTop()) {
            return new IntValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            return new IntValue(getConcreteValue() ^ otherInt.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public IntValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public IntValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public IntValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        return toBoolValue().not(inst, forwardState);
    }



    public BoolValue toBoolValue() {
        if (isBottom()) {
            return new BoolValue();
        } else if (isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if (getConcreteValue() != null) {
            if (getConcreteValue() == 0) {
                return new BoolValue(false);
            } else {
                return new BoolValue(true);
            }
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public IntValue minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (isTop() || isBottom()) {
            return new IntValue(element);
        } else {
            return new IntValue(getConcreteValue() * -1);
        }
    }

    public FloatValue toFloatValue() {
        if (isBottom()) {
            return new FloatValue();
        } else if (isTop()) {
            return new FloatValue(LatticeTop.TOP);
        } else if (getConcreteValue() != null) {
            return new FloatValue(getConcreteValue().floatValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    @Override
    public IntValue copy() {
         if (isTop()) {
             return new IntValue(LatticeTop.TOP);
         } else if (isBottom()) {
             return new IntValue(LatticeBottom.BOTTOM);
         } else {
            return new IntValue(getConcreteValue());
         }
    }

    @Override
    public boolean isSame(IntValue other) {
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
