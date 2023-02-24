package client.engine;

import analysis.backward.BackwardAnalyzerThresher;
import analysis.exception.AttributeExceptionData;
import analysis.exception.ElementExceptionData;
import analysis.exception.IExceptionData;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.tracer.MakingExceptionDataTracer;
import client.callgraph.PypstaConstructorTargetSelector;
import client.callgraph.PypstaSpecialMethodTargetSelector;
import client.callgraph.nCFAContextComparableSelector;

import client.cls.PypstaFakeClass;
import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ipa.callgraph.PythonScopeMappingInstanceKeys;
import com.ibm.wala.cast.python.ipa.callgraph.PythonTrampolineTargetSelector;
import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions;
import com.ibm.wala.cast.python.ipa.summaries.PythonComprehensionTrampolines;
import com.ibm.wala.cast.python.ipa.summaries.PythonSuper;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.parser.AbstractParser;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.collections.Iterator2List;

import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PypstaAnalysisEngine extends PythonAnalysisEngine<ForwardAnalyzer> {
    public static boolean DEBUG = false;
    public static String TOP_MODULE_NAME = "";

    protected ForwardAnalyzer forwardAnalyzer;
    protected BackwardAnalyzerThresher backwardAnalyzerThresher;

    private final String relPath2z3Dir;

    static {
        try {
            Class<?> j3 = Class.forName("client.loader.PypstaLoaderFactory");
            PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j3);
            Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
            PythonInterpreter.setInterpreter((PythonInterpreter)i3.newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            assert false : e.getMessage();
        }
    }

    public PypstaAnalysisEngine() {
        this.relPath2z3Dir = "";
    }

    public PypstaAnalysisEngine(String relPath2z3Dir) {
        this.relPath2z3Dir = relPath2z3Dir;
    }

    public ForwardAnalyzer getForwardAnalyzer() {
        return forwardAnalyzer;
    }

    public BackwardAnalyzerThresher getBackwardAnalyzerThresher() {
        return backwardAnalyzerThresher;
    }

    protected boolean doBackward() {
        return !relPath2z3Dir.isEmpty();
    }

    protected String[] entryPoints = new String[0];
    public void setEntryPoints(String[] entryPoints) {
        this.entryPoints = entryPoints;
    }

    /**
     * Load z3 library.
     */
    protected void loadZ3Library() {
        String current = System.getProperty("user.dir");
        System.load(Paths.get(current, relPath2z3Dir, "libz3.dll").toString());
        System.load(Paths.get(current, relPath2z3Dir, "libz3java.dll").toString());
    }

    @Override
    public ForwardAnalyzer performAnalysis(PropagationCallGraphBuilder CGBuilder) {
        forwardAnalyzer = new ForwardAnalyzer(CGBuilder, new MakingExceptionDataTracer());
        forwardAnalyzer.analyze();

        if (doBackward()) {
            loadZ3Library();

            backwardAnalyzerThresher = new BackwardAnalyzerThresher(
                    scope,
                    CGBuilder,
                    CGBuilder.getCallGraph(),
                    (AnalysisCache) CGBuilder.getAnalysisCache(),
                    forwardAnalyzer
            );
            backwardAnalyzerThresher.analyze();
        }
        return forwardAnalyzer;
    }

    public void outputErrorLikeMonat(PrintStream out) {
        out.print("\n\n\n");

        boolean hasElementException = false;
        boolean hasKeyException = false;
        boolean hasException = false;

        for (IExceptionData data:
                ((MakingExceptionDataTracer) forwardAnalyzer.getTracer()).getExceptionDataSet()) {
            if (data.getFoundWitness()) {
                if (data instanceof ElementExceptionData) {
                    hasElementException = true;
                } else if (data instanceof AttributeExceptionData && ((AttributeExceptionData) data).getObjectTypes().contains(PythonTypes.dict)){
                    hasKeyException = true;
                } else {
                    out.println(data);
                }
                hasException = true;
            }
        }

        if (hasElementException) {
            out.println("There is 'ElementException'");
        }
        if (hasKeyException) {
            out.println("There is 'KeyException'");
        }

        if (!hasException) {
            out.println("No errors are found!!!");
        }

        out.print("\n\n\n");
    }

    @Override
    protected void addBypassLogic(IClassHierarchy cha, AnalysisOptions options) {
        options.setSelector(
                new PypstaSpecialMethodTargetSelector(
                        new PythonTrampolineTargetSelector(
                                new PypstaConstructorTargetSelector(
                                        new PythonComprehensionTrampolines(
                                                options.getMethodTargetSelector())))));

        BuiltinFunctions builtins = new BuiltinFunctions(cha);
        options.setSelector(
                builtins.builtinClassTargetSelector(
                        options.getClassTargetSelector()));

        // Add summary files.
        addSummaryBypassLogic(options, "stub/builtins.xml");
        addSummaryBypassLogic(options, "stub/pbkdf2.xml");
        addSummaryBypassLogic(options, "stub/pyperf.xml");
        addSummaryBypassLogic(options, "stub/stdlib/abc.xml");
        addSummaryBypassLogic(options, "stub/stdlib/argparse.xml");
        addSummaryBypassLogic(options, "stub/stdlib/array.xml");
        addSummaryBypassLogic(options, "stub/stdlib/datetime.xml");
        addSummaryBypassLogic(options, "stub/stdlib/io.xml");
//        addSummaryBypassLogic(options, "stub/stdlib/functools.xml");
        addSummaryBypassLogic(options, "stub/stdlib/itertools.xml");
        addSummaryBypassLogic(options, "stub/stdlib/math.xml");
        addSummaryBypassLogic(options, "stub/stdlib/os.xml");
        addSummaryBypassLogic(options, "stub/stdlib/queue.xml");
        addSummaryBypassLogic(options, "stub/stdlib/random.xml");
        addSummaryBypassLogic(options, "stub/stdlib/re.xml");
        addSummaryBypassLogic(options, "stub/stdlib/subprocess.xml");
        addSummaryBypassLogic(options, "stub/stdlib/sys.xml");
        addSummaryBypassLogic(options, "stub/stdlib/textwrap.xml");
        addSummaryBypassLogic(options, "stub/stdlib/typing.xml");
    }

    @Override
    protected PythonSSAPropagationCallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache2) {
        IAnalysisCacheView cache = new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory(), options.getSSAOptions());

        options.setSelector(new ClassHierarchyClassTargetSelector(cha));
        options.setSelector(new ClassHierarchyMethodTargetSelector(cha));

        addBypassLogic(cha, options);

        options.setUseConstantSpecificKeys(true);

        SSAOptions ssaOptions = options.getSSAOptions();
        ssaOptions.setDefaultValues(new SSAOptions.DefaultValues() {
            @Override
            public int getDefaultValue(SymbolTable symtab, int valueNumber) {
                return symtab.getNullConstant();
            }
        });
        options.setSSAOptions(ssaOptions);

        PythonSSAPropagationCallGraphBuilder builder =
                makeBuilder(cha, options, cache);

        AstContextInsensitiveSSAContextInterpreter interpreter = new AstContextInsensitiveSSAContextInterpreter(options, cache);
        builder.setContextInterpreter(interpreter);

        /**
         * Do context insensitive analysis. The method 'getCallGraphBuilder' in 'PythonAnalysisEngine.java' sets k-limiting
         * context sensitive analysis, K-CFA policy. So we override the method and change policy.
         */
