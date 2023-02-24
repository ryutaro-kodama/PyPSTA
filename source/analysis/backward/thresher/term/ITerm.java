package analysis.backward.thresher.term;

import analysis.backward.thresher.factor.IProgramFactor;
import com.microsoft.z3.*;

import java.util.Collection;

public interface ITerm extends Comparable  {
    String VAR = "VAR";

    String TYPE = "TYPE";

    String BOOL = "BOOL";
    String INT = "INT";
    String FLOAT = "FLOAT";
    String STR = "STR";

    boolean hasBool();

    boolean hasInt();

    boolean hasFloat();

    boolean hasString();

    BoolExpr getBoolExpr(Context ctx);

    IntExpr getIntExpr(Context ctx);

    FPExpr getFloatExpr(Context ctx);

    SeqExpr<CharSort> getStringExpr(Context ctx);

    Expr getTypeExpr(Context ctx);

    boolean isConstant();

    Object getConstant();

    Collection<? extends IProgramFactor> getVars();

    void setSubstituted(boolean b);

    boolean isSubstituted();

    ITerm substitute(ITerm toTerm, VariableTerm fromTerm);
}
