package analysis.backward.thresher.query;

import analysis.backward.thresher.constraint.AbstractConstraint;
import analysis.backward.thresher.constraint.IConstraint;
import analysis.backward.thresher.constraint.ValueConstraint;
import analysis.backward.thresher.constraint.TypeConstraint;
import analysis.backward.thresher.factor.*;
import analysis.backward.thresher.term.*;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.AllocatePoints;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.cast.ipa.callgraph.AstCallGraph;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonPropertyRead;
import com.ibm.wala.cast.python.ssa.PythonPropertyWrite;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;
import edu.colorado.thresher.core.*;
import edu.colorado.thresher.core.Util;
import util.AriadneSupporter;
import util.ConstantConverter;
import util.PythonBool;
import util.PythonCalc;

import java.util.*;

public abstract class AbstractPypstaQuery implements IPypstaQuery {
    protected final ForwardAnalyzer forwardAnalyzer;

    protected final Context ctx;
    protected final Solver solver;

    // set of path constraints; implicit AND exists between all of them
    protected final Set<IConstraint> constraints; // these need to be ordered for comparison

    // a z3 representation of the z3 assumption tied to the current path constraints
    protected BoolExpr currentPathAssumption;

    // list of path constraints that make this query unsatisfiable (lazily instantiated).
    protected List<IConstraint> unsatCore;

    // set of variables contained in our path constraints. used for relevancy lookups
    protected final Set<IProgramFactor> pathVars;

    // Information for def-use relation.
    private Set<Integer> defBBIds = HashSetFactory.make();
    private Set<Integer> useBBIds = HashSetFactory.make();
    private boolean hasNoDUInfoVar = true;

    protected boolean fakeWitness = false;
    protected boolean feasible;

    // TODO: hack! but z3 really slows things down once it can't decide
    private static boolean z3Panic = false;

    public AbstractPypstaQuery(ForwardAnalyzer forwardAnalyzer) {
        this.forwardAnalyzer = forwardAnalyzer;
        this.ctx = new Context();
        this.solver = ctx.mkSolver();
        this.constraints = HashSetFactory.make();
        this.pathVars = HashSetFactory.make();
        this.cantEEGDrop = HashSetFactory.make();
    }

    public AbstractPypstaQuery(ForwardAnalyzer forwardAnalyzer,
                               Context ctx,
                               Solver solver,
                               Set<IConstraint> constraints,
                               Set<IProgramFactor> pathVars,
                               boolean feasible,
                               Set<AllocatePoint> cantEEGDrop) {
        this.forwardAnalyzer = forwardAnalyzer;
        this.ctx = ctx;
        this.solver = solver;
        this.constraints = constraints;
        this.pathVars = pathVars;
        this.feasible = feasible;
        this.cantEEGDrop = cantEEGDrop;
        rebuildZ3Constraints();
    }

    // TODO: Delete this.
    public Set<IConstraint> getConstraints() {
        return constraints;
    }

    @Override
    public boolean foundWitness() {
//        return !feasible;
        if (fakeWitness)
            return fakeWitness;
        if (Options.SYNTHESIS)
            return false;
        return false;
//        return constraints.isEmpty(); // can't have a witness while there are still path constraints to produce
    }

    protected void rebuildZ3Constraints() {
        pathVars.clear();
        this.currentPathAssumption = null;
        solver.reset();
        if (constraints.size() > 0) {
            BoolExpr[] constraintsArr = new BoolExpr[constraints.size()];

            int i = 0;
            for (IConstraint constraint: constraints) {
                pathVars.addAll(constraint.getVars());
                AST z3Constraint = constraint.toZ3AST(ctx);
                constraintsArr[i++] = (BoolExpr) z3Constraint;
            }

            try {
                BoolExpr pathConstraint = ctx.mkAnd(constraintsArr);
                BoolExpr newAssumption = Util.makeFreshPropositionalVar(ctx);
                solver.add(ctx.mkImplies(newAssumption, pathConstraint));
                this.currentPathAssumption = newAssumption;
            } catch (Z3Exception e) {
                Util.Assert(false, "problem with z3 " + e);
            }

            rebuildDUInfo();
        }
        // else, do nothing; no constraints to work with
    }

    private void rebuildDUInfo() {
        hasNoDUInfoVar = false;
        defBBIds.clear();
        useBBIds.clear();
        for (IProgramFactor var: pathVars) {
            if (var.noDefUseInfo()) {
                // Constraints have the variable which has no def-use information such as global variable.
                hasNoDUInfoVar = true;
                break;
            }

            defBBIds.add(var.getDefBBId());
            if (var.needUseCheck()) {
                useBBIds.addAll(var.getUseBBIds());
            }
        }
    }

    @Override
    public boolean addConstraintFromBranchPoint(IBranchPoint point, boolean trueBranchFeasible) {
        return addConstraintFromConditional(point.getInstr(), point.getMethod(), trueBranchFeasible);
    }

