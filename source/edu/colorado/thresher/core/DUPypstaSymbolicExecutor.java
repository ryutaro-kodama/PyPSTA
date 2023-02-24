package edu.colorado.thresher.core;

import analysis.backward.thresher.query.IPypstaQuery;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.graph.dominators.Dominators;

import java.util.*;

public class DUPypstaSymbolicExecutor extends PypstaSymbolicExecutor {
    private static final Map<MethodReference, boolean[]> canSkipBlockCache = new HashMap<>();

    public DUPypstaSymbolicExecutor(CallGraph callGraph, Logger logger) {
        super(callGraph, logger);
    }

    @Override
    boolean executeAllInstructionsInCurrentBlock(IPathInfo path,
                                                 LinkedList<IPathInfo> splitPaths,
                                                 SSACFG.BasicBlock loopHead) {
        if (canSkipBlockOnInstr(path.getCurrentBlock(), path.getCurrentNode())
                && canSkipBlockOnDU(path.getCurrentBlock(), (IPypstaQuery) path.query)) {
            path.setCurrentLineNum(0);
            return true;
        } else {
            final boolean isLoopHead = (loopHead != null);
            final IR ir = path.getCurrentNode().getIR();
            final SSACFG cfg = ir.getControlFlowGraph();
            final SSACFG.BasicBlock currentBlock = path.getCurrentBlock();
            int startLine = path.getCurrentLineNum();
            List<SSAInstruction> instrs = currentBlock.getAllInstructions();
            Collection<ISSABasicBlock> preds = cfg.getNormalPredecessors(currentBlock);

            Map<Integer, List<IPathInfo>> phiIndexPathsMap = HashMapFactory.make();

            for (int i = instrs.size() - 1; i > -1; i--) {
                SSAInstruction instr = instrs.get(i);
                if (i <= startLine) {
                    if (Options.DEBUG)
                        Util.Debug("instr " + instr);
                    if (instr instanceof SSAPhiInstruction) {
                        // found a phi node; need to do path splitting early in order to
                        // decide which value is assigned on which path
                        int phiIndex = instr.getNumberOfUses() - 1;
                        if (phiIndexPathsMap.isEmpty()) {
                            if (splitPaths.isEmpty())
                                initializeSplitPaths(splitPaths, preds, path);
                            for (IPathInfo choice : splitPaths) {
                                List<IPathInfo> list = new LinkedList<IPathInfo>();
                                list.add(choice);
                                phiIndexPathsMap.put(phiIndex, list);
                                phiIndex--;
                            }
                            phiIndex = instr.getNumberOfUses() - 1; // reset phi index
                        }

                        for (; phiIndex >= 0; phiIndex--) {
                            List<IPathInfo> choicesForIndex = phiIndexPathsMap.get(phiIndex);
                            // When you use def-use information, there is a possibility that
                            // path is not created.
                            if (choicesForIndex == null) continue;

                            List<IPathInfo> toRemove = new LinkedList<IPathInfo>(), toAdd = new LinkedList<IPathInfo>();
                            for (IPathInfo choice : choicesForIndex) {
                                if (!isLoopHead || !WALACFGUtil.isLoopEscapeBlock(choice.getCurrentBlock(), loopHead, ir)) {
                                    if (Options.DEBUG)
                                        Util.Debug("correlating phi index " + phiIndex + " with block " + choice.getCurrentBlock());
                                    List<IPathInfo> cases = visitPhi((SSAPhiInstruction) instr, choice, phiIndex);
                                    if (cases == IPathInfo.INFEASIBLE) {
                                        // phi visit made path infeasible;
                                        toRemove.add(choice);
                                    } else if (!cases.isEmpty()) {
                                        // case split while visiting phi
                                        toAdd.addAll(cases);
                                    }
                                }
                            }
                            choicesForIndex.removeAll(toRemove);
                            choicesForIndex.addAll(toAdd);
                        }
                    } else {
                        // "normal" case. this should precede executing any phi instructions,
                        // so we don't need to consider the case splits here
                        path.setCurrentLineNum(i - 1);
                        if (!visit(instr, path)) {
                            // if (Options.CHECK_ASSERTS) Util.Assert(!path.isFeasible() ||
                            // split,
                            // "path should be marked infeasible here or we should have a split");
                            return false;
                        }
                    }
                }
            }
            if (!phiIndexPathsMap.isEmpty()) { // push the content of the phi index map
                // back into split paths
                splitPaths.clear();
                for (List<IPathInfo> list : phiIndexPathsMap.values()) {
                    splitPaths.addAll(list);
                }
            }
            return true;
        }
    }

