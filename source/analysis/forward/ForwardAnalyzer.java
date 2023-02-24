package analysis.forward;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.object.InstanceObjectValue;
import analysis.forward.fixpoint.FakeRootForwardFixSolver;
import analysis.forward.fixpoint.ForwardFixSolver;
import analysis.forward.fixpoint.ForwardFramework;
import analysis.forward.fixpoint.ForwardTransferFunction;
import client.engine.PypstaAnalysisEngine;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import analysis.forward.tracer.ITracer;

import java.util.*;
import java.util.stream.Collectors;

public class ForwardAnalyzer {
    /** Hold solvers based on the call graph node. */
    private final HashMap<CGNode, ForwardFixSolver> solversMap = new HashMap<>();

    private final PropagationCallGraphBuilder cgBuilder;

    private final ITracer tracer;

    public ForwardAnalyzer(PropagationCallGraphBuilder cgBuilder, ITracer tracer) {
        this.cgBuilder = cgBuilder;
        this.tracer = tracer;
    }

    public void analyze() {
        init();

        CGNode fakeRootCGNode = cgBuilder.getCallGraph().getFakeRootNode();
        ForwardFramework framework = new ForwardFramework(
                fakeRootCGNode.getIR().getControlFlowGraph(), new ForwardTransferFunction()
        );
        ForwardFixSolver fakeRootSolver = null;
        if (PypstaAnalysisEngine.TOP_MODULE_NAME.isEmpty()) {
            fakeRootSolver = new ForwardFixSolver(framework, this, fakeRootCGNode);
        } else {
            fakeRootSolver = new FakeRootForwardFixSolver(framework, this, fakeRootCGNode);
        }

        solversMap.put(fakeRootCGNode, fakeRootSolver);

        fakeRootSolver.initForFirstSolve();

        try {
            fakeRootSolver.solve(null);
        } catch (CancelException e) {
            e.printStackTrace();
        }

        ArrayList<ForwardFixSolver> workList = new ArrayList<>();
        boolean remain = true;
        while (remain) {
            remain = false;

            for (Map.Entry<CGNode, ForwardFixSolver> m: solversMap.entrySet()) {
                ForwardFixSolver solver = m.getValue();
                if (!solver.emptyWorkList()) {
                    workList.add(m.getValue());
                    remain = true;
                }
            }

            if (!remain) break;

            while (!workList.isEmpty()) {
                ForwardFixSolver targetSolver = workList.remove(0);

                try {
                    targetSolver.solve(null);
                } catch (CancelException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void init() {
        ExceptionManager.setTracer(tracer);
        GlobalCollector.reset();
        LexicalCollector.reset();

        // TODO: Delete this statement.
        InstanceObjectValue.cha = getClassHierarchy();
    }

    public HashMap<CGNode, ForwardFixSolver> getSolversMap() {
        return solversMap;
    }

    public ITracer getTracer() {
        return tracer;
    }

    public PropagationCallGraphBuilder getCGBuilder() {
        return cgBuilder;
    }

    public IClassHierarchy getClassHierarchy() {
        return cgBuilder.getClassHierarchy();
    }

    public ForwardFixSolver getSolver(CGNode cgNode) {
        return solversMap.get(cgNode);
    }

    public ForwardFixSolver getOrCreateSolver(CGNode cgNode) {
        ForwardFixSolver solver = getSolver(cgNode);
        if (solver == null) {
            ForwardFramework framework = new ForwardFramework(
                    cgNode.getIR().getControlFlowGraph(), new ForwardTransferFunction());
            solver = new ForwardFixSolver(framework, this, cgNode);

            // Collect the solver.
            solversMap.put(cgNode, solver);

            solver.initForFirstSolve();
        }
        return solver;
    }

    public ForwardResult getForwardResult(CGNode cgNode, ISSABasicBlock bb) {
        ForwardFixSolver solver = solversMap.get(cgNode);
        ForwardState lastState = solver.getOut(bb);
        return new ForwardResult(lastState);
    }

    public List<ForwardFixSolver> sortSolver() {
        return solversMap.values().stream().sorted(
                (s1, s2) -> s1.getMethod().toString().compareTo(s2.getMethod().toString())
        ).collect(Collectors.toList());
    }

    /**
     * The class for holding the result of forward analysis.
     */
    public class ForwardResult {
        private final ForwardState state;

        public ForwardResult (ForwardState state) {
            this.state = state;
        }

        public ForwardAbstractValue getValue(int varId) {
            return (ForwardAbstractValue) state.getValue(varId);
        }

        public ForwardState getState() {
            return state;
        }

        public boolean isConstant(int varId) {
            return isNullConstant(varId)
                    || isBoolConstant(varId)
                    || isIntConstant(varId)
                    || isFloatConstant(varId)
                    || isStringConstant(varId);
        }

        public Object getConstant(int varId) {
            if (isBoolConstant(varId)) {
                return getBoolConstant(varId);
            } else if (isIntConstant(varId)) {
                return getIntConstant(varId);
            } else if (isFloatConstant(varId)) {
                return getFloatConstant(varId);
            } else if (isStringConstant(varId)) {
                return getStringConstant(varId);
            }
            return null;
        }

        public boolean isNullConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getUndefValue().isBottom()
                    && abstractValue.getNoneValue().isTop()
                    && abstractValue.getBoolValue().isBottom()
                    && abstractValue.getIntValue().isBottom()
                    && abstractValue.getFloatValue().isBottom()
                    && abstractValue.getStringValue().isBottom()
                    && abstractValue.getAllocatePoints().isBottom();
        }

        public boolean isBoolConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getUndefValue().isBottom()
                    && abstractValue.getNoneValue().isBottom()
                    && !abstractValue.getBoolValue().isTop()
                    && !abstractValue.getBoolValue().isBottom()
                    && abstractValue.getIntValue().isBottom()
                    && abstractValue.getFloatValue().isBottom()
                    && abstractValue.getStringValue().isBottom()
                    && abstractValue.getAllocatePoints().isBottom();
        }

        public boolean getBoolConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getBoolValue().getConcreteValue();
        }

        public boolean isIntConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getUndefValue().isBottom()
                    && abstractValue.getNoneValue().isBottom()
                    && abstractValue.getBoolValue().isBottom()
                    && !abstractValue.getIntValue().isTop()
                    && !abstractValue.getIntValue().isBottom()
                    && abstractValue.getFloatValue().isBottom()
                    && abstractValue.getStringValue().isBottom()
                    && abstractValue.getAllocatePoints().isBottom();
        }

        public int getIntConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getIntValue().getConcreteValue();
        }

