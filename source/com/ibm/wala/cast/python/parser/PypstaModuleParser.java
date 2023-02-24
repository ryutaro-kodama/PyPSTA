package com.ibm.wala.cast.python.parser;

import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.collections.HashSetFactory;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CharStream;
import org.python.antlr.ast.*;
import analysis.forward.Arguments;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PypstaModuleParser extends PypstaParser<ModuleEntry> {
    private final Set<String> localModules = HashSetFactory.make();

    private final SourceModule fileName;

    private final Set<Arguments> args;

    public PypstaModuleParser(SourceModule fileName,
                              CAstTypeDictionaryImpl<String> types,
                              List<Module> allModules,
                              Set<Arguments> args) {
        super(types, args);
        this.fileName = fileName;
        this.args = args;
        allModules.forEach(m -> {
            m.getEntries().forEachRemaining(new Consumer<ModuleEntry>() {
                @Override
                public void accept(ModuleEntry f) {
                    if (f.isModuleFile()) {
                        f.asModule().getEntries().forEachRemaining(sm -> {
                            accept(sm);
                        });
                    } else {
                        System.err.println("**CLS: " + scriptName((SourceModule)f));
                        localModules.add(scriptName((SourceModule)f));
                    }
                }
            });
        });
    }

    protected URL getParsedURL() throws IOException {
        return fileName.getURL();
    }

    protected WalaPythonParser makeParser() throws IOException {
        CharStream file = new ANTLRInputStream(fileName.getInputStream());
        return new WalaPythonParser(file, fileName.getName(), "UTF-8");
    }

    @Override
    protected PypstaParser<ModuleEntry>.CAstVisitor makeVisitor(PythonParser.WalkContext context, WalaPythonParser parser) {
        return new PypstaParser.CAstVisitor(context, parser) {

            @Override
            public CAstNode visitImportFrom(ImportFrom arg0) throws Exception {
                Optional<String> s = arg0.getInternalModuleNames().stream()
                        .map(n -> { return n.getInternalId(); })
                        .reduce((a, b) -> { return a + "/" + b; });
                if (s.isPresent()) {
                    String moduleName = s.get();
                    if (! localModules.contains(moduleName + ".py")) {
                        moduleName = s.get() + "/__init__";
                    }
                    if (localModules.contains(moduleName + ".py")) {
                        String yuck = moduleName;
                        return Ast.makeNode(CAstNode.BLOCK_STMT,
                                arg0.getInternalNames().stream()
                                        .map(a -> a.getInternalName())
                                        .map(n -> Ast.makeNode(CAstNode.DECL_STMT,
                                                Ast.makeConstant(new CAstSymbolImpl(n, PythonCAstToIRTranslator.Any)),
                                                Ast.makeNode(CAstNode.PRIMITIVE,
                                                        Ast.makeConstant("import"),
                                                        Ast.makeConstant(yuck),
                                                        Ast.makeConstant(n))))
                                        .collect(Collectors.toList()));

                    }
                }

                return super.visitImportFrom(arg0);
            }

        };
    }

    @Override
    protected String scriptName() {
        return scriptName(fileName);
    }

    private static String scriptName(SourceModule fileName) {
        if (fileName instanceof FileModule) {
            return fileName.getClassName();
        } else {
            return fileName.getName();
        }
    }

    @Override
    protected Reader getReader() throws IOException {
        return new InputStreamReader(fileName.getInputStream());
    }
}