    /**
     * Check whether this basic block has the specific instructions, which may be affect to
     * current constraints. In order to avoid checking all instructions many times, the result
     * per basic block is saved in map.
     * @param targetBlock the target basic block
     * @param cgNode the current call graph node
     * @return true iff can skip
     */
    protected boolean canSkipBlockOnInstr(ISSABasicBlock targetBlock, CGNode cgNode) {
        boolean[] cacheArray = canSkipBlockCache.get(cgNode.getMethod().getReference());
        if (cacheArray == null) {
            final SSACFG cfg = cgNode.getIR().getControlFlowGraph();
            int bbNum = cfg.getNumberOfNodes();
            cacheArray = new boolean[bbNum];

            final SSAInstruction[] allInstructions = cfg.getInstructions();

            int currentBBId = 0;
            int bbLastInstrIndex = cfg.getNode(currentBBId).getLastInstructionIndex();

            for (; bbLastInstrIndex < 0;
                    bbLastInstrIndex = cfg.getNode(++currentBBId).getLastInstructionIndex()) {
                // Set 'true' for all basic blocks which has no instruction (the last instruction index
                // is less than 0)
                cacheArray[currentBBId] = true;
            }

            boolean canSkip = true;
            for (int i = 0; i < allInstructions.length; i++) {
                SSAInstruction instr = allInstructions[i];

                if (instr != null && (instr instanceof SSAAbstractInvokeInstruction
                                        || instr instanceof PythonInvokeInstruction
                                        || instr instanceof AstLexicalWrite
                                        || instr instanceof AstGlobalWrite
                                        || instr instanceof SSAConditionalBranchInstruction)) {
                    canSkip = false;

                    // Skip all instructions in current basic block.
                    i = bbLastInstrIndex;
                }

                if (i >= bbLastInstrIndex) {
                    cacheArray[currentBBId] = canSkip;

                    // Jump to next basic block.
                    currentBBId++;
                    bbLastInstrIndex = cfg.getNode(currentBBId).getLastInstructionIndex();
                    canSkip = true;
                }
            }

            for (; currentBBId < bbNum; currentBBId++) {
                // The basic block has no instructions.
                cacheArray[currentBBId] = true;
            }

            canSkipBlockCache.put(cgNode.getMethod().getReference(), cacheArray);
        }

        return cacheArray[targetBlock.getGraphNodeId()];
    }

    /**
     * Check whether this current basic block affect to current constraints. If there is no affect,
     * return true.
     * @param basicBlock the target basic block
     * @param query the current query
     * @return true iff can skip
     */
    protected boolean canSkipBlockOnDU(ISSABasicBlock basicBlock, IPypstaQuery query) {
        int bbId = basicBlock.getGraphNodeId();
        return !query.hasNoDUInfoVar() && !query.getDefBBIds().contains(bbId) && !query.getUseBBIds().contains(bbId);
    }

