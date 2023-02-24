package analysis.backward.thresher.term;

import analysis.backward.thresher.factor.FieldVarFactor;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.StringFieldVarFactor;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;

import java.util.*;

public class VariableTerm implements ITerm {
    private final IProgramFactor var;

    private final String repr;

    private boolean substituted = false;

    // Whether this variable has each value, which is judged by forward result.
    private final boolean noBool;
    private final boolean noInt;
    private final boolean noFloat;
    private final boolean noString;

    public VariableTerm(IProgramFactor var) {
        this(var, false, false, false, false);
    }

    public VariableTerm(IProgramFactor var, ForwardAbstractValue forwardAbstractValue) {
        this(
                var,
                forwardAbstractValue.getBoolValue().isBottom(),
                forwardAbstractValue.getIntValue().isBottom(),
                forwardAbstractValue.getFloatValue().isBottom(),
                forwardAbstractValue.getStringValue().isBottom()
        );
    }

    protected VariableTerm(IProgramFactor var, boolean noBool, boolean noInt, boolean noFloat, boolean noString) {
        this.var = var;
        this.repr = this.toString();

        this.noBool = noBool;
        this.noInt = noInt;
        this.noFloat = noFloat;
        this.noString = noString;
    }

    // TODO: remove this method.
    public IProgramFactor getVar() {
        return var;
    }

    @Override
    public boolean hasBool() {
        return !noBool || !noInt || !noFloat || !noString;
    }

    @Override
    public boolean hasInt() {
        return !noBool || !noInt || !noFloat;
    }

    @Override
    public boolean hasFloat() {
        return !noBool || !noInt || !noFloat;
    }

    @Override
    public boolean hasString() {
        return !noString;
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
        return new HashSet<IProgramFactor>(){{addAll(var.getFactors());}};
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
        } else if (toTerm instanceof ConstantTerm && toTerm != ConstantTerm.NULL) {
            // TODO: This implementation ignores even if the type has impossible attributes.
            // This means that the chance of refutation is ignored.
        } else if (var instanceof FieldVarFactor) {
            IProgramFactor fromVar = fromTerm.getVar();

            // Not contain 'fromVar'.
            if (!var.getFactors().contains(fromVar)) return this;

            IProgramFactor toVar = null;
            if (toTerm == ConstantTerm.NULL && var.getFactors().contains(fromVar)) {
                // This means replaced var is 'None.attr'. This is infeasible path, so return
                // object which solver always return false.
                ConstantTerm result = new ConstantTerm(null);
                result.setSubstituted(true);
                return result;
            } else if (toTerm instanceof VariableTerm) {
                toVar = ((VariableTerm) toTerm).getVar();
            } else if (toTerm instanceof ComplexVariableTerm) {
                toVar = ((ComplexVariableTerm) toTerm).getVar();
            } else if (!var.getFactors().contains(fromVar)) {
                this.setSubstituted(false);
                return this;
            } else if (var instanceof StringFieldVarFactor
                    && ((StringFieldVarFactor) var).getValFactor().equals(fromVar)
                    && toTerm instanceof ConstantTypeTerm) {
                // If attribute is string constant and you want to replace to constant type,
                // get the type of the constant type's the string attribute.
                TypeReference valType = ((ConstantTypeTerm) toTerm).getConstant();
                String attr = ((StringFieldVarFactor) var).getFieldName();
                TypeReference resolvedType = null;
                if (valType.getName().toString().endsWith("_instance")) {
                    // If 'val' in 'val.attr' is instance, get trampoline method's type.
                    resolvedType = TypeReference.findOrCreate(
                            PythonTypes.pythonLoader,
                            valType.getName().toString()
                                    .replace("Lscript", "L$script")
                                    .replace("_instance", "") + "/" + attr
                    );
                } else {
                    resolvedType = TypeReference.findOrCreate(
                            PythonTypes.pythonLoader, valType.getName().toString() + "/" + attr);
                }
                ConstantTypeTerm resolvedTypeTerm = new ConstantTypeTerm(resolvedType);
                resolvedTypeTerm.setSubstituted(true);
                return resolvedTypeTerm;
            } else {
                // There is a possibility of missing replace but drop constraint.
                // TODO: Don't set 'substituted' to static variable.
                ConstantTerm.TRUE.setSubstituted(true);
                return ConstantTerm.TRUE;
            }

            FieldVarFactor fieldVar = (FieldVarFactor) var;
            IProgramFactor newVar = fieldVar.replace(toVar, fromVar);
            if (fieldVar != newVar) {
                VariableTerm newTerm = new VariableTerm(newVar);
                newTerm.setSubstituted(true);
                return newTerm;
            }
        }

        this.setSubstituted(false);
        return this;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof BinOpTerm)
            return 1;
        return this.repr.compareTo(((VariableTerm) o).repr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableTerm that = (VariableTerm) o;
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
