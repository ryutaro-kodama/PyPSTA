package analysis.backward.thresher.term;

import analysis.backward.thresher.factor.IProgramFactor;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class ConstantTypeTerm implements ITerm {
//    public static final ConstantTypeTerm NULL = new ConstantTypeTerm();
//    public static final ConstantTypeTerm NON_NULL = new ConstantTypeTerm();

    private final TypeReference constant;

    private boolean substituted = false;

    private final String repr;

//    private ConstantTypeTerm() {
//        this.constant = null;
//        this.repr = "";
//    }

    public ConstantTypeTerm(TypeReference constant) {
        if (constant == null)
            this.constant = TypeReference.find(PythonTypes.pythonLoader, "LNone");
        else
            this.constant = constant;
        this.repr = this.toString();
    }

    @Override
    public boolean hasBool() {
        return false;
    }

    @Override
    public boolean hasInt() {
        return false;
    }

    @Override
    public boolean hasFloat() {
        return false;
    }

    @Override
    public boolean hasString() {
        return false;
    }

    private BoolExpr boolExpr;
    @Override
    public BoolExpr getBoolExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    private IntExpr intExpr;
    @Override
    public IntExpr getIntExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    private FPExpr floatExpr;
    @Override
    public FPExpr getFloatExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    private SeqExpr<CharSort> stringExpr;
    @Override
    public SeqExpr<CharSort> getStringExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    private Expr<SeqSort<CharSort>> typeExpr;
    private int usedId = 0;
    @Override
    public Expr<SeqSort<CharSort>> getTypeExpr(Context ctx) {
        if (typeExpr == null) {
            if (constant.equals(TypeReference.Boolean)) {
                return ctx.mkConst(ITerm.BOOL, ctx.getStringSort());
            } else if (constant.equals(TypeReference.Int)) {
                return ctx.mkConst(ITerm.INT, ctx.getStringSort());
            } else if (constant.equals(TypeReference.Float)) {
                return ctx.mkConst(ITerm.FLOAT, ctx.getStringSort());
            } else if (constant.equals(PythonTypes.string)) {
                return ctx.mkConst(ITerm.STR, ctx.getStringSort());
//            } else if (type.equals(PypstaSymbolicExecutor.top)) {
//                return ctx.mkBool(true);
            } else {
                return ctx.mkConst(
                        constant.getName().toString() + "-" + usedId++, ctx.getStringSort());
            }
        }
        return typeExpr;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public TypeReference getConstant() {
        return constant;
    }

    @Override
    public Collection<? extends IProgramFactor> getVars() {
        return Collections.EMPTY_SET;
    }

    @Override
    public void setSubstituted(boolean b) {
        this.substituted = b;
    }

    @Override
    public boolean isSubstituted() {
        return substituted;
    }

    @Override
    public ITerm substitute(ITerm toTerm, VariableTerm fromTerm) {
        return this;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof BinOpTerm)
            return 1;
        return this.repr.compareTo(((ConstantTypeTerm) o).repr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantTypeTerm that = (ConstantTypeTerm) o;
        return Objects.equals(constant, that.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constant);
    }

    @Override
    public String toString() {
        return constant.toString();
    }
}
