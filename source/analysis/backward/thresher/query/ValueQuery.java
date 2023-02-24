package analysis.backward.thresher.query;

import analysis.backward.thresher.constraint.ValueConstraint;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.VariableFactor;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import edu.colorado.thresher.core.IQuery;
import edu.colorado.thresher.core.Util;

import java.util.Set;

public class ValueQuery extends AbstractPypstaQuery implements IPypstaQuery {
    private final int initVarId;
    private final CGNode initCGNode;
    private final Object initValue;

    public ValueQuery(ForwardAnalyzer forwardAnalyzer,
                      int initVarId,
                      CGNode node,
                      Object initValue) {
        super(forwardAnalyzer);
        this.initVarId = initVarId;
        this.initCGNode = node;
        this.initValue = initValue;

        VariableFactor initVarFactor = new VariableFactor(node, initVarId);
        addConstraint(new ValueConstraint(initVarFactor, initValue, IConditionalBranchInstruction.Operator.EQ));

        this.feasible = true;
    }

    protected ValueQuery(ForwardAnalyzer forwardAnalyzer,
                         int initVarId,
                         CGNode node,
                         Object initValue,
                         Context ctx,
                         Solver solver,
                         Set constraints,
                         Set<IProgramFactor> pathVars,
                         boolean feasible,
                         Set<AllocatePoint> cantEEGDrop) {
        super(forwardAnalyzer, ctx, solver, constraints, pathVars, feasible, cantEEGDrop);
        this.initVarId = initVarId;
        this.initCGNode = node;
        this.initValue = initValue;
    }

    public int getInitVarId() {
        return initVarId;
    }

    public CGNode getInitCGNode() {
        return initCGNode;
    }

    public Object getInitValue() {
        return initValue;
    }

    @Override
    public IQuery deepCopy() {
        return new ValueQuery(
                forwardAnalyzer, initVarId, initCGNode, initValue, ctx, solver,
                Util.deepCopySet(constraints), Util.deepCopySet(pathVars), feasible, cantEEGDrop
        );
    }

    @Override
    public boolean contains(IQuery other) {
        if (!(other instanceof ValueQuery))
            return false;
        ValueQuery otherQuery = (ValueQuery) other;
        return this.constraints.containsAll(otherQuery.constraints);
    }

//    @Override
//    public boolean canMerge(IQuery query) {
//        return false;
//    }
//
//    @Override
//    public void merge(IQuery query) {
//
//    }
}
