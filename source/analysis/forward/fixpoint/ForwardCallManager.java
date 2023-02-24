package analysis.forward.fixpoint;

import analysis.forward.Arguments;
import analysis.forward.BuiltinFunctionSummaries;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.FunctionSummaryCache;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.StringValue;
import analysis.forward.abstraction.value.object.*;
import client.cls.PypstaFakeClass;
import client.engine.PypstaAnalysisEngine;
import client.loader.PythonSpecialMethodCallSiteReference;
import client.method.PypstaFakeMethod;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PypstaLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Set;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;
import java.util.stream.StreamSupport;

public class ForwardCallManager {
    public final static List<String> specialMethodNames = new ArrayList<String>() {{
        add("__call__");
        add("__add__");
        add("__sub__");
        add("__mul__");
        add("__rmul__");
        add("__div__");

        add("__setitem__");
        add("__getitem__");
    }};

    private static final Stack<ForwardState> stateStack = new Stack<>();

    public static Stack<ForwardState> getStateStack() {
        return stateStack;
    }

    private static boolean isInvalidCacheMethod(MethodReference methodRef) {
        String[] invalidCacheMethods = new String[]{
                "Lpyperf/Runner/bench_func", "Lpyperf/Runner/bench_time_func"
        };

        String methodName = methodRef.getDeclaringClass().getName().toString();
        for (String invalidCacheMethod: invalidCacheMethods) {
            if (invalidCacheMethod.equals(methodName)) return true;
        }
        return false;
    }

    private static boolean isXMLSummary(
            ForwardFixSolver solver, ObjectValue target, SSAInstruction inst) {
        return !target.getFullName().startsWith("Lscript ")
                && !target.getFullName().startsWith("L$script ")
                && !BuiltinFunctionSummaries.summaries.containsKey(
                        target.getFullName())
                && target instanceof FunctionObjectValue
                && !isInvalidCacheMethod(((FunctionObjectValue) target).getMethodReference())
                && inst instanceof SSAAbstractInvokeInstruction
                && getTargetMethod(
                        solver.getCGNode(),
                        solver.getAnalyzer(),
                        ((FunctionObjectValue) target).getMethodReference(),
                        ((SSAAbstractInvokeInstruction) inst).getCallSite()
                    ) instanceof SummarizedMethod;
    }

    private static IForwardAbstractValue xmlSummaryCall(ForwardState callerState,
                                                        MethodReference methodRef,
                                                        SSAAbstractInvokeInstruction inst,
                                                        CGNode cgCalleeNode) {
        SummarizedMethod summarizedMethod = (SummarizedMethod) getTargetMethod(
                callerState.getCGNode(),
                callerState.getSolver().getAnalyzer(),
                methodRef,
                inst.getCallSite()
        );

        if (FunctionSummaryCache.hasCache(summarizedMethod)) {
            return FunctionSummaryCache.get(summarizedMethod);
        }

        // Get callee solver.
        ForwardFixSolver calleeSolver = callerState.getSolver().getAnalyzer().getOrCreateSolver(cgCalleeNode);
        stateStack.push(callerState);

        try {
            calleeSolver.solve(null);
        } catch (CancelException e) {
            e.printStackTrace();
        }

        stateStack.pop();

        ForwardState calleeExitState = calleeSolver.getOut(cgCalleeNode.getIR().getExitBlock());
        AllocatePointTable calleeExitAPTable = calleeExitState.getAllocatePointTable();
        AllocatePointTable callerAPTable = callerState.getAllocatePointTable();

        // Calculate real arguments' side effects.
        calculateRealArgsSideEffect(
                Arguments.getPositionalParams(inst), callerState, calleeExitState
        );

        IForwardAbstractValue returnValue = calculateReturnValue(
                calleeExitState, calleeExitAPTable, callerAPTable);

        return returnValue;
    }