    @Override
    public boolean addConstraintFromConditional(SSAConditionalBranchInstruction instr,
                                                CGNode currentCGNode,
                                                boolean trueBranchFeasible) {
        ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                currentCGNode, currentCGNode.getIR().getBasicBlockForInstruction(instr)
        );
        Set<IConstraint> constraint = getPathConstraintFromCondBranch(
                instr, forwardResult, currentCGNode, !trueBranchFeasible);
        Util.Print("adding guard constraint " + constraint);
        if (constraint == AbstractConstraint.TRUE) {
            return true;
        } else if (constraint == AbstractConstraint.FALSE) {
            return false;
        }
        if (addConstraints(constraint))
            return isFeasible();
        return true;
        // else, constraint already in set; no need to check feasibility
    }

    /**
     * Add some constraints.
     *
     * @param constraints constraints to add
     * @return true if less than one constraint was successfully added, false if all constraints
     * are contained
     */
    public boolean addConstraints(Set<IConstraint> constraints) {
        boolean result = false;
        for (IConstraint constraint: constraints) {
            result |= addConstraint(constraint);
        }
        return result;
    }

    /**
     * Add constraint should always be done through this method.
     *
     * @param newConstraint constraint to add
     * @return true if constraint was successfully added, false if we already have it in our set
     */
    @Override
    public boolean addConstraint(IConstraint newConstraint) {
        // decline adding path constraints if we already have more than the max number
        if (constraints.size() >= Options.MAX_PATH_CONSTRAINT_SIZE) {
            Util.Debug("!!!!!!!!!!not adding constraint " + newConstraint + " due to size restrictions!!!!!!!!!!");
            return true;
        }

        if (newConstraint instanceof TypeConstraint
                && newConstraint.getLhs() instanceof VariableTerm
                && newConstraint.getRhs().isConstant()) {
            for (IConstraint existingConstraint: constraints) {
                if ( !(existingConstraint instanceof TypeConstraint
                        && existingConstraint.getLhs() instanceof VariableTerm
                        && existingConstraint.getRhs().isConstant()) ) continue;

                VariableTerm newLhsVarTerm = (VariableTerm) newConstraint.getLhs();
                VariableTerm existingLhsVarTerm = (VariableTerm) existingConstraint.getLhs();

                ConstantTypeTerm attrType = null;
                StringFieldVarFactor attrVar = null;
                ConstantTypeTerm objType = null;
                VariableFactor objVar = null;
                if (newLhsVarTerm.getVar() instanceof StringFieldVarFactor
                        && ((StringFieldVarFactor) newLhsVarTerm.getVar()).getValFactor()
                                instanceof VariableFactor
                        && existingLhsVarTerm.getVar() instanceof VariableFactor
                        && ((StringFieldVarFactor) newLhsVarTerm.getVar()).getValFactor()
                                .equals(existingLhsVarTerm.getVar()) ) {
                    // New constraint is `1.attr == ATTR_TYPE` and existing constraint is `1 == OBJ_TYPE`.
                    attrType = (ConstantTypeTerm) newConstraint.getRhs();
                    attrVar = (StringFieldVarFactor) newLhsVarTerm.getVar();
                    objType = (ConstantTypeTerm) existingConstraint.getRhs();
                    objVar = (VariableFactor) existingLhsVarTerm.getVar();
                } else if (existingLhsVarTerm.getVar() instanceof StringFieldVarFactor
                        && ((StringFieldVarFactor) existingLhsVarTerm.getVar()).getValFactor()
                                instanceof VariableFactor
                        && newLhsVarTerm.getVar() instanceof VariableFactor
                        && ((StringFieldVarFactor) existingLhsVarTerm.getVar()).getValFactor()
                                .equals(newLhsVarTerm.getVar()) ) {
                    // Existing constraint is `1.attr == ATTR_TYPE` and new constraint is `1 == OBJ_TYPE`.
                    attrType = (ConstantTypeTerm) existingConstraint.getRhs();
                    attrVar = (StringFieldVarFactor) existingLhsVarTerm.getVar();
                    objType = (ConstantTypeTerm) newConstraint.getRhs();
                    objVar = (VariableFactor) newLhsVarTerm.getVar();
                } else {
                    continue;
                }

                ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                        objVar.getNode(), objVar.getNode().getIR().getExitBlock()
                );
                ForwardState forwardState = forwardResult.getState();
                ForwardAbstractValue objAbstractValue = forwardResult.getValue(objVar.getVariableId());

                boolean matched = false;
                for (ObjectValue objValue:
                        objAbstractValue.getAllocatePoints().getObjectsIterable(forwardState.getAllocatePointTable())) {
                    if (objValue.getTypeReference().equals(objType.getConstant())
                            && objValue.hasAttr(
                                    attrVar.getFieldName(), forwardState.getAllocatePointTable())) {
                        ForwardAbstractValue objAttrAbstractValue
                                = (ForwardAbstractValue) objValue.getAttr(
                                        attrVar.getFieldName(), forwardState.getAllocatePointTable());
                        if (( !objAttrAbstractValue.getBoolValue().isBottom()
                                    && attrType.equals(TypeReference.Boolean) )
                                || (!objAttrAbstractValue.getIntValue().isBottom()
                                        && attrType.equals(TypeReference.Int) )
                                || (!objAttrAbstractValue.getFloatValue().isBottom()
                                        && attrType.equals(TypeReference.Float) )
                                || (!objAttrAbstractValue.getBoolValue().isBottom()
                                        && attrType.equals(PythonTypes.string) ) ) {
                            matched = true;
                        } else {
                            for (ObjectValue objAttrObject:
                                    objAttrAbstractValue.getAllocatePoints()
                                            .getObjectsIterable(forwardState.getAllocatePointTable())) {
                                if (objAttrObject.getTypeReference().equals(attrType.getConstant())) {
                                    matched = true; break;
                                }
                            }
                        }

                        if (matched) break;
                    }
                }

                if (!matched) {
                    makeUnsatCore(newConstraint);
                    this.feasible = false;
                    return false;
                }
            }
        }

        if (newConstraint instanceof TypeConstraint) {
            for (IConstraint existingConstraint: constraints) {
                if (existingConstraint instanceof TypeConstraint
                        && newConstraint.getRhs() instanceof ConstantTypeTerm
                        && existingConstraint.getRhs() instanceof ConstantTypeTerm
                        && newConstraint.getLhs().equals(existingConstraint.getLhs())) {
                    if (newConstraint.getRhs().equals(existingConstraint.getRhs())) {
                        // pass
                    } else {
                        // TODO: Consider class inheritance.
                        this.feasible = false;
                        return false;
                    }
                }
            }
        }

        if (constraints.add(newConstraint)) {
            rebuildZ3Constraints();
            return true;
        }
        return false;
    }

    /**
     * Remove constraints should always be done through this method.
     *
     * @param constraint constraint to remove
     * @return true if constraint was successfully removed, false otherwise
     */
    protected boolean removeConstraint(IConstraint constraint) {
        if (constraints.remove(constraint)) {
            rebuildZ3Constraints();
            return true;
        } else {
            Assertions.UNREACHABLE();
            return false;
//            for (IConstraint con : constraints) {
//                Util.Debug(con + " eq " + constraint + " ?" + con.equals(constraint) + " compared " + con.compareTo(constraint));
//            }
//            Util.Unimp("couldn't remove " + constraint + " from " + Util.printCollection(this.constraints) + " contains? "
//                    + constraints.contains(constraint));
        }
    }

    protected Set<IConstraint> getPathConstraintFromCondBranch(SSAConditionalBranchInstruction instruction,
                                                               ForwardAnalyzer.ForwardResult forwardResult,
                                                               CGNode node, boolean trueBranchFeasible) {
        int uses = instruction.getNumberOfUses();
        Util.Assert(uses == 2, "ONLY TWO USES please");
        int use0 = instruction.getUse(0), use1 = instruction.getUse(1);

        ConditionalBranchInstruction.Operator op;
        if (trueBranchFeasible)
            op = (ConditionalBranchInstruction.Operator) instruction.getOperator();
        else
            op = Util.negateOperator((ConditionalBranchInstruction.Operator) instruction.getOperator());

        Set<IConstraint> constraints = new HashSet<>();

        boolean lhsConstant = forwardResult.isConstant(use0), rhsConstant = forwardResult.isConstant(use1);
        if (lhsConstant && rhsConstant) {
            // constants on both sides of operator;
            Object lhs = forwardResult.getConstant(use0);
            Object rhs = forwardResult.getConstant(use1);
            Object result = null;
            switch (op) {
                case EQ: result = PythonCalc.eq(lhs, rhs); break;
                case NE: result = PythonCalc.ne(lhs, rhs); break;
                default: Assertions.UNREACHABLE();
            }
            if (PythonBool.isTrue(result)) {
                return AbstractConstraint.TRUE;
            } else {
                return AbstractConstraint.FALSE;
            }
        } else if (lhsConstant) {
            VariableFactor var1 = new VariableFactor(node, use1);
            VariableTerm var1Term;
            if (forwardResult.getValue(use1).isBottom()) {
                var1Term = new VariableTerm(var1);
            } else {
                var1Term = new VariableTerm(var1, forwardResult.getValue(use1));
            }
            if (forwardResult.isNullConstant(use0)) {
                constraints.add(new ValueConstraint(var1Term, ConstantTerm.NULL, op));
            } else {
                constraints.add(
                        new ValueConstraint(
                                var1Term, new ConstantTerm(forwardResult.getConstant(use0)), op
                        )
                );
            }
        } else if (rhsConstant) {
            VariableFactor var0 = new VariableFactor(node, use0);
            VariableTerm var0Term;
            if (forwardResult.getValue(use0).isBottom()) {
                var0Term = new VariableTerm(var0);
            } else {
                var0Term = new VariableTerm(var0, forwardResult.getValue(use0)
                );
            }
            if (forwardResult.isNullConstant(use1)) {
                constraints.add(new ValueConstraint(var0Term, ConstantTerm.NULL, op));
            } else {
                constraints.add(
                        new ValueConstraint(
                                var0Term, new ConstantTerm(forwardResult.getConstant(use1)), op
                        )
                );
            }
        } else {
            VariableFactor var0 = new VariableFactor(node, use0);
            VariableTerm var0Term;
            if (forwardResult.getValue(use0).isBottom()) {
                var0Term = new VariableTerm(var0);
            } else {
                var0Term = new VariableTerm(var0, forwardResult.getValue(use0)
                );
            }
            VariableFactor var1 = new VariableFactor(node, use1);
            VariableTerm var1Term;
            if (forwardResult.getValue(use1).isBottom()) {
                var1Term = new VariableTerm(var1);
            } else {
                var1Term = new VariableTerm(var1, forwardResult.getValue(use1));
            }
            constraints.add(new ValueConstraint(var0Term, var1Term, op));
        }
        return constraints;
    }

    @Override
    public List<IQuery> addPathConstraintFromSwitch(SSASwitchInstruction instr, SSACFG.BasicBlock lastBlock, CGNode currentNode) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public boolean addPathConstraintFromSwitch(SSAConditionalBranchInstruction switchCase, CGNode currentNode, boolean negated) {
        Assertions.UNREACHABLE();
        return false;
    }

    @Override
    public void intersect(IQuery other) {
        Assertions.UNREACHABLE();
    }

    @Override
    public boolean containsConstraint(Constraint constraint) {
        Assertions.UNREACHABLE();
        return false;
    }

    @Override
    public List<AtomicPathConstraint> getIndexConstraintsFor(FieldReference fld) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public List<IQuery> visit(SSAInstruction instr, IPathInfo currentPath) {
        CGNode node = currentPath.getCurrentNode();
        ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                node, node.getIR().getBasicBlockForInstruction(instr)
        );
        boolean isInloopBody = WALACFGUtil.isInLoopBody(
                (SSACFG.BasicBlock) ((AstCallGraph.AstCGNode) node).getCFG()
                        .getBlockForInstruction(instr.iIndex()),
                node.getIR()
        );
        boolean result = false;

