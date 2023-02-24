package edu.colorado.thresher.core;

import analysis.backward.thresher.constraint.IConstraint;
import analysis.backward.thresher.constraint.TypeConstraint;
import analysis.backward.thresher.factor.VariableFactor;
import analysis.backward.thresher.pathinfo.PypstaPathInfo;
import analysis.backward.thresher.query.AbstractPypstaQuery;
import analysis.backward.thresher.query.IPypstaQuery;
import analysis.backward.thresher.term.ConstantTypeTerm;
import analysis.backward.thresher.term.VariableTerm;
import client.loader.PythonSpecialMethodCallSiteReference;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

import java.util.*;
import java.util.stream.Collectors;

public class PypstaSymbolicExecutor extends OptimizedPathSensitiveSymbolicExecutor {
    public static int instrCount = 0;

    protected int pathCount = 0;
    private IPathInfo prevPath = null;

    public static TypeReference top = TypeReference.findOrCreate(
            PythonTypes.pythonLoader, "PypstaBackwardTop"
    );

    public PypstaSymbolicExecutor(CallGraph callGraph, Logger logger) {
        super(callGraph, logger);
    }

    public int getPathCount() {
        return pathCount;
    }

    @Override
    public boolean executeBackward(CGNode startNode, ISSABasicBlock startBlk, int startLine, IQuery query) {
        constraintCacheManager.setTmpCache(startNode, startBlk.getGraphNodeId(), (IPypstaQuery) query);
        return super.executeBackward(startNode, startBlk, startLine, query);
    }

    @Override
    public boolean executeBackward() {
        boolean foundWitness = super.executeBackward();

        constraintCacheManager.setFoundWitness(foundWitness);
        return foundWitness;
    }

    @Override
    public IPathInfo makePath(CGNode startNode, ISSABasicBlock startBlk, int startLine, IQuery query) {
        return new PypstaPathInfo(startNode, startBlk, startLine, (IPypstaQuery) query);
    }

    @Override
    public IPathInfo selectPath() {
        IPathInfo path = null;
        if (!pathsToExplore.isEmpty()) {
            path = selectPathInternal();

            // special case for merging loops; we must do so eagerly or we will drop too many constraints
            if (path.isLoopMergeIndicator()) {
                if (Options.DEBUG) Util.Debug("forcing loop merge");
                if (!branchPointStack.isEmpty())
                    path = mergeBranchPointForLoopHead(path.getCurrentBlock());
                else
                    path = this.selectPath(); // else do nothing
            }
        } else {
            if (!branchPointStack.isEmpty()) {
                // no paths left in stack; merge branch points, if there are any
                path = mergeNextBranchPoint();
            } else {
                // no paths left in stack and no branch points left to merge
            }
        }

        if (path != null) {
            if (prevPath == null) {
                pathCount++;
            } else if (path.getLastBlock() != prevPath.getCurrentBlock() && path.getPathId() != prevPath.getPathId()) {
                pathCount++;
            }
        }
        prevPath = path;
        return path;
    }

    private IPathInfo selectPathInternal() {
        IPathInfo next = null;
        for (IPathInfo path: pathsToExplore) {
            // Prioritize not merged path.
            if (!path.isLoopMergeIndicator()) {
                next = path;
                break;
            }
        }

        if (next == null) {
            return pathsToExplore.removeFirst();
        } else {
            pathsToExplore.remove(next);
            return next;
        }
    }

    private static ConstraintCacheManager constraintCacheManager = new ConstraintCacheManager();
    @Override
    boolean executeAllInstructionsInCurrentBlock(IPathInfo path,
                                                 LinkedList<IPathInfo> splitPaths,
                                                 SSACFG.BasicBlock loopHead) {
        boolean result;
        if (path.getCurrentBlock().isEntryBlock()) {
            // Get cache element which is saved at this call graph node and this entry basic block.
            ConstraintCacheManager.CacheElement cache = constraintCacheManager.getCache(
                    path.getCurrentNode(),
                    path.getCurrentBlock().getGraphNodeId(),
                    ((PypstaPathInfo) path).getQuery()
            );

            if (cache != null) {
                // There is a cache element. If the result is true, use the result (there is a witness).
                result = cache.foundWitness;
                if (result) {
                    ((PypstaPathInfo) path).proveWitness();
                }
            } else {
                // There is no cache.
                result = super.executeAllInstructionsInCurrentBlock(path, splitPaths, loopHead);
                constraintCacheManager.setTmpCache(
                        path.getCurrentNode(),
                        path.getCurrentBlock().getGraphNodeId(),
                        ((PypstaPathInfo) path).getQuery()
                );
            }
        } else {
            result = super.executeAllInstructionsInCurrentBlock(path, splitPaths, loopHead);
        }

        return result;
    }

