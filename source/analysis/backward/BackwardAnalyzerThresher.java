package analysis.backward;

import analysis.backward.thresher.BackwardFunctionSummaries;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import edu.colorado.thresher.core.PypstaSymbolicExecutor;
import analysis.backward.thresher.QueryFactory;
import analysis.backward.thresher.query.IPypstaQuery;
import analysis.backward.thresher.query.TypeQuery;
import analysis.exception.IExceptionData;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.fixpoint.ForwardFixSolver;
import analysis.forward.tracer.MakingExceptionDataTracer;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Set;
import edu.colorado.thresher.core.*;

import java.util.*;

public class BackwardAnalyzerThresher {
    private final AnalysisScope scope;
    private final CallGraphBuilder builder;
    private final CallGraph cg;
    private final AnalysisCache cache;

    private final ForwardAnalyzer forwardAnalyzer;

    private final Set<IPypstaQuery> querySet = new HashSet<>();

    private int pathCount = 0;
    private int execCount = 0;
    private int refutedCount = 0;

    public BackwardAnalyzerThresher(AnalysisScope scope,
                                    CallGraphBuilder builder,
                                    CallGraph cg,
                                    AnalysisCache cache,
                                    ForwardAnalyzer forwardAnalyzer) {
        this.scope = scope;
        this.builder = builder;
        this.cg = cg;
        this.cache = cache;
        this.forwardAnalyzer = forwardAnalyzer;

        Options.MAX_PATH_CONSTRAINT_SIZE = 10;
        Options.USE_SUMMARIES = false;
        Options.MAX_CALLSTACK_DEPTH = 5;
        BackwardFunctionSummaries.forwardAnalyzer = forwardAnalyzer;
    }

    public int getPathCount() {
        return pathCount;
    }

    public int getExecCount() {
        return execCount;
    }

    public int getRefutedCount() {
        return refutedCount;
    }

    public void analyze() {
        QueryFactory.analyzer = forwardAnalyzer;

        for (IExceptionData data:
                ((MakingExceptionDataTracer) forwardAnalyzer.getTracer()).getExceptionDataSet()) {
            if (data.ignore()) continue;

            SSAInstruction inst = data.getInst();

            CGNode cgNode = data.getState().getCGNode();

            final Set<IPypstaQuery> queries = QueryFactory.make(data, cgNode);
            querySet.addAll(queries);

            boolean refutedException = false;

            for (IPypstaQuery query: queries) {
                ISSABasicBlock startBlk = cgNode.getIR().getBasicBlockForInstruction(inst);
                int startLineBlkIndex = WALACFGUtil.findInstrIndexInBlock(inst, (SSACFG.BasicBlock) startBlk);

                Logger logger = new Logger();
                PypstaSymbolicExecutor exec = new PypstaSymbolicExecutor(cg, logger);

                boolean foundWitness = executeBackward(
                        exec, cgNode, startBlk, startLineBlkIndex - 1, query
                );

                if (!foundWitness) {
                    refutedException = true;
                }
            }

            if (refutedException) {
                data.setFoundWitness(false);
            } else {
                data.setFoundWitness(true);
            }
        }
    }

    public Set<IPypstaQuery> getQuerySet() {
        return querySet;
    }

