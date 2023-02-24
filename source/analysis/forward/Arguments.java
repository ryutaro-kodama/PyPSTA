package analysis.forward;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.element.AllocatePoints;
import analysis.forward.abstraction.value.object.*;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.python.loader.PypstaLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import org.python.antlr.ast.*;
import org.python.antlr.base.expr;

import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Arguments {
    private static final String STARED_ARG_PREFIX = "pypsta_stared_arg";

    private final String name;
    private final String funcName;

    private final List<Pair<String, Object>> args;
    private final List<Pair<String, Object>> kwArgs;
    private final String varArgName;
    private final String kwVarArgName;

    private final List<String> decoratorList;

    public Arguments(String name, String funcName, int parameterNum) {
        this.name = name;
        this.funcName = funcName;
        this.args = new ArrayList<>();
        for (int i = 0; i < parameterNum; i++) {
            this.args.add(null);
        }
        this.kwArgs = new ArrayList<>();
        this.varArgName = null;
        this.kwVarArgName = null;
        this.decoratorList = new ArrayList<>();
    }

    public Arguments(Arguments base) {
        this.name = base.name;
        this.funcName = base.funcName;
        this.args = base.args;
        this.kwArgs = base.kwArgs;
        this.varArgName = base.varArgName;
        this.kwVarArgName = base.kwVarArgName;
        this.decoratorList = base.decoratorList;
    }

    public Arguments(String name, String funcName, arguments arguments, List<expr> decoratorList) {
        this.name = name;
        this.funcName = funcName;

        assert arguments.getInternalArgs().size() == arguments.getInternalDefaults().size();
        args = new ArrayList<>();
        for (int i = 0; i < arguments.getInternalArgs().size(); i++) {
            args.add(Pair.make(
                    arguments.getInternalArgs().get(i).getInternalArg(),
                    getValue(arguments.getInternalDefaults().get(i))
            ));
        }

        assert arguments.getInternalKwonlyargs().size() == arguments.getInternalKw_defaults().size();
        kwArgs = new ArrayList<>();
        for (int i = 0; i < arguments.getInternalKwonlyargs().size(); i++) {
            kwArgs.add(Pair.make(
                    arguments.getInternalKwonlyargs().get(i).getInternalArg(),
                    getValue(arguments.getInternalKw_defaults().get(i))
            ));
        }

        varArgName = (arguments.getInternalVararg()==null)
                ? null : arguments.getInternalVararg().getInternalArg();
        kwVarArgName = (arguments.getInternalKwarg()==null)
                ? null : arguments.getInternalKwarg().getInternalArg();

        this.decoratorList = new ArrayList<>();
        for (expr decorator: decoratorList) {
            assert decorator instanceof Name;
            this.decoratorList.add((String) getValue(decorator));
        }

    }

    private static Object getValue(expr arg0) {
        if (arg0 == null) {
            return null;
        } else if (arg0 instanceof Str) {
            return ((Str) arg0).getInternalS();
        } else if (arg0 instanceof Num) {
            return ((Num) arg0).getInternalN();
        } else if (arg0 instanceof Name) {
            return ((Name) arg0).getInternalId();
        } else {
            Assertions.UNREACHABLE(); return null;
        }
    }

    /**
     * Create this function's name. The way of creation is obeyed {@link com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator#composeEntityName(AstTranslator.WalkContext, CAstEntity) composeEntityName}
     * @param name this function's name itself
     * @param fileEntityName the name of file in which this function is defined
     * @param context the string context in which this function is defined
     * @return this function's name
     */
    private String makeFuncName(String name, String fileEntityName, String[] context) {
        StringBuilder funcName = new StringBuilder(fileEntityName);
        for (String c: context) {
            funcName.append('/').append(c);
        }
        funcName.append('/').append(name);
        return funcName.toString();
    }

    /**
     * Get this function's name.
     * @return this function's name
     */
    public String getFuncName() {
        return funcName;
    }

    public int getNumberOfParameters() {
        return 1  // Arguments contain the function object itself.
                + args.size()
                + kwArgs.size()
                + (varArgName != null ? 1 : 0)
                + (kwVarArgName != null ? 1 : 0);
    }

    public int getNumberOfDefaultParameters() {
        return (int) (args.stream().filter(p -> p.snd != null).count()
                + kwArgs.stream().filter(p -> p.snd != null).count());
    }

    public int getNumberOfPositionalParameters() {
        return args.size();
    }

    /**
     * Check the specific variable name in arguments.
     * @param varName the target variable name
     * @return whether the variable name exists
     */
    public boolean hasVarName(String varName) {
        int count = (int) (args.stream().filter(p -> p.fst.equals(varName)).count()
                + kwArgs.stream().filter(p -> p.fst.equals(varName)).count());
        assert count == 0 || count == 1;
        return count == 1;
    }

    public boolean hasVariadicArg() {
        return varArgName != null;
    }

    public boolean hasKwVariadicArg() {
        return kwVarArgName != null;
    }

    public List<String> getDecoratorList() {
        return decoratorList;
    }

    private Set<Integer> getStaredArgIds(SSAInstruction inst) {
        Set<Integer> result = HashSetFactory.make();
        if (inst instanceof PythonInvokeInstruction) {
            PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;
            for (String keyword : inst1.getKeywords()) {
                if (keyword.startsWith(Arguments.STARED_ARG_PREFIX)) {
                    result.add(inst1.getUse(keyword));
                }
            }
        }
        return result;
    }

    /**
     * Set the abstract values to the callee entry state.
     * @param callerState the caller state
     * @param calleeEntryState the callee entry state
     * @param inst the call instruction
     * @return whether the state of arguments has changed
     */
    public boolean parseArguments(ObjectValue calleeObject,
                                  ForwardState callerState,
                                  ForwardState calleeEntryState,
                                  SSAInstruction inst) {
        if (!(callerState.getSolver().getMethod() instanceof PypstaLoader.IPypstaMethod)
                || ((PypstaLoader.IPypstaMethod) callerState.getSolver().getMethod()).isTrampoline()) {
            // If caller is fake root or trampoline method, set the abstract values.
            List<IForwardAbstractValue> realArgsValues = convertToAbstractValues(callerState, inst);

            if (realArgsValues.size() < getNumberOfPositionalParameters() + 1) {
                // The formal arguments are longer than real arguments.
                // Set the default argument.
                for (int i = realArgsValues.size(); i <= getNumberOfPositionalParameters(); i++) {
                    IForwardAbstractValue defaultValue = getDefaultValue(
                            i + (hasVariadicArg() ? 1 : 0) + (hasKwVariadicArg() ? 1 : 0),
                            /**
                             * Caution: Ariadne assumes that there is no variadic and keyword variadic argument, so
                             * the default value suffix is different from true suffix
                             * (, see {@link com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator#leaveFunctionEntity(CAstEntity, AstTranslator.WalkContext, AstTranslator.WalkContext, CAstVisitor)}).
                             */
                            calleeEntryState.getSolver().getCGNode().getMethod().getReference().getDeclaringClass(),
                            callerState
                    );
                    realArgsValues.add(defaultValue);
                }
            } else if (realArgsValues.size() > getNumberOfPositionalParameters() + 1) {
                // Use variadic arg.
            }

            // '@classmethod' decorator handling.
            if (decoratorList.contains("classmethod")
                    && callerState.getSolver().getMethod().getName().toString().contains("trampoline")) {
                AllocatePoints result = new AllocatePoints();
                // Change instance to class
                for (ObjectValue arg1Obj:
                        ((ForwardAbstractValue) realArgsValues.remove(1)).getAllocatePoints()
                                .getObjectsIterable(callerState.getAllocatePointTable())) {
                    if (arg1Obj instanceof InstanceObjectValue) {
                        result.add(((InstanceObjectValue) arg1Obj).getBaseClassAP());
                    }
                }
                realArgsValues.add(1, new ForwardAbstractValue(result));
            } else if (decoratorList.contains("staticmethod")
                    && callerState.getSolver().getMethod().getName().toString().contains("trampoline")) {
                realArgsValues.remove(1);
            }

            IForwardAbstractValue[] realArgsValuesArray
                    = realArgsValues.toArray(new IForwardAbstractValue[0]);

            for (int i = 0; i < realArgsValuesArray.length; i++) {
                if (realArgsValuesArray[i].isBottom()) {
                    // If there is bottom value, set default value.
                    IForwardAbstractValue defaultValue = getDefaultValue(
                            i, calleeObject.getTypeReference(), callerState
                    );
                    if (defaultValue != null)
                        realArgsValuesArray[i] = defaultValue;
                }
            }

            boolean hasChanged = checkArgsChanged(realArgsValuesArray, callerState, calleeEntryState);
            if (hasChanged) {
                setArguments(realArgsValuesArray, callerState, calleeEntryState);
            }
            return hasChanged;
        }

        int[] realArgsIds = getPositionalParams(inst);
        IForwardAbstractValue[] realArgs = new IForwardAbstractValue[getNumberOfParameters()];
        realArgs[0] = new ForwardAbstractValue(calleeObject);

        // For variadic argument
        int variadicIndex = 0;
        TupleObjectValue variadicArg = null;
        if (hasVariadicArg()) {
            variadicArg = new TupleObjectValue(
                    new AllocatePoint(
                            callerState.getCGNode(),
                            new TmpInstInArg(-1, PythonTypes.tuple, inst)
                    )
            );
            callerState.getAllocatePointTable().newAllocation(variadicArg);
        }

        int realArgsSize = realArgsIds.length;
        int posArgSize = getNumberOfPositionalParameters();
        Set<Integer> staredArgIds = getStaredArgIds(inst);

        int i = 1;
        while (i < realArgsSize || i <= posArgSize) {
                                    // Caution: posArgSize doesn't contain the function object itself.
            // Set arguments from head.

            if (i < realArgsSize && i <= posArgSize) {
                int realArgId = realArgsIds[i];
                if (staredArgIds.contains(realArgId)) {
                    ForwardAbstractValue staredArg
                            = (ForwardAbstractValue) callerState.getValue(realArgId);

                    ArrayList<IForwardAbstractValue> extendedArgs = new ArrayList<>();
                    for (ObjectValue staredObject: staredArg.getAllocatePoints().getObjectsIterable(
                            callerState.getAllocatePointTable())) {
                        assert staredObject instanceof ListObjectValue
                                || staredObject instanceof TupleObjectValue;
                        ComplexObjectValue staredObject1 = ((ComplexObjectValue<?>) staredObject);

                        IForwardAbstractValue topValue = staredObject1.getIntTopAccessedValue();
                        assert topValue.isBottom();

                        for (Object m: staredObject1.getIntAccessElements().entrySet()) {
                            int index = ((Map.Entry<Integer, IForwardAbstractValue>) m).getKey();
                            IForwardAbstractValue value
                                    = ((Map.Entry<Integer, IForwardAbstractValue>) m).getValue();
                            if (extendedArgs.size() <= index) {
                                extendedArgs.add(index, new ForwardAbstractValue());
                            }
                            extendedArgs.get(index).union(value);
                        }
                    }

                    for (IForwardAbstractValue extendedArg: extendedArgs) {
                        realArgs[i++] = extendedArg;
                    }
                    i--;
                } else {
                    // Set positional passed argument.
                    realArgs[i] = callerState.getValue(realArgId);
                }
            } else if (i >= realArgsSize && i <= posArgSize) {
                // The formal arguments are longer than.
                // Set the default argument.
                IForwardAbstractValue defaultValue = getDefaultValue(
                        i + (hasVariadicArg() ? 1 : 0) + (hasKwVariadicArg() ? 1 : 0),
                        /**
                         * Caution: Ariadne assumes that there is no variadic and keyword variadic argument, so
                         * the default value suffix is different from true suffix
                         * (, see {@link com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator#leaveFunctionEntity(CAstEntity, AstTranslator.WalkContext, AstTranslator.WalkContext, CAstVisitor)}).
                         */
                        calleeEntryState.getSolver().getCGNode().getMethod().getReference().getDeclaringClass(),
                        callerState
                );
                realArgs[i] = defaultValue;
            } else if (i < realArgsSize && i > posArgSize) {
                // The real arguments are longer than.
                // Set them to variadic argument.
                if (hasVariadicArg()) {
                    assert variadicArg != null;
                    variadicArg.setElement(variadicIndex++, callerState.getValue(realArgsIds[i]));
                } else {
                    // TODO:
                    // Assertions.UNREACHABLE();
                }
            } else {
                Assertions.UNREACHABLE();
            }

            i++;
        }

        // Set variadic argument.
        if (hasVariadicArg()) {
            int index = getVarIdFromName(varArgName, calleeEntryState.getSolver().getCGNode()) - 1;
            realArgs[index] = new ForwardAbstractValue(variadicArg);
        }

        // For keyword variadic argument
        DictObjectValue kwVariadicArg = null;
        if (hasKwVariadicArg()) {
            kwVariadicArg = new DictObjectValue(
                    new AllocatePoint(
                            callerState.getCGNode(),
                            new TmpInstInArg(-1, PythonTypes.dict, inst)
                    ),
                    callerState.getAllocatePointTable()
            );
            callerState.getAllocatePointTable().newAllocation(kwVariadicArg);
        }

        // Set the abstract values passed by keywords.
        for (Pair<String, Integer> p: getKeywordParams(inst)) {
            if (p.fst.startsWith(Arguments.STARED_ARG_PREFIX)) {
                continue;
            }
            if (hasVarName(p.fst)) {
                int index = getVarIdFromName(p.fst, calleeEntryState.getSolver().getCGNode()) - 1;
                realArgs[index] = callerState.getValue(p.snd);
            } else {
                assert hasKwVariadicArg();
                kwVariadicArg.setAttr(p.fst, callerState.getValue(p.snd));
            }
        }

        // Set variadic argument.
        if (hasKwVariadicArg()) {
            int index = getVarIdFromName(kwVarArgName, calleeEntryState.getSolver().getCGNode()) - 1;
            realArgs[index] = new ForwardAbstractValue(kwVariadicArg);
        }

        // '@classmethod' decorator handling.
        if (decoratorList.contains("classmethod")) {
            // Move values.
            for (int tmp = realArgs.length - 1; tmp > 1; tmp--) {
                realArgs[tmp] = realArgs[tmp-1];
            }

            // Set class object to first index.
            TypeReference funcType = calleeObject.getTypeReference();
            TypeReference classType = TypeReference.find(
                    PythonTypes.pythonLoader, "L" + funcType.getName().getPackage().toString()
            );
            IForwardAbstractValue classAbstractValue = GlobalCollector.get(classType, callerState);
            realArgs[1] = classAbstractValue;
        }

        boolean argsChanged = checkArgsChanged(realArgs, callerState, calleeEntryState);

        if (argsChanged) {
            // Only when the state of arguments has changed, set the abstract values.
            setArguments(realArgs, callerState, calleeEntryState);
        }

        return argsChanged;
    }

    /**
     * Parse the arguments of synthetic method (ex. summary method or trampoline method).
     * @param targetObjectValue the target object value
     * @param callerState the caller state
     * @param calleeEntryState the callee entry state
     * @param inst the call instruction
     * @return the abstract values of arguments
     */
    public static boolean parseArgumentsOfSyntheticMethod(ObjectValue targetObjectValue,
                                                          ForwardState callerState,
                                                          ForwardState calleeEntryState,
                                                          SSAInstruction inst) {
        assert calleeEntryState.getSolver().getCGNode().getMethod() instanceof SummarizedMethod;

        ArrayList<IForwardAbstractValue> realArgsValue = convertToAbstractValues(callerState, inst);

        // Replace the first abstract value to that only contains target object value.
        // This replacement is needed when target object values are multiple, or target is
        // '__call__' method (the first abstract value must not be the instance but trampoline method)
        realArgsValue.remove(0);
        realArgsValue.add(0, new ForwardAbstractValue(targetObjectValue));

        if (inst instanceof PythonInvokeInstruction) {
            PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;
            List<String> staredArgs = inst1.getKeywords().stream()
                    .filter(s -> s.startsWith(Arguments.STARED_ARG_PREFIX))
                    .collect(Collectors.toList());

            // If there are stared argument, also passed in 'pypsta_stared_arg' keywords.
            if (!staredArgs.isEmpty()) {
                for (String staredArgName: staredArgs) {
                    int staredArgVarId = inst1.getUse(staredArgName);
                    int staredArgIndex = -1;
                    for (int i = 0; i < inst1.getNumberOfPositionalParameters(); i++) {
                        if (inst1.getUse(i) == staredArgVarId) {
                            staredArgIndex = i;
                        }
                    }

                    ForwardAbstractValue staredArg
                            = (ForwardAbstractValue) realArgsValue.remove(staredArgIndex);

                    ArrayList<IForwardAbstractValue> extendedArgs = new ArrayList<>();
                    for (ObjectValue staredObject: staredArg.getAllocatePoints().getObjectsIterable(
                                    callerState.getAllocatePointTable())) {
                        assert staredObject instanceof ListObjectValue
                                || staredObject instanceof TupleObjectValue;
                        ComplexObjectValue staredObject1 = ((ComplexObjectValue<?>) staredObject);

                        IForwardAbstractValue topValue = staredObject1.getIntTopAccessedValue();
                        assert topValue.isBottom();

                        for (Object m: staredObject1.getIntAccessElements().entrySet()) {
                            int index = ((Map.Entry<Integer, IForwardAbstractValue>) m).getKey();
                            IForwardAbstractValue value
                                    = ((Map.Entry<Integer, IForwardAbstractValue>) m).getValue();
                            if (extendedArgs.size() <= index) {
                                extendedArgs.add(index, new ForwardAbstractValue());
                            }
                            extendedArgs.get(index).union(value);
                        }
                    }

                    // Insert extended values to stared arg position.
                    realArgsValue.addAll(staredArgIndex, extendedArgs);
                }
            }
        }

        boolean argsChanged = checkArgsChanged(
                realArgsValue.toArray(new IForwardAbstractValue[0]), callerState, calleeEntryState);

        if (argsChanged) {
            setArguments(realArgsValue.toArray(new IForwardAbstractValue[0]), callerState, calleeEntryState);
        }

        return argsChanged;
    }

    /**
     * From the array of real argument ids, make the list of abstract values.
     * @param callerState the caller state
     * @param inst the call instruction node
     * @return the list of real arguments abstract values
     */
    public static ArrayList<IForwardAbstractValue> convertToAbstractValues(ForwardState callerState,
                                                                           SSAInstruction inst) {
        int[] realArgsIds = getPositionalParams(inst);
        ArrayList<IForwardAbstractValue> realArgsValues = new ArrayList<>();

        for (int i = 0; i < realArgsIds.length; i++) {
            realArgsValues.add(callerState.getValue(realArgsIds[i]));
        }
        return realArgsValues;
    }

    /**
     * Return the array of real arguments' variable ids.
     * @param inst the caller instruction
     * @return the array of real arguments
     */
    public static int[] getPositionalParams(SSAInstruction inst) {
        int[] result;
        if (inst instanceof SSAInvokeInstruction) {
            SSAInvokeInstruction inst1 = (SSAInvokeInstruction) inst;
            result = new int[inst1.getNumberOfPositionalParameters()];
        } else if (inst instanceof PythonInvokeInstruction) {
            PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;
            result = new int[inst1.getNumberOfPositionalParameters()];
        } else {
            result = new int[inst.getNumberOfUses()];
        }
        for (int i = 0; i < result.length; i++) {
            result[i] = inst.getUse(i);
        }
        return result;
    }

    /**
     * Return the array of tuple in which 0th element is keyword name, 1st element is the variable id.
     * @param inst the caller instruction
     * @return the array of tuple
     */
    public static Pair<String, Integer>[] getKeywordParams(SSAInstruction inst) {
        if (inst instanceof PythonInvokeInstruction) {
            PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;

            Pair<String, Integer>[] kwParams = new Pair[inst1.getNumberOfKeywordParameters()];
            for (int i = 0; i < kwParams.length; i++) {
                kwParams[i] = Pair.make(
                        inst1.getKeywords().get(i),
                        inst1.getUse(i + inst1.getNumberOfPositionalParameters())
                );
            }
            return  kwParams;
        } else {
            return new Pair[0];
        }
    }

    /**
     * The ssa instruction to represent the allocation point of variadic variables.
     */
    private class TmpInstInArg extends SSAInstruction {
        private final TypeReference typeReference;
        private final SSAInstruction inst;

        private TmpInstInArg(int iindex, TypeReference typeReference, SSAInstruction inst) {
            super(iindex);
            this.typeReference = typeReference;
            this.inst = inst;
        }

        @Override
        public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
            return null;
        }

        @Override
        public String toString(SymbolTable symbolTable) { return ""; }

        @Override
        public void visit(IVisitor v) {}

        @Override
        public int hashCode() {
            return Objects.hash(typeReference, inst);
        }

        @Override
        public boolean isFallThrough() { return false; }
    }

    /**
     * Get the default value from global.
     * @param index the index of formal arguments
     * @param calleeTypeRef the type reference of callee
     * @param callerState the caller state
     * @return the default value
     */
    private IForwardAbstractValue getDefaultValue(
            int index, TypeReference calleeTypeRef, ForwardState callerState) {
        String defaultVarSuffix = Integer.toString(index);
        IForwardAbstractValue defaultValue = GlobalCollector.get(
                "global " + calleeTypeRef.getName().toString()
                        + "_defaults_" + defaultVarSuffix,
                callerState
        );
        return defaultValue;
    }

    /**
     * Get variable id from the variable name.
     * @param varName the target variable's name
     * @param targetNode the target node
     * @return the variable id
     */
    protected int getVarIdFromName(String varName, CGNode targetNode) {
        int varId = 1;
        for (; varId <= targetNode.getMethod().getNumberOfParameters(); varId++) {
            String[] localNames = targetNode.getIR().getLocalNames(-1, varId);
            if (localNames.length == 1 && localNames[0].equals(varName))
                break;
        }
        return varId;
    }

    /**
     * Check whether real the abstract values of arguments have been changed.
     * @param newRealArgs the list of new arguments abstract values
     * @param callerState the caller state
     * @param calleeEntryState the callee entry state
     * @return whether the arguments have changed
     */
    private static boolean checkArgsChanged(IForwardAbstractValue[] newRealArgs,
                                            ForwardState callerState,
                                            ForwardState calleeEntryState) {
        AllocatePointTable calleeEntryAPTable = calleeEntryState.getAllocatePointTable();
        AllocatePointTable callerAPTable = callerState.getAllocatePointTable();

        boolean argsChanged = false;
        for (int i = 0; i < newRealArgs.length; i++) {
            IForwardAbstractValue newRealArg = newRealArgs[i];

            // The check of abstract value.
            if (!newRealArg.isSame(calleeEntryState.getValue(i+1))) {
                argsChanged = true;
                break;
            }

            // The check that object has been changed.
            for (AllocatePoint calleeEntryAP:
                    calleeEntryAPTable.collectUsedAllocatePoints(newRealArg, new HashSet<>())) {
                if (calleeEntryAPTable.containsKey(calleeEntryAP)) {
                    ObjectValue calleeEntryObjectValue = calleeEntryAPTable.get(calleeEntryAP);
                    if (!callerAPTable.get(calleeEntryAP).isSame(calleeEntryObjectValue, calleeEntryState.getAllocatePointTable())) {
                        // Object value has changed.
                        argsChanged = true;
                        break;
                    }
                } else {
                    // The corresponding object value is none, so state of argument is updated.
                    argsChanged = true;
                    break;
                }
            }

            if (argsChanged) break;
        }

        return argsChanged;
    }

    /**
     * Set new real arguments.
     * @param newRealArgs the array of new real arguments abstract values
     * @param callerState the caller state
     * @param calleeEntryState the entry state of callee
     */
    private static void setArguments(IForwardAbstractValue[] newRealArgs,
                                     ForwardState callerState,
                                     ForwardState calleeEntryState) {
        AllocatePointTable callerAPTable = callerState.getAllocatePointTable();
        AllocatePointTable calleeEntryAPTable = calleeEntryState.getAllocatePointTable();

        for (int i = 0; i < newRealArgs.length; i++) {
            IForwardAbstractValue oldAbstractValue = calleeEntryState.getValue(i+1);
            IForwardAbstractValue newAbstractValue = new ForwardAbstractValue();
            newAbstractValue.union(oldAbstractValue);
            newAbstractValue.union(newRealArgs[i]);

            // Set abstract values.
            calleeEntryState.setValue(i+1, newAbstractValue);

            // Set related object values.
            calleeEntryAPTable.takeInSingleValue(newRealArgs[i], callerAPTable);
        }
    }
}