    boolean executeAllInstructionsInLoopHeadSequence(IPathInfo info,
                                                     LinkedList<IPathInfo> splitPaths,
                                                     boolean justGetToLoopHead) {
        if (Options.DEBUG) Util.Pre(splitPaths.isEmpty(), "not expecting any split paths here!");
        // !justGetToLoopHead => path is not at loop head already
        Util.Pre(!justGetToLoopHead ||
                !WALACFGUtil.isLoopHead(info.getCurrentBlock(), info.getCurrentNode().getIR()));

        // list to handle case splits in straight-line code (i.e. many applicable rules)
        Set<IPathInfo> cases = HashSetFactory.make();
        cases.add(info);
        // run path through all of the instructions in the loop head sequence,
        // storing any case splits in splitPaths
        // do NOT add any paths to the path manager; add them to caseSplits instead.
        final CGNode startNode = info.getCurrentNode();
        final IR ir = startNode.getIR();
        final SSACFG cfg = ir.getControlFlowGraph();
        SSACFG.BasicBlock currentBlock = info.getCurrentBlock();
        int startLine = info.getCurrentLineNum();
        if (Options.DEBUG) Util.Debug("executing loop head sequence");
        Collection<ISSABasicBlock> preds = cfg.getNormalPredecessors(currentBlock);

        // map from phi index to paths corresponding to that phi index
        Map<Integer, List<IPathInfo>> phiIndexMap = null;
        for (;;) {
            List<SSAInstruction> instrs = currentBlock.getAllInstructions();
            for (int i = instrs.size() - 1; i > -1; i--) {
                SSAInstruction instr = instrs.get(i);
                if (Options.DEBUG) Util.Debug("loop head instr " + instr);
                if (i <= startLine) {
                    if (instr instanceof SSAAbstractInvokeInstruction) {
                        if (Options.DEBUG) Util.Assert(splitPaths.isEmpty(), "shouldn't have split yet!");
                        Set<IPathInfo> extraPaths = HashSetFactory.make();
                        for (IPathInfo path : cases) {
                            path.setCurrentLineNum(i - 1);
                            if (Options.SYNTHESIS) visitInvokeAsCallee((SSAAbstractInvokeInstruction) instr, path);
                            else extraPaths.addAll(visitCallInLoopHead((SSAAbstractInvokeInstruction) instr, path));
                        }
                        if (!Options.SYNTHESIS) cases = extraPaths;
                        //cases.clear();
                        //cases.addAll(extraPaths);

                        if (Options.DEBUG) {
                            for (IPathInfo path : cases) {
                                Util.Assert(path.isFeasible());
                                Util.Assert(path.getCurrentNode().equals(startNode),
                                        "path " + path.getPathId() + " in node " + path.getCurrentNode() + " instead of " + startNode);
                            }
                        }

                    } else if (instr instanceof SSAPhiInstruction) {
                        // found a phi node; need to do path splitting early in order to
                        // decide which value is assigned on which path
                        if (phiIndexMap == null) {
                            phiIndexMap = HashMapFactory.make();
                            for (IPathInfo path : cases) {
                                path.setCurrentLineNum(i - 1);
                                splitPaths.clear();
                                //if (justGetToLoopHead) Util.Assert(preds.size() < 2, " bad IR " + ir);
                                initializeSplitPaths(splitPaths, preds, path);
                                int phiIndex = instr.getNumberOfUses() - 1;
                                for (IPathInfo choice : splitPaths) {
                                    List<IPathInfo> choices = new LinkedList<IPathInfo>();
                                    choices.add(choice);
                                    phiIndexMap.put(phiIndex, choices);
                                    phiIndex--;
                                }
                            }
                        }
                        List<IPathInfo> toAdd = new LinkedList<IPathInfo>();
                        List<IPathInfo> toRemove = new LinkedList<IPathInfo>();
                        for (int key : phiIndexMap.keySet()) {
                            List<IPathInfo> values = phiIndexMap.get(key);
                            for (IPathInfo choice : values) {
                                List<IPathInfo> phiCases = visitPhi((SSAPhiInstruction) instr, choice, key);
                                if (phiCases == IPathInfo.INFEASIBLE) {
                                    toRemove.add(choice); // phi visit made path infeasible
                                } else if (!phiCases.isEmpty()) {
                                    toAdd.addAll(phiCases);
                                }
                            }
                            values.addAll(toAdd);
                            values.removeAll(toRemove);
                            toAdd.clear();
                            toRemove.clear();
                        }
                    } else {
                        // "normal" case
                        if (Options.DEBUG)
                            Util.Assert(!(instr instanceof SSAConditionalBranchInstruction), "should never be executing conditionals here!");
                        if (Options.DEBUG)
                            Util.Assert(splitPaths.isEmpty(), "shouldn't have split yet!");
                        List<IPathInfo> toAdd = new LinkedList<IPathInfo>();
                        List<IPathInfo> toRemove = new LinkedList<IPathInfo>();
                        for (IPathInfo path : cases) {
                            path.setCurrentLineNum(i - 1);
                            List<IPathInfo> splits = path.visit(instr);
                            if (splits == IPathInfo.INFEASIBLE)
                                toRemove.add(path); // infeasible
                            else
                                toAdd.addAll(splits);
                        }
                        cases.addAll(toAdd);
                        cases.removeAll(toRemove);
                    }
                }
            }
            // keep executing straightline code
            if (preds.size() == 1) {
                currentBlock = (SSACFG.BasicBlock) preds.iterator().next();
                if (Options.DEBUG) Util.Assert(!cases.isEmpty(), "no paths left to execute!");
                if (Options.DEBUG) Util.Assert(phiIndexMap == null,
                        "phiIndex map should not be initialized!");
                // if we've made it to the loop head and that's our only goal, we're done.
                boolean returnNow = justGetToLoopHead && WALACFGUtil.isLoopHead(currentBlock, ir);

                for (IPathInfo choice : cases) {
                    choice.setCurrentBlock(currentBlock);
                    choice.setCurrentLineNum(currentBlock.getAllInstructions().size() - 1);
                    if (returnNow) splitPaths.add(choice);
                }
                if (returnNow) return !splitPaths.isEmpty();

                startLine = currentBlock.getAllInstructions().size() - 1;
                preds = cfg.getNormalPredecessors(currentBlock);
            } else {
                if (Options.DEBUG)
                    Util.Assert(preds.size() > 1, "loop should split at some point!");
                // Util.Assert(initSplitPaths, "split paths not initialized!");
                splitPaths.clear();
                if (phiIndexMap == null) { // phiIndexMap was never initialized. need to
                    // split into a case for each pred
                    if (Options.DEBUG) Util.Assert(splitPaths.isEmpty(),
                            "split paths should be empty here!");
                    if (cases.isEmpty()) {
                        Util.Debug("cases empty");
                        info.refute();
                        return false;
                    }

                    if (!justGetToLoopHead) {
                        for (IPathInfo _case : cases) {
                            initializeSplitPaths(splitPaths, preds, _case);
                        }
                    } else {
                        splitPaths.addAll(cases);
                        if (Options.DEBUG) {
                            for (IPathInfo splitPath : splitPaths) {
                                Util.Assert(WALACFGUtil.isLoopHead(splitPath.getCurrentBlock(), splitPath.getCurrentNode().getIR()),
                                        splitPath + " not at loop head " + splitPath.getCurrentBlock() + " " + splitPath.getCurrentNode().getIR());
                            }
                        }
                    }
                    return true;
                } // else, phiIndexMap was already initialized
                Collection<List<IPathInfo>> lists = phiIndexMap.values();
                for (List<IPathInfo> list : lists) {
                    splitPaths.addAll(list);
                }

                if (Options.DEBUG && justGetToLoopHead) {
                    for (IPathInfo splitPath : splitPaths) {
                        Util.Assert(WALACFGUtil.isLoopHead(splitPath.getCurrentBlock(), splitPath.getCurrentNode().getIR()),
                                splitPath + " not at loop head " + splitPath.getCurrentBlock() + " " + splitPath.getCurrentNode().getIR());
                    }
                }

                return !splitPaths.isEmpty();
            }
        }
    }

