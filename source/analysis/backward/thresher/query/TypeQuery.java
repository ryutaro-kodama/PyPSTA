package analysis.backward.thresher.query;

import analysis.backward.thresher.constraint.TypeConstraint;
import analysis.backward.thresher.factor.*;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import edu.colorado.thresher.core.IQuery;
import edu.colorado.thresher.core.Util;

import java.util.*;

public class TypeQuery extends AbstractPypstaQuery implements IPypstaQuery {
    private final int initVarId;
    private final CGNode initCGNode;
    private final TypeReference initType;

    public TypeQuery(ForwardAnalyzer forwardAnalyzer,
                     int initVarId,
                     CGNode node,
                     TypeReference initType) {
        super(forwardAnalyzer);
        this.initVarId = initVarId;
        this.initCGNode = node;
        this.initType = initType;

        VariableFactor initVarFactor = new VariableFactor(node, initVarId);
        addConstraint(new TypeConstraint(initVarFactor, initType));

        this.feasible = true;
    }

    protected TypeQuery(ForwardAnalyzer forwardAnalyzer,
                        int initVarId,
                        CGNode node,
                        TypeReference initType,
                        Context ctx,
                        Solver solver,
                        Set constraints,
                        Set<IProgramFactor> pathVars,
                        boolean feasible,
                        Set<AllocatePoint> cantEEGDrop) {
        super(forwardAnalyzer, ctx, solver, constraints, pathVars, feasible, cantEEGDrop);
        this.initVarId = initVarId;
        this.initCGNode = node;
        this.initType = initType;
    }

    public int getInitVarId() {
        return initVarId;
    }

    public CGNode getInitCGNode() {
        return initCGNode;
    }

    public TypeReference getInitType() {
        return initType;
    }

    @Override
    public IQuery deepCopy() {
        return new TypeQuery(
                forwardAnalyzer, initVarId, initCGNode, initType, ctx, solver,
                Util.deepCopySet(constraints), Util.deepCopySet(pathVars), feasible, Util.deepCopySet(cantEEGDrop));
    }

    @Override
    public boolean contains(IQuery other) {
        if (!(other instanceof TypeQuery))
            return false;
        TypeQuery otherQuery = (TypeQuery) other;
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
