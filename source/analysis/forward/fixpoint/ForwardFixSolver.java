package analysis.forward.fixpoint;

import analysis.forward.ConstantConverter;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.object.ModuleObjectValue;
import client.engine.PypstaAnalysisEngine;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.summaries.SummarizedMethodWithNames;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.debug.Assertions;

public class ForwardFixSolver extends DataflowSolver<ISSABasicBlock, ForwardState> {
    public static String indent = "";

    private final ForwardAnalyzer analyzer;

    private final CGNode cgNode;

    private final SSAInstruction entryInst;

    /**
     * @param problem
     * @param analyzer
     * @param cgNode
     */
    public ForwardFixSolver(IKilldallFramework<ISSABasicBlock, ForwardState> problem,
                            ForwardAnalyzer analyzer,
                            CGNode cgNode) {
        super(problem);
        this.analyzer = analyzer;
        this.cgNode = cgNode;
        this.entryInst = new SSAInstruction(-1) {
            @Override
            public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
                return null;
            }

            @Override
            public String toString(SymbolTable symbolTable) { return null; }

            @Override
            public void visit(IVisitor v) {}

            @Override
            public int hashCode() { return 0; }

            @Override
            public boolean isFallThrough() { return false; }
        };
    }

    @Override
    public void initForFirstSolve() {
        super.initForFirstSolve();

        IMethod method = getMethod();
        ForwardState entryState = getOut(getCFG().entry());

        if (method instanceof FakeRootMethod) {
            // Set this module to variable number 1.
            entryState.setValue(1, new ForwardAbstractValue(
                    new ModuleObjectValue(
                            new AllocatePoint(entryState.getCGNode(), entryInst),
                            TypeReference.findOrCreate(
                                    method.getReference().getDeclaringClass().getClassLoader(),
                                    "FakeRootMethod"
                            )
                    )
            ));
        } else if (method instanceof FakeWorldClinitMethod) {
        } else if (method instanceof PythonSummarizedFunction) {
        } else if (method instanceof SummarizedMethodWithNames) {
        } else {
            // Add all constants (abstract value) to current environment.
            SymbolTable symtab = ((CAstAbstractModuleLoader.DynamicMethodObject) method).symbolTable();
            for (int varNum = 2; varNum <= symtab.getMaxValueNumber(); varNum++) {
                Value value = symtab.getValue(varNum);
                if(value != null) {
                    entryState.setValue(varNum, ConstantConverter.convert((ConstantValue) value));
                }
            }
        }
    }

    @Override
    public boolean solve(MonitorUtil.IProgressMonitor monitor) throws CancelException {
        if (PypstaAnalysisEngine.DEBUG) {
            indent = indent.concat(" ");
            System.out.println(indent + "Start solve: " + this);
        }

        ForwardInstructionVisitor oldVisitor = ForwardInstructionVisitor.instance();
        ForwardState callerState = oldVisitor.getForwardState();
        if (callerState != null) {
            oldVisitor.globalUpdate(callerState);
            oldVisitor.lexicalUpdate(callerState);
        }

        ForwardInstructionVisitor.push();

        boolean result = super.solve(monitor);

        ForwardInstructionVisitor currentVisitor = ForwardInstructionVisitor.instance();
        ForwardState exitState = getOut(getCFG().exit());
        currentVisitor.globalUpdate(exitState);
        currentVisitor.lexicalUpdate(exitState);

        if (PypstaAnalysisEngine.DEBUG) {
            System.out.println(indent + "Finish solve: " + this);
            indent = indent.substring(1);
        }

        ForwardInstructionVisitor.pop();

        return result;
    }

    /**
     * @param n a node
     * @param IN whether this is IN node
     * @return a fresh variable to represent the lattice value at the IN or OUT of n
     */
    @Override
    protected ForwardState makeNodeVariable(ISSABasicBlock n, boolean IN) {
        return new ForwardState(this, n, IN);
    }

    @Override
    protected ForwardState makeEdgeVariable(ISSABasicBlock src, ISSABasicBlock dst) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    protected ForwardState[] makeStmtRHS(int size) {
        return new ForwardState[size];
    }

    public ForwardAnalyzer getAnalyzer() {
        return analyzer;
    }

    public CGNode getCGNode() {
        return cgNode;
    }

    public IMethod getMethod() {
        return getCGNode().getMethod();
    }

    public SSACFG getCFG() {
        return cgNode.getIR().getControlFlowGraph();
    }

    @Override
    public String toString() {
        return "solver: " + cgNode.getMethod().getReference().getDeclaringClass().getName().toString()
                + '[' + cgNode.getContext().toString() + ']';
    }
}