    /**
     * Calculate callee's abstract return value.
     * @param inst the call instruction
     * @param callerState the caller state.
     * @return the abstract value of callee's return value.
     */
    public static IForwardAbstractValue call(PythonInvokeInstruction inst, ForwardState callerState) {
        IForwardAbstractValue result = new ForwardAbstractValue();

        ForwardFixSolver solver = callerState.getSolver();
        ForwardAnalyzer analyzer = solver.getAnalyzer();

        CGNode callerNode = solver.getCGNode();

        Set<ObjectValue> calleeObjects = Iterator2Set.toSet(
                ((ForwardAbstractValue) callerState.getValue(inst.getUse(0)))
                        .getAllocatePoints()
                        .getObjectsIterator(callerState.getAllocatePointTable())
        );

        for (ObjectValue calleeObject: calleeObjects) {
            assert calleeObject.isCallable(callerState.getAllocatePointTable());

            IForwardAbstractValue returnValue = null;

            if (calleeObject instanceof InstanceObjectValue && calleeObject.hasAttr("__call__", callerState.getAllocatePointTable())) {
                returnValue = callSpecialFunction(inst, calleeObject, callerState, "__call__");
            } else {
                IMethod targetMethod = null;
                BuiltinFunctionSummaries.Summary summary = null;
                CGNode calleeNode = null;
                if (calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockRangeTypeRef)) {
                    targetMethod = getTargetMethod(
                            callerNode,
                            analyzer,
                            TypeReference.find(PythonTypes.pythonLoader, "Lwala/builtin/range"),
                            inst.getCallSite()
                    );

                    ExplicitCallGraph cg = analyzer.getCGBuilder().getCallGraph();

                    // Remove call graph edge to mock 'range' node.
                    IMethod rangeMockMethod = getTargetMethod(
                            callerNode, analyzer, calleeObject.getTypeReference(), inst.getCallSite()
                    );
                    CGNode rangeMockCGNode = null;
                    try {
                        rangeMockCGNode = cg.findOrCreateNode(
                                rangeMockMethod,
                                analyzer.getCGBuilder().getContextSelector().getCalleeTarget(
                                        solver.getCGNode(),
                                        inst.getCallSite(),
                                        rangeMockMethod,
                                        new InstanceKey[0]
                                )
                        );
                    } catch (CancelException e) {
                        e.printStackTrace();
                    }
                    if (cg.hasEdge(callerNode, rangeMockCGNode)) {
                        cg.removeEdge(callerNode, rangeMockCGNode);
                        ((ExplicitCallGraph.ExplicitNode) callerNode).removeTarget(rangeMockCGNode);
                    }

                    summary = BuiltinFunctionSummaries.summaries.get("Lwala/builtin/range");

                    // Add call graph edge to the callee node.
                    calleeNode = addCGEdge(inst, solver, targetMethod);
                } else if (calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaIterTypeRef)
                        || calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaNextTypeRef)
                        || calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaSliceTypeRef)) {
                    ExplicitCallGraph cg = analyzer.getCGBuilder().getCallGraph();

                    // Remove call graph edge to mock 'iter' or 'next' node.
                    IMethod pypstaMockMethod = getTargetMethod(
                            callerNode, analyzer, calleeObject.getTypeReference(), inst.getCallSite()
                    );
                    CGNode pypstaMockCGNode = null;
                    try {
                        pypstaMockCGNode = cg.findOrCreateNode(
                                pypstaMockMethod,
                                analyzer.getCGBuilder().getContextSelector().getCalleeTarget(
                                        solver.getCGNode(),
                                        inst.getCallSite(),
                                        pypstaMockMethod,
                                        new InstanceKey[0]
                                )
                        );
                    } catch (CancelException e) {
                        e.printStackTrace();
                    }
                    if (cg.hasEdge(callerNode, pypstaMockCGNode)) {
                        cg.removeEdge(callerNode, pypstaMockCGNode);
                        ((ExplicitCallGraph.ExplicitNode) callerNode).removeTarget(pypstaMockCGNode);
                    }

                    if (calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaIterTypeRef)) {
                        summary = BuiltinFunctionSummaries.summaries.get("Liter");
                    } else if (calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaNextTypeRef)) {
                        summary = BuiltinFunctionSummaries.summaries.get("Lnext");
                    } else if (calleeObject.getTypeReference().equals(BuiltinFunctionSummaries.mockPypstaSliceTypeRef)) {
                        summary = BuiltinFunctionSummaries.summaries.get("Lwala/builtin/slice");
                    } else {
                        Assertions.UNREACHABLE();
                    }
                } else {
                    targetMethod = getTargetMethod(
                            callerNode, analyzer, calleeObject.getTypeReference(), inst.getCallSite()
                    );

                    if (calleeObject instanceof BuiltinFunctionSummaries.Summary) {
                        summary = (BuiltinFunctionSummaries.Summary) calleeObject;
                    } else if (StringValue.methods.containsKey(calleeObject.getObjectName())) {
                        summary = StringValue.methods.get(calleeObject.getObjectName());
                    } else if (BuiltinFunctionSummaries.summaries.containsKey(calleeObject.getFullName())) {
                        summary = BuiltinFunctionSummaries.summaries.get(calleeObject.getFullName());
                    } else {
                        // Don't use summary.
                        assert calleeObject instanceof CallableObjectValue;
                    }

                    // Add call graph edge to the callee node.
                    calleeNode = addCGEdge(inst, solver, targetMethod);
                }

                if (summary != null) {
                    ArrayList<IForwardAbstractValue> realArgsValue =
                            Arguments.convertToAbstractValues(callerState, inst);
                    if (((ForwardAbstractValue) realArgsValue.get(0)).getAllocatePoints().size() != 1) {
                        realArgsValue.remove(0);
                        realArgsValue.add(0, new ForwardAbstractValue(calleeObject));
                    }
                    // If there is a summary object, call the summary.
                    returnValue = summary.call(callerState, realArgsValue, inst);
                } else if (isXMLSummary(solver, calleeObject, inst)) {
                    returnValue = xmlSummaryCall(
                            callerState,
                            ((CallableObjectValue) calleeObject).getMethodReference(),
                            inst,
                            calleeNode
                    );
                } else if (callerNode.getMethod().equals(targetMethod)) {
                    // Recursive calling.
                    CallGraph cg = solver.getAnalyzer().getCGBuilder().getCallGraph();

                    cg.removeEdge(callerNode, calleeNode);
                    ((ExplicitCallGraph.ExplicitNode) callerNode).removeTarget(calleeNode);

                    // Add call graph edge to self call graph node.
                    cg.addEdge(callerNode, callerNode);
                    ((ExplicitCallGraph.ExplicitNode) callerNode).addTarget(inst.getCallSite(), callerNode);

                    // Calculate return value.
                    ForwardState calleeExitState = solver.getOut(callerNode.getIR().getExitBlock());
                    IForwardAbstractValue returnVal = calculateReturnValue(
                            calleeExitState, calleeExitState.getAllocatePointTable(), callerState.getAllocatePointTable()
                    );

                    return returnVal;
                } else {
                    // If there is no summary object, call normal may.
                    returnValue = newContext(inst, calleeObject, callerState, calleeNode);
                }
            }
            result.union(returnValue);
        }

        return result;
    }

    /**
     * Add call graph edge to call graph and the caller node.
     * @param inst the call instruction.
     * @param callerSolver the solver of caller.
     * @param calleeMethod the callee target method.
     * @return the callee call graph node.
     */
    private static CGNode addCGEdge(PythonInvokeInstruction inst,
                                    ForwardFixSolver callerSolver,
                                    IMethod calleeMethod) {
        CGNode callerNode = callerSolver.getCGNode();
        ForwardAnalyzer analyzer = callerSolver.getAnalyzer();
        ExplicitCallGraph cg = analyzer.getCGBuilder().getCallGraph();

        InstanceKey[] instanceKeys = null;
        if (calleeMethod.getDeclaringClass().getReference().equals(PythonTypes.superfun)) {
            PointerAnalysis pointerAnalysis = analyzer.getCGBuilder().getPointerAnalysis();
            PointerKey calleeInstanceKey = (PointerKey) StreamSupport
                    .stream(pointerAnalysis.getPointerKeys().spliterator(), true)
                    .filter(k -> k instanceof LocalPointerKey)
                    .filter(k -> ((LocalPointerKey) k).getNode().equals(callerNode))
                    .filter(k -> ((LocalPointerKey) k) .getValueNumber() == inst.getUse(0))
                    .findFirst()
                    .orElse(null);
            InstanceKey mappedObject = (InstanceKey) pointerAnalysis.getPointsToSet(calleeInstanceKey).iterator().next();
            instanceKeys = new InstanceKey[1];
            instanceKeys[0] = mappedObject;
        } else {
            instanceKeys = new InstanceKey[0];
        }
        CGNode calleeNode = null;
        try {
            calleeNode = cg.findOrCreateNode(
                    calleeMethod,
                    analyzer.getCGBuilder().getContextSelector().getCalleeTarget(
                            callerSolver.getCGNode(),
                            inst.getCallSite(),
                            calleeMethod,
                            instanceKeys
                    )
            );
        } catch (CancelException e) {
            e.printStackTrace();
        }

        if (!cg.hasEdge(callerNode, calleeNode)) {
            cg.addEdge(callerNode, calleeNode);
            callerNode.addTarget(inst.getCallSite(), calleeNode);
        }

        return calleeNode;
    }

    /**
     * From 'TYpeReference', get method which it refers.
     * @param callerNode the call graph node of caller.
     * @param analyzer the forward analyzer to use class hierarchy and so on.
     * @param typeRef The target method's type reference object.
     * @param callerSite The caller site where this calling is invoked.
     * @return The method object which has the type reference object.
     */
    private static IMethod getTargetMethod(CGNode callerNode,
                                           ForwardAnalyzer analyzer,
                                           TypeReference typeRef,
                                           CallSiteReference callerSite) {
        return getTargetMethod(
                callerNode,
                analyzer,
                MethodReference.findOrCreate(
                        PythonLanguage.Python,
                        typeRef,
                        AstMethodReference.fnAtomStr,
                        "()" + AstTypeReference.rootTypeDescStr
                ),
                callerSite);
    }

    /**
     * From 'MethodReference', get method which it refers.
     * @param callerNode the call graph node of caller.
     * @param analyzer the forward analyzer to use class hierarchy and so on.
     * @param methodRef The target's method reference object.
     * @param callerSite The caller site where this calling is invoked.
     * @return The method object which has the method reference object.
     */
    private static IMethod getTargetMethod(CGNode callerNode,
                                           ForwardAnalyzer analyzer,
                                           MethodReference methodRef,
                                           CallSiteReference callerSite) {
        IClassHierarchy cha = analyzer.getClassHierarchy();
        IClass kls = cha.lookupClass(methodRef.getDeclaringClass());
        if (kls == null) {
            kls = analyzer.getCGBuilder().getOptions().getClassTargetSelector().getAllocatedTarget(
                    callerNode, new NewSiteReference(0, methodRef.getDeclaringClass())
            );
        }

        // The inherited classes don't have inherited parents' methods. These methods are
        // not analyzed in pointer analysis, so their trampoline methods aren't created.
        // So create 'PythonInstanceMethodTrampoline' class and create trampoline method
        // in 'PythonTrampolineTargetSelector'.
        if (kls == null && methodRef.getDeclaringClass().getName().toString().contains("$")) {
            kls = new PythonInstanceMethodTrampoline(
                    TypeReference.find(
                            PythonTypes.pythonLoader,
                            "L" + methodRef.getDeclaringClass().getName().toString().substring(2)
                    ),
                    cha
            );
        }

        if (kls == null
                || kls.getReference().equals(PythonTypes.tuple)
                || kls.getReference().equals(PythonTypes.set)
                || kls.getReference().equals(PythonTypes.filter)) {
            // If 'IClass' object is not created in pointer analysis, create (fake) 'IClass' object here.
            kls = new PypstaFakeClass(methodRef.getDeclaringClass(), cha);
            PypstaFakeMethod fakeMethod = new PypstaFakeMethod(methodRef, kls);
            return fakeMethod;
        } else {
            return analyzer.getCGBuilder().getOptions().getMethodTargetSelector().getCalleeTarget(
                    callerNode, callerSite, kls
            );
        }
    }



    public static IForwardAbstractValue callSpecialFunction(SSAInstruction inst,
                                                            ObjectValue instance,
                                                            ForwardState callerState,
                                                            String methodName) {
        ForwardFixSolver callerSolver = callerState.getSolver();
        ExplicitCallGraph cg = callerSolver.getAnalyzer().getCGBuilder().getCallGraph();

        IForwardAbstractValue result = new ForwardAbstractValue();

        for (ObjectValue targetMethodObj: ((ForwardAbstractValue) instance.getAttr(methodName, callerState.getAllocatePointTable()))
                .getAllocatePoints()
                .getObjectsIterable(callerState.getAllocatePointTable())) {
            PythonSpecialMethodCallSiteReference site = new PythonSpecialMethodCallSiteReference(
                    targetMethodObj.getTypeReference(), inst.iIndex(), inst
            );

            // Get target method represented by Ariadne.
            IMethod targetMethod = getTargetMethod(
                    callerSolver.getCGNode(),
                    callerSolver.getAnalyzer(),
                    targetMethodObj.getTypeReference(),
                    site
            );

            try {
                // Create target call graph node. (When you build the call graph, pointer analysis
                // system can't know what function you call from here. So there is no call graph node.)
                CGNode targetNode = cg.findOrCreateNode(
                        targetMethod,
                        callerSolver.getAnalyzer().getCGBuilder().getContextSelector().getCalleeTarget(
                                callerSolver.getCGNode(), site, targetMethod, new InstanceKey[0]
                        ));

                if (!cg.hasEdge(callerSolver.getCGNode(), targetNode)) {
                    callerSolver.getCGNode().addTarget(site, targetNode);
                    cg.addEdge(callerSolver.getCGNode(), targetNode);
                }

                IForwardAbstractValue returnValue = null;
                if (isXMLSummary(callerSolver, targetMethodObj, inst)) {
                    returnValue = xmlSummaryCall(
                            callerState,
                            ((CallableObjectValue) targetMethodObj).getMethodReference(),
                            (SSAAbstractInvokeInstruction) inst,
                            targetNode
                    );
                } else {
                    returnValue = newContext(inst, targetMethodObj, callerState, targetNode);
                }
                result.union(returnValue);
            } catch (CancelException e) {
                e.printStackTrace();
                Assertions.UNREACHABLE();
            }
        }
        return result;
    }

    /**
     * Change the environment, context and so on.
     * @param inst the call instruction
     * @param targetObject the target (callee) object value.
     * @param callerState the current transferring state.
     * @param cgCalleeNode the target call graph node.
     * @return the abstract value of callee's return value.
     */
    private static IForwardAbstractValue newContext(SSAInstruction inst,
                                                    ObjectValue targetObject,
                                                    ForwardState callerState,
                                                    CGNode cgCalleeNode) {
        // Get callee solver and set real arguments.
        ForwardFixSolver calleeSolver = callerState.getSolver().getAnalyzer().getSolver(cgCalleeNode);
        if (calleeSolver == null) {
            calleeSolver = callerState.getSolver().getAnalyzer().getOrCreateSolver(cgCalleeNode);
        }

        // Set real arguments.
        ForwardState calleeEntryState = calleeSolver.getIn(
                cgCalleeNode.getIR().getControlFlowGraph().entry());
        boolean hasChanged;
        if (cgCalleeNode.getMethod() instanceof PypstaLoader.IPypstaMethod) {
            hasChanged = ((PypstaLoader.IPypstaMethod) cgCalleeNode.getMethod()).getArgs()
                    .parseArguments(targetObject, callerState, calleeEntryState, inst);
        } else {
            hasChanged = Arguments.parseArgumentsOfSyntheticMethod(
                    targetObject, callerState, calleeEntryState, inst);
        }

        if (hasChanged) {
            // Add entry state to work list.
            calleeSolver.changedVariable(calleeEntryState);
        } else {
            if (PypstaAnalysisEngine.DEBUG) {
                System.out.println(ForwardFixSolver.indent + " " + "Skip call: " + targetObject);
            }
        }

        stateStack.push(callerState);

        try {
            calleeSolver.solve(null);
        } catch (CancelException e) {
            e.printStackTrace();
        }

        stateStack.pop();

        ForwardState calleeExitState = calleeSolver.getOut(cgCalleeNode.getIR().getExitBlock());
        AllocatePointTable calleeExitAPTable = calleeExitState.getAllocatePointTable();
        AllocatePointTable callerAPTable = callerState.getAllocatePointTable();

        // Calculate real arguments' side effects.
        calculateRealArgsSideEffect(
                Arguments.getPositionalParams(inst), callerState, calleeExitState
        );

        IForwardAbstractValue returnValue = calculateReturnValue(calleeExitState, calleeExitAPTable, callerAPTable);

        return returnValue;
    }

    /**
     * Calculate real arguments' side effects.
     * @param callerRealArgIds the variable ids of real arguments
     * @param callerState the caller state
     * @param calleeExitState the callee exit state
     */
    private static void calculateRealArgsSideEffect(int[] callerRealArgIds,
                                                    ForwardState callerState,
                                                    ForwardState calleeExitState) {
        AllocatePointTable callerAPTable = callerState.getAllocatePointTable();
        AllocatePointTable calleeExitAPTable = calleeExitState.getAllocatePointTable();

        if (calleeExitState.getSolver().getMethod() instanceof PypstaLoader.IPypstaMethod
                && ((PypstaLoader.IPypstaMethod)calleeExitState.getSolver().getMethod())
                        .getArgs().getDecoratorList().contains("classmethod")) {
            int d = 0;
            for (int i = 0; i < calleeExitState.getSolver().getMethod().getNumberOfParameters() && i < callerRealArgIds.length+1; i++) {
                int calleeFormalArgId = i+1;
                if (calleeFormalArgId == 2) {
                    // TODO: Propagate effects for class. (In @classmethod, second formal arg is 'cls' object)
                    d = 1;
                    continue;
                }

                IForwardAbstractValue calleeFormalValue = calleeExitState.getValue(calleeFormalArgId);
                callerAPTable.takeInSingleValue(calleeFormalValue, calleeExitAPTable);

                int callerRealArgId = callerRealArgIds[i - d];
                IForwardAbstractValue callerRealArg = callerState.getValue(callerRealArgId);
                callerRealArg.union(calleeFormalValue);
            }
        } else {
            int i = 0;

            String methodName = calleeExitState.getSolver().getMethod().getReference().getDeclaringClass().getName().getClassName().toString();
            if (specialMethodNames.contains(methodName)) {
                // If the callee is special method, don't merge the side effect of callee object.
                i = 1;
            }

            for (; i < calleeExitState.getSolver().getMethod().getNumberOfParameters() && i < callerRealArgIds.length; i++) {
                int calleeFormalArgId = i+1;
                IForwardAbstractValue calleeFormalValue = calleeExitState.getValue(calleeFormalArgId);
                callerAPTable.takeInSingleValue(calleeFormalValue, calleeExitAPTable);

                int callerRealArgId = callerRealArgIds[i];
                IForwardAbstractValue callerRealArg = callerState.getValue(callerRealArgId);
                callerRealArg.union(calleeFormalValue);
            }
        }
    }

    /**
     * Calculate the abstract value of value flow graph based on exit state. Also, caller allocate point table take in
     * the objects which the return values are points to.
     * @param calleeExitState the callee exit state
     * @param calleeExitAPTable allocate point table of callee
     * @param callerAPTable allocate point table of caller point
     * @return the abstract value of return value
     */
    private static IForwardAbstractValue calculateReturnValue(ForwardState calleeExitState,
                                                              AllocatePointTable calleeExitAPTable,
                                                              AllocatePointTable callerAPTable) {
        // Calculate abstract value of return value.
        IForwardAbstractValue returnValue = new ForwardAbstractValue();
        Integer[] returnVarIds
                = Arrays.stream(
                    calleeExitState.getCGNode().getIR().getControlFlowGraph().getInstructions())
                .parallel()
                .filter(i -> i instanceof SSAReturnInstruction)
                .map(i -> (SSAReturnInstruction) i)
                .map(i -> i.getResult())
                .toArray(Integer[]::new);

        for (int returnVarId: returnVarIds) {
            IForwardAbstractValue singleReturnValue = calleeExitState.getValue(returnVarId);

            // The objects pointed from return value is taken to caller allocate point table.
            callerAPTable.takeInSingleValue(singleReturnValue, calleeExitAPTable);
            returnValue.union(singleReturnValue.copy());
        }

        return returnValue;
    }
}
