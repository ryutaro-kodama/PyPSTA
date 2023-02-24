package client.callgraph;

import analysis.forward.Arguments;
import analysis.forward.PypstaInitTrampFunction;
import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PypstaLoader;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

import java.util.Collections;
import java.util.Map;

public class PypstaConstructorTargetSelector implements MethodTargetSelector {
    private final Map<IClass, IMethod> ctors = HashMapFactory.make();

    private final MethodTargetSelector base;

    public PypstaConstructorTargetSelector(MethodTargetSelector base) {
        this.base = base;
    }

    @Override
    public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
        if (receiver != null) {
            IClassHierarchy cha = receiver.getClassHierarchy();
            if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.object)) && receiver instanceof PythonLoader.PythonClass) {
                if (!ctors.containsKey(receiver)) {
                    // Create constructor, which means the function that sets attributes to
                    // the instance and call '__init__' method if there is.

                    TypeReference initTypeRef = TypeReference.findOrCreate(
                            receiver.getClassLoader().getReference(), receiver.getName() + "/__init__"
                    );
                    IClass initCls = cha.lookupClass(initTypeRef);
                    IMethod initMethod
                            = (initCls == null)
                                    ? null : initCls.getMethod(AstMethodReference.fnSelector);

                    AstInstructionFactory instrFactory = PythonLanguage.Python.instructionFactory();

                    int paramsNum = (initMethod == null) ? 1 : initMethod.getNumberOfParameters();
                    int localVarId = paramsNum + 2;
                    int iindex = 0;

                    MethodReference constructorMethodRef = MethodReference.findOrCreate(
                            receiver.getReference(), site.getDeclaredTarget().getSelector()
                    );
                    PythonSummary summary = new PythonSummary(constructorMethodRef, paramsNum);

                    int instanceId = localVarId++;
                    // Create object for instance.
                    // TODO: This created object's type must be not 'object' but class's instance.
                    summary.addStatement(
                            instrFactory.NewInstruction(
                                    iindex,
                                    instanceId,
                                    NewSiteReference.make(iindex, PythonTypes.object)));
                    iindex++;

                    if (initMethod != null) {
                        int initMethodVarId = localVarId++;
                        // Get the "__init__" method object.
                        summary.addStatement(
                                instrFactory.GetInstruction(
                                        iindex,
                                        initMethodVarId,
                                        1,
                                        FieldReference.findOrCreate(
                                                PythonTypes.Root,
                                                Atom.findOrCreateUnicodeAtom("__init__"),
                                                PythonTypes.Root)));
                        iindex++;

                        int[] initTrampMethodParams = new int[ initMethod.getNumberOfParameters() ];
                        initTrampMethodParams[0] = initMethodVarId;
                        initTrampMethodParams[1] = instanceId;
                        for(int j = 2; j < initMethod.getNumberOfParameters(); j++) {
                            initTrampMethodParams[j]= j;
                        }

                        int result = localVarId++;
                        int except = localVarId++;

                        // Call "__init__" method.
                        CallSiteReference cref
                                = new DynamicCallSiteReference(site.getDeclaredTarget(), iindex);
                        summary.addStatement(
                                new PythonInvokeInstruction(
                                        2, result, except, cref, initTrampMethodParams, new Pair[0]));
                        iindex++;
                    }

                    summary.addStatement(instrFactory.ReturnInstruction(iindex++, instanceId, false));

                    // TODO: Add more variable ids.
                    summary.setValueNames(Collections.singletonMap(1, Atom.findOrCreateUnicodeAtom("self")));

                    // Get 'Arguments' object.
                    Arguments initArg = (initMethod == null)
                            ? new Arguments("__init__", initTypeRef.getName().toString(), 0)
                                    : ((PypstaLoader.IPypstaMethod) initMethod).getArgs();

                    ctors.put(
                            receiver,
                            new PypstaInitTrampFunction(
                                    constructorMethodRef, summary, receiver, initMethod, initArg)
                    );
                }

                return ctors.get(receiver);
            }
        }
        return base.getCalleeTarget(caller, site, receiver);
    }
}