        public boolean isFloatConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getUndefValue().isBottom()
                    && abstractValue.getNoneValue().isBottom()
                    && abstractValue.getBoolValue().isBottom()
                    && abstractValue.getIntValue().isBottom()
                    && !abstractValue.getFloatValue().isTop()
                    && !abstractValue.getFloatValue().isBottom()
                    && abstractValue.getStringValue().isBottom()
                    && abstractValue.getAllocatePoints().isBottom();
        }

        public float getFloatConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getFloatValue().getConcreteValue();
        }

        public boolean isStringConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getUndefValue().isBottom()
                    && abstractValue.getNoneValue().isBottom()
                    && abstractValue.getBoolValue().isBottom()
                    && abstractValue.getIntValue().isBottom()
                    && abstractValue.getFloatValue().isBottom()
                    && !abstractValue.getStringValue().isTop()
                    && !abstractValue.getStringValue().isBottom()
                    && abstractValue.getAllocatePoints().isBottom();
        }

        public String getStringConstant(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            return abstractValue.getStringValue().getConcreteValue();
        }

        public Set<TypeReference> getValueTypes(int varId) {
            ForwardAbstractValue abstractValue = (ForwardAbstractValue) state.getValue(varId);
            Set<TypeReference> result =  abstractValue.getAllocatePoints().stream()
                    .map(ap -> state.getAllocatePointTable().get(ap))
                    .map(o -> o.getTypeReference())
                    .collect(Collectors.toSet());

            if (!abstractValue.getBoolValue().isBottom()) result.add(TypeReference.Boolean);
            if (!abstractValue.getIntValue().isBottom()) result.add(TypeReference.Int);
            if (!abstractValue.getFloatValue().isBottom()) result.add(TypeReference.Float);
            if (!abstractValue.getStringValue().isBottom()) result.add(PythonTypes.string);

            return result;
        }
    }
}
