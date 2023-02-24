package analysis.backward.thresher.term;

import analysis.backward.thresher.factor.IProgramFactor;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;
import util.AriadneSupporter;

import java.util.*;

public class ConstantTerm implements ITerm {
    public static final ConstantTerm NULL = new ConstantTerm();
    public static final ConstantTerm NON_NULL = new ConstantTerm();

    // Constraints having this term is always evaluated as true (feasible).
    public static final ConstantTerm TRUE = new ConstantTerm();

    private final Object constant;

    private boolean substituted = false;

    private final String repr;

    private ConstantTerm() {
        this.constant = null;
        this.repr = "";
    }

    public ConstantTerm(Object constant) {
        if (constant == null)
            this.constant = TypeReference.find(PythonTypes.pythonLoader, "LNone");
        else
            this.constant = constant;
        this.repr = this.toString();
    }

    @Override
    public boolean hasBool() {
        // In python, almost all value con be interpreted as boolean.
        return constant != null;
    }

    @Override
    public boolean hasInt() {
        return constant instanceof Integer
                || constant instanceof Boolean
                || constant instanceof Long
                || constant instanceof Float;
    }

    @Override
    public boolean hasFloat() {
        return constant instanceof Float
                || constant instanceof Boolean
                || constant instanceof Integer;
    }

    @Override
    public boolean hasString() {
        return constant instanceof String;
    }

    private BoolExpr boolExpr;
    @Override
    public BoolExpr getBoolExpr(Context ctx) {
        if (boolExpr == null){
            if (constant instanceof Boolean) {
                boolExpr = ctx.mkBool((Boolean) constant);
            } else if (constant instanceof Integer) {
                if (constant.equals(0))
                    boolExpr = ctx.mkBool(false);
                else
                    boolExpr = ctx.mkBool(true);
            } else if (constant instanceof Long) {
                if (constant.equals(0l))
                    boolExpr = ctx.mkBool(false);
                else
                    boolExpr = ctx.mkBool(true);
            } else if (constant instanceof Float) {
                if (constant.equals(0.0f))
                    boolExpr = ctx.mkBool(false);
                else
                    boolExpr = ctx.mkBool(true);
            } else if (constant instanceof Double) {
                if (constant.equals(0.0d))
                    boolExpr = ctx.mkBool(false);
                else
                    boolExpr = ctx.mkBool(true);
            } else if (constant instanceof String) {
                // `bool("OK")` returns `True`
                boolExpr = ctx.mkBool(true);
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return boolExpr;
    }

    private IntExpr intExpr;
    @Override
    public IntExpr getIntExpr(Context ctx) {
        if (intExpr == null) {
            if (constant instanceof Boolean) {
                if (constant.equals(Boolean.FALSE)) {
                    intExpr = ctx.mkInt(0);
                } else {
                    intExpr = ctx.mkInt(1);
                }
            } else if (constant instanceof Integer) {
                intExpr = ctx.mkInt((Integer) constant);
            } else if (constant instanceof Long) {
                intExpr = ctx.mkInt(Math.toIntExact((Long) constant));
            } else if (constant instanceof Float) {
                intExpr = ctx.mkInt(((Float) constant).intValue());
            } else if (constant instanceof Double) {
                intExpr = ctx.mkInt(((Double) constant).intValue());
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return intExpr;
    }

    private FPExpr floatExpr;
    @Override
    public FPExpr getFloatExpr(Context ctx) {
        if (floatExpr == null) {
            FPSort double_sort = ctx.mkFPSort(11, 53);
            if (constant instanceof Boolean) {
                if (constant.equals(Boolean.FALSE)) {
                    floatExpr = ctx.mkFP(0.0f, double_sort);
                } else {
                    floatExpr = ctx.mkFP(1.0f, double_sort);
                }
            } else if (constant instanceof Integer) {
                floatExpr = ctx.mkFP(((Integer) constant).floatValue(), double_sort);
            } else if (constant instanceof Float) {
                floatExpr = ctx.mkFP((Float) constant, double_sort);
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return floatExpr;
    }

    private SeqExpr<CharSort> stringExpr;
    @Override
    public SeqExpr<CharSort> getStringExpr(Context ctx) {
        if (stringExpr == null) {
            if (constant instanceof String) {
                stringExpr = ctx.mkString((String) constant);
            } else if (constant instanceof Integer) {
                stringExpr = ctx.mkString(((Integer) constant).toString());
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return stringExpr;
    }

    private Expr<SeqSort<CharSort>> typeExpr;
    @Override
    public Expr<SeqSort<CharSort>> getTypeExpr(Context ctx) {
        if (typeExpr == null) {
            if (AriadneSupporter.isPythonBool(constant)) {
                typeExpr = ctx.mkString(BOOL);
            } else if (AriadneSupporter.isPythonInt(constant)) {
                typeExpr = ctx.mkString(INT);
            } else if (AriadneSupporter.isPythonFloat(constant)) {
                typeExpr = ctx.mkString(FLOAT);
            } else if (AriadneSupporter.isPythonString(constant)) {
                typeExpr = ctx.mkString(STR);
            } else if (constant instanceof TypeReference) {
                typeExpr = ctx.mkString(((TypeReference) constant).getName().toString());
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return typeExpr;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Object getConstant() {
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
        return this.repr.compareTo(((ConstantTerm) o).repr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantTerm that = (ConstantTerm) o;
        return Objects.equals(constant, that.constant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constant);
    }

    @Override
    public String toString() {
        if (constant == null) {
            return "";
        } else {
            return constant.toString();
        }
    }
}