    @Override
    void initializeSplitPaths(LinkedList<IPathInfo> splitPaths,
                              Collection<ISSABasicBlock> preds,
                              IPathInfo path) {
        final CGNode currentNode = path.getCurrentNode();
        final IR ir = currentNode.getIR();
        final SSACFG.BasicBlock currentBlock = path.getCurrentBlock();
        final IPypstaQuery query = (IPypstaQuery) path.query;

        if (WALACFGUtil.isLoopHead(currentBlock, ir)) {
            // If current basic block is loop head block, there is a possibility that ignore all
            // paths. So you don't use def-use skip.
            super.initializeSplitPaths(splitPaths, preds, path);
            return;
        }

        if (preds.size() < 2) {
            // sometimes, we need to add the exceptional predecessors here
            SSACFG cfg = ir.getControlFlowGraph();
            Collection<ISSABasicBlock> newPreds = cfg.getExceptionalPredecessors(currentBlock);
            preds.addAll(newPreds);
        }
        if (Options.DEBUG)
            Util.Assert(preds.size() > 1, "can't create split path lists with less than 2 preds!");

        if (currentBlock.hasPhi() && !canSkipBlockOnDU(currentBlock, (IPypstaQuery) path.query)) {
            // When this target basic block has phi instruction and the lhs variable is used in constraints,
            // you can't skip path, so call super function.
            super.initializeSplitPaths(splitPaths, preds, path);
            return;
        }

        Dominators<ISSABasicBlock> dominators = WALACFGUtil.getDominators(ir);
        ISSABasicBlock immediateDominator = dominators.getIdom(currentBlock);

        SSACFG cfg = path.getCurrentNode().getIR().getControlFlowGraph();

        for (ISSABasicBlock predBlock: preds) {
            List<ISSABasicBlock> workList = new ArrayList<>();
            workList.add(predBlock);

            // Don't check the immediate dominator.
            workList.remove(immediateDominator);

            Set<ISSABasicBlock> visited = new HashSet<>();

            boolean canSkip = true;
            while (!workList.isEmpty()) {
                ISSABasicBlock targetBlock = workList.remove(0);
                visited.add(targetBlock);

                if (!canSkipBlockOnInstr(targetBlock, path.getCurrentNode()) || !canSkipBlockOnDU(targetBlock, query)) {
                    canSkip = false;
                    break;
                }

                List<ISSABasicBlock> targetPredBlocks = Iterator2List.toList(cfg.getPredNodes(targetBlock));
                targetPredBlocks.forEach(b -> {
                    if (!visited.contains(b) && !b.equals(immediateDominator))
                        workList.add(b);
                });
            }

            IPathInfo copy = null;
            if (canSkip && visited.size() != 0) {
                // If `canSkip == true`, you can skip the path from the previous basic block to immediate
                // the basic block.
                // But if there is no basic blocks, you can't skip any basic blocks, so don't skip operation.
                SSACFG.BasicBlock nextBlock = (SSACFG.BasicBlock) immediateDominator;
                copy = path.deepCopy();
                copy.setCurrentBlock(nextBlock);
                assert nextBlock.getLastInstruction() instanceof SSAConditionalBranchInstruction;
                copy.setCurrentLineNum(nextBlock.getAllInstructions().size() - 1 - 1);
                // More '-1' means skipping the conditional branch instruction.
            } else {
                // If there is a basic block which cannot be skipped on the path from the previous
                // basic blocks to the immediate dominator basic block, you have to analyze this path.
                SSACFG.BasicBlock nextBlock = (SSACFG.BasicBlock) predBlock;
                copy = path.deepCopy();
                copy.setCurrentBlock(nextBlock);
                copy.setCurrentLineNum(nextBlock.getAllInstructions().size() - 1);
            }

            boolean isAdd = true;
            for (IPathInfo splitPath: splitPaths) {
                // If there is not the same path to 'copy', which means 'currentBasicBlock' and
                // 'currentLineNum' are the same, add this copy path.
                if (splitPath.getCurrentBlock().equals(copy.getCurrentBlock())
                        && splitPath.getCurrentLineNum() == copy.getCurrentLineNum()) {
                    isAdd = false;
                    continue;
                }
            }
            if (isAdd) {
                splitPaths.addFirst(copy);
            }
        }
    }
}
