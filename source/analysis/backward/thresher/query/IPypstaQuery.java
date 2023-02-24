package analysis.backward.thresher.query;

import analysis.backward.thresher.constraint.IConstraint;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.term.ITerm;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.debug.Assertions;
import edu.colorado.thresher.core.IPathInfo;
import edu.colorado.thresher.core.IQuery;
import edu.colorado.thresher.core.PointerVariable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IPypstaQuery extends IQuery {
    boolean substituteExpForVar(ITerm toTerm, IProgramFactor from);

    boolean addConstraint(IConstraint constraint);

    void setFoundWitness(boolean foundWitness);

    boolean isFoundWitness();

    default void dropConstraintsContaining(Set<PointerVariable> vars) {
        Assertions.UNREACHABLE();
    }

    /**
     * Drop the constraint which use the variable passed in arguments.
     * @param vars the target variables
     */
    void dropConstraintsContaining(Collection<? extends IProgramFactor> vars);

    void dropConstraintsProduceableInCall(
            PythonInvokeInstruction instr, CGNode caller, CGNode callee, boolean dropPtConstraints);

    @Override
    default List<IQuery> enterCall(SSAInvokeInstruction instr, CGNode caller, IPathInfo currentPath) {
        Assertions.UNREACHABLE();
        return null;
    }

    List<IQuery> enterCall(PythonInvokeInstruction instr, CGNode callee, IPathInfo currentPath);

    @Override
    default List<IQuery> returnFromCall(SSAInvokeInstruction instr, CGNode callee, IPathInfo currentPath, boolean backward) {
        Assertions.UNREACHABLE();
        return null;
    }

    List<IQuery> returnFromCall(PythonInvokeInstruction instr, CGNode callee, IPathInfo currentPath, boolean backward);

//    boolean canMerge(IQuery query);
//
//    void merge(IQuery query);

    Set<IProgramFactor> getPathVars();

    boolean isDispatchFeasible(PythonInvokeInstruction instr, CGNode caller, CGNode callee);

    Set<Integer> getDefBBIds();

    Set<Integer> getUseBBIds();

    boolean hasNoDUInfoVar();
}