//        if (instr instanceof SSAArrayStoreInstruction)
//            result = visit((SSAArrayStoreInstruction) instr, node, tbl);
//        else if (instr instanceof SSAArrayLengthInstruction)
//            result = visit((SSAArrayLengthInstruction) instr, node);
//        else if (instr instanceof SSAArrayLoadInstruction)
//            result = visit((SSAArrayLoadInstruction) instr, node, tbl);
//        else
        if (instr instanceof SSAUnaryOpInstruction)
            result = visit((SSAUnaryOpInstruction) instr, node, forwardResult, isInloopBody);
        else if (instr instanceof SSABinaryOpInstruction)
            result = visit((SSABinaryOpInstruction) instr, node, forwardResult, isInloopBody);
        else if (instr instanceof SSACheckCastInstruction)
            result = visit((SSACheckCastInstruction) instr, node, forwardResult);
        else if (instr instanceof SSAGotoInstruction)
            return IQuery.FEASIBLE; // goto's are a nop for us
//        else if (instr instanceof SSASwitchInstruction)
//            //result = visit((SSASwitchInstruction) instr, node, tbl);
//            return IQuery.FEASIBLE; // switch is a nop
//        else if (instr instanceof SSALoadMetadataInstruction)
//            result = visit((SSALoadMetadataInstruction) instr, node);
//        else if (instr instanceof SSAConversionInstruction)
//            result = visit((SSAConversionInstruction) instr, node);
//        else if (instr instanceof SSAInstanceofInstruction)
//            result = visit((SSAInstanceofInstruction) instr, node);
//        else if (instr instanceof SSAComparisonInstruction)
//            result = visit((SSAComparisonInstruction) instr, node, tbl);
//        else if (instr instanceof SSAMonitorInstruction)
//            return IQuery.FEASIBLE; // have no idea what this is. nop!
//        else if (instr instanceof SSAGetCaughtExceptionInstruction)
//            return IQuery.FEASIBLE; // TODO: handle this properly
//        else if (instr instanceof SSAThrowInstruction)
//            result = visit((SSAThrowInstruction) instr, node);
//        else if (instr instanceof SSAInvokeInstruction) {
//            //Util.Assert(false, "should this happen?");
//            result = visit((SSAInvokeInstruction) instr, node);
        else if (instr instanceof SSANewInstruction)
            result = visit((SSANewInstruction) instr, node, forwardResult);
        else if (instr instanceof SSAReturnInstruction)
            result = visit((SSAReturnInstruction) instr, node, forwardResult);
//            // else if (instr instanceof SSAStoreIndirectInstruction) return
//            // query.visit((SSAStoreIndirectInstruction) instr);
        else if (instr instanceof AstGlobalRead)
            result = visit((AstGlobalRead) instr, node, forwardResult);
        else if (instr instanceof SSAGetInstruction)
            result = visit((SSAGetInstruction) instr, node, forwardResult);
        else if (instr instanceof AstGlobalWrite)
            result = visit((AstGlobalWrite) instr, node, forwardResult);
        else if (instr instanceof SSAPutInstruction)
            result = visit((SSAPutInstruction) instr, node, forwardResult);
        else if (instr instanceof PythonPropertyRead)
            result = visit((PythonPropertyRead) instr, node, forwardResult);
        else if (instr instanceof PythonPropertyWrite)
            result = visit((PythonPropertyWrite) instr, node, forwardResult);
        else if (instr instanceof EachElementGetInstruction)
            result = visit((EachElementGetInstruction) instr, node, forwardResult);
        else if (instr instanceof AstLexicalRead)
            result = visit((AstLexicalRead) instr, node, forwardResult);
        else if (instr instanceof AstLexicalWrite)
            result = visit((AstLexicalWrite) instr, node, forwardResult);
        else if (instr instanceof AstAssertInstruction)
            result = visit((AstAssertInstruction) instr, node, forwardResult);
        else if (instr instanceof AstIsDefinedInstruction)
            result = visit((AstIsDefinedInstruction) instr, node, forwardResult);
