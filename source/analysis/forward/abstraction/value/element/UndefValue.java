package analysis.forward.abstraction.value.element;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.lattice.BinaryLattice;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.ExceptionManager;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;

import java.util.Collections;
import java.util.Set;

public class UndefValue extends BinaryLattice<UndefValue> implements IForwardAbstractValueElement<UndefValue> {
    public UndefValue() {
        super();
    }

    public UndefValue(LatticeTop top) {
        super(top);
    }

    public UndefValue(LatticeBottom button) {
        super(button);
    }

    public UndefValue(boolean element) {
        super(element);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        return Collections.emptySet();
    }

    private BoolValue illegalCompOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalCompareException(this, other);
        return new BoolValue();
    }

    /***************      Equal compare     ****************/
    public BoolValue eq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue eq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue eq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue eq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue eq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }

    /***************      Not equal compare      ****************/
    public BoolValue neq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue neq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue neq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue neq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue neq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }

    /***************      Less than compare      ****************/
    public BoolValue lt(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue lt(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherNone, inst, forwardState);
    }

    public BoolValue lt(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue lt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
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
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue lte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue lte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
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
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue gt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue gt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
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
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue gte(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue gte(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
    }

    public BoolValue gte(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherString, inst, forwardState);
    }

    public BoolValue gte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }



    private UndefValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new UndefValue();
    }

    /***************      Add calculations      ****************/
    public UndefValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public UndefValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public UndefValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public UndefValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public UndefValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public UndefValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public UndefValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public UndefValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitxor calculations      ****************/
    public UndefValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public UndefValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public UndefValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public UndefValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public UndefValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public UndefValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public UndefValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new BoolValue();
    }

    public UndefValue minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new UndefValue();
    }



    @Override
    public UndefValue copy() {
         return new UndefValue(getElement());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UndefValue)) return false;

        return getElement() == ((UndefValue) obj).getElement();
    }
}
