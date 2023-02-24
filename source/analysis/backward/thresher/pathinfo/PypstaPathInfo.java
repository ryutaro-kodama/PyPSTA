package analysis.backward.thresher.pathinfo;

import analysis.backward.thresher.BackwardFunctionSummaries;
import analysis.backward.thresher.query.IPypstaQuery;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.collections.Pair;
import edu.colorado.thresher.core.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PypstaPathInfo extends IPathInfo {
    private final IPypstaQuery query;

    private final LinkedList<PypstaStackFrame> callStack;

    private boolean proveWitness = false;

    public PypstaPathInfo(
            CGNode currentNode, ISSABasicBlock currentBlock, int currentLineNum, IPypstaQuery query) {
        this(currentNode, (SSACFG.BasicBlock) currentBlock, currentLineNum, query);
    }

    public PypstaPathInfo(
            CGNode currentNode, SSACFG.BasicBlock currentBlock, int currentLineNum, IPypstaQuery query) {
        super(currentNode, currentBlock, currentLineNum, query);
        this.query = query;
        this.callStack = new LinkedList<>();
    }

    // constructor to be used only for deep copying
    private PypstaPathInfo(
            CGNode currentNode, SSACFG.BasicBlock currentBlock, SSACFG.BasicBlock lastBlock,
            int currentLineNum, LinkedList<IStackFrame> callStack,
            Set<Pair<CGNode, SSACFG.BasicBlock>> loopHeadSet, IQuery query,
            IQuery initialQuery, PiecewiseGraph piecewiseGraph) {
        super(currentNode, currentBlock, lastBlock, currentLineNum, callStack, loopHeadSet, query,
                initialQuery, piecewiseGraph);
        this.query = (IPypstaQuery) query;
        this.callStack = new LinkedList<>();
        for (IStackFrame oldStackFrame: callStack) {
            this.callStack.push((PypstaStackFrame) oldStackFrame);
        }
    }

    public IPypstaQuery getQuery() {
        return query;
    }

    public PypstaPathInfo deepCopy() {
        Util.Pre(!atBranchPoint());
        return this.deepCopyWithQuery(query.deepCopy());
    }

    public void proveWitness() {
        this.proveWitness = true;
    }

    @Override
    public boolean foundWitness() {
        if (proveWitness)
            return true;
        return super.foundWitness();
    }

    //    public static boolean mergePathWithPathSet(IPathInfo info, Set<IPathInfo> pathSet) {
//        if (Options.USE_SUMMARIES) {
//            List<IPathInfo> toRemove = new ArrayList<IPathInfo>(pathSet.size());
//            for (IPathInfo path : pathSet) {
//                if (info == path) continue;
//
//                if (info.callStacksEqual(path)) {
//                    PypstaPathInfo path1 = (PypstaPathInfo) path;
//                    if (info.containsQuery(path)) {
//                        // already seen path simpler than this; don't add
//                        return false;
//                    } else if (path.containsQuery(info)) {
//                        toRemove.add(path);
//                    } else if (path1.canMerge(info)) {
//                        path1.merge(info);
//                        return false;
//                    }
//                }
//            }
//            for (IPathInfo removeMe : toRemove) pathSet.remove(removeMe);
//        }
//        return pathSet.add(info);
//    }
//
//    private boolean canMerge(IPathInfo info) {
//        return query.canMerge(info.query);
//    }
//
//    private void merge(IPathInfo info) {
//        query.merge(info.query);
//    }

    /**
     * copy the current path, but replace the current query with a newQuery NOTE:
     * newQuery should be deep copied before it is passed
     */
    public PypstaPathInfo deepCopyWithQuery(IQuery newQuery) {
        LinkedList<IStackFrame> copiedStack = new LinkedList<>();
        for (PypstaStackFrame frame: this.callStack) {
            copiedStack.push(frame);
        }

        return new PypstaPathInfo(
                getCurrentNode(), getCurrentBlock(), getLastBlock(), getCurrentLineNum(),
                copiedStack, Util.deepCopySet(getLoopHeadSet()), newQuery, getInitialQuery(),
                getPiecewiseGraph().deepCopy()
        );
    }

    @Override
    public boolean returnFromCall() {
        Util.Pre(!callStack.isEmpty(), "Can't pop an empty call stack!");

        PypstaStackFrame frame = callStack.pop();
        CGNode callee = getCurrentNode();
        if (Options.DEBUG)
            Util.Debug("returning from " + callee.getMethod().getName() + " "
                    + callee + " to " + frame.getCGNode());

        // cleanup: forget which loop heads we saw in the callee, in case we come
        // back to the callee later
        List<Pair<CGNode, SSACFG.BasicBlock>> toRemove = new LinkedList<Pair<CGNode, SSACFG.BasicBlock>>();
        for (Pair<CGNode, SSACFG.BasicBlock> pair : getLoopHeadSet()) {
            if (pair.fst.equals(callee))
                toRemove.add(pair);
        }

        for (Pair<CGNode, SSACFG.BasicBlock> pair : toRemove) {
            boolean removed = getLoopHeadSet().remove(pair);
            Util.Assert(removed, "couldn't remove " + pair);
        }

        Util.Assert(frame.getBlock() != null);

        // reset caller state
        setCurrentNode(frame.getCGNode());
        setCurrentBlock(frame.getBlock());
        setCurrentLineNum(frame.getLineNum());

        // reflect return in query
        List<IQuery> caseSplits = query.returnFromCall(frame.getCallInst(), callee, this, true);

        // we should never have case splits here because the arguments to the callee
        // were already known before we entered it
        Util.Post(caseSplits == IQuery.INFEASIBLE || caseSplits.isEmpty(), "Shouldn't have case splits after leaving callee!");

        return caseSplits == IQuery.INFEASIBLE ? false : true;
    }

    public List<IPathInfo> returnFromCall(PythonInvokeInstruction instr, CGNode callee, boolean backward) {
        Util.Pre(
                this.callStack.isEmpty(),
                "Should only call 'PypstaPathInfo.returnFromCall' with empty call stack!");

        // cleanup: forget which loop heads we have seen. otherwise, if we come back
        // to this method later, we will neglect to execute loops we've already seen
        getLoopHeadSet().clear();

        // have query reflect return from call
        List<IQuery> caseSplits = query.returnFromCall(instr, callee, this, backward);
        return handleQueryCaseSplitReturn(caseSplits);
    }

    // wrapper
    public List<IPathInfo> enterCall(PythonInvokeInstruction instr, CallGraph cg, CGNode callee) {
        return enterCall(instr, cg, callee, false);
    }

    private List<IPathInfo> enterCall(PythonInvokeInstruction instr, CallGraph cg, CGNode callee, boolean skip) {
        if (callee != null && BackwardFunctionSummaries.contain(callee) && !skip) {
            // Call summary function.
            return BackwardFunctionSummaries.call(instr, this, callee, query);
        }

        String calleeName = null;
        if (callee != null)
            calleeName = callee.getMethod().toString();
        else
            Util.Assert(skip);

        // TMP!
        if (calleeName == null) {
            return IPathInfo.FEASIBLE;
        }

        // if this call is relevant
        if (skip || callee.getIR() == null) {
//      if (skip || callee.getIR() == null || !isCallRelevantToQuery(instr, callee, cg)) {
            // heuristic: want to avoid executing equals(), hashCode() e.t.c because they're a time
            // sink and are unlikely to lead to refutation
            if (Options.DEBUG) Util.Debug("skipping call " + instr + " and dropping produced constraints");
            query.dropConstraintsProduceableInCall(instr, this.getCurrentNode(), callee, true);
            return IPathInfo.FEASIBLE;
        } else if (callee.equals(getCurrentNode())) { // is this a recursive call?
            if (Options.DEBUG) {
                Util.Debug("skipping recursive call " + callee.getMethod().toString()
                        + " and dropping produced constraints");
            }
            // this is both a recursive call and relevant. overapproximate its effects by dropping constraints
            // that it could possibly produce
            query.dropConstraintsProduceableInCall(instr, this.getCurrentNode(), callee, true);
            return IPathInfo.FEASIBLE;
        } else if (callStack.stream().filter(f -> f.getCGNode().getMethod().equals(callee.getMethod())).count() > 0) {
            // Causing circle calling (the callee method has already pushed on stack). We assume we analyze methods
            // on single path as much as once. So skip this callee calling
            if (Options.DEBUG)
                Util.Debug("skipping ordinary call " + callee + " due to circle calling");
            query.dropConstraintsProduceableInCall(instr, this.getCurrentNode(), callee, true);
            return IPathInfo.FEASIBLE;
        } else {
            if (Options.DEBUG)
                Util.Debug("call stack size is " + getCallStack().size());
            // else, we should enter the call...if our call stack is not already too deep

            if (getCallStack().size() >= Options.MAX_CALLSTACK_DEPTH) { // is our call stack too deep?
                if (Options.DEBUG)
                    Util.Debug("skipping ordinary call " + callee
                            + " due to call stack depth and dropping produced constraints");
                query.dropConstraintsProduceableInCall(instr, this.getCurrentNode(), callee, true);
                return IPathInfo.FEASIBLE;
            }
        }

        if (Options.DEBUG) {
            Util.Debug("entering call " + callee.getMethod().getName() +  " " + callee.getMethod() +
                    " from " + getCurrentNode().getMethod().toString());
        }

        // push caller onto call stack and set current node to callee for the current path
        List<IQuery> caseSplits = query.enterCall(instr, callee, this);
        pushCallStack(instr, callee);

        return handleQueryCaseSplitReturn(caseSplits);
    }

    public boolean isDispatchFeasible(PythonInvokeInstruction instr, CGNode caller, CGNode callee) {
        return query.isDispatchFeasible(instr, caller, callee);
    }

    public List<IPathInfo> skipCall(PythonInvokeInstruction instr, CallGraph cg, CGNode callee) {
        return enterCall(instr, cg, callee, true);
    }

    private void pushCallStack(PythonInvokeInstruction instr, CGNode callee) {
        Util.Pre(callee.getIR() != null, "no IR for " + callee);
        Util.Pre(callee.getIR().getExitBlock() != null, "no exit block!");

        if (getCurrentNode().equals(callee)) {
            Util.Unimp("recursion");
        }

        PypstaStackFrame newFrame = new PypstaStackFrame(
                instr, getCurrentNode(), getCurrentBlock(), getCurrentLineNum());
        callStack.push(newFrame);

        setCurrentNode(callee);
        setCurrentBlock(callee.getIR().getExitBlock());
        setCurrentLineNum(getCurrentBlock().getLastInstructionIndex());
    }


    @Override
    public boolean isCallStackEmpty() {
        return callStack.isEmpty();
    }

    @Override
    public int getCallStackDepth() {
        return callStack.size();
    }

//    @Override
//    public LinkedList<IStackFrame> getCallStack() {
//        return super.getCallStack();
//    }

    @Override
    public boolean callStacksEqual(IPathInfo other) {
        if (other instanceof PypstaPathInfo)
            return this.callStack.equals(((PypstaPathInfo) other).callStack);
        else
            return super.callStacksEqual(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PypstaPathInfo that = (PypstaPathInfo) o;
        return query.equals(that.query)
                && getCurrentNode().equals(that.getCurrentNode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(query.hashCode(), getCurrentNode().hashCode());
    }
}