    @Override
    Set<IPathInfo> visitCallInLoopHead(SSAInvokeInstruction instr, IPathInfo path) {
        Assertions.UNREACHABLE();
        return null;
    }

    Set<IPathInfo> visitCallInLoopHead(SSAAbstractInvokeInstruction instr, IPathInfo path) {
        Util.Debug("visiting call " + instr.getDeclaredTarget() + " in loop head on path " + path.getPathId());
        int startingCallStackDepth = path.getCallStackDepth();
        final CGNode startingNode = path.getCurrentNode();

        int pathListSize = this.pathsToExplore.size(), branchStackSize = this.branchPointStack.size();
        addPathAndBranchPlaceholders();

        Set<IPathInfo> extraPaths = HashSetFactory.make();
        if (visitInvokeAsCallee(instr, path)) addPath(path);
        Util.Assert(!path.foundWitness());

        IPathInfo extraPath = selectNonDummyPath();
        // keep executing all paths until they have returned from the call
        while (extraPath != null) { //this.pathsToExplore.get(0).getPathId() != topPath) {
            Util.Assert(!extraPath.foundWitness());
            if (extraPath.getCallStackDepth() == startingCallStackDepth) {
                //Util.Assert(WALACFGUtil.isLoopHead(extraPath.getCurrentBlock(), extraPath.getCurrentNode().getIR()));
                // we're back to the original call stack depth. add the path to the list to return
                extraPaths.add(extraPath);
            } else {
                // not back to starting function yet--execute it
                boolean hitProcBoundary = executeBackwardsPathIntraprocedural(extraPath);
                if (hitProcBoundary && extraPath.returnFromCall()) {
                    // returned from some call - add path back to list
                    addPath(extraPath);
                }
            }
            extraPath = selectNonDummyPath();
        }
        cleanupPathAndBranchPlaceholders();

        // don't want to add current path more than once because we are continuing execution on it.
        // TODO: this is a bad hack
        //this.pathsToExplore.remove(path);
        //Util.Assert(path.getCallStackDepth() == startingCallStackDepth, "path " + path.getPathId() +
        //  " at " + path.getCurrentNode() + " started at " + startingCallStackDepth);

        if (Options.DEBUG) {
            for (IPathInfo checkMe : extraPaths) {
                Util.Assert(checkMe.isFeasible());
                Util.Assert(checkMe.getCurrentNode().equals(startingNode));
            }
            Util.Assert(this.pathsToExplore.size() == pathListSize, "shouldn't add or remove paths!");
            Util.Assert(this.branchPointStack.size() == branchStackSize, "shouldn't add or remove branches!");
        }
        //if (extraPaths.isEmpty()) extraPaths.add(path);
        Util.Debug("returning " + extraPaths.size() + " paths.");
        //Util.Assert(!extraPaths.isEmpty());
        return extraPaths;
    }

    @Override
    Collection<IPathInfo> executeAllInstructionsInLoopHeadBlock(IPathInfo info, SSACFG.BasicBlock loopHeadBlock) {
        Collection<IPathInfo> paths = executeAllInstructionsInLoopHeadBlockImpl(info);
        if (Options.DEBUG && paths != null) {
            for (IPathInfo path : paths) {
                Util.Post(WALACFGUtil.isLoopEscapeBlock(path.getCurrentBlock(), loopHeadBlock, path.getCurrentNode().getIR()),
                        "needed loop escape block for loop headed by " + loopHeadBlock + " but found " +
                                path.getCurrentBlock() + " " + path.getCurrentNode().getIR());
            }
        }
        return paths;
    }