//
        else
            Util.Unimp("visiting instr " + instr);

        /*
         * // SANITY CHECK Iterator<AtomicPathConstraint> iter =
         * constraints.descendingIterator(); AtomicPathConstraint last = null; while
         * (iter.hasNext()) { AtomicPathConstraint next = iter.next(); if (last !=
         * null) { Util.Assert(last.compareTo(next) > 0, "constraint " + last +
         * " not greater than " + next); } last = next; }
         */

        if (!result) {
            this.feasible = false;
            stackedQueries.clear();
            return IQuery.INFEASIBLE;
        } else if (stackedQueries.size() > 0) {
            List<IQuery> ret = new ArrayList<>(stackedQueries);
            stackedQueries.clear();
            return ret;
        }
        return IQuery.FEASIBLE;
    }

    // The queries which are created in each visit functions.
    private final List<AbstractPypstaQuery> stackedQueries = new ArrayList<>();

    protected boolean visit(SSAUnaryOpInstruction instr,
                            CGNode currentCGNode,
                            ForwardAnalyzer.ForwardResult forwardResult,
                            boolean isInloopBody) {
        VariableFactor defVar = new VariableFactor(currentCGNode, instr.getDef());
        int rhsVarId = instr.getUse(0);
        boolean isConstant = forwardResult.isConstant(rhsVarId);

        if (isConstant) { // constants on both sides of operator;
            if (instr.getOpcode() instanceof CAstUnaryOp) {
                CAstUnaryOp op = (CAstUnaryOp) instr.getOpcode();

                Object lhsVal = null;
                switch (op) {
                    case MINUS:
                        if (forwardResult.isBoolConstant(rhsVarId)) {
                            boolean rhsVal = forwardResult.getBoolConstant(rhsVarId);
                            lhsVal = -1 * ConstantConverter.toIntValue(rhsVal);
                        } else if (forwardResult.isIntConstant(rhsVarId)) {
                            int rhsVal = forwardResult.getIntConstant(rhsVarId);
                            lhsVal = -1 * rhsVal;
                        } else if (forwardResult.isFloatConstant(rhsVarId)) {
                            float rhsVal = forwardResult.getFloatConstant(rhsVarId);
                            lhsVal = -1 * rhsVal;
                        } else {
                            Assertions.UNREACHABLE();
                        }
                        break;
                    case BITNOT:
                        Assertions.UNREACHABLE();
                        break;
                    case PLUS:
                        Assertions.UNREACHABLE();
                        break;
                    default: Assertions.UNREACHABLE();
                }
                ConstantTerm constant = new ConstantTerm(lhsVal);
                substituteExpForVar(constant, defVar);
            } else if (instr.getOpcode() instanceof IUnaryOpInstruction.Operator) {
                IUnaryOpInstruction.Operator op = (IUnaryOpInstruction.Operator) instr.getOpcode();

                // evaluate!
                if (forwardResult.isBoolConstant(rhsVarId)) {
                    boolean rhs = forwardResult.getBoolConstant(rhsVarId);
                    boolean value = false;
                    switch (op) {
                        case NEG:
                            value = !rhs;
                            break;
                        default:
                            Util.Unimp("unrecognized op" + op);
                    }
                    ConstantTerm constant = new ConstantTerm(value);
                    substituteExpForVar(constant, defVar);
                } else {
                    // TODO: implemenet unhanlded constants; drop ths constraint
                    // Util.Unimp("evaluation of non-integer constant binops in instr " + instr);
                    Assertions.UNREACHABLE();
                    dropConstraintsContaining(defVar);
                    return true;
                }
            }
        } else {
            VariableFactor rhsVar = new VariableFactor(currentCGNode, rhsVarId);
            UnaryOpTerm rhsTerm = new UnaryOpTerm(rhsVar, instr.getOpcode());
            substituteExpForVar(rhsTerm, defVar);
        }

        return isFeasible();
    }

    protected boolean visit(SSABinaryOpInstruction instr,
                            CGNode currentCGNode,
                            ForwardAnalyzer.ForwardResult forwardResult,
                            boolean isInLoopBody) {
        Util.Assert(instr.getNumberOfDefs() == 1, "Expecting only 1 def; found " + instr.getNumberOfDefs());
        Util.Assert(instr.getNumberOfUses() == 2, "Expecting only 2 uses; found " + instr.getNumberOfUses());

        VariableFactor defVar = new VariableFactor(currentCGNode, instr.getDef());
        if (!pathVars.contains(defVar)) return true;

        boolean lhsConstant = forwardResult.isConstant(instr.getUse(0));
        boolean rhsConstant = forwardResult.isConstant(instr.getUse(1));
        int use0 = instr.getUse(0); int use1 = instr.getUse(1);

        if (isInLoopBody) {
            Set<TypeReference> defVarForwardResultTypes = forwardResult.getValueTypes(instr.getDef());

            dropConstraintsContaining(defVar);

            // constants on both sides of operator;
            if (instr.getOperator() instanceof CAstBinaryOp) {
                // This means compare binary operation.
                // So result must be boolean.
                if (defVarForwardResultTypes.size() == 1) {
                    // Manual evaluation using forward result.
                    for (TypeReference defVarForwardResultType: defVarForwardResultTypes) {
                        if (!defVarForwardResultType.equals(TypeReference.Boolean))
                            return false;
                    }
                } else {
                    TypeConstraint generatedConstraint = new TypeConstraint(defVar, TypeReference.Boolean);
                    addConstraint(generatedConstraint);
                }
            } else if (instr.getOperator() instanceof IBinaryOpInstruction.Operator) {
                if (lhsConstant && rhsConstant) {
                    Assertions.UNREACHABLE();
                } else if (lhsConstant) {
                    Set<TypeReference> use1ForwardResultTypes = forwardResult.getValueTypes(use1);
                    if (defVarForwardResultTypes.size() == 1 && use1ForwardResultTypes.size() == 1) {
                        // Manual evaluation using forward result.
                        TypeReference defVarForwardResultType = defVarForwardResultTypes.iterator().next();
                        TypeReference use1ForwardResultType = use1ForwardResultTypes.iterator().next();
                        if (!defVarForwardResultType.equals(use1ForwardResultType))
                            return false;
                    } else {
                        VariableFactor rhs = new VariableFactor(currentCGNode, use1);
                        TypeConstraint generatedConstraint = new TypeConstraint(defVar, rhs);
                        addConstraint(generatedConstraint);
                    }
                } else if (rhsConstant) {
                    Set<TypeReference> use0ForwardResultTypes = forwardResult.getValueTypes(use0);
                    if (defVarForwardResultTypes.size() == 1 && use0ForwardResultTypes.size() == 1) {
                        // Manual evaluation using forward result.
                        TypeReference defVarForwardResultType = defVarForwardResultTypes.iterator().next();
                        TypeReference use0ForwardResultType = use0ForwardResultTypes.iterator().next();
                        if (!defVarForwardResultType.equals(use0ForwardResultType))
                            return false;
                    } else {VariableFactor lhs = new VariableFactor(currentCGNode, use0);
                        TypeConstraint generatedConstraint = new TypeConstraint(defVar, lhs);
                        addConstraint(generatedConstraint);
                    }
                } else {
                    // TODO: This is not really correct, because there is a possibility that in python,
                    // the arguments of arithmetic calculations are not the same type.

                    Set<TypeReference> use0ForwardResultTypes = forwardResult.getValueTypes(use0);
                    if (defVarForwardResultTypes.size() == 1 && use0ForwardResultTypes.size() == 1) {
                        // Manual evaluation using forward result.
                        TypeReference defVarForwardResultType = defVarForwardResultTypes.iterator().next();
                        TypeReference use0ForwardResultType = use0ForwardResultTypes.iterator().next();
                        if (!defVarForwardResultType.equals(use0ForwardResultType))
                            return false;
                    } else {
                        VariableFactor lhs = new VariableFactor(currentCGNode, use0);
                        TypeConstraint generatedConstraint1 = new TypeConstraint(defVar, lhs);
                        addConstraint(generatedConstraint1);
                    }

                    Set<TypeReference> use1ForwardResultTypes = forwardResult.getValueTypes(use1);
                    if (defVarForwardResultTypes.size() == 1 && use1ForwardResultTypes.size() == 1) {
                        // Manual evaluation using forward result.
                        TypeReference defVarForwardResultType = defVarForwardResultTypes.iterator().next();
                        TypeReference use1ForwardResultType = use1ForwardResultTypes.iterator().next();
                        if (!defVarForwardResultType.equals(use1ForwardResultType))
                            return false;
                    } else {
                        VariableFactor rhs = new VariableFactor(currentCGNode, use1);
                        TypeConstraint generatedConstraint2 = new TypeConstraint(defVar, rhs);
                        addConstraint(generatedConstraint2);
                    }
                }
            }
            return isFeasible();
        }

        if (forwardResult.getValueTypes(instr.getDef()).contains(PythonTypes.list)) {
            // TODO: This is hard to make SOUND constraints when 'list' * 'int'
            return true;
        }

        if (lhsConstant && rhsConstant) { // constants on both sides of operator;
            if (instr.getOperator() instanceof CAstBinaryOp) {
                 CAstBinaryOp op = (CAstBinaryOp) instr.getOperator();

                // evaluate!
                if (forwardResult.isIntConstant(use0) && forwardResult.isIntConstant(use1)) {
                    int lhs = forwardResult.getIntConstant(use0);
                    int rhs = forwardResult.getIntConstant(use1);
                    boolean value = true;
                    switch (op) {
                        case EQ: value = lhs == rhs; break;
                        case NE: value = lhs != rhs; break;
                        case GT: value = lhs > rhs; break;
                        case GE: value = lhs >= rhs; break;
                        case LT: value = lhs < rhs; break;
                        case LE: value = lhs <= rhs; break;
                        default: Util.Unimp("unrecognized op" + op);
                    }
                    ConstantTerm constant = new ConstantTerm(value);
                    substituteExpForVar(constant, defVar);
                } else {
                    // TODO: implemenet unhanlded constants; drop ths constraint
                    // Util.Unimp("evaluation of non-integer constant binops in instr " + instr);
                    Assertions.UNREACHABLE();
                    dropConstraintsContaining(defVar);
                    return true;
                }
            } else if (instr.getOperator() instanceof IBinaryOpInstruction.Operator) {
                BinaryOpInstruction.Operator op = (BinaryOpInstruction.Operator) instr.getOperator();
                // TODO: not currently supporting XOR or bitwise AND/OR; would require using Z3 bitvector theory
                if (op == BinaryOpInstruction.Operator.XOR || op == BinaryOpInstruction.Operator.AND || op == BinaryOpInstruction.Operator.OR) {
                    Assertions.UNREACHABLE();
                    // // TODO: even if we don't want to represent everything as Z3 bitvectors, we can just translate this constraint to "true:
                    // // TODO: in Z3 and keep our own local representation so we can do bw and etc on constants
                    // if (Options.DEBUG)
                    //     Util.Debug("dropping constraints due to unsupported op " + op);
                    // dropConstraintsContaining(varName);
                    // return true;
                    // // Util.Unimp("operator " + op);
                }

                // evaluate!
                if (forwardResult.isIntConstant(use0) && forwardResult.isIntConstant(use1)) {
                    int lhs = forwardResult.getIntConstant(use0);
                    int rhs = forwardResult.getIntConstant(use1);
                    int value = -1;
                    switch (op) {
                        case ADD: value = lhs + rhs; break;
                        case SUB: value = lhs - rhs; break;
                        case MUL: value = lhs * rhs; break;
                        case DIV: value = lhs / rhs; break;
                        case REM: value = lhs % rhs; break;
                        case XOR: value = lhs ^ rhs; break;
                        case AND: value = lhs & rhs; break;
                        default: Util.Unimp("unrecognized op" + op);
                    }
                    ConstantTerm constant = new ConstantTerm(value);
                    substituteExpForVar(constant, defVar);
                } else {
                    // TODO: implemenet unhanlded constants; drop ths constraint
                    // Util.Unimp("evaluation of non-integer constant binops in instr " + instr);
                    Assertions.UNREACHABLE();
                    dropConstraintsContaining(defVar);
                    return true;
                }
            }
        } else if (lhsConstant) { // constant on left side of binary operator only
            VariableTerm var1;
            if (forwardResult.getValue(use1).isBottom()) {
                var1 = new VariableTerm(new VariableFactor(currentCGNode, use1));
            } else {
                var1 = new VariableTerm(
                        new VariableFactor(currentCGNode, use1), forwardResult.getValue(use1)
                );
            }
            BinOpTerm binExp = null;
            if (forwardResult.isNullConstant(use0)) {
                binExp = new BinOpTerm((ITerm) var1, ConstantTerm.NULL, instr.getOperator());
            } else {
                binExp = new BinOpTerm(var1, forwardResult.getConstant(use0), instr.getOperator());
            }
            substituteExpForVar(binExp, defVar);
        } else if (rhsConstant) { // constant on right of binary operator only
            VariableTerm var0;
            if (forwardResult.getValue(use0).isBottom()) {
                var0 = new VariableTerm(new VariableFactor(currentCGNode, use0));
            } else {
                var0 = new VariableTerm(
                        new VariableFactor(currentCGNode, use0), forwardResult.getValue(use0)
                );
            }
            BinOpTerm binExp = null;
            if (forwardResult.isNullConstant(use1)) {
                binExp = new BinOpTerm((ITerm) var0, ConstantTerm.NULL, instr.getOperator());
            } else {
                binExp = new BinOpTerm(var0, forwardResult.getConstant(use1), instr.getOperator());
            }
            substituteExpForVar(binExp, defVar);
        } else { // no constants
            VariableTerm lhsVar;
            if (forwardResult.getValue(use0).isBottom()) {
                lhsVar = new VariableTerm(new VariableFactor(currentCGNode, use0));
            } else {
                lhsVar = new VariableTerm(
                        new VariableFactor(currentCGNode, use0), forwardResult.getValue(use0)
                );
            }
            VariableTerm rhsVar;
            if (forwardResult.getValue(use1).isBottom()) {
                rhsVar = new VariableTerm(new VariableFactor(currentCGNode, use1));
            } else {
                rhsVar = new VariableTerm(
                        new VariableFactor(currentCGNode, use1), forwardResult.getValue(use1)
                );
            }
            BinOpTerm binExp = new BinOpTerm(lhsVar, rhsVar, instr.getOperator());
            substituteExpForVar(binExp, defVar);
        }

        return isFeasible();
    }

    protected boolean visit(SSANewInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        // If the allocated object is not what you don't interested in, skip this instruction.
        if (!isInterestedNew(instr)) return true;

        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());
        if (pathVars.contains(lhs)) {
            IClassHierarchy cha = currentCGNode.getMethod().getClassHierarchy();
            boolean isClassCall = cha.isSubclassOf(
                    cha.lookupClass(currentCGNode.getMethod().getDeclaringClass().getReference()),
                    cha.lookupClass(PythonTypes.object)
            );

            ConstantTypeTerm rhsTerm;
            if (isClassCall) {
                // Callee is class so created object is instance.
                rhsTerm = new ConstantTypeTerm(
                        TypeReference.find(
                                PythonTypes.pythonLoader,
                                currentCGNode.getMethod().getDeclaringClass().getReference()
                                        .getName().toString() + "_instance"
                        )
                );
            } else {
                rhsTerm = new ConstantTypeTerm(instr.getConcreteType());
            }

            // Check this allocate point is contained in 'cantEEGDrop'. If contained, this means
            // that there is a possibility that you haven't generate some constraints for the list's
            // element. But in order to drop EEG constraint, you have to generate constraints
            // for all 'may' elements. So you can't drop EEG constraint.
            if (!cantEEGDrop.contains(new AllocatePoint(currentCGNode, instr))) {
                dropEEGConstraint(lhs);
            }

            substituteExpForVar(rhsTerm, lhs);
            return isFeasible();
        }
        return true;
    }

    private boolean isInterestedNew(SSANewInstruction instr) {
        return !instr.getNewSite().getDeclaredType().getName().toString().startsWith("Lwala/builtin");
    }

    // The set of allocate point where you can't drop constraints having each element get instruction
    // for the list created in.
    protected Set<AllocatePoint> cantEEGDrop;
    public void addCantEEGDrop(AllocatePoints allocatePoints) {
        this.cantEEGDrop.addAll(allocatePoints);
    }

    /**
     * Drop the constraint which contains the 'EachElementGet' instruction. The constraint have the form
     * of 'lhs.each_element_get'.
     * @param lhs the object variable.
     */
    private void dropEEGConstraint(VariableFactor lhs) {
        for (IConstraint constraint: constraints) {
            if (constraint.getVars().contains(lhs)) {
                ITerm constraintLhs = constraint.getLhs();
                Collection<? extends IProgramFactor> constraintLhsVars = constraintLhs.getVars();

                for (IProgramFactor constraintLhsVar: constraintLhsVars) {
                    while (constraintLhsVar instanceof FieldVarFactor) {
                        FieldVarFactor constraintLhsFieldVar = (FieldVarFactor) constraintLhsVar;
                        if (constraintLhsFieldVar instanceof NotConstantFieldVarFactor
                                && ((NotConstantFieldVarFactor) constraintLhsFieldVar).getFieldFactor()
                                        instanceof EachElementGetVarFactor
                                && constraintLhsFieldVar.getValFactor().equals(lhs)) {
                            this.feasible = false;
                        }
                        IProgramFactor valVar = constraintLhsFieldVar.getValFactor();
                        constraintLhsVar = valVar;
                    }
                }
            }
        }
    }

    protected boolean visit(SSACheckCastInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());
        VariableFactor rhs = new VariableFactor(currentCGNode, instr.getVal());

        for (TypeReference forwardResultLhsType: forwardResult.getValueTypes(instr.getDef())) {
            for (TypeReference t: instr.getDeclaredResultTypes()) {
                // Manual evaluation!
                if (forwardResultLhsType.equals(t)) {
                    continue;
                } else {
                    // If lhs's type is not equal to cast type, this path is infeasible.
                    return false;
                }
            }
        }

        if (pathVars.contains(lhs)) {
            substituteExpForVar(new VariableTerm(rhs), lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(SSAReturnInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        int resultNum = instr.getResult();

        if (resultNum >= 0) { // if the function is a non-void function
            ReturnVarFactor retVarFactor = new ReturnVarFactor(currentCGNode);
            if (pathVars.contains(retVarFactor)) {
                if (forwardResult.isConstant(resultNum)) {
                    ConstantTerm returnValTerm =
                            new ConstantTerm(forwardResult.getConstant(instr.getResult()));
                    substituteExpForVar(returnValTerm, retVarFactor);
                } else {
                    VariableFactor result = new VariableFactor(currentCGNode, resultNum);
                    substituteExpForVar(new VariableTerm(result), retVarFactor);
                }
                return isFeasible();
            }
        }
        return true; // didn't add any constraints, can't be infeasible
    }

    protected boolean visit(AstGlobalRead instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());
        if (pathVars.contains(lhs)) {
            GlobalVarFactor rhs = new GlobalVarFactor(instr.getDeclaredField());
            substituteExpForVar(new VariableTerm(rhs), lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(SSAGetInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());
        if (pathVars.contains(lhs)) {
            VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getUse(0));
            StringFieldVarFactor rhs =
                    new StringFieldVarFactor(valFactor, instr.getDeclaredField().getName().toString());
            substituteExpForVar(new VariableTerm(rhs), lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(AstGlobalWrite instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        GlobalVarFactor lhs = new GlobalVarFactor(instr.getDeclaredField());
        if (pathVars.contains(lhs)) {
            VariableFactor rhs = new VariableFactor(currentCGNode, instr.getVal());
            substituteExpForVar(new VariableTerm(rhs), lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(SSAPutInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getRef());
        StringFieldVarFactor lhs = new StringFieldVarFactor(
                valFactor, instr.getDeclaredField().getName().toString()
        );
        if (pathVars.contains(lhs)) {
            VariableFactor rhs = new VariableFactor(currentCGNode, instr.getVal());
            substituteExpForVar(new VariableTerm(rhs), lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(PythonPropertyRead instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        int fieldVarId = instr.getMemberRef();
        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());

        if (pathVars.contains(lhs)) {
            ITerm rhsTerm = null;
            if (forwardResult.isStringConstant(fieldVarId)) {
                String attr = forwardResult.getStringConstant(fieldVarId);

                VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
                FieldVarFactor rhs = new StringFieldVarFactor(valFactor, attr);
                rhsTerm = new VariableTerm(rhs);

                // In order to optimize constraints, when the instruction is 'Z' = 'X.Y', if 'X' and
                // 'X.Y.$self' point same objects, replace 'Z.$self' with 'X'. For example, if there
                // is an instance 'X' which has method 'Y', the '$self' attribute's value of method
                // 'Y' is 'X'. This means 'Z.$self' (= 'X.Y.$self') is the same to 'X'. So replace
                // 'Z.$self' with 'X'.
                StringFieldVarFactor lhsSelfAttrVar = new StringFieldVarFactor(lhs, "$self");
                if (pathVars.contains(lhsSelfAttrVar)) {
                    ForwardState forwardState = forwardResult.getState();
                    ForwardAbstractValue value1 = (ForwardAbstractValue) forwardState.getValue(instr.getObjectRef());
                    ForwardAbstractValue value1AttrValues = new ForwardAbstractValue();
                    for (ObjectValue value1Obj :
                            value1.getAllocatePoints().getObjectsIterable(forwardState.getAllocatePointTable())) {
                        if (value1Obj.hasAttr(attr, forwardState.getAllocatePointTable())) {
                            value1AttrValues.union((ForwardAbstractValue) value1Obj.getAttr(attr, forwardState.getAllocatePointTable()));
                        }
                    }

                    ForwardAbstractValue value2 = new ForwardAbstractValue();
                    for (ObjectValue value1AttrSelfObj :
                            value1AttrValues.getAllocatePoints().getObjectsIterable(forwardState.getAllocatePointTable())) {
                        if (value1AttrSelfObj.hasAttr("$self", forwardState.getAllocatePointTable())) {
                            value2.union((ForwardAbstractValue) value1AttrSelfObj.getAttr("$self", forwardState.getAllocatePointTable()));
                        }
                    }

                    if (value1.isSame(value2)) {
                        substituteExpForVar(new VariableTerm(valFactor), lhsSelfAttrVar);
                    }
                }
            } else if (forwardResult.isIntConstant(fieldVarId)) {
                VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
                FieldVarFactor rhs = new IntConstFieldVarFactor(
                        valFactor, forwardResult.getIntConstant(fieldVarId)
                );
                rhsTerm = new VariableTerm(rhs);
            } else {
                VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
                VariableFactor fieldFactor = new VariableFactor(currentCGNode, fieldVarId);
                NotConstantFieldVarFactor rhs = new NotConstantFieldVarFactor(valFactor, fieldFactor);
                rhsTerm = new ComplexVariableTerm(rhs);
            }

            substituteExpForVar(rhsTerm, lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(PythonPropertyWrite instr,
                            CGNode currentCGNode,
                            ForwardAnalyzer.ForwardResult forwardResult) {
        int fieldVarId = instr.getMemberRef();
        FieldVarFactor lhs;
        if (forwardResult.isStringConstant(fieldVarId)) {
            VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
            lhs = new StringFieldVarFactor(
                    valFactor, forwardResult.getStringConstant(fieldVarId)
            );
        } else if (forwardResult.isIntConstant(fieldVarId)) {
            VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
            lhs = new ConstIntFieldVarFactor(valFactor, forwardResult.getIntConstant(fieldVarId));
        } else {
            VariableFactor valFactor = new VariableFactor(currentCGNode, instr.getObjectRef());
            VariableFactor fieldFactor = new VariableFactor(currentCGNode, fieldVarId);
            lhs = new NotConstantFieldVarFactor(valFactor, fieldFactor);
        }

        ITerm rhsTerm = new VariableTerm(new VariableFactor(currentCGNode, instr.getValue()));

        Collection<IProgramFactor> commonFactors = HashSetFactory.make(pathVars);
        commonFactors.retainAll(lhs.getFactors());
        if (commonFactors.size() > 0) {
            for (IProgramFactor var: pathVars) {
                if (var instanceof NotConstantFieldVarFactor) {
                    NotConstantFieldVarFactor notConstantFieldVar = (NotConstantFieldVarFactor) var;
                    // Filter by the equation of 'val' variable.
                    if (!notConstantFieldVar.getValFactor().equals(lhs.getValFactor())) continue;

                    if (notConstantFieldVar.getFieldFactor() instanceof EachElementGetVarFactor) {
                        AbstractPypstaQuery dupQuery = (AbstractPypstaQuery) deepCopy();
                        dupQuery.substituteExpForVar(new VariableTerm(lhs), notConstantFieldVar);
                        dupQuery.substituteExpForVar(rhsTerm, lhs);
                        stackedQueries.add(dupQuery);
                    }
                }
            }
            substituteExpForVar(rhsTerm, lhs);
            return isFeasible();
        }
        return true;
    }

    protected boolean visit(EachElementGetInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        EachElementGetVarFactor var =
                new EachElementGetVarFactor(currentCGNode, instr.getUse(0), instr.getDef());
        VariableFactor oldVar = new VariableFactor(currentCGNode, instr.getDef());
        if (pathVars.contains(oldVar)) {
            substituteExpForVar(new VariableTerm(var), oldVar);
            return isFeasible();
        }
        return true;
    }

    private static final SSAIdTable ssaIdTable = new SSAIdTable();
    boolean visit(AstLexicalRead instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        for (AstLexicalAccess.Access access: instr.getAccesses()) {
            VariableFactor lhs = new VariableFactor(currentCGNode, access.valueNumber);
            if (pathVars.contains(lhs)) {
                LexicalVarFactor rhs = new LexicalVarFactor(
                        currentCGNode, access, ssaIdTable.read(access.variableName)
                );
                substituteExpForVar(new VariableTerm(rhs), lhs);
                return isFeasible();
            }
        }
        return true;
    }

    boolean visit(AstLexicalWrite instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        for (AstLexicalAccess.Access access: instr.getAccesses()) {
            LexicalVarFactor lhs = new LexicalVarFactor(
                    currentCGNode, access, ssaIdTable.write(access.variableName)
            );
            if (pathVars.contains(lhs)) {
                VariableFactor rhs = new VariableFactor(currentCGNode, access.valueNumber);
                substituteExpForVar(new VariableTerm(rhs), lhs);
                return isFeasible();
            }
        }
        return true;
    }

    private static class SSAIdTable {
        private final HashMap<String, Integer> table = new HashMap<>();

        public int read(String key) {
            if (table.containsKey(key)) {
                return table.get(key);
            } else {
                table.put(key, 0);
                return 0;
            }
        }

        public int write(String key) {
            if (table.containsKey(key)) {
                int id = table.get(key);
                table.put(key, id+1);
                return id;
            } else {
                table.put(key, 0);
                return 0;
            }
        }
    }

    boolean visit(AstAssertInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        if (forwardResult.isConstant(instr.getUse(0))) {
            // Manual evaluation.
            if (forwardResult.getBoolConstant(instr.getUse(0))) {
                return true;
            } else {
                return false;
            }
        } else {
            VariableFactor var = new VariableFactor(currentCGNode, instr.getUse(0));
            ValueConstraint valueConstraint = new ValueConstraint(
                    var, instr.isFromSpecification(), IConditionalBranchInstruction.Operator.EQ
            );
            addConstraint(valueConstraint);
        }
        return isFeasible();
    }

    boolean visit(AstIsDefinedInstruction instr, CGNode currentCGNode, ForwardAnalyzer.ForwardResult forwardResult) {
        VariableFactor lhs = new VariableFactor(currentCGNode, instr.getDef());
        if (pathVars.contains(lhs)) {
            ConstantTypeTerm lhsType = new ConstantTypeTerm(TypeReference.Boolean);
            substituteExpForVar(lhsType, lhs);
            return isFeasible();
        }

        return true;
    }

    boolean visit(PythonInvokeInstruction instr, CGNode callee, CGNode caller) {
        if (instr.hasDef()) {
            VariableFactor lhs = new VariableFactor(caller, instr.getDef());
            ReturnVarFactor retVar = new ReturnVarFactor(callee);

            IClassHierarchy cha = callee.getMethod().getClassHierarchy();

            IClass calleeCls = cha.lookupClass(callee.getMethod().getDeclaringClass().getReference());
            IClass objectCls = cha.lookupClass(PythonTypes.object);

            if (calleeCls != null && cha.isSubclassOf(calleeCls, objectCls)) {
                // If you invoke class call, substitute the instance's attributes.
                IClass cls = calleeCls;
                while (!cls.equals(objectCls) && (cls instanceof PythonLoader.PythonClass)) {
                    boolean substituted = false;

                    PythonLoader.PythonClass cls1 = (PythonLoader.PythonClass) cls;
                    for (MethodReference m: cls1.getMethodReferences()) {
                        String methodName = m.getName().toString();
                        StringFieldVarFactor methodVar = new StringFieldVarFactor(lhs, methodName);
                        if (pathVars.contains(methodVar)) {
                            // Substitute 'val.method' to 'trampMethodTmpVar'.
                            VariableFactor trampMethodTmpVar = new VariableFactor(callee, -1);
                            substituted |= substituteExpForVar(
                                    new VariableTerm(trampMethodTmpVar), methodVar);

                            // Substitute 'trampMethodTmpVar.$self' to 'lhs', which the instance
                            // is assigned to.
                            StringFieldVarFactor trampSelfVar
                                    = new StringFieldVarFactor(trampMethodTmpVar, "$self");
                            substituted |= substituteExpForVar(new VariableTerm(lhs), trampSelfVar);

                            // Substitute 'trampMethodTmpVar.$function' to the method's type.
                            StringFieldVarFactor trampFuncVar
                                    = new StringFieldVarFactor(trampMethodTmpVar, "$function");
                            substituted |= substituteExpForVar(
                                    new ConstantTerm(m.getDeclaringClass()), trampFuncVar);

                            TypeReference trampMethodType = TypeReference.findOrCreate(
                                    PythonTypes.pythonLoader,
                                    m.getDeclaringClass().getName().toString().replace("Lscript", "L$script")
                            );
                            substituted |= substituteExpForVar(new ConstantTypeTerm(trampMethodType), trampMethodTmpVar);
                        }
                    }

                    if (substituted) {
                        // If the substitutions are occurred, do attribute's substitution
                        // from child class again.
                        cls = calleeCls;
                    } else {
                        cls = cls.getSuperclass();
                    }
                }
            }

            if (pathVars.contains(lhs)) {
                substituteExpForVar(new VariableTerm(retVar), lhs);
            }
            // There is a possibility that attributes substitutions are occurred, so check feasibility.
            return isFeasible();
        }
        return true; // didn't add any constraints, can't be infeasible
    }

    public boolean substituteExpForVar(ITerm toTerm, IProgramFactor from) {
        // 高速化のため、subFor変数をtoSubに置き換え、総変数数を少なくする
        if (Options.DEBUG)
            Util.Debug("subsExpForVar subbing " + toTerm + " for " + from);
        VariableTerm fromTerm = new VariableTerm(from);

        Set<IConstraint> toAdd = HashSetFactory.make();
        List<IConstraint> toRemove = new LinkedList<>();

        for (IConstraint constraint : constraints) {
            // AtomicPathConstraints are (almost) pure, so we can't do substitution in-place
            Set<IConstraint> newConstraints = constraint.substitute(toTerm, fromTerm);

            if (newConstraints == AbstractConstraint.FALSE) {
                // The 'constraint' makes FALSE, so there is no witness and refuted.
                if (Options.DEBUG || Options.PRINT_REFS)
                    Util.Debug("refuted " + this + " by " + constraint);
                makeUnsatCore(constraint);
                this.feasible = false;
                return false;
            } else if (newConstraints == AbstractConstraint.TRUE) {
                // The 'constraint' makes no means, so delete the 'constraint'.
                toRemove.add(constraint);
            } else {
                long substitutedNum = newConstraints.stream().filter(IConstraint::isSubstituted).count();
                if (substitutedNum == 0L) {
                    assert newConstraints.size() == 1;
                } else {
                    for (IConstraint newConstraint: newConstraints) {
                        // We assume that the all constraints in 'newConstraints' are substituted.
                        assert newConstraint.isSubstituted();

                        toAdd.add(newConstraint);
                    }
                    toRemove.add(constraint);
                }
            }
        }

        // remove old (pre-substitution) constraints
        for (IConstraint constraint : toRemove) {
            removeConstraint(constraint);
        }

        // add new (post-substitution) constraints
        for (IConstraint constraint : toAdd) {
            addConstraint(constraint);
        }
        return !toRemove.isEmpty() || !toAdd.isEmpty();
    }

    @Override
    public List<IQuery> visitPhi(SSAPhiInstruction instr, int phiIndex, IPathInfo currentPath) {
        CGNode currentNode = currentPath.getCurrentNode();

        // Get rhs's type from forward result.
        ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                currentNode, currentNode.getIR().getBasicBlockForInstruction(instr)
        );

        // lhsVar is the x in x = phi(y,z)
        VariableFactor lhsVar = new VariableFactor(currentNode, instr.getDef());

        if (pathVars.contains(lhsVar)) {
            Util.Assert(instr.getNumberOfDefs() == 1, "expecting one def");

            int useId = instr.getUse(phiIndex);
            if (WALACFGUtil.isInLoopBody(currentPath.getCurrentBlock(), currentNode.getIR())){
                // There is a possibility that Phi instruction in loop makes infinite loop.
                // For example,
                //   x = ~
                //   while ~:
                //     y = x[a]
                //     x = Phi(x, y)
                // This make the variable, (((x[a])[a])[a]).....
                // So in Phi instruction in loop, you replace lhs to not rhs variable
                // but rhs variable's type.

                ForwardAbstractValue rhsAbstractValue = forwardResult.getValue(useId);

                Set<TypeReference> rhsTypes = new HashSet<>();
                for (ObjectValue objectValue:
                        rhsAbstractValue.getAllocatePoints().getObjectsIterable(forwardResult.getState().getAllocatePointTable())) {
                    rhsTypes.add(objectValue.getTypeReference());
                }
                if (!rhsAbstractValue.getBoolValue().isBottom()) {
                    rhsTypes.add(TypeReference.Boolean);
                }
                if (!rhsAbstractValue.getIntValue().isBottom()) {
                    rhsTypes.add(TypeReference.Int);
                }
                if (!rhsAbstractValue.getFloatValue().isBottom()) {
                    rhsTypes.add(TypeReference.Float);
                }
                if (!rhsAbstractValue.getStringValue().isBottom()) {
                    rhsTypes.add(PythonTypes.string);
                }

                if (rhsTypes.size() == 1) {
                    ConstantTypeTerm attrType = new ConstantTypeTerm(rhsTypes.iterator().next());
                    substituteExpForVar(attrType, lhsVar);
                } else {
                    if (rhsAbstractValue.getNoneValue().isTop()) {
                        // TODO:
                        // substituteExpForVar(ConstantTerm.NULL, lhsVar);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }
            } else {
                ITerm toTerm;
                if (forwardResult.isConstant(useId)) {
                    if (forwardResult.isNullConstant(useId)) {
                        toTerm = ConstantTerm.NULL;
                    } else {
                        toTerm = new ConstantTerm(forwardResult.getConstant(useId));
                    }
                } else {
                    toTerm = new VariableTerm(new VariableFactor(currentNode, useId));
                }

                // sub the LHS of the phi for the appropriate term on the right
                substituteExpForVar(toTerm, lhsVar);
            }

            isFeasible();
            // if (!isFeasible()) {
                // When you return the INFEASIBLE and all phi paths are solved as infeasible,
                // the `splitPath` variable becomes empty. This causes that you can not enter
                // the path of `PathSensitiveSymbolicExecutor@L215`, but you enter the path of
                // `PathSensitiveSymbolicExecutor@L282`, you split path and continue backward.
                // So you don't return INFEASIBLE.
                // this.feasible = false;
                // return IQuery.INFEASIBLE;
            // }
        }
        return IQuery.FEASIBLE;
    }

    @Override
    public boolean containsStaleConstraints(CGNode currentNode) {
        return false;
    }

    @Override
    public void declareFakeWitness() {
        Assertions.UNREACHABLE();
    }

    @Override
    public boolean isCallRelevant(SSAInvokeInstruction instr, CGNode caller, CGNode callee, CallGraph cg) {
        Assertions.UNREACHABLE();
        return false;
    }

    @Override
    public boolean isFeasible() {
        if (!feasible) {
            // if (!deleted) ctx.delete(); //occasionally causes Z3 to die
            return false;
        }

        if (z3Panic) {
            // problem with z3 -- don't try to use it to check constraints
            return true;
        }

        if (currentPathAssumption == null) return true;

        // call Z3 to check for feasibility
        BoolExpr[] assumptionsArr = new BoolExpr[] { currentPathAssumption };
        Status status = null;
        Util.Debug("Start constraints solving");
        try {
            status = solver.check(assumptionsArr);
        } catch (Z3Exception e) {
            Util.Assert(false, "problem with z3 " + e);
        }
        Util.Debug("Finish constraints solving");

        if (status == Status.UNKNOWN) {
            if (Options.DEBUG) Util.Debug("Z3 decidability problem. giving up on z3 checking");
            // z3 can't solve our current constraints. give up
            z3Panic = true;
            return true;
        } else if (status == Status.UNSATISFIABLE) {
            this.feasible = false;
            if (Options.DEBUG || Options.PRINT_REFS) Util.Debug("refuted by path constraint (z3)!");
            return false;
        }
        // else, sat
        return true;
    }

    @Override
    public void dropConstraintsContaining(Collection<? extends IProgramFactor> factors) {
        for (IProgramFactor factor: factors) {
            dropConstraintsContaining(factor);
        }
    }

    private void dropConstraintsContaining(IProgramFactor var) {
        Set<IConstraint> toRemove = HashSetFactory.make();
        for (IConstraint constraint: constraints) {
            if (constraint.getVars().contains(var)) {
                toRemove.add(constraint);
            }
        }
        constraints.removeAll(toRemove);
    }

    @Override
    public void dropConstraintsProduceableInCall(SSAInvokeInstruction instr, CGNode caller, CGNode callee, boolean dropPtConstraints) {
        Assertions.UNREACHABLE();
    }

    @Override
    public void dropReturnValueConstraintsForCall(SSAInvokeInstruction instr, CGNode caller) {
        Assertions.UNREACHABLE();
    }

    @Override
    public boolean addContextualConstraints(CGNode node, IPathInfo currentPath) {
        // Context sensitiveに行った場合に、追加の制約があるならここで追加する
        // {@link PointsToQuery#addContextualConstraints(CGNode, IPathInfo)}
        // infeasibleな場合にのみfalseを返す
        return true;
    }

    @Override
    public boolean isDispatchFeasible(SSAInvokeInstruction instr, CGNode caller, CGNode callee) {
        Assertions.UNREACHABLE();
        return false;
    }

    /**
     * Can instr dispatch constraints from caller to callee?
     * @param instr the invoked instruction
     * @param caller the caller call graph node
     * @param callee the callee call graph node
     * @return return true if the witness of this query is feasible
     */
    @Override
    public boolean isDispatchFeasible(PythonInvokeInstruction instr, CGNode caller, CGNode callee) {
        return true;
    }

    @Override
    public void removeAllLocalConstraints() {
        // TODO:
    }

    @Override
    public AbstractDependencyRuleGenerator getDepRuleGenerator() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public Iterator<? extends Constraint> constraints() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public Set<FieldReference> getFields() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public void removeAllNonClinitConstraints() {
        Assertions.UNREACHABLE();
    }

    @Override
    public Map<Constraint, Set<CGNode>> getModifiersForQuery() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public void removeLoopProduceableConstraints(SSACFG.BasicBlock loopHead, CGNode currentNode) {
        // loop中で生成された制約の中で、不必要なものを消す
        // return;
    }

    @Override
    public boolean containsLoopProduceableConstraints(SSACFG.BasicBlock loopHead, CGNode currentNode) {
        Assertions.UNREACHABLE();
        return false;
    }

    @Override
    public void enterCallFromJump(CGNode callee) {
        Assertions.UNREACHABLE();
    }

    protected void makeUnsatCore(IConstraint constraint) {
        unsatCore = new LinkedList<>();
        unsatCore.add(constraint);
    }

    @Override
    public List<DependencyRule> getWitnessList() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public Map<Constraint, Set<CGNode>> getRelevantNodes() {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public boolean initializeInstanceFieldsToDefaultValues(CGNode constructor) {
        // In Python instance attributes are initialized in '__init__' method, so we need to
        // initialize in this.
        // TODO: But we need to initialize method attributes in this method.
        return true;
    }

    @Override
    public PointerVariable getPointedTo(PointerVariable var) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public boolean initializeStaticFieldsToDefaultValues() {
        // TODO: We need to initialize class attributes in this method.
        return true;
    }

    @Override
    public void dispose() {
        ctx.close();
    }


    private boolean foundWitness = true;

    @Override
    public void setFoundWitness(boolean foundWitness) {
        this.foundWitness = foundWitness;
    }

    @Override
    public boolean isFoundWitness() {
        return foundWitness;
    }

    @Override
    public void dropConstraintsProduceableInCall(
            PythonInvokeInstruction instr, CGNode caller, CGNode callee, boolean dropPtConstraints) {

    }

    @Override
    public List<IQuery> enterCall(PythonInvokeInstruction instr, CGNode callee, IPathInfo currentPath) {
        boolean result = substituteFormalsForActuals(instr, currentPath.getCurrentNode(), callee);
        if (!result)
            return IQuery.INFEASIBLE;

        result = visit(instr, callee, currentPath.getCurrentNode());
        if (!result)
            return IQuery.INFEASIBLE;
        return IQuery.FEASIBLE;
    }

    @Override
    public List<IQuery> returnFromCall(PythonInvokeInstruction instr, CGNode callee, IPathInfo currentPath, boolean backward) {
        if (callee.getMethod().getReference().getDeclaringClass().getName()
                .getClassName().toString().equals("__init__")) {
            // If the callee is "__init__" method.
            if (!initializeInstanceFieldsToDefaultValues(callee))
                return IQuery.INFEASIBLE;
        }

        // done initializing to default values; now do substitution
        if (!substituteActualsForFormals(
                instr, currentPath.getCurrentNode(), callee,
                currentPath.getCurrentNode().getIR().getSymbolTable())) {
            return IQuery.INFEASIBLE;
        }

        TypeReference funcType = callee.getMethod().getReference().getDeclaringClass();
        if (AriadneSupporter.isObjectSpecialMethod(funcType.getName().getClassName().toString())) {
            // If calling special method, there is no variable id for the function object. So don't add constraints.
            return IQuery.FEASIBLE;
        } else {
            // Add the constraint of called function object type. (Use for)
            ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                    currentPath.getCurrentNode(), currentPath.getCurrentBlock()
            );
            Set<TypeReference> forwardResultFuncTypes = forwardResult.getValueTypes(instr.getUse(0));
            if (forwardResultFuncTypes.size() == 1) {
                // Manual evaluation using forward result.
                TypeReference forwardResultFuncType = forwardResultFuncTypes.iterator().next();
                if (forwardResultFuncType.equals(funcType)) {
                    return IQuery.FEASIBLE;
                } else {
                    return IQuery.INFEASIBLE;
                }
            } else if (forwardResultFuncTypes.size() > 1) {
                VariableFactor funcVar = new VariableFactor(currentPath.getCurrentNode(), instr.getUse(0));
                addConstraint(new TypeConstraint(funcVar, funcType));

                Util.Post(!this.containsStaleConstraints(callee), "should not contain stale constraints after substitution!");
                return IQuery.FEASIBLE;
            } else {
                Assertions.UNREACHABLE();
                return null;
            }
        }
    }

    protected boolean substituteActualsForFormals(PythonInvokeInstruction instr,
                                                  CGNode callerNode,
                                                  CGNode calleeNode,
                                                  SymbolTable symbolTable) {
        if (Options.DEBUG) Util.Debug("substituting actuals for formals in path query");

        for (int i = 0; i < instr.getNumberOfPositionalParameters(); i++) {
            int useNum = instr.getUse(i);

            VariableFactor actual = new VariableFactor(callerNode, useNum);
            VariableFactor formal = new VariableFactor(calleeNode, i + 1);
            VariableTerm actualTerm = new VariableTerm(actual);
            substituteExpForVar(actualTerm, formal);
        }
        return isFeasible();
    }

    /**
     * substitute formals for actuals in our constraint set (i.e., when entering call
     * @param instr the function call instruction
     * @param callerNode the caller call graph node
     * @param calleeNode the callee call graph node
     * @return true if this query is feasible, false if infeasible
     */
    protected boolean substituteFormalsForActuals(PythonInvokeInstruction instr,
                                                  CGNode callerNode,
                                                  CGNode calleeNode) {
        if (Options.DEBUG) Util.Debug("substituting formals for actuals in path query");

        for (int i = 0; i < instr.getNumberOfPositionalParameters(); i++) {
            int useNum = instr.getUse(i);

            VariableFactor actual = new VariableFactor(callerNode, useNum);
            VariableFactor formal = new VariableFactor(calleeNode, i + 1);
            VariableTerm formalTerm = new VariableTerm(formal);
            substituteExpForVar(formalTerm, actual);
        }
        return isFeasible();
    }

    @Override
    public Set<IProgramFactor> getPathVars() {
        return pathVars;
    }

    @Override
    public Set<Integer> getDefBBIds() {
        return defBBIds;
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return useBBIds;
    }

    @Override
    public boolean hasNoDUInfoVar() {
        return hasNoDUInfoVar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractPypstaQuery that = (AbstractPypstaQuery) o;
        return Objects.equals(constraints, that.constraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints);
    }
}
