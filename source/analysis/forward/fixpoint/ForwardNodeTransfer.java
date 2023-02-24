package analysis.forward.fixpoint;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.tracer.MakingExceptionDataTracer;
import client.engine.PypstaAnalysisEngine;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.INodeWithNumber;

public class ForwardNodeTransfer extends UnaryOperator<ForwardState> {
    private final ISSABasicBlock basicBlock;

    public ForwardNodeTransfer(ISSABasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    /**
     * Update the state based on value flow graph node's transfer function.
     * @param lhs new (OUT) state
     * @param rhs old (IN) state
     * @return whether change is occurred
     */
    @Override
    public byte evaluate(ForwardState lhs, ForwardState rhs) {
        if (lhs == null) {
            throw new IllegalArgumentException("lhs is null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("rhs is null");
        }

        ForwardState result = new ForwardState(lhs.getSolver(), lhs.getBasicBlock(), false);
        result.copyState(rhs);

        ForwardInstructionVisitor visitor = ForwardInstructionVisitor.instance();
        visitor.setForwardState(result);

        if (PypstaAnalysisEngine.DEBUG) {
            System.out.println(ForwardFixSolver.indent + "Start visit: "
                + lhs + "(" + lhs.getSolver().getMethod().getDeclaringClass().getName() + ")");
        }

        // for debug
        if (PypstaAnalysisEngine.DEBUG) {
            if (lhs.getBasicBlock().getGraphNodeId() == 1
                    && lhs.getSolver().getCGNode().getMethod().getReference().getDeclaringClass()
                    .getName().toString().startsWith("Lscript")
                    && lhs.getSolver().getCGNode().getMethod().getReference().getDeclaringClass()
                    .getName().toString().endsWith("SimpleSurface/colourAt"))
                System.out.println("");
        }

        for (SSAInstruction inst: Iterator2Iterable.make(basicBlock.iterator())) {
            MakingExceptionDataTracer.currentInst = inst;
            MakingExceptionDataTracer.currentState = lhs;
            inst.visit(visitor);
        }

        boolean hasChanged = lhs.union(result);

        // If there is a self recursion, update return value of self recursion.
        if (hasChanged && lhs.getBasicBlock().isExitBlock()) {
            ForwardFixSolver solver = lhs.getSolver();
            CallGraph cg = solver.getAnalyzer().getCGBuilder().getCallGraph();
            CGNode currentNode = lhs.getCGNode();

            // Check whether this function has self recursion.
            if (cg.hasEdge(currentNode, currentNode)) {
                SSACFG cfg = solver.getCFG();
                for (CallSiteReference callSite:
                        Iterator2Iterable.make(cg.getPossibleSites(currentNode, currentNode))) {
                    SSACFG.BasicBlock invokeBB = cfg.getBlockForInstruction(callSite.getProgramCounter());
                    ForwardState invokeState = solver.getOut(invokeBB);

                    // Get invoke state's statement and add to work list.
                    for (INodeWithNumber statement:
                            Iterator2Iterable.make(solver.getFixedPointSystem().getStatementsThatDef(invokeState))) {
                        solver.addToWorkList((AbstractStatement) statement);
                    }
                }
            }
        }

        if (hasChanged) {
            return CHANGED;
        } else {
            return NOT_CHANGED;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ForwardNodeTransfer);
    }

    @Override
    public String toString() {
        return "NodeTrans@BB" + basicBlock.getGraphNodeId();
    }
}