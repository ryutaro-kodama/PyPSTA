package analysis.forward.fixpoint;

import analysis.forward.ForwardAnalyzer;
import analysis.forward.GlobalCollector;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import client.engine.PypstaAnalysisEngine;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import java.util.HashSet;
import java.util.Set;

// We assume we are going to analysis multiple files (ignoring 'pypsta_mock.py').
// In order to reflect each module file construction to global variables, do module
// calling repeatedly.
// TODO: Introduce module dependency analysis and sort the order of analyzing module.
public class FakeRootForwardFixSolver extends ForwardFixSolver {

    public static final Set<Integer> moduleCallingBBIds = new HashSet<>();
    /**
     * @param problem
     * @param analyzer
     * @param cgNode
     */
    public FakeRootForwardFixSolver(IKilldallFramework<ISSABasicBlock, ForwardState> problem,
                                    ForwardAnalyzer analyzer,
                                    CGNode cgNode) {
        super(
                new ForwardFramework(
                        cgNode.getIR().getControlFlowGraph(),
                        new FakeRootForwardTransferFunction()
                ),
                analyzer,
                cgNode
        );

        IR fakeRootIR = cgNode.getIR();

        for (SSAInstruction instr: cgNode.getIR().getInstructions()) {
            if (instr instanceof PythonInvokeInstruction) {
                PythonInvokeInstruction inst = (PythonInvokeInstruction) instr;
                String calledModuleName = inst.getCallSite().getDeclaredTarget().getDeclaringClass().getName().toString();
                if (calledModuleName.endsWith(".py") && !calledModuleName.contains("pypsta")) {
                    moduleCallingBBIds.add(fakeRootIR.getBasicBlockForInstruction(inst).getGraphNodeId());
                }
            }
        }
    }
}

class FakeRootForwardTransferFunction extends ForwardTransferFunction {
    @Override
    public UnaryOperator<ForwardState> getNodeTransferFunction(ISSABasicBlock node) {
        return new FakeRootForwardNodeTransfer(node);
    }
}

class FakeRootForwardNodeTransfer extends ForwardNodeTransfer {
    private final TypeReference TOP_MODULE_TYPE;
    private final AstGlobalRead dummyInstr;
    private static boolean moduleUpdated;

    public FakeRootForwardNodeTransfer(ISSABasicBlock basicBlock) {
        super(basicBlock);
        TOP_MODULE_TYPE = TypeReference.find(
                PythonTypes.pythonLoader,
                TypeName.string2TypeName(PypstaAnalysisEngine.TOP_MODULE_NAME)
        );
        dummyInstr = new AstGlobalRead(
                -1, -1, FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom("global " + PypstaAnalysisEngine.TOP_MODULE_NAME),
                        PythonTypes.Root
                )
        );
    }

    @Override
    public byte evaluate(ForwardState lhs, ForwardState rhs) {
        if (!FakeRootForwardFixSolver.moduleCallingBBIds.contains(lhs.getBasicBlock().getGraphNodeId())) {
            return super.evaluate(lhs, rhs);
        } else {
            byte superResult;
            if (!moduleUpdated) {
                ForwardAnalyzer forwardAnalyzer = lhs.getSolver().getAnalyzer();
                ExplicitCallGraph cg = forwardAnalyzer.getCGBuilder().getCallGraph();
                Set<CGNode> calleeNodes = cg.getPossibleTargets(
                        lhs.getCGNode(),
                        ((PythonInvokeInstruction) lhs.getBasicBlock().getLastInstruction()).getCallSite()
                );
                for (CGNode calleeNode: calleeNodes) {
                    ForwardFixSolver calleeSolver = forwardAnalyzer.getSolver(calleeNode);
                    if (calleeSolver != null)
                        calleeSolver.addAllStatementsToWorkList();
                }

                ForwardState tmp = new ForwardState(lhs.getSolver(), lhs.getBasicBlock(), false);
                tmp.copyState(rhs);

                IForwardAbstractValue moduleBefore = GlobalCollector.get(
                        dummyInstr, tmp
                );
                int apTableSizeBefore = tmp.getAllocatePointTable().size();

                superResult = super.evaluate(lhs, rhs);

                IForwardAbstractValue moduleAfter = GlobalCollector.get(
                        dummyInstr, tmp
                );
                int apTableSizeAfter = tmp.getAllocatePointTable().size();

                moduleUpdated |= (apTableSizeAfter != apTableSizeBefore);
            } else {
                superResult = super.evaluate(lhs, rhs);
            }

            if (moduleUpdated) {
                long moduleCallInstrNumAfterThis = FakeRootForwardFixSolver.moduleCallingBBIds.stream()
                        .filter(i -> i > lhs.getBasicBlock().getGraphNodeId())
                        .count();
                if (moduleCallInstrNumAfterThis == 0l) {
                    // There is no module calling after this instruction. So reset flag.
                    lhs.getSolver().addAllStatementsToWorkList();
                    moduleUpdated = false;
                }
            }
            return superResult;
        }
    }
}
