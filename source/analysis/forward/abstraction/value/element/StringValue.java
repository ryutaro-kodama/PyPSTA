package analysis.forward.abstraction.value.element;

import analysis.forward.BuiltinFunctionSummaries;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.lattice.ThreeHeightLattice;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.ExceptionManager;
import analysis.forward.abstraction.value.object.FunctionObjectValue;
import analysis.forward.abstraction.value.object.ListObjectValue;
import analysis.forward.abstraction.value.object.TupleObjectValue;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class StringValue extends ThreeHeightLattice<String, StringValue> implements IForwardAbstractValueElement<StringValue> {
    public StringValue() {
        super();
    }

    public StringValue(ILatticeElement latticeElement) {
        super(latticeElement);
    }

    public StringValue(String value) {
        super(value);
    }

    @Override
    public Set<TypeReference> getTypes(ForwardState state) {
        Set<TypeReference> result = HashSetFactory.make(1);
        result.add(PythonTypes.string);
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
        } else if (isTop() || getConcreteValue() != null) {
            return new BoolValue(false);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue eq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().equals(otherString.getConcreteValue()));
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
        } else if (isTop() || getConcreteValue() != null) {
            return new BoolValue(false);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue neq(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(!getConcreteValue().equals(otherString.getConcreteValue()));
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
        return illegalCompOperation(otherBool, inst, forwardState);
    }

    public BoolValue lt(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherInt, inst, forwardState);
    }

    public BoolValue lt(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherFloat, inst, forwardState);
    }

    public BoolValue lt(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherString.getConcreteValue()) < 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
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
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherString.getConcreteValue()) <= 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
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
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherString.getConcreteValue()) > 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
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
        if (isBottom() || otherString.isBottom()) {
            return new BoolValue();
        } else if (isTop() || otherString.isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new BoolValue(getConcreteValue().compareTo(otherString.getConcreteValue()) >= 0);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public BoolValue gte(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalCompOperation(otherObjects, inst, forwardState);
    }



    private StringValue illegalBinOperation(IForwardAbstractValueElement other, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (!isBottom() && !other.isBottom())
            ExceptionManager.illegalBinOpeException(this, other, inst);
        return new StringValue();
    }

    /***************      Add calculations      ****************/
    public StringValue add(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue add(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue add(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue add(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue add(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue add(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherString.isBottom()) {
            return new StringValue();
        } else if (isTop() || otherString.isTop()) {
            return new StringValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherString.getConcreteValue() != null)) {
            return new StringValue(getConcreteValue() + otherString.getConcreteValue());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public StringValue add(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Sub calculations      ****************/
    public StringValue sub(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue sub(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue sub(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue sub(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue sub(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue sub(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue sub(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mult calculations      ****************/
    public StringValue mult(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue mult(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue mult(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return mult(otherBool.toIntValue(), inst, forwardState);
    }

    public StringValue mult(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        if (isBottom() || otherInt.isBottom()) {
            return new StringValue();
        } else if (isTop() || otherInt.isTop()) {
            return new StringValue(LatticeTop.TOP);
        } else if ((getConcreteValue() != null) && (otherInt.getConcreteValue() != null)) {
            StringBuilder s = new StringBuilder();
            String str = getConcreteValue();
            for (int i = 1; i <= otherInt.getConcreteValue(); i++)
                s.append(str);
            return new StringValue(s.toString());
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public StringValue mult(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue mult(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue mult(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Div calculations      ****************/
    public StringValue div(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue div(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue div(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue div(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue div(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue div(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue div(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Floor div calculations      ****************/
    public StringValue fdiv(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue fdiv(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue fdiv(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue fdiv(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue fdiv(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue fdiv(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue fdiv(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Mod calculations      ****************/
    public StringValue mod(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue mod(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    public StringValue mod(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    public StringValue mod(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    public StringValue mod(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    public StringValue mod(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    public StringValue mod(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return new StringValue(LatticeTop.TOP);
    }

    /***************      Bitor calculations      ****************/
    public StringValue bitor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue bitor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue bitor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue bitor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue bitor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue bitor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue bitor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitand calculations      ****************/
    public StringValue bitand(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue bitand(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue bitand(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue bitand(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue bitand(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue bitand(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue bitand(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Bitxor calculations      ****************/
    public StringValue bitxor(UndefValue otherUndef, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherUndef, inst, forwardState);
    }

    public StringValue bitxor(NoneValue otherNone, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherNone, inst, forwardState);
    }

    public StringValue bitxor(BoolValue otherBool, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherBool, inst, forwardState);
    }

    public StringValue bitxor(IntValue otherInt, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherInt, inst, forwardState);
    }

    public StringValue bitxor(FloatValue otherFloat, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherFloat, inst, forwardState);
    }

    public StringValue bitxor(StringValue otherString, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherString, inst, forwardState);
    }

    public StringValue bitxor(AllocatePoints otherObjects, SSABinaryOpInstruction inst, ForwardState forwardState) {
        return illegalBinOperation(otherObjects, inst, forwardState);
    }

    /***************      Negate calculations     ****************/
    public BoolValue not(SSAUnaryOpInstruction inst, ForwardState forwardState) {
        return toBoolValue().not(inst, forwardState);
    }

    public StringValue minus(SSAUnaryOpInstruction instUnary, ForwardState forwardState) {
        if (!isBottom())
            ExceptionManager.illegalUnaryOpeException(this);
        return new StringValue();
    }



    public BoolValue toBoolValue() {
        if (isBottom()) {
            return new BoolValue();
        } else if (isTop()) {
            return new BoolValue(LatticeTop.TOP);
        } else if (getConcreteValue() != null) {
            return new BoolValue(true);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    @Override
    public StringValue copy() {
        if (isTop()) {
            return new StringValue(LatticeTop.TOP);
        } else if (isBottom()) {
            return new StringValue(LatticeBottom.BOTTOM);
        } else {
            return new StringValue(getConcreteValue());
        }
    }

    @Override
    public boolean isSame(StringValue other) {
        if (isTop()) {
            return other.isTop();
        } else if (isBottom()) {
            return other.isBottom();
        } else {
            if (other.isTop() || other.isBottom()) {
                return false;
            } else {
                return getConcreteValue().equals(other.getConcreteValue());
            }
        }
    }

    /** The mapping from function name to object value. */
    public static final HashMap<String, BuiltinFunctionSummaries.Summary> methods = new HashMap<>();

    static {
        String decode = "decode";
        methods.put(decode, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String encode = "encode";
        methods.put(encode, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String format = "format";
        methods.put(format, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String join = "join";
        methods.put(join, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String lower = "lower";
        methods.put(lower, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String partition = "partition";
        methods.put(partition, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                TupleObjectValue tuple = new TupleObjectValue(new AllocatePoint(state.getCGNode(), inst));
                state.getAllocatePointTable().newAllocation(tuple);
                tuple.setElement(0, new ForwardAbstractValue(new StringValue(LatticeTop.TOP)));
                tuple.setElement(1, new ForwardAbstractValue(new StringValue(LatticeTop.TOP)));
                tuple.setElement(2, new ForwardAbstractValue(new StringValue(LatticeTop.TOP)));
                return new ForwardAbstractValue(tuple);
            }
        });

        String replace = "replace";
        methods.put(replace, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String rstrip = "rstrip";
        methods.put(rstrip, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });

        String startswith = "startswith";
        methods.put(startswith, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new BoolValue(LatticeTop.TOP));
            }
        });

        String split = "split";
        methods.put(split, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ListObjectValue list = new ListObjectValue(
                        new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(list);
                list.getIntTopAccessedValue().getStringValue().union(new StringValue(LatticeTop.TOP));
                return new ForwardAbstractValue(list);
            }
        });

        String splitlines = "splitlines";
        methods.put(splitlines, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ListObjectValue list = new ListObjectValue(
                        new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(list);
                list.getIntTopAccessedValue().getStringValue().union(new StringValue(LatticeTop.TOP));
                return new ForwardAbstractValue(list);
            }
        });

        String strip = "strip";
        methods.put(strip, new BuiltinFunctionSummaries.Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
            }
        });
    }

    /**
     * Get object value of string method function.
     * @param keyAbstractValue the abstract value which has the function's name.
     * @param state the state of reading instruction
     * @param inst the reading instruction
     * @return the object value of string method function
     */
    public static ForwardAbstractValue getMethod(IForwardAbstractValue keyAbstractValue, ForwardState state, SSAInstruction inst) {
        StringValue keyValue = ((ForwardAbstractValue) keyAbstractValue).getStringValue();
        if (keyValue.isBottom() || keyValue.isTop()) {
            // TODO:
            Assertions.UNREACHABLE();
        }

        String methodName = keyValue.getConcreteValue();
        FunctionObjectValue result = new FunctionObjectValue(
                new AllocatePoint(state.getCGNode(), inst),
                TypeReference.findOrCreate(
                        PythonTypes.pythonLoader,
                        PythonTypes.string.getName().toString() + "/" + methodName
                )
        );

        state.getAllocatePointTable().newAllocation(result);
        return new ForwardAbstractValue(result);
    }
}
