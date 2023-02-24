package analysis.backward.thresher.term;

import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.NotConstantFieldVarFactor;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;

import java.util.Collection;
import java.util.HashSet;

public class ComplexVariableTerm implements ITerm {
    private final NotConstantFieldVarFactor var;

    private final String repr;

    private boolean substituted = false;

    public ComplexVariableTerm(NotConstantFieldVarFactor var) {
        this.var = var;
        this.repr = this.toString();
    }

    // TODO: remove this method.
    public NotConstantFieldVarFactor getVar() {
        return var;
    }

    @Override
    public boolean hasBool() {
        return true;
    }

    @Override
    public boolean hasInt() {
        return true;
    }

    @Override
    public boolean hasFloat() {
        return true;
    }

    @Override
    public boolean hasString() {
        return true;
    }

    private String makeVarName(final String type) {
        return ITerm.VAR + var.getVariableName() + "_" + type;
    }

    private BoolExpr boolExpr;
    @Override
    public BoolExpr getBoolExpr(Context ctx) {
        if (boolExpr == null)
            boolExpr = ctx.mkBoolConst(makeVarName(ITerm.BOOL));
        return boolExpr;
    }

    private IntExpr intExpr;
    @Override
    public IntExpr getIntExpr(Context ctx) {
        if (intExpr == null)
            intExpr = ctx.mkIntConst(makeVarName(ITerm.INT));
        return intExpr;
    }

    private FPExpr floatExpr;
    @Override
    public FPExpr getFloatExpr(Context ctx) {
        if (floatExpr == null) {
            FPSort double_sort = ctx.mkFPSort(11, 53);
            floatExpr = (FPExpr) ctx.mkConst(makeVarName(ITerm.FLOAT), double_sort);
        }
        return floatExpr;
    }

    private SeqExpr stringExpr;
    @Override
    public SeqExpr<CharSort> getStringExpr(Context ctx) {
        if (stringExpr == null)
            stringExpr = (SeqExpr) ctx.mkConst(makeVarName(ITerm.STR), ctx.getStringSort());
        return stringExpr;
    }

    private Expr<SeqSort<CharSort>> typeExpr;
    @Override
    public Expr<SeqSort<CharSort>> getTypeExpr(Context ctx) {
        if (typeExpr == null) {
            typeExpr = ctx.mkConst(var.getVariableName() + "_" + ITerm.TYPE, ctx.mkStringSort());
        }
        return typeExpr;
    }
    
    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Object getConstant() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public Collection<? extends IProgramFactor> getVars() {
        return new HashSet<IProgramFactor>(){{
            addAll(var.getFactors());
        }};
    }

    @Override
    public void setSubstituted(boolean b) {
        substituted = b;
    }

    @Override
    public boolean isSubstituted() {
        return substituted;
    }

    @Override
    public ITerm substitute(ITerm toTerm, VariableTerm fromTerm) {
        if (this.equals(fromTerm)) {
            toTerm.setSubstituted(true);
            return toTerm;
        } else if (toTerm.isConstant()) {
            this.setSubstituted(false);
            return this;
        } else if (var.getValFactor().equals(fromTerm.getVar())) {
            IProgramFactor newVal = var.getValFactor().replace(
                    toTerm.getVars().iterator().next(), fromTerm.getVar()
            );
            NotConstantFieldVarFactor newVar = new NotConstantFieldVarFactor(
                    newVal, var.getFieldFactor()
            );
            ComplexVariableTerm newTerm = new ComplexVariableTerm(newVar);
            newTerm.setSubstituted(true);
            return newTerm;
        } else {
            this.setSubstituted(false);
            return this;
        }
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof BinOpTerm)
            return 1;
        return this.repr.compareTo(((ComplexVariableTerm) o).repr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexVariableTerm that = (ComplexVariableTerm) o;
        return repr.equals(that.repr);
    }

    @Override
    public int hashCode() {
        return repr.hashCode();
    }

    @Override
    public String toString() {
        return var.toString();
    }
}
