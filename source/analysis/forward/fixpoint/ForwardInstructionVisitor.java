package analysis.forward.fixpoint;

import analysis.forward.ExceptionManager;
import analysis.forward.GlobalCollector;
import analysis.forward.LexicalCollector;
import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.*;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.abstraction.value.object.*;
import client.engine.PypstaAnalysisEngine;
import com.ibm.wala.cast.ir.ssa.*;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.parser.AbstractParser;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonPropertyRead;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import util.operator.IPythonOperator;
import util.operator.PythonOperatorFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ForwardInstructionVisitor implements PythonInstructionVisitor {
    private static final Stack<ForwardInstructionVisitor> stack = new Stack<>();

    private static ForwardInstructionVisitor instance = new ForwardInstructionVisitor();

    private ForwardInstructionVisitor() {}

    public static ForwardInstructionVisitor instance() {
        return instance;
    }

    public static void push() {
        stack.push(instance);
        instance = new ForwardInstructionVisitor();
    }

    public static void pop() {
        instance = stack.pop();
    }

    // Save the variable id and corresponding global/lexical identifiers.
    // This is used to update global/lexical values which are updated after write instruction.
    // There are some cases where more than 2 read instructions are in the one function,
    // so this table's key must be variable id, not the global/lexical identifier.
    private HashMap<Integer, String> globalVarTable = new HashMap<>();
    private HashMap<Integer, String> lexicalVarTable = new HashMap<>();

    public void globalUpdate(ForwardState exitState) {
        for (Map.Entry<Integer, String> m: globalVarTable.entrySet()) {
            GlobalCollector.update(m.getValue(), m.getKey(), exitState);
        }
    }

    public void lexicalUpdate(ForwardState exitState) {
        for (Map.Entry<Integer, String> m: lexicalVarTable.entrySet()) {
            LexicalCollector.update(m.getValue(), m.getKey(), exitState);
        }
    }

    private ForwardState forwardState;

    public ForwardState getForwardState() {
        return forwardState;
    }

    public void setForwardState(ForwardState forwardState) {
        this.forwardState = forwardState;
    }

    private boolean changed = false;

    public void resetChanged() {
        changed = false;
    }

    private void unionChanged(boolean b) {
        changed |= b;
    }

    public boolean isChanged() {
        return changed;
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead inst) {
        if (inst.getGlobalName().contains("global ")) {
            IForwardAbstractValue globalValue = GlobalCollector.get(inst, forwardState);
            unionChanged(forwardState.updateValue(inst.getDef(), globalValue));
        } else {
            Assertions.UNREACHABLE();
        }
        globalVarTable.put(inst.getDef(), inst.getGlobalName());
    }

    @Override
    public void visitAstGlobalWrite(AstGlobalWrite inst) {
        GlobalCollector.put(inst, forwardState, forwardState.getValue(inst.getVal()));

        // Delete old entry.
        Set<Integer> toRemove = globalVarTable.entrySet().stream().parallel()
                        .filter(m -> m.getValue().equals(inst.getGlobalName()))
                        .map(m -> m.getKey())
                        .collect(Collectors.toSet());
        toRemove.forEach(i -> globalVarTable.remove(i));

        // Register the result var id for this global identifier to table.
        globalVarTable.put(inst.getUse(0), inst.getGlobalName());
    }

    @Override
    public void visitAstLexicalWrite(AstLexicalWrite inst) {
        for (AstLexicalAccess.Access access: inst.getAccesses()) {
            LexicalCollector.put(access, forwardState);
            lexicalVarTable.put(access.valueNumber, access.variableName);

            // Delete old entry.
            Set<Integer> toRemove = lexicalVarTable.entrySet().stream().parallel()
                    .filter(m -> m.getValue().equals(access.variableName))
                    .map(m -> m.getKey())
                    .collect(Collectors.toSet());
            toRemove.forEach(i -> lexicalVarTable.remove(i));

            // Register the result var id for this lexical identifier to table.
            lexicalVarTable.put(access.valueNumber, access.variableName);
        }
    }

    @Override
    public void visitAstLexicalRead(AstLexicalRead inst) {
        for (AstLexicalAccess.Access access: inst.getAccesses()) {
            IForwardAbstractValue copiedLexicalValue = LexicalCollector.get(
                    access, forwardState.getAllocatePointTable()
            );
            if (copiedLexicalValue == null)
                copiedLexicalValue = new ForwardAbstractValue();

            assert inst.getAccesses().length == 1;
            unionChanged(forwardState.updateValue(access.valueNumber, copiedLexicalValue));

            // Register the result var id for this lexical identifier to table.
            lexicalVarTable.put(access.valueNumber, access.variableName);
        }
    }

    @Override
    public void visitNew(SSANewInstruction inst) {
        unionChanged(forwardState.updateValue(inst.getDef(), allocate(inst, inst.getConcreteType())));
    }

    @Override
    public void visitGet(SSAGetInstruction inst) {
        int result = inst.getDef();
        int object = inst.getRef();
        FieldReference fieldRef = inst.getDeclaredField();

        IForwardAbstractValue abstractObject = forwardState.getValue(object);

        ForwardAbstractValue abstractResult = new ForwardAbstractValue();
        for (ObjectValue objectValue:
                ((ForwardAbstractValue) abstractObject).getAllocatePoints().getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.hasAttr(fieldRef, forwardState.getAllocatePointTable())) {
                abstractResult.union((ForwardAbstractValue) objectValue.getAttr(fieldRef, forwardState.getAllocatePointTable()));
            } else {
                ExceptionManager.attributeException(objectValue, fieldRef);
            }
        }

        unionChanged(forwardState.updateValue(result, abstractResult));
    }

    @Override
    public void visitPut(SSAPutInstruction inst) {
        int value = inst.getVal();
        int object = inst.getRef();
        FieldReference fieldRef = inst.getDeclaredField();

        IForwardAbstractValue abstractValue = forwardState.getValue(value);
        IForwardAbstractValue abstractObject = forwardState.getValue(object);

        for (ObjectValue objectValue:
                ((ForwardAbstractValue) abstractObject).getAllocatePoints()
                        .getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (fieldRef.getName().toString().equals("*")) {
                // Handling '*' import.
                // This means that rhs variable (whose id is 'value') contains all imported objects in package,
                // and you don't know the objects' attribute name. So get module object from previous instruction
                // and copy all attributes and its values to module object.

                // This instruction is 'MODULE.*'.
                PythonPropertyRead starImportInstr = Arrays.stream(forwardState.getCGNode().getIR().getInstructions())
                        .parallel()
                        .filter(i -> i instanceof PythonPropertyRead)
                        .map(i -> (PythonPropertyRead) i)
                        .filter(i -> i.getDef() == value)
                        .collect(Collectors.toList())
                        .get(0);
                int moduleVarId = starImportInstr.getObjectRef();

                ForwardAbstractValue starImportedObjsAbstractValue
                        = (ForwardAbstractValue) forwardState.getValue(moduleVarId);
                for (ObjectValue starImportedEachObject:
                        starImportedObjsAbstractValue.getAllocatePoints()
                                .getObjectsIterable(forwardState.getAllocatePointTable())) {
                    for (Object attr: starImportedEachObject.getAttributes().keySet()) {
                        objectValue.setAttr(
                                (String) attr,
                                starImportedEachObject.getAttr(
                                        (String) attr, forwardState.getAllocatePointTable()
                                )
                        );
                    }
                }
            } else if (fieldRef.getName().toString().equals("__init__")
                    && forwardState.getCGNode().getMethod().getDeclaringClass().getReference().getName().toString()
                            .equals("Lscript __init__.py")
                    && value == 1) {
                // Declared variable in '__init__.py' can be accessed as its package attributes (<PACKAGE>.<VAR_NAME>).
                // So set each value to package object in each variable names, not '__init__'.
                for (ObjectValue moduleObjectValue:
                        ((ForwardAbstractValue) abstractValue).getAllocatePoints()
                                .getObjectsIterable(forwardState.getAllocatePointTable())) {
                    for (Object attr : moduleObjectValue.getAttributes().keySet()) {
                        objectValue.setAttr(
                                (String) attr,
                                moduleObjectValue.getAttr(
                                        (String) attr, forwardState.getAllocatePointTable()
                                )
                        );
                    }
                }
            } else {
                objectValue.setAttr(fieldRef, abstractValue);
            }
        }
    }

    @Override
    public void visitPropertyRead(AstPropertyRead inst) {
        int result = inst.getDef();
        int object = inst.getObjectRef();
        int field = inst.getMemberRef();

        IForwardAbstractValue abstractObject = forwardState.getValue(object);

        if (((ForwardAbstractValue) abstractObject).getNoneValue().isTop()) {
            ExceptionManager.noneException(object);
        }

        // Check the target object abstract value has object values.
        if (((ForwardAbstractValue) abstractObject).getAllocatePoints().size() < 1) {
            IForwardAbstractValue resultValue = null;
            if (((ForwardAbstractValue) abstractObject).getStringValue().isBottom()) {
                resultValue = new ForwardAbstractValue();
            } else {
                IForwardAbstractValue abstractField = forwardState.getValue(field);
                // If there is a string value, this reading may get single character or the method of string object.
                if (!((ForwardAbstractValue) abstractField).getIntValue().isBottom()) {
                    // Access for character for string.
                    // TODO: More specific.
                    resultValue = new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
                } else {
                    // Access for method of 'str' class.
                    resultValue = StringValue.getMethod(abstractField, forwardState, inst);
                }
            }
            unionChanged(forwardState.updateValue(result, resultValue));
            return;
        }

        IForwardAbstractValue abstractField = forwardState.getValue(field);
        ForwardAbstractValue abstractResult = new ForwardAbstractValue();
        if (abstractField.isBottom()) {
            forwardState.updateValue(result, abstractResult);
            return;
        }

        for (ObjectValue objectValue: ((ForwardAbstractValue) abstractObject).getAllocatePoints().getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue instanceof ModuleObjectValue
                    && ((ForwardAbstractValue) abstractField).getStringValue().getConcreteValue() != null
                    && ((ForwardAbstractValue) abstractField).getStringValue().getConcreteValue().equals("*")) {
                // Handle '*' import.
                for (Object attr: objectValue.getAttributes().keySet()) {
                    abstractResult.union((ForwardAbstractValue) objectValue.getAttr(
                            (String) attr, forwardState.getAllocatePointTable()
                    ));
                }
            } else if (objectValue instanceof EnumerateObjectValue) {
                abstractResult.union((ForwardAbstractValue) ((EnumerateObjectValue) objectValue).getElement(forwardState, inst));
            } else if (objectValue instanceof ZipObjectValue) {
                abstractResult.union((ForwardAbstractValue) ((ZipObjectValue) objectValue).getElement(forwardState, inst));
            } else if (objectValue instanceof DictObjectValue) {
                abstractResult.union((ForwardAbstractValue) ((DictObjectValue) objectValue).getElement(forwardState, abstractField));
            } else if (objectValue.isComplex()) {
                abstractResult.union((ForwardAbstractValue) ((ComplexObjectValue) objectValue).getElement(abstractField, forwardState.getAllocatePointTable()));
            } else{
                if (objectValue.hasAttr(abstractField, forwardState.getAllocatePointTable())) {
                    abstractResult.union((ForwardAbstractValue) objectValue.getAttr(abstractField, forwardState.getAllocatePointTable()));
                } else if (objectValue.hasAttr("__getitem__", forwardState.getAllocatePointTable())) {
                    // Call special method.
                    IForwardAbstractValue retVal =
                            ForwardCallManager.callSpecialFunction(
                                    inst, objectValue, forwardState, "__getitem__");
                    abstractResult.union((ForwardAbstractValue) retVal);
                } else {
                    ExceptionManager.attributeException(
                            objectValue,
                            ((ForwardAbstractValue) abstractField).getStringValue().getConcreteValue()
                    );
                }
            }
        }

        unionChanged(forwardState.updateValue(result, abstractResult));
    }

    @Override
    public void visitPropertyWrite(AstPropertyWrite inst) {
        int value = inst.getValue();
        int object = inst.getObjectRef();
        int field = inst.getMemberRef();

        IForwardAbstractValue abstractValue = forwardState.getValue(value);
        IForwardAbstractValue abstractObject = forwardState.getValue(object);

        IForwardAbstractValue abstractField = forwardState.getValue(field);
        for (ObjectValue objectValue:
                ((ForwardAbstractValue) abstractObject).getAllocatePoints()
                        .getObjectsIterable(forwardState.getAllocatePointTable())) {
            if (objectValue.isComplex()) {
                ((ComplexObjectValue) objectValue).setElement(abstractField, abstractValue);
            } else if (objectValue.hasAttr("__setitem__", forwardState.getAllocatePointTable())) {
                // Call special method.
                // TODO: Differentiate between `x.attr = val` and `x[index] = val`.
                // If not, when `x.attr = val`, call this `__setitem__` function.
                ForwardCallManager.callSpecialFunction(
                        inst, objectValue, forwardState, "__setitem__");
            } else {
                objectValue.setAttr(abstractField, abstractValue);
            }
        }
    }

    @Override
    public void visitEachElementGet(EachElementGetInstruction inst) {
        // This instruction means to get all property names of 'inst.value1'.
        AllocatePoints allocatePoints = ((ForwardAbstractValue) forwardState.getValue(inst.getUse(0))).getAllocatePoints();
        ForwardAbstractValue result = new ForwardAbstractValue();

        for (ObjectValue objectValue: allocatePoints.getObjectsIterable(forwardState.getAllocatePointTable())) {
            assert objectValue instanceof ComplexObjectValue;
            // TODO: This assertion is not correct. Check the object has '__iter__' or '__getitem__'.

            if (objectValue instanceof EnumerateObjectValue) {
                //  Special handling for enumerate object.
                result.union((ForwardAbstractValue) ((EnumerateObjectValue) objectValue).getKeys(forwardState, inst));
            } else if (objectValue instanceof ZipObjectValue) {
                //  Special handling for zip object.
                result.union((ForwardAbstractValue) ((ZipObjectValue) objectValue).getKeys(forwardState, inst));
            } else if (objectValue instanceof DictObjectValue) {
                //  Special handling for dict object.
                result.union((ForwardAbstractValue) ((DictObjectValue) objectValue).getKeys(forwardState, inst));
            } else {
                result.union((ForwardAbstractValue) objectValue.getKeys());
            }
        }
        unionChanged(forwardState.updateValue(inst.getDef(), result));
    }

    @Override
    public void visitInvoke(SSAInvokeInstruction inst) {
        if (inst.getCallSite().getDeclaredTarget().getName().toString().equals("import")) {
            String identifier = inst.getCallSite().getDeclaredTarget().getDeclaringClass().getName().toString();
            identifier = identifier.replace("L", "");
            if (Arrays.asList(AbstractParser.defaultImportNames).contains(identifier)) {
                unionChanged(
                        forwardState.updateValue(
                                inst.getDef(),
                                allocate(
                                        inst,
                                        inst.getCallSite().getDeclaredTarget().getDeclaringClass())));
            } else {
                if (inst.getCallSite().getDeclaredTarget().getName().toString().equals("import")) {
                    IForwardAbstractValue result = new ForwardAbstractValue();

                    if (identifier.equals(".")) {
                        // Handle relative import.
                        ModuleObjectValue module = new ModuleObjectValue(
                                new AllocatePoint(forwardState.getCGNode(), inst),
                                TypeReference.findOrCreate(PythonTypes.pythonLoader, "RELATIVE_IMPORT_TMP")
                        );
                        forwardState.getAllocatePointTable().newAllocation(module);

                        ClassHierarchy cha = (ClassHierarchy) forwardState.getSolver().getAnalyzer().getClassHierarchy();
                        for (IClass codeCls: cha.getImmediateSubclasses(cha.lookupClass(PythonTypes.CodeBody))) {
                            String codeClsClassName = codeCls.getReference().getName().getClassName().toString();
                            if (codeClsClassName.endsWith(".py") && !codeClsClassName.contains("pypsta_mock")) {
                                String filename = codeClsClassName.replace("script ", "")
                                        .replace(".py", "");
                                IForwardAbstractValue moduleAbstractValue
                                        = GlobalCollector.get(codeCls.getReference(), forwardState);
                                module.setAttr(filename, moduleAbstractValue);
                            }
                        }

                        result = new ForwardAbstractValue(module);
                    } if (identifier.equals(PypstaAnalysisEngine.TOP_MODULE_NAME)) {
                        // Top module (package) object is already created in global collector
                        // (and there is no method corresponding to the module). So get module
                        // object from global collector
                        result = GlobalCollector.get(
                                TypeReference.find(
                                        PythonTypes.pythonLoader, TypeName.string2TypeName("L" + PypstaAnalysisEngine.TOP_MODULE_NAME)
                                ),
                                forwardState
                        );
                        globalVarTable.put(inst.getDef(), "global " + PypstaAnalysisEngine.TOP_MODULE_NAME);
                    } else if (identifier.equals("__future__")) {
                        // Ignore '__future__'
                    } else {
                        // When there is a 'import' statement, call summary value flow graph that is made from xml summary files.
                        IClassHierarchy cha = forwardState.getSolver().getAnalyzer().getClassHierarchy();
                        ExplicitCallGraph cg = forwardState.getSolver().getAnalyzer().getCGBuilder().getCallGraph();

                        MethodReference importMethodRef = inst.getDeclaredTarget();
                        IClass kls = cha.lookupClass(importMethodRef.getDeclaringClass());
                        IMethod importMethod = forwardState.getSolver().getAnalyzer().getCGBuilder().getOptions().getMethodTargetSelector().getCalleeTarget(
                                forwardState.getCGNode(), inst.getCallSite(), kls
                        );
                        if (importMethod == null)
                            Assertions.UNREACHABLE();

                        CGNode importCGNode = null;
                        try {
                            importCGNode = cg.findOrCreateNode(
                                    importMethod,
                                    forwardState.getSolver().getAnalyzer().getCGBuilder().getContextSelector().getCalleeTarget(
                                            forwardState.getSolver().getCGNode(),
                                            inst.getCallSite(),
                                            importMethod,
                                            new InstanceKey[0]
                                    )
                            );
                        } catch (CancelException e) {
                            e.printStackTrace();
                        }

                        Iterator<CGNode> sucNodeIter = cg.getSuccNodes(forwardState.getCGNode());
                        while (sucNodeIter.hasNext()) {
                            CGNode sucNode = sucNodeIter.next();
                            if (!sucNode.equals(importCGNode)) {
                                cg.removeEdge(forwardState.getCGNode(), importCGNode);
                                ((ExplicitCallGraph.ExplicitNode) forwardState.getCGNode())
                                        .removeTarget(importCGNode);
                            }
                        }
                        cg.addEdge(forwardState.getCGNode(), importCGNode);
                        forwardState.getCGNode().addTarget(inst.getCallSite(), importCGNode);

                        ForwardFixSolver calleeSolver =
                                forwardState.getSolver().getAnalyzer().getOrCreateSolver(importCGNode);
                        try {
                            calleeSolver.solve(null);
                        } catch (CancelException e) {
                            e.printStackTrace();
                        }

                        ForwardState calleeExitState = calleeSolver.getOut(importCGNode.getIR().getExitBlock());
                        AllocatePointTable calleeExitAPTable = calleeExitState.getAllocatePointTable();
                        AllocatePointTable callerAPTable = forwardState.getAllocatePointTable();

                        // Calculate abstract value of return value.
                        Integer[] returnVarIds
                                = Arrays.stream(
                                        calleeExitState.getCGNode().getIR().getControlFlowGraph().getInstructions())
                                .parallel()
                                .filter(i -> i instanceof SSAReturnInstruction)
                                .map(i -> (SSAReturnInstruction) i)
                                .map(i -> i.getResult())
                                .toArray(Integer[]::new);

                        for (int returnVarId : returnVarIds) {
                            IForwardAbstractValue singleReturnValue = calleeExitState.getValue(returnVarId);

                            // The objects pointed from return value is taken to caller allocate point table.
                            callerAPTable.takeInSingleValue(singleReturnValue, calleeExitAPTable);
                            result.union(singleReturnValue.copy());
                        }
                    }
                    unionChanged(forwardState.updateValue(inst.getDef(), result));
                }
            }
        } else if (inst.getCallSite().getDeclaredTarget().getName().toString().equals("fakeWorldClinit")) {
        } else {
            Assertions.UNREACHABLE("Don't consider when 'invokestatic' is made except 'import'.");
        }
    }

    @Override
    public void visitPythonInvoke(PythonInvokeInstruction inst) {
        unionChanged(
                forwardState.updateValue(inst.getDef(), ForwardCallManager.call(inst, forwardState))
        );
    }

    @Override
    public void visitReturn(SSAReturnInstruction inst) {}

    @Override
    public void visitBinaryOp(SSABinaryOpInstruction inst) {
        IForwardAbstractValue val1 = forwardState.getValue(inst.getUse(0));
        IForwardAbstractValue val2 = forwardState.getValue(inst.getUse(1));
        IPythonOperator op = PythonOperatorFactory.convert(inst.getOperator());

        IForwardAbstractValue result =
                (IForwardAbstractValue) op.calc(val1, val2, inst, forwardState);
        unionChanged(forwardState.updateValue(inst.getDef(), result));
    }

    @Override
    public void visitUnaryOp(SSAUnaryOpInstruction inst) {
        IForwardAbstractValue abstractValue = forwardState.getValue(inst.getUse(0));
        IForwardAbstractValue resultValue =
                (IForwardAbstractValue) PythonOperatorFactory.convert(inst.getOpcode()).calc(
                        abstractValue, null, inst, forwardState
                );
        unionChanged(forwardState.updateValue(inst.getDef(), resultValue));
    }

    @Override
    public void visitConditionalBranch(SSAConditionalBranchInstruction inst) {
    }

    @Override
    public void visitPhi(SSAPhiInstruction inst) {
    }

    @Override
    public void visitGoto(SSAGotoInstruction inst) {
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction inst) {
        unionChanged(
                forwardState.updateValue(inst.getDef(), forwardState.getValue(inst.getUse(0)))
        );
    }


    ////////////////////////////////////////////////////////////////////////

    @Override
    public void visitAssert(AstAssertInstruction instruction) {
        BoolValue boolValue =
                ((ForwardAbstractValue) forwardState.getValue(instruction.getUse(0)))
                        .getBoolValue();

        if (boolValue.isBottom()) {
            ExceptionManager.bottomException(boolValue);
        } else if (boolValue.isTop() || boolValue.getConcreteValue() == false) {
            ExceptionManager.assertException(boolValue);
        } else {
            // Not error in assertion.
        }
    }

    @Override
    public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitIsDefined(AstIsDefinedInstruction inst) {
        // Set bool top value because you don't know whether this instruction returns true or false.
        unionChanged(
                forwardState.updateValue(
                        inst.getDef(),
                        new ForwardAbstractValue(new BoolValue(LatticeTop.TOP))
                )
        );
    }

    @Override
    public void visitEcho(AstEchoInstruction inst) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitYield(AstYieldInstruction inst) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitConversion(SSAConversionInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitComparison(SSAComparisonInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitSwitch(SSASwitchInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitArrayLength(SSAArrayLengthInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitThrow(SSAThrowInstruction instruction) {
        // TODO:
    }

    @Override
    public void visitMonitor(SSAMonitorInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitInstanceof(SSAInstanceofInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitPi(SSAPiInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
        // TODO:
    }

    @Override
    public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
        Assertions.UNREACHABLE("Not implemented in visitor.");
    }

    private ForwardAbstractValue allocate(SSAInstruction instruction, TypeReference concreteType) {
        ForwardAbstractValue newValue;
        if (concreteType == PythonTypes.string) {
            newValue = new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
        } else if (concreteType == TypeReference.Int) {
            newValue = new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
        } else if (concreteType == TypeReference.Double) {
            newValue = new ForwardAbstractValue(new FloatValue(LatticeTop.TOP));
        } else if (concreteType == TypeReference.Boolean) {
            newValue = new ForwardAbstractValue(new BoolValue(LatticeTop.TOP));
        } else if (concreteType.equals(TypeReference.find(concreteType.getClassLoader(), "L__name__"))) {
            newValue = new ForwardAbstractValue(new StringValue(LatticeTop.TOP));
        } else if (concreteType.equals(TypeReference.find(concreteType.getClassLoader(), "LNone"))) {
            newValue = new ForwardAbstractValue(new NoneValue(LatticeTop.TOP));
        } else {
            ObjectValue newObjValue;
            AllocatePoint allocatePoint = new AllocatePoint(forwardState.getCGNode(), instruction);
            if (concreteType == PythonTypes.list) {
                newObjValue = new ListObjectValue(allocatePoint, forwardState.getAllocatePointTable());
            } else if (concreteType == PythonTypes.tuple) {
                newObjValue = new TupleObjectValue(allocatePoint);
            } else if (concreteType == PythonTypes.set) {
                newObjValue = new SetObjectValue(allocatePoint, forwardState.getAllocatePointTable());
            } else if (concreteType == PythonTypes.dict) {
                newObjValue = new DictObjectValue(allocatePoint, forwardState.getAllocatePointTable());
            } else if (concreteType == PythonTypes.object) {
                if (forwardState.getSolver().getAnalyzer().getClassHierarchy().lookupClass(
                        forwardState.getSolver().getMethod().getReference().getDeclaringClass()
                ) instanceof PythonLoader.PythonClass
                        && concreteType.getName().toString().equals(PythonTypes.object.getName().toString())) {
                    // When caller module's type reference is python class, this created object is recognized as instance.
                    ClassObjectValue baseClass = (ClassObjectValue) forwardState.getAllocatePointTable().get(
                            ((ForwardAbstractValue) forwardState.getValue(1)).getAllocatePoints().get(0)
                    );
                    newObjValue = new InstanceObjectValue(
                            new AllocatePoint(forwardState.getCGNode(), instruction),
                            TypeReference.findOrCreate(
                                    PythonTypes.pythonLoader,
                                    baseClass.getTypeReference().getName().toString()+"_instance"
                            ),
                            baseClass
                    );
                } else {
                    // Return 'object' class.
                    newObjValue = new ClassObjectValue(allocatePoint, concreteType, null);
                }
            } else if (forwardState.getSolver().getAnalyzer().getClassHierarchy().lookupClass(concreteType) instanceof PythonLoader.PythonClass) {
                // When allocated object's type reference is python class.
                IClass parentClass = forwardState.getSolver().getAnalyzer().getClassHierarchy().lookupClass(concreteType).getSuperclass();
                if (parentClass.getReference().equals(PythonTypes.object)) {
                    newObjValue = new ClassObjectValue(allocatePoint, concreteType, null);
                } else {
                    ForwardAbstractValue globalVal
                            = (ForwardAbstractValue) GlobalCollector.get(
                                    parentClass.getReference(), forwardState);
                    assert globalVal.getAllocatePoints().size() == 1;
                    ObjectValue objectValue = globalVal.getAllocatePoints()
                            .getObjectsIterator(forwardState.getAllocatePointTable()).next();
                    assert objectValue instanceof ClassObjectValue;
                    newObjValue = new ClassObjectValue(
                            allocatePoint, concreteType, objectValue.getAllocatePoint());
                }
            } else if (concreteType.getName().getPackage() != null && concreteType.getName().getPackage().contains((byte) '$')) {
                // When allocated object's type is trampoline method.
                newObjValue = new FunctionTrampolineObjectValue(allocatePoint, concreteType);
            } else if (concreteType.getName().toString().equals("Lobject/instance")) {
                // TODO: Ad hoc implementation. If you want to return instance of object class, direct return value's type as 'Lobject/instance'.
                newObjValue = new InstanceObjectValue(allocatePoint, ClassObjectValue.objectClass);
            } else if (forwardState.getBasicBlock().getMethod().getReference().getSelector().getName().toString().equals("import")) {
                // When module import.
                // MUST: In stub xml file, specify 'function' in package name if the module is function, or specify 'class' if class.
                String typeName = concreteType.getName().toString();
                if (typeName.contains("class")) {
                    newObjValue = new ClassObjectValue(allocatePoint, concreteType, null);
                } else if (typeName.contains("function")) {
                    newObjValue = new FunctionObjectValue(allocatePoint, concreteType);
                } else {
                    newObjValue = new ModuleObjectValue(allocatePoint, concreteType);
                }
            } else if (concreteType.getName().toString().endsWith(".py")) {
                // TODO: Is this correct?
                newObjValue = new ModuleObjectValue(allocatePoint, concreteType);
            } else {
                newObjValue = new FunctionObjectValue(allocatePoint, concreteType);
            }

            forwardState.getAllocatePointTable().newAllocation(newObjValue);
            newValue = new ForwardAbstractValue(newObjValue);
        }

        return newValue;
    }
}
