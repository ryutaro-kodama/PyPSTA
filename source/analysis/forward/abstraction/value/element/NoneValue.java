package analysis.forward.abstraction.value.element;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.lattice.BinaryLattice;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.ExceptionManager;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

import java.util.Set;

public class NoneValue extends BinaryLattice<NoneValue> implements IForwardAbstractValueElement<NoneValue> {
    public NoneValue() {
        super();
    }

    public NoneValue(LatticeTop top) {
        super(top);
    }

    public NoneValue(LatticeBottom button) {
        super(button);
    }

    public NoneValue(boolean element) {
        super(element);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        Set<TypeReference> result = HashSetFactory.make(1);
        result.add(TypeReference.find(PythonTypes.pythonLoader, "LNone"));
        return result;
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
        if (isBottom() || otherNone.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
    }

    public BoolValue eq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherBool.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    public BoolValue eq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    public BoolValue eq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    public BoolValue eq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherObjects.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    /***************      Not equal compare      ****************/
    public BoolValue neq(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherUndef, inst, forwardState);
    }

    public BoolValue neq(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherNone.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(false);
        }
    }

    public BoolValue neq(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherBool.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
    }

    public BoolValue neq(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
    }

    public BoolValue neq(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherFloat.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
    }

    public BoolValue neq(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherObjects.isBottom()) {
            return new BoolValue();
        } else {
            return new BoolValue(true);
        }
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



    private NoneValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new NoneValue();
    }

    /***************      Add calculations      ****************/
    public NoneValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public NoneValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public NoneValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public NoneValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public NoneValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public NoneValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitor calculations      ****************/
    public NoneValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public NoneValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitxor calculations      ****************/
    public NoneValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public NoneValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public NoneValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public NoneValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public NoneValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public NoneValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public NoneValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new BoolValue();
    }

    public NoneValue minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new NoneValue();
    }



    @Override
    public NoneValue copy() {
        return new NoneValue(getElement());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NoneValue)) return false;

        return getElement() == ((NoneValue) obj).getElement();
    }
}
