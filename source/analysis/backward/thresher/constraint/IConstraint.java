package analysis.backward.thresher.constraint;

import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.term.ITerm;
import analysis.backward.thresher.term.VariableTerm;
import com.microsoft.z3.AST;
import com.microsoft.z3.Context;

import java.util.Collection;
import java.util.Set;

public interface IConstraint {
    AST toZ3AST(Context ctx);

    ITerm getLhs();

    ITerm getRhs();

    boolean isConstant();

    Collection<? extends IProgramFactor> getVars();

    void setSubstituted(boolean b);

    boolean isSubstituted();

    Set<IConstraint> substitute(ITerm toTerm, VariableTerm fromTerm);

    boolean manualEvaluation();
}