    private PrunedCallGraph refutedCG;
    public void cgAnalyze(boolean defUse) {
        QueryFactory.analyzer = forwardAnalyzer;

        CallGraph updatedCG = cg;
        Map<CGNode, Set<CGNode>> removeMap = new HashMap<>();
        Map<IMethod, Set<IMethod>> provedEdge = new HashMap<>();

        for (ForwardFixSolver s: forwardAnalyzer.sortSolver()) {
            for (SSAInstruction inst: s.getCGNode().getIR().getInstructions()) {
                Set<CGNode> possibleCallees = new HashSet<>();
                if (inst instanceof PythonInvokeInstruction) {
                    possibleCallees = updatedCG.getPossibleTargets(
                            s.getCGNode(), ((PythonInvokeInstruction) inst).getCallSite()
                    );
                } else if (inst instanceof SSABinaryOpInstruction) {
                    CGNode solverNode = s.getCGNode();
                    for (CGNode possibleTarget: Iterator2Iterable.make(updatedCG.getSuccNodes(solverNode))) {
                        CallString cs = (CallString) possibleTarget.getContext().get(CallStringContextSelector.CALL_STRING);
                        if (cs == null) Assertions.UNREACHABLE();

                        if (cs.getCallSiteRefs()[0].getProgramCounter() == inst.iIndex()) {
                            // Found callee target node.
                            possibleCallees.add(possibleTarget);
                        }
                    }
                } else {
                    continue;
                }

                // There are more than 2 callee.
                if (possibleCallees.size() > 1) {
                    ISSABasicBlock startBlk = s.getCGNode().getIR().getBasicBlockForInstruction(inst);
                    int startLineBlkIndex = WALACFGUtil.findInstrIndexInBlock(inst, (SSACFG.BasicBlock) startBlk);

                    for (CGNode possibleCallee: possibleCallees) {
                        // If the call graph edge from 's.getMethod()' to 'possibleCall.getMethod'
                        // has already been proved to correct, skip the backward analysis for edge
                        // between the same caller method to the same callee method.
                        if (provedEdge.containsKey(s.getMethod())
                                && provedEdge.get(s.getMethod()).contains(possibleCallee.getMethod()))
                            continue;

                        TypeReference initType = null;
                        if (inst instanceof PythonInvokeInstruction) {
                            initType = possibleCallee.getMethod().getDeclaringClass().getReference();
                            if (initType.getName().getClassName().toString().equals("__call__")) {
                                initType = TypeReference.find(
                                        PythonTypes.pythonLoader,
                                        initType.getName().toString().replace("L$script", "Lscript")
                                                .replace("/__call__", "_instance")
                                );
                            }
                        } else if (inst instanceof SSABinaryOpInstruction) {
                            initType = TypeReference.find(
                                    PythonTypes.pythonLoader,
                                    ("L" + possibleCallee.getMethod().getDeclaringClass().getName().getPackage().toString() + "_instance")
                                            .replace("$", "")
                            );
                        }
                        TypeQuery query = new TypeQuery(
                                forwardAnalyzer, inst.getUse(0), s.getCGNode(), initType
                        );
                        querySet.add(query);

                        Logger logger = new Logger();
                        PypstaSymbolicExecutor exec;
                        if (defUse) {
                            exec = new DUPypstaSymbolicExecutor(updatedCG, logger);
                        } else {
                            exec = new PypstaSymbolicExecutor(updatedCG, logger);
                        }

                        // Start at line BEFORE the invoke instruction.
                        boolean foundWitness = executeBackward(
                                exec, s.getCGNode(), startBlk, startLineBlkIndex - 1, query
                        );

                        if (foundWitness) {
                            // Set edge information to 'provedEdge'.
                            Set<IMethod> calledMethods = provedEdge.getOrDefault(s.getMethod(), new HashSet<>());
                            calledMethods.add(possibleCallee.getMethod());
                            provedEdge.put(s.getMethod(), calledMethods);
                        } else {
                            Util.Print("Refute edge [" + s + "\n  -> [" + possibleCallee + "]");

                            // Set edge information to 'removeMap'.
                            Set<CGNode> removeCallees = removeMap.getOrDefault(s.getCGNode(), new HashSet<>());
                            removeCallees.add(possibleCallee);
                            removeMap.put(s.getCGNode(), removeCallees);

                            updatedCG = new PrunedCallGraph(cg, Iterator2Set.toSet(cg.iterator()), removeMap);
                        }
                    }
                }
            }
        }

        refutedCG = new PrunedCallGraph(cg, Iterator2Set.toSet(cg.iterator()), removeMap);
    }

    private boolean executeBackward(PypstaSymbolicExecutor executor,
                                    CGNode startNode,
                                    ISSABasicBlock startBlock,
                                    int startLine,
                                    IPypstaQuery query) {

        Util.Debug("---------- Start executor ----------");

        // Start at line BEFORE the invoke instruction.
        boolean foundWitness = true;
        try {
            foundWitness = executor.executeBackward(
                    startNode, startBlock, startLine, query
            );
        } catch (Exception e) {
            if (Options.EXIT_ON_FAIL) throw e;
            Util.Print("FAILED " + e + " " + Util.printArray(e.getStackTrace()));
        }

        Util.Debug("\n\n");

        query.setFoundWitness(foundWitness);
        pathCount += executor.getPathCount();
        execCount++;
        if (!foundWitness) refutedCount++;

        return foundWitness;
    }

    public PrunedCallGraph getRefutedCG() {
        return refutedCG;
    }
}
