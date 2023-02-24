package pypsta;

import client.engine.PypstaAnalysisEngine;
import client.engine.PypstaCGAnalysisEngine;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.driver.Driver;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.parser.AbstractParser;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import edu.colorado.thresher.core.PypstaSymbolicExecutor;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.*;

public class PypstaDriver extends Driver {

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

    /** Add builtin functions other than 'BuiltinFunctions.java' */
    private static void addBuiltins() {
        List<String> oldImportNames = new ArrayList<>(Arrays.asList(AbstractParser.defaultImportNames));
        String[] otherImportNames = new String[] {
                "input", "object", "isinstance"
        };
        for (String otherImportName: otherImportNames) {
            if (!oldImportNames.contains(otherImportName))
                oldImportNames.add(otherImportName);
        }
        AbstractParser.defaultImportNames = oldImportNames.toArray(new String[0]);
    }

    public <T> T runit(PythonAnalysisEngine<T> E, String... args) throws IOException, CancelException {
        Set<Module> sources = HashSetFactory.make();
        for(String file : args) {
            File fo = new File(file);
            if (fo.isDirectory()) {
                sources.add(new SourceDirectoryTreeModule(fo, ".py"));
            } else {
                sources.add(new SourceFileModule(fo, file, null));
            }
        }
        E.setModuleFiles(sources);

        // Add more builtin functions
        addBuiltins();

        CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();

        CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

//        System.err.println(CG);
//
//        PointerAnalysis<InstanceKey> PA = (PointerAnalysis<InstanceKey>) builder.getPointerAnalysis();
//
//        CAstCallGraphUtil.AVOID_DUMP = false;
//        CAstCallGraphUtil.dumpCG(((SSAPropagationCallGraphBuilder)builder).getCFAContextInterpreter(), PA, CG);
//
//        SDG<InstanceKey> SDG = new SDG<InstanceKey>(CG, PA, Slicer.DataDependenceOptions.NO_EXCEPTIONS, Slicer.ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
//        System.err.println(SDG);

        return E.performAnalysis((PropagationCallGraphBuilder) builder);
    }

    public static void main(String... args) throws IllegalArgumentException, IOException, CancelException {
        Options options = new Options();

        OptionGroup modeGroup = new OptionGroup();
        modeGroup.setRequired(true);
        modeGroup.addOption(Option.builder().longOpt("type").desc("Analyze variable type").build());
        modeGroup.addOption(Option.builder().longOpt("cg").desc("Analyze call graph. Must specify '-b' option").build());
        options.addOptionGroup(modeGroup);

        options.addOption(Option.builder().option("b")
                .longOpt("backward")
                .argName("Z3DIR")
                .numberOfArgs(1)
                .desc("Do backward analysis to refinement\nSpecify rel-path to directory which has z3 library")
                .build());
        options.addOption(Option.builder().longOpt("defuse").desc("Use def-use information in CG analysis").build());
        options.addOption("d", "debug", false, "Execute on debug mode");
        options.addOption(Option.builder()
                .longOpt("pycg-json")
                .argName("OUTPUT_PATH")
                .numberOfArgs(1)
                .desc("Output the cg as json. You must select cg mode. Specify the path to output file")
                .build());
        options.addOption("t", "time", false, "Calculate execution time");
        options.addOption(Option.builder()
                .option("m")
                .longOpt("top-module")
                .argName("TOP_MODULE_NAME")
                .numberOfArgs(1)
                .desc("Specify top level module name.")
                .build());
        options.addOption(Option.builder()
                .option("e")
                .longOpt("entry-point")
                .hasArgs()
                .argName("ENTRY_POINTS")
                .desc("Specify entry points.")
                .build());

        // Output usage information.
        HelpFormatter hf = new HelpFormatter();
        hf.setOptionComparator(null);
        String syntax = "java -jar <PATH_TO_FAT_JAR> <FILES...>";
        String header = " <FILES...>              The rel-path to files to analyze";
        hf.printHelp(syntax, header, options, "", true);

        CommandLineParser parser = new DefaultParser();
        CommandLine line =  null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }

        PypstaAnalysisEngine E = null;
        if (line.hasOption("type")) {
            if (line.hasOption("b")) {
                E = new PypstaAnalysisEngine(line.getOptionValue("b"));
            } else {
                E = new PypstaAnalysisEngine();
            }
        } else if (line.hasOption("cg")) {
            if (!line.hasOption("b")) {
                System.err.println("You must specify '-b' option!!!");
                System.exit(1);
            }
            E = new PypstaCGAnalysisEngine(line.hasOption("defuse"), line.getOptionValue("b"));
        }

        if (line.hasOption("d")) {
            PypstaAnalysisEngine.DEBUG = true;
            edu.colorado.thresher.core.Options.DEBUG = true;
        }

        if (line.hasOption("m")) {
            PypstaAnalysisEngine.TOP_MODULE_NAME = line.getOptionValue("m");
        } else {
            PypstaAnalysisEngine.TOP_MODULE_NAME = "";
        }

        if (line.hasOption("e")) {
            E.setEntryPoints(line.getOptionValues("e"));
        } else {
            E.setEntryPoints(new String[0]);
        }

        long startTime = 0l;
        if (line.hasOption("t")) {
            startTime = System.currentTimeMillis();
        }

        new PypstaDriver().runit(E, line.getArgs());

        if (line.hasOption("t")) {
            long endTime = System.currentTimeMillis();
            System.out.println("Time: " + Long.toString(endTime - startTime) + " ms");
        }

        if (line.hasOption("cg")) {
            OutputStream out;
            if (line.hasOption("pycg-json")) {
                out = new FileOutputStream(line.getOptionValue("pycg-json"));
            } else {
                out = System.out;
            }
            ((PypstaCGAnalysisEngine) E).outputPyCGJsonFormat(out);
        } else if (line.hasOption("type")) {
            ((PypstaAnalysisEngine) E).outputErrorLikeMonat(System.out);
        }

        if (line.hasOption("d")) {
            System.out.println("Instr count: " + PypstaSymbolicExecutor.instrCount);
            System.out.println("Path count: " + E.getBackwardAnalyzerThresher().getPathCount());
            System.out.println("Executor count: " + E.getBackwardAnalyzerThresher().getExecCount());
            System.out.println("Refuted count: " + E.getBackwardAnalyzerThresher().getRefutedCount());
        }
    }
}