    /**
     * execute all instructions, making the phi choice corresponding to the loop escape block
     */
    private Collection<IPathInfo> executeAllInstructionsInLoopHeadBlockImpl(IPathInfo info) {
        if (Options.DEBUG) Util.Debug("executing loop head blk for " + info.getCurrentBlock() + " line " + info.getCurrentLineNum());
        final IR ir = info.getCurrentNode().getIR();
        final SSACFG cfg = ir.getControlFlowGraph();
        SSACFG.BasicBlock currentBlock = info.getCurrentBlock();
        final SSACFG.BasicBlock loopHead = currentBlock;
        //int startLine = currentBlock.getLastInstructionIndex();//info.getCurrentLineNum();
        int startLine = info.getCurrentLineNum();
        List<SSAInstruction> instrs = currentBlock.getAllInstructions();
        Collection<ISSABasicBlock> preds = cfg.getNormalPredecessors(currentBlock);
        Set<IPathInfo> caseSplits = HashSetFactory.make();

        // make sure this isn't an explicitly infinite loop (no branching).
        // otherwise, we would spin around the loop below forever
        if (WALACFGUtil.isExplicitlyInfiniteLoop(currentBlock, ir)) {
            if (Options.DEBUG) Util.Debug("explicitly infinite loop!");
            // yes; find the block that precedes the loop, and execute backwards from there
            SSACFG.BasicBlock escapeBlk = WALACFGUtil.getEscapeBlockForLoop(currentBlock, ir);
            if (escapeBlk == null) return IPathInfo.INFEASIBLE; // no way out, refute
            info.setCurrentBlock(escapeBlk);
            info.setCurrentLineNum(escapeBlk.getAllInstructions().size() - 1);
            caseSplits.add(info);
            return caseSplits;
        }

        caseSplits.add(info);
        info = null; // should read info out of caseSplits hereafter

        for (;;) {
            for (int i = instrs.size() - 1; i > -1; i--) {
                SSAInstruction instr = instrs.get(i);
                if (i <= startLine) {
                    if (Options.DEBUG) Util.Debug("INSTR " + instr);
                    if (instr instanceof SSAPhiInstruction) {
                        // the loop escape block is always the last choice in the phi
                        int phiIndex = preds.size() - 1;
                        // we are leaving the loop, so choose the escape block last item on the list of
                        List<IPathInfo> toAdd = new LinkedList<IPathInfo>(), toRemove = new LinkedList<IPathInfo>();
                        for (IPathInfo path : caseSplits) {
                            path.setCurrentLineNum(i - 1);
                            Util.Debug("visiting phi " + instr);
                            List<IPathInfo> cases = visitPhi((SSAPhiInstruction) instr, path, phiIndex);
                            if (cases == IPathInfo.INFEASIBLE)
                                toRemove.add(path);
                            else
                                toAdd.addAll(cases);
                        }
                        caseSplits.addAll(toAdd);
                        caseSplits.removeAll(toRemove);
                    } else if (instr instanceof SSAAbstractInvokeInstruction) {
                        Set<IPathInfo> extraPaths = HashSetFactory.make();
                        for (IPathInfo path : caseSplits) {
                            path.setCurrentLineNum(i - 1);
                            if (Options.SYNTHESIS) visitInvokeAsCallee((SSAAbstractInvokeInstruction) instr, path);
                            else extraPaths.addAll(visitCallInLoopHead((SSAAbstractInvokeInstruction) instr, path));
                        }
                        if (!Options.SYNTHESIS) caseSplits = extraPaths;
                        //caseSplits.addAll(extraPaths);
                    } else {
                        //if (Options.DEBUG)
                        Util.Assert(!(instr instanceof SSAConditionalBranchInstruction), "should never execute branch instr's here!");
                        // "normal" case
                        List<IPathInfo> toAdd = new LinkedList<IPathInfo>(), toRemove = new LinkedList<IPathInfo>();
                        for (IPathInfo path : caseSplits) {
                            path.setCurrentLineNum(i - 1);
                            List<IPathInfo> splits = path.visit(instr);
                            if (splits == IPathInfo.INFEASIBLE)
                                toRemove.add(path); // infeasible
                            else
                                toAdd.addAll(splits);
                        }
                        caseSplits.addAll(toAdd);
                        caseSplits.removeAll(toRemove);
                        // Util.Assert(visit(instr, path), "phi made path infeasible!");
                    }
                }
            }

            if (preds.size() == 1) { // keep executing straight-line code
                currentBlock = (SSACFG.BasicBlock) preds.iterator().next();
                // Util.Assert(!caseSplits.isEmpty(), "no paths left to execute!");
                if (caseSplits.isEmpty()) return IPathInfo.INFEASIBLE;
                for (IPathInfo path : caseSplits) {
                    path.setCurrentBlock(currentBlock);
                }
                startLine = currentBlock.getAllInstructions().size() - 1;
                instrs = currentBlock.getAllInstructions();
                preds = cfg.getNormalPredecessors(currentBlock);
            } else { // we've reached the splitting point. find the loop escape block
                if (Options.DEBUG)
                    Util.Assert(!preds.isEmpty(), "loop should split eventually!");
                if (caseSplits.isEmpty()) return IPathInfo.INFEASIBLE;
                if (preds.isEmpty()) return caseSplits;
                for (ISSABasicBlock pred : preds) {
                    SSACFG.BasicBlock nextBlock = (SSACFG.BasicBlock) pred;
                    if (WALACFGUtil.isLoopEscapeBlock(nextBlock, currentBlock, ir)) {
                        //if (WALACFGUtil.isLoopEscapeBlock(nextBlock, loopHead, ir)) {
                        for (IPathInfo path : caseSplits) {
                            // Util.Debug("selecting loop escape block " + nextBlock + " for "
                            // + path.getPathId());
                            path.setCurrentBlock(nextBlock);
                            path.setCurrentLineNum(nextBlock.getAllInstructions().size() - 1);
                        }
                        return caseSplits;
                    }
                }
                // special case (terrible hack) for do...while loops
                //SSACFG.BasicBlock escapeBlock = (SSACFG.BasicBlock) WALACFGUtil.findEscapeBlockForDoWhileLoop(currentBlock, ir);
                SSACFG.BasicBlock escapeBlock = (SSACFG.BasicBlock) WALACFGUtil.findEscapeBlockForDoWhileLoop(loopHead, ir);
                for (IPathInfo path : caseSplits) {
                    // Util.Debug("selecting loop escape block " + nextBlock + " for "
                    // + path.getPathId());
                    path.setCurrentBlock(escapeBlock);
                    path.setCurrentLineNum(escapeBlock.getAllInstructions().size() - 1);
                }
                return caseSplits;

                // Util.Unimp("couldn't find escape block for loop head " + currentBlock + " in\n" + ir);
            }
        }
    }