//        builder.setContextSelector(new ContextInsensitiveSelector());
        builder.setContextSelector(new nCFAContextComparableSelector(20, new ContextInsensitiveSelector()));

        builder.setInstanceKeys(new PythonScopeMappingInstanceKeys(builder, new ZeroXInstanceKeys(options, cha, interpreter, ZeroXInstanceKeys.ALLOCATIONS)));

        new PythonSuper(cha).handleSuperCalls(builder, options);

        return builder;
    }

    @Override
    protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
        Iterable<Entrypoint> defaultEntryPoints = super.makeDefaultEntrypoints(cha);
        if (entryPoints.length == 0) return defaultEntryPoints;

        List<Entrypoint> defaultEntryPointList = Iterator2List.toList(defaultEntryPoints.iterator());

        // Add entry points specified in command line.
        for (String entryPoint: entryPoints) {
            String[] filePathElements = entryPoint.split("\\.");
            if (filePathElements[0].equals(PypstaAnalysisEngine.TOP_MODULE_NAME))
                // Replace '<TOP_MODULE_NAME>.<ENTRY_MODULE_FILE>.<ENTRY_METHOD>'
                // to '<ENTRY_MODULE_FILE>.<ENTRY_METHOD>'
                filePathElements = Arrays.copyOfRange(filePathElements, 1, filePathElements.length);
            // Replace '<ENTRY_MODULE_FILE>' to '<ENTRY_MODULE_FILE>.py'
            filePathElements[filePathElements.length-2] = filePathElements[filePathElements.length-2] + ".py";

            String entryPointFilePath = String.join("/", filePathElements);
            TypeReference entryPointMethodType = TypeReference.findOrCreate(
                    PythonTypes.pythonLoader, TypeName.string2TypeName("Lscript " + entryPointFilePath)
            );
            MethodReference er = MethodReference.findOrCreate(entryPointMethodType, AstMethodReference.fnSelector);

            defaultEntryPointList.add(new DefaultEntrypoint(er, cha));
        }
        return defaultEntryPointList;
    }

    protected PypstaSSAPropagationCallGraphBuilder makeBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
        return new PypstaSSAPropagationCallGraphBuilder(cha, options, cache, new AstCFAPointerKeys());
    }

    public class PypstaSSAPropagationCallGraphBuilder extends PythonSSAPropagationCallGraphBuilder {
        public PypstaSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache, PointerKeyFactory pointerKeyFactory) {
            super(cha, options, cache, pointerKeyFactory);
        }

        @Override
        public PypstaConstraintVisitor makeVisitor(CGNode node) {
            return new PypstaConstraintVisitor(this, node);
        }

        @Override
        public CallGraph makeCallGraph(AnalysisOptions options, MonitorUtil.IProgressMonitor monitor)
                throws IllegalArgumentException, CallGraphBuilderCancelException {
            if (!PypstaAnalysisEngine.TOP_MODULE_NAME.isEmpty()) {
                // If 'TOP_MODULE_NAME' is specified in command line, create the module (package) object
                // in the fake root method.
                AbstractRootMethod fakeRootMethod = (AbstractRootMethod) callGraph.getFakeRootNode().getMethod();
                TypeReference packageType = TypeReference.findOrCreate(
                        PythonTypes.pythonLoader, TypeName.string2TypeName("L" + PypstaAnalysisEngine.TOP_MODULE_NAME)
                );

                IClassHierarchy cha = getClassHierarchy();
                if (cha.lookupClass(packageType) == null) {
                    PypstaFakeClass packageKls = new PypstaFakeClass(packageType, cha);
                    cha.addClass(packageKls);
                }

                SSANewInstruction packageNewInst = fakeRootMethod.addAllocation(packageType);
                fakeRootMethod.statements.add(
                        PythonLanguage.Python.instructionFactory().GlobalWrite(
                                packageNewInst.iIndex()+1,
                                FieldReference.findOrCreate(
                                        PythonTypes.Root,
                                        Atom.findOrCreateUnicodeAtom("global " + PypstaAnalysisEngine.TOP_MODULE_NAME),
                                        PythonTypes.Root
                                ),
                                packageNewInst.getDef()
                        )
                );
            }
            return super.makeCallGraph(options, monitor);
        }
    }

    public class PypstaConstraintVisitor extends PythonSSAPropagationCallGraphBuilder.PythonConstraintVisitor {
        public PypstaConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, CGNode node) {
            super(builder, node);
        }

        @Override
        public void visitBinaryOp(SSABinaryOpInstruction instruction) {
            super.visitBinaryOp(instruction);

            if (instruction.getOperator().equals(IBinaryOpInstruction.Operator.MUL)) {
                // 'list * int' generates new list.
                InstanceKey iKey = getInstanceKeyForAllocation(new NewSiteReference(instruction.iIndex(), PythonTypes.list));
                PointerKey def = getPointerKeyForLocal(instruction.getDef());

                if (contentsAreInvariant(symbolTable, du, instruction.getUse(0))) {
                    InstanceKey[] arg0IKeys = getInvariantContents(symbolTable, du, node, instruction.getUse(0));
                    for (InstanceKey arg0IKey: arg0IKeys) {
                        if (arg0IKey.getConcreteType().getReference().equals(PythonTypes.list)) {
                            system.newConstraint(def, iKey);
                        }
                    }
                }

                if (contentsAreInvariant(symbolTable, du, instruction.getUse(1))) {
                    InstanceKey[] arg1IKeys = getInvariantContents(symbolTable, du, node, instruction.getUse(1));
                    for (InstanceKey arg1IKey: arg1IKeys) {
                        if (arg1IKey.getConcreteType().getReference().equals(PythonTypes.list)) {
                            system.newConstraint(def, iKey);
                        }
                    }
                }
            }
        }

        @Override
        protected void visitInvokeInternal(SSAAbstractInvokeInstruction instruction, InvariantComputer invs) {
            super.visitInvokeInternal(instruction, invs);

            if (Arrays.asList(AbstractParser.defaultImportNames)
                    .contains(instruction.getDeclaredResultType().getName().getClassName().toString())) {
                // When you import default import names, create instances and track it.

                IClass cls = builder.getClassHierarchy().lookupClass(instruction.getDeclaredResultType());
                // If you don't write summary to like 'builtins.xml', the 'IClass' object is not created.
                if (cls == null) return;

                InstanceKey iKey = new NormalAllocationInNode(
                        node,
                        NewSiteReference.make(instruction.getProgramCounter(), instruction.getDeclaredResultType()),
                        cls
                );

                PointerKey def = getPointerKeyForLocal(instruction.getDef());

                if (!contentsAreInvariant(symbolTable, du, instruction.getDef())) {
                    system.newConstraint(def, iKey);
                } else {
                    system.findOrCreateIndexForInstanceKey(iKey);
                    system.recordImplicitPointsToSet(def);
                }
            }
        }
    }
}
