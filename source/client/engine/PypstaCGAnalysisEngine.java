package client.engine;

import analysis.backward.BackwardAnalyzerThresher;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.tracer.MakingExceptionDataTracer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Iterator2Set;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class PypstaCGAnalysisEngine extends PypstaAnalysisEngine {
    private final boolean defUse;

    public PypstaCGAnalysisEngine(boolean defUse, String relPath2z3Dir) {
        super(relPath2z3Dir);
        this.defUse = defUse;
    }

    @Override
    public ForwardAnalyzer performAnalysis(PropagationCallGraphBuilder CGBuilder) {
        loadZ3Library();

        forwardAnalyzer = new ForwardAnalyzer(CGBuilder, new MakingExceptionDataTracer());
        forwardAnalyzer.analyze();

        backwardAnalyzerThresher = new BackwardAnalyzerThresher(
                scope,
                CGBuilder,
                CGBuilder.getCallGraph(),
                (AnalysisCache) CGBuilder.getAnalysisCache(),
                forwardAnalyzer
        );
        backwardAnalyzerThresher.cgAnalyze(defUse);
        return forwardAnalyzer;
    }

    public void outputPyCGJsonFormat(OutputStream out) throws IOException {
        PrunedCallGraph prunedCG = backwardAnalyzerThresher.getRefutedCG();
        ExplicitCallGraph originCG = forwardAnalyzer.getCGBuilder().getCallGraph();
        CGNode fakeRootNode = originCG.getFakeRootNode();
        CGNode fakeWorldClinitNode = originCG.getFakeWorldClinitNode();

        Map<String, Set<String>> callerCalleeMap = new HashMap<>();

        List<CGNode> workList = new ArrayList<>();
        workList.add(fakeRootNode);
        Set<CGNode> visited = new HashSet<>();
        while (!workList.isEmpty()) {
            CGNode caller = workList.remove(0);
            visited.add(caller);

            Set<CGNode> sucNodes = Iterator2Set.toSet(prunedCG.getSuccNodes(caller));
            sucNodes.removeAll(visited);
            workList.addAll(sucNodes);

            // Pass dummy nodes.
            if (caller.equals(fakeRootNode)) continue;
            if (caller.equals(fakeWorldClinitNode)) continue;
            // Pass mock nodes.
            if (caller.getMethod().getReference().getDeclaringClass().getName().toString().equals("Lscript pypsta_mock.py")) continue;
            // Pass method of 'list'
            if (caller.getMethod().getReference().getDeclaringClass().getName().toString().equals("Llist.pop")) continue;
            if (caller.getMethod().getReference().getDeclaringClass().getName().toString().equals("Llist.insert")) continue;
            // Pass direct class object calls.
            if (isClassObj(caller)) continue;
            // Pass calls from trampoline method
            if (isTrampoline(caller)) continue;

            String callerMethodFormatName = getPyCGJsonFormat(caller);
            Set<String> calleeSet = callerCalleeMap.get(callerMethodFormatName);
            if (calleeSet == null) {
                calleeSet = new HashSet<>();
                callerCalleeMap.put(callerMethodFormatName, calleeSet);
            }

            for (CGNode callee: Iterator2Iterable.make(prunedCG.getSuccNodes(caller))) {
                if (caller.getMethod().getReference().getDeclaringClass().getName().toString()
                        .equals("Lscript pypsta_mock.py")) continue;

                if (callee.getMethod().getDeclaringClass().getReference().equals(PythonTypes.list)
                        || callee.getMethod().getDeclaringClass().getReference().equals(PythonTypes.set)
                        || callee.getMethod().getDeclaringClass().getReference().equals(PythonTypes.tuple)
                        || callee.getMethod().getDeclaringClass().getReference().equals(PythonTypes.dict)) {
                    calleeSet.add(getPyCGJsonFormat(callee));
                    continue;
                }

                if (isClassObj(callee) || isTrampoline(callee)) {
                    // Callee is class object.
                    assert prunedCG.getSuccNodeCount(callee) == 1;
                    for (CGNode calleeCallee: Iterator2Iterable.make(prunedCG.getSuccNodes(callee))) {
                        calleeSet.add(getPyCGJsonFormat(calleeCallee));
                    }
                } else {
                    // Callee is normal function or method.
                    calleeSet.add(getPyCGJsonFormat(callee));
                }
            }
        }

        String[] mapKey = callerCalleeMap.keySet().toArray(new String[0]);
        Arrays.sort(mapKey);


        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(out);

        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        jsonGenerator.setPrettyPrinter(printer);

        jsonGenerator.writeStartObject();

        for (String callerName: mapKey) {
            Set<String> calleeNames = callerCalleeMap.get(callerName);

            jsonGenerator.writeFieldName(callerName);
            jsonGenerator.writeStartArray();

            for (String callee: calleeNames) {
                jsonGenerator.writeString(callee);
            }

            jsonGenerator.writeEndArray();
        }

        jsonGenerator.writeEndObject();

        jsonGenerator.flush();
    }

    private String getPyCGJsonFormat(CGNode cgNode) {
        String ariadneMethodName = cgNode.getMethod().getReference().getDeclaringClass().getName().toString();
        String[] paths = ariadneMethodName.split("\\\\");
        String moduleName = paths[paths.length-1];
        String moduleNameNoExt = moduleName.replace(".py", "");
        String result = moduleNameNoExt.replace("/", ".");
        return result;
    }

    private IClassHierarchy cha = null;
    private IClass object = null;
    private boolean isClassObj(CGNode cgNode) {
        if (cha == null) {
            cha = forwardAnalyzer.getClassHierarchy();
            object = cha.lookupClass(PythonTypes.object);
        }

        return cha.isSubclassOf(cgNode.getMethod().getDeclaringClass(), object);
    }

    private boolean isTrampoline(CGNode cgNode) {
        return cgNode.getMethod().getName().toString().contains(
                PythonTypes.trampoline.getName().getClassName().toString()
        );
    }
}