    @Override
    public boolean handleInterproceduralExecution(IPathInfo path) {
        // Use 'PypstaPathInfo' instead of 'IPathInfo' and
        // 'PythonInvokeInstruction' instead of 'SSAAbstractInvokeInstruction'.

        PypstaPathInfo path1 = (PypstaPathInfo) path;
        if (!path1.isCallStackEmpty() || isPathInSummary(path1)) {
            return super.handleInterproceduralExecution(path1);
        }

        CGNode callee = path1.getCurrentNode();

        if (this.callGraph.getEntrypointNodes().contains(callee)
                && this.callGraph.getPredNodeCount(callee) == 1) {
            return path.isFeasible();
        }

        if (path1.getCurrentNode().getMethod().isClinit()) {
            return super.handleInterproceduralExecution(path1);
        }

        Iterator<CGNode> callers = getCallers(path1, this.callGraph);
        if (!callers.hasNext()) {
            Util.Debug("no callers found! refuted.");
            return false;
        }

        int callCount = 1;
        while (callers.hasNext()) {
            CGNode caller = callers.next();

            if (caller.getMethod().getDeclaringClass().getName().toString().equals("Lmap")
                    || caller.getMethod().getDeclaringClass().getName().toString().equals("Largparse/ArgumentParser/parse_args")) {
                // Special handling. These functions are calling function inner and write summary
                // in forward analysis.
                CGNode callerCaller = callGraph.getPredNodes(caller).next();
                PypstaPathInfo newPath = path1.deepCopy();
                newPath.setCurrentNode(callerCaller);
                CallSiteReference callSite = callGraph.getPossibleSites(callerCaller, caller).next();
                SSACFG.BasicBlock callBB = (SSACFG.BasicBlock) callerCaller.getIR().getBasicBlocksForCall(callSite)[0];
                newPath.setCurrentBlock(callBB);
                newPath.setCurrentLineNum(callBB.getAllInstructions().size()-1-1);
                addPath(newPath);
                continue;
            }

            if (Options.DEBUG) Util.Debug("trying caller " + caller + " " + (callCount++));
            IR callerIR = caller.getIR();
            SSACFG callerCFG = callerIR.getControlFlowGraph();
            SSAInstruction[] instrs = callerIR.getInstructions();
            Iterator<CallSiteReference> sites = callGraph.getPossibleSites(caller, callee);

            while (sites.hasNext()) { // for each caller
                CallSiteReference possibleCaller = sites.next();
                // caller may call callee multiple times. consider each call site
                IntSet indices = callerIR.getCallInstructionIndices(possibleCaller);
                IntIterator indexIter = indices.intIterator();
                Util.Assert(indexIter.hasNext(), "no call sites found in method " + possibleCaller);
                Util.Debug(indices.size() + " possible call instrs in this caller.");
                while (indexIter.hasNext()) {
                    int callIndex = indexIter.next();
                    SSAInstruction instr = instrs[callIndex];
                    if (Options.DEBUG) Util.Debug("Trying call instr " + instr);

                    // This statement is changed.
                    Util.Assert(instr instanceof PythonInvokeInstruction, "Expecting a call instruction, found " + instr);

                    SSACFG.BasicBlock callBlk = callerCFG.getBlockForInstruction(callIndex);
                    int callLine = callBlk.getAllInstructions().size() - 1;
                    Util.Assert(callBlk.getAllInstructions().get(callLine).equals(instr), "calls " + instr + " and "
                            + callBlk.getAllInstructions().get(callLine).equals(instr) + " don't match");
                    PypstaPathInfo newPath = path1.deepCopy();
                    if (caller.equals(callee)) { // is this a recursive call?
                        if (Options.DEBUG)
                            Util.Debug("skipping recursive call " + callee.getMethod().toString() + " and remvoing produced constraints, if any.");
                        // this is both a recursive call and relevant. overapproximate its
                        // effects by dropping constraints
                        // that it could possibly produce
                        newPath.skipCall((PythonInvokeInstruction) instr, this.callGraph, caller);
                        // query.dropConstraintsProduceableInCall(instr,
                        // this.getCurrentNode(), callee);
//                        if (newPath.foundWitness())
//                            return true; // if dropping constraints led to fake witness, we're
//                        // done
//                        addPath(newPath); // add path and try next caller
                        continue;
                    }

                    if (!caller.getMethod().getName().toString()
                            .startsWith(PythonTypes.trampoline.getName().toString().substring(1))) {
                        // Check whether caller is not trampoline method.

                        boolean skipped = false;
                        CallString cs = (CallString) callee.getContext().get(
                                CallStringContextSelector.CALL_STRING);
                        for (int i = 1; i < cs.getMethods().length - 1; i++) {
                            IMethod method = cs.getMethods()[i];
                            if (method.equals(caller.getMethod())
                                    && cs.getMethods()[i-1].equals(callee.getMethod())) {
                                // Check whether on the context there is the same method to 'caller
                                // whose callee method is the same to current node's method.

                                CallSiteReference callSiteRef = cs.getCallSiteRefs()[i];
                                if (possibleCaller.equals(callSiteRef)) {
                                    // Check whether this (callee) method is called from the same
                                    // program point.

                                    // On the context, there is the calling whose caller is the same
                                    // to (current) 'caller' and whose callee is the same to 'path.
                                    // currentNode' and call point is the same to the call point of
                                    // 'caller'. So we assume that even if skip the 'caller', we can
                                    // analyze the same method with same call point.
                                    newPath.skipCall((PythonInvokeInstruction) instr, this.callGraph, caller);
                                    skipped = true;
                                    if (newPath.foundWitness())
                                        return true; // if dropping constraints led to fake witness, we're
                                    newPath.setCurrentNode(caller);
                                    newPath.setCurrentBlock(callBlk);
                                    newPath.setCurrentLineNum(callLine);
                                    visitInvokeAsCaller((PythonInvokeInstruction) instr, callee, newPath);

                                    // Set to start from caller's entry block.
                                    newPath.setCurrentBlock(callerCFG.entry());
                                    newPath.setCurrentLineNum(0);
                                    addPath(newPath);
                                    break;
                                }
                            }
                        }

                        if (skipped) continue;   // Try next caller.
                    }

                    newPath.setCurrentNode(caller);
                    if (WALACFGUtil.isFakeWorldClinit(caller, this.callGraph)) {
                        if (handleFakeWorldClinit(newPath)) {
                            // this is not a fake witness. the problem is that newPath can
                            // split into multiple paths and we can find a witness on a
                            // different one
                            newPath.declareFakeWitness();
                            return true; // found witness in fakeWorldClinit; done
                        }
                        continue; // else, path refuted; try another
                    }
                    newPath.setCurrentBlock(callBlk);

                    if (visitInvokeAsCaller((PythonInvokeInstruction) instr, callee, newPath)) {
                        // Execute call instruction.
                        if (Options.DEBUG) Util.Debug("done visiting caller\n" + newPath);
                        if (newPath.foundWitness()) return true; // check for witness

                        // start from line before the call
                        newPath.setCurrentLineNum(callLine - 1);
                        // path is feasible, add to paths to explore
                        addPath(newPath);
                    } else if (Options.DEBUG)
                        // else, path infeasible; don't copy or add
                        Util.Debug("visiting caller yielded infeasible path; refuting");
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<CGNode> getCallers(IPathInfo path, Graph<CGNode> graph) {
        Iterator<CGNode> parentResult = super.getCallers(path, graph);

        Set<CGNode> result = new HashSet<>();
        Set<CGNode> infiniteMutualRecursion = new HashSet<>();

        while (parentResult.hasNext()) {
            CGNode node = parentResult.next();
            if (isInfiniteMutualRecursion(node)) {
                infiniteMutualRecursion.add(node);
            } else {
                result.add(node);
            }
        }

        if (result.isEmpty()) {
            // If there are only callers which have infinite mutual recursion, reluctantly use
            // the callers.
            return infiniteMutualRecursion.iterator();
        } else {
            // If there are callers which don't have mutual recursion, select them.
            return result.iterator();
        }
    }

    private boolean isInfiniteMutualRecursion(CGNode node) {
        CallString cs = (CallString) node.getContext().get(CallStringContextSelector.CALL_STRING);
        if (cs.getMethods().length < 2) return false;

        IMethod topMethod = cs.getMethods()[0];
        CallSiteReference callSiteCallingTopMethod = cs.getCallSiteRefs()[1];
        for (int i = 1; i < cs.getMethods().length-1; i++) {
            // Find same method and call site on context top.
            IMethod method = cs.getMethods()[i];
            CallSiteReference csRef = cs.getCallSiteRefs()[i+1];
            if (method.equals(topMethod) && csRef.equals(callSiteCallingTopMethod)) {
                // Have found!

                int j = 1; i++;
                while (i < cs.getMethods().length-1) {
                    // Check the context is the same.
                    if (cs.getMethods()[i].equals(cs.getMethods()[j])
                            && cs.getCallSiteRefs()[i+1].equals(cs.getCallSiteRefs()[j+1])) {
                        i++; j++;
                        continue;
                    } else {
                        // If there isn't the same method or call site relation, return false.
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    boolean visitSwitch(SSASwitchInstruction instr, IPathInfo info) {
        instrCount++;
        return super.visitSwitch(instr, info);
    }

    @Override
    boolean visitConditional(SSAConditionalBranchInstruction instr, IPathInfo info) {
        instrCount++;

        CGNode currentNode = info.getCurrentNode();
        SSACFG.BasicBlock currentBB = info.getCurrentBlock();

        // Manual evaluation. If executor passed inner of loop and query has a constraint that
        // iteration variable is "None" type, this witness is false.
        if (WALACFGUtil.isLoopHead(currentBB, currentNode.getIR())
                && WALACFGUtil.isInLoopBody(info.getLastBlock(), currentBB, currentNode.getIR())) {
            // Have passed through inner of loop and current basic block is loop head.

            int iIndex = instr.iIndex() - 1;
            SSAInstruction predInst = currentNode.getIR().getInstructions()[iIndex];
            while (predInst == null && iIndex > 0) {
                iIndex--;
                predInst = currentNode.getIR().getInstructions()[iIndex];
            }

            SymbolTable symbolTable = currentNode.getIR().getSymbolTable();
            if (predInst != null && predInst instanceof SSABinaryOpInstruction) {
                SSABinaryOpInstruction binOpInst = (SSABinaryOpInstruction) predInst;

                // Check the binary operation instruction is "<VAR> NE None"
                if (symbolTable.isNullConstant(binOpInst.getUse(1))
                        && binOpInst.getOperator().equals(CAstBinaryOp.NE)) {
                    PypstaPathInfo path = (PypstaPathInfo) info;
                    for (IConstraint constraint:
                            ((AbstractPypstaQuery) path.getQuery()).getConstraints()) {
                        // Check whether query has the type constraint that "<VAR> == None"
                        if (constraint instanceof TypeConstraint
                                && constraint.getLhs() instanceof VariableTerm
                                && ((VariableTerm) constraint.getLhs()).getVar() instanceof VariableFactor
                                && ((VariableFactor) ((VariableTerm) constraint.getLhs()).getVar()).getVariableId() == binOpInst.getUse(0)
                                && ((VariableFactor) ((VariableTerm) constraint.getLhs()).getVar()).getNode().equals(currentNode)
                                && constraint.getRhs() instanceof ConstantTypeTerm
                                && ((ConstantTypeTerm) constraint.getRhs()).getConstant().getName().toString().equals("LNone")) {
                            return false;
                        }
                    }
                }
            }
        }

        return super.visitConditional(instr, info);
    }

    @Override
    protected boolean visitIPathInfoWrapper(SSAInstruction instr, IPathInfo info) {
        instrCount++;

        // TODO: Add when __getitem__ and __setitem__.
        if (instr instanceof SSABinaryOpInstruction) {
            CGNode currentNode = info.getCurrentNode();

            Set<CGNode> possibleTargets = new HashSet<>();

            for (CGNode possibleTarget: Iterator2Iterable.make(callGraph.getSuccNodes(currentNode))) {
                CallString cs = (CallString) possibleTarget.getContext().get(CallStringContextSelector.CALL_STRING);
                if (cs == null) Assertions.UNREACHABLE();

                if (cs.getCallSiteRefs()[0].getProgramCounter() == instr.iIndex()) {
                    // Found callee target node.
                    possibleTargets.add(possibleTarget);
                }
            }

            if (possibleTargets.size() == 0) {
                return super.visitIPathInfoWrapper(instr, info);
            } else {
                boolean result = false;
                for (CGNode calleeNode: possibleTargets) {
                    PythonSpecialMethodCallSiteReference site = new PythonSpecialMethodCallSiteReference(
                            calleeNode.getMethod().getReference().getDeclaringClass(), instr.iIndex(), instr
                    );
                    PythonInvokeInstruction callInstr = new PythonInvokeInstruction(instr.iIndex(), instr.getDef(), -1, site,
                            new int[]{-1, instr.getUse(0), instr.getUse(1)}, new Pair[0]);
                    result |= visitInvokeAsCallee(callInstr, info);
                }
                return result;
            }
        }
        return super.visitIPathInfoWrapper(instr, info);
    }

    protected boolean visitInvokeAsCaller(SSAAbstractInvokeInstruction instr, CGNode callee, IPathInfo info) {
        if (instr instanceof PythonInvokeInstruction) {
            return visitCallerWrapper(instr, callee, info);
        } else {
            return super.visitInvokeAsCaller(instr, callee, info);
        }
    }

    protected boolean visitCallerWrapper(SSAAbstractInvokeInstruction instr, CGNode callee, IPathInfo info) {
        if (instr instanceof PythonInvokeInstruction) {
            PypstaPathInfo info1 = (PypstaPathInfo) info;
            List<IPathInfo> caseSplits = info1.returnFromCall(
                    (PythonInvokeInstruction) instr, callee, true
            );
            if (caseSplits == null) return false; // infeasible
            for (IPathInfo path : caseSplits) {
                addPath(path);
            }
            return true;
        } else {
            return super.visitCalleeWrapper(instr, callee, info);
        }
    }

    @Override
    protected boolean visitInvokeAsCallee(SSAAbstractInvokeInstruction instr, IPathInfo info) {
        // Get cache element which is saved at this call graph node and this basic block.
        ConstraintCacheManager.CacheElement cache = constraintCacheManager.getCache(
                info.getCurrentNode(),
                info.getCurrentBlock().getGraphNodeId(),
                ((PypstaPathInfo) info).getQuery()
        );
        if (cache != null) {
            // There is a cache element so use saved result.
            boolean result = cache.foundWitness;
            if (result) {
                ((PypstaPathInfo) info).proveWitness();
                return true;
            } else {
                return false;
            }
        } else {
            // There is no cache element.
            constraintCacheManager.setTmpCache(
                    info.getCurrentNode(),
                    info.getCurrentBlock().getGraphNodeId(),
                    ((PypstaPathInfo) info).getQuery()
            );
        }

        instrCount++;

        if (!isInterestedCall(instr))
            return true;

        Set<CGNode> callees = callGraph.getPossibleTargets(info.getCurrentNode(), instr.getCallSite());
        // we get empty call sites when we don't have stubs for something
        if (callees.isEmpty()) {
            if (Options.SYNTHESIS) {
                // replace with <base obj>.<call>
                info.visit(instr);
            } else {
                Util.Debug("callees empty...skipping call");
                ((PypstaPathInfo) info).skipCall((PythonInvokeInstruction) instr, callGraph, null);
            }
            return true;
        }

        if (callees.size() == 1) { // normal case
            CGNode callee = callees.iterator().next();
            if (WALACFGUtil.isFakeWorldClinit(instr.getDeclaredTarget(), this.callGraph)) {
                Assertions.UNREACHABLE();
            }

            if (!visitCalleeWrapper(instr, callee, info)) return false; // refuted by parameter binding
            // else, ordinary call

            addPath(info);
            // if (Options.DEBUG_ASSERTS) split = true;
            // don't want to continue executing instructions that occur before call in caller, so return false
            return false;
        } else { // dynamic dispatch case
            Util.Debug("dynamic dispatch!");
            boolean allRefuted = true;
            PythonInvokeInstruction invoke = (PythonInvokeInstruction) instr;
            for (CGNode callee : callees) { // consider case for each potential callee
                // make sure callee is feasible w.r.t to constraints
                if (!((PypstaPathInfo) info).isDispatchFeasible(invoke, info.getCurrentNode(), callee)) {
                    if (!info.isFeasible()) return false; // can be refuted by dispatch on null
                    continue;
                }

                if (Options.SKIP_DYNAMIC_DISPATCH) {
                    // heuristic: skip any dynamic dispatch. exploration cost is not worth it
                    ((PypstaPathInfo) info).skipCall((PythonInvokeInstruction) instr, this.callGraph, callee);
                    if (info.foundWitness()) return true;
                    allRefuted = false;
                } else {
                    IPathInfo copy = info.deepCopy();
                    if (visitCalleeWrapper(instr, callee, copy)) {
                        if (copy.foundWitness()) {
                            info.declareFakeWitness();
                            return true;
                        }
                        addPath(copy);
                        allRefuted = true;
                        // if (Options.DEBUG_ASSERTS) split = true;
                    } // else, refuted by parameter binding
                }
            }
            return !allRefuted;
        }
        // return super.visitInvokeAsCallee(instr, info);
    }

    private boolean isInterestedCall(SSAAbstractInvokeInstruction instr) {
        return !(instr.isStatic()
                && instr.getCallSite().getDeclaredTarget().getName().toString().equals("import"));
    }

    @Override
    protected boolean visitCalleeWrapper(
            SSAAbstractInvokeInstruction instr, CGNode callee, IPathInfo info) {
        if (instr instanceof PythonInvokeInstruction && info instanceof PypstaPathInfo) {
            List<IPathInfo> caseSplits =
                    ((PypstaPathInfo) info).enterCall((PythonInvokeInstruction) instr, callGraph, callee);

            if (caseSplits == null)
                return false; // infeasible

            for (IPathInfo path : caseSplits) {
                addPath(path);
            }
            return true;
        } else {
            return super.visitCalleeWrapper(instr, callee, info);
        }
    }

    @Override
    protected boolean handleFakeWorldClinit(IPathInfo path) {
        // Do nothing.
        return true;
    }

    @Override
    boolean handleFakeRootMethod(IPathInfo path, CGNode entrypoint) {
        // Do nothing.
        return true;
    }

    private static class ConstraintCacheManager {
        private Set<CacheElement> constraintCache = new HashSet<>();
        private Set<CacheElement> tmpCache = new HashSet<>();

        /**
         * Get the cached result which created in this cg node and basic block.
         * @param currentNode current cg node
         * @param bbId current basic block
         * @param query current query
         * @return cached element iff there is a cache and current query contains the cached
         * element's query
         */
        public CacheElement getCache(CGNode currentNode, int bbId, IPypstaQuery query) {
            CacheElement currentCacheElement = new CacheElement(currentNode, bbId, query);

            Set<CacheElement> cgNodeMatchedCaches = constraintCache.parallelStream()
                    .filter(e -> e.cgNode.equals(currentNode))
                    .filter(e -> e.bbId == bbId)
                    .collect(Collectors.toSet());
            for (CacheElement cgNodeMatchedCache: cgNodeMatchedCaches) {
                if (currentCacheElement.contains(cgNodeMatchedCache)) {
                    return cgNodeMatchedCache;
                }
            }

            return null;
        }

        /**
         * Create cache element instance related on current cg node and basic block. After finishing
         * of this backward analysis, set the result of this backward analysis.
         * @param currentNode current cg node
         * @param bbId current basic block
         * @param query current query
         */
        public void setTmpCache(CGNode currentNode, int bbId, IPypstaQuery query) {
            CacheElement cacheElement = new CacheElement(currentNode, bbId, query);
            tmpCache.add(cacheElement);
        }

        /**
         * Set the result of this backward analysis to caches.
         * @param foundWitness the result of this backward analysis
         */
        public void setFoundWitness(boolean foundWitness) {
            for (CacheElement cacheElement: tmpCache) {
                cacheElement.foundWitness = foundWitness;
                constraintCache.add(cacheElement);
            }

            tmpCache.clear();
        }

        private class CacheElement {
            private boolean foundWitness = true;

            private final CGNode cgNode;
            private final int bbId;
            private final Set<String> strConstraints;

            public CacheElement(CGNode cgNode, int bbId, IPypstaQuery query) {
                this.cgNode = cgNode;
                this.bbId = bbId;
                this.strConstraints = new HashSet<>();
                for (IConstraint c: ((AbstractPypstaQuery) query).getConstraints()) {
                    strConstraints.add(c.toString());
                }
            }

            /**
             * Return whether this cached constraints contain the other cached constraints.
             * Compare each constraint and return false if the other constraint has a constraint
             * which this cached constraints doesn't have.
             * @param other the other cache element
             * @return true iff all constraints in other cache element are contained in this
             */
            public boolean contains(CacheElement other) {
                for (String otherConstraint: other.strConstraints) {
                    if (!this.strConstraints.contains(otherConstraint))
                        return false;
                }
                return true;
            }

            @Override
            public String toString() {
                StringBuilder s = new StringBuilder();
                s.append(cgNode.toString());
                s.append(": ");
                for (String strConstraint: strConstraints) {
                    s.append(strConstraint).append(", ");
                }
                return s.toString();
            }
        }
    }
}
