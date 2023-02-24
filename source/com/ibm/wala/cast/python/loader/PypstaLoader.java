package com.ibm.wala.cast.python.loader;

import analysis.forward.Arguments;
import com.ibm.wala.cast.ir.translator.*;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.parser.PypstaModuleParser;
import com.ibm.wala.cast.python.util.Python3Interpreter;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.rewrite.*;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import org.python.core.PyObject;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PypstaLoader extends Python3Loader {
    private final HashSet<Arguments> args = new HashSet<>();

    private final CAst Ast = new CAstImpl();

    protected final CAstPattern pypstaSliceAssign = CAstPattern.parse("<top>ASSIGN(CALL(VAR(\"pypsta_slice\"),<args>**),<value>*)");

    protected final CAstPattern pypstaSliceAssignOp = CAstPattern.parse("<top>ASSIGN_POST_OP(CALL(VAR(\"pypsta_slice\"),<args>**),<value>*,<op>*)");

    protected final CAstPattern loop = CAstPattern.parse(
            "<top>BLOCK_EXPR("
                + "DECL_STMT(<decl_obj_stmts>**),"
                + "ASSIGN(**),"
                + "LOOP("
                    + "BINARY_EXPR("
                        + "<compare>**,"
                        + "BLOCK_EXPR("
                            + "ASSIGN(<attr_target>*,<each_element_get>*)"
                        + ")"
                    + "),"
                    + "BLOCK_EXPR("
                        + "ASSIGN(<val_target>*,OBJECT_REF(<obj_name>*,<attr_name>*)),"
                        + "<body>*"
                    + ")"
                + ")"
            + ")");

    protected final CAstPattern loopTwiceDecl = CAstPattern.parse(
            "<top>BLOCK_EXPR("
                + "<outer_decl>DECL_STMT(**),"
                + "<outer_attr_assign>ASSIGN(**),"
                + "LOOP("
                    + "<outer_test>BINARY_EXPR(**),"
                    + "BLOCK_EXPR("
                        + "<outer_target_assign>ASSIGN(**),"
                        + "<outer_body>BLOCK_EXPR("
                            + "BLOCK_EXPR("
                                + "BLOCK_EXPR("
                                    + "BLOCK_EXPR("
                                        + "<inner_decl>DECL_STMT(**),"
                                        + "ASSIGN(**),"
                                        + "LOOP(**)"
                                    + "),LABEL_STMT(**)"
                                + ")"
                            + "),LABEL_STMT(**)"
                        + ")"
                    + ")"
                + ")"
            + ")");

    protected final CAstPattern assign2Container = CAstPattern.parse(
            "<top>BLOCK_EXPR("
                + "<assign>ASSIGN("
                    + "<object>OBJECT_LITERAL(**),"
                    + "**"
                + ")"
            + ")");

    private int tmpIndex = 0;

    public PypstaLoader(IClassHierarchy cha, IClassLoader parent) {
        super(cha, parent);
    }

    public PypstaLoader(IClassHierarchy cha) {
        super(cha);
    }

    @Override
    protected TranslatorToIR initTranslator(Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
        return new PypstaCAstToIRTranslator(this, topLevelEntities);
    }

    @Override
    protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M, List<Module> allModules) throws IOException {
        PypstaModuleParser parser = new PypstaModuleParser((SourceModule) M, typeDictionary, allModules, args) {
            @Override
            public CAstEntity translateToCAst() throws Error, IOException {
                CAstEntity ce = super.translateToCAst();
                return new AstConstantFolder().fold(ce);
            }
        };

        RewritingTranslatorToCAst x = new RewritingTranslatorToCAst(M, parser);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public PatternBasedRewriter createCAstRewriter(CAst ast) {
                return new PatternBasedRewriter(ast, pypstaSliceAssign, (CAstPattern.Segments s) -> { return rewriteSubscriptAssign(s); });
            }
        }, false);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public PatternBasedRewriter createCAstRewriter(CAst ast) {
                return new PatternBasedRewriter(ast, pypstaSliceAssignOp, (CAstPattern.Segments s) -> { return rewriteSubscriptAssignOp(s); });
            }
        }, false);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public ConstantFoldingRewriter createCAstRewriter(CAst ast) {
                return new ConstantFoldingRewriter(ast) {
                    @Override
                    protected Object eval(CAstOperator op, Object lhs, Object rhs) {
                        try {
                            PyObject x = Python3Interpreter.getInterp().eval(lhs + " " + op.getValue() + " " + rhs);
                            if (x.isNumberType()) {
                                System.err.println(lhs + " " + op.getValue() + " " + rhs + " -> " + x.asInt());
                                return x.asInt();
                            }
                        } catch (Exception e) {
                            // interpreter died for some reason, so no information.
                        }
                        return null;
                    }
                };
            }

        }, false);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public RecursivePatternBasedRewriter createCAstRewriter(CAst ast) {
                return new RecursivePatternBasedRewriter(ast, loop, (CAstPattern.Segments s) -> {
                    return rewriteForLoop(s);
                });
            }
        }, false);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public RecursivePatternBasedRewriter createCAstRewriter(CAst ast) {
                return new RecursivePatternBasedRewriter(ast, loopTwiceDecl, (CAstPattern.Segments s) -> {
                    return rewriteTwiceDecl(s);
                });
            }
        }, false);

        x.addRewriter(new CAstRewriterFactory<CAstBasicRewriter.NonCopyingContext, CAstBasicRewriter.NoKey>() {
            @Override
            public RecursivePatternBasedRewriter createCAstRewriter(CAst ast) {
                return new RecursivePatternBasedRewriter(ast, assign2Container, (CAstPattern.Segments s) -> {
                    return rewriteAssign2Container(s);
                });
            }
        }, false);

        return x;
    }

    @Override
    protected CAstNode rewriteSubscriptAssign(CAstPattern.Segments s) {
        int i = 0;
        CAstNode[] args = new CAstNode[ s.getMultiple("args").size() + 1];
        for(CAstNode arg : s.getMultiple("args")) {
            args[i++] = arg;
        }
        args[i++] = s.getSingle("value");

        return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("pypsta_slice")), args);
    }

    @Override
    protected CAstNode rewriteSubscriptAssignOp(CAstPattern.Segments s) {
        int i = 0;
        CAstNode[] args = new CAstNode[ s.getMultiple("args").size() + 1];
        for(CAstNode arg : s.getMultiple("args")) {
            args[i++] = arg;
        }
        args[i++] = s.getSingle("value");

        return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("pypsta_slice")), args);
    }

    protected CAstNode rewriteForLoop(CAstPattern.Segments s) {
        if (s.getMultiple("compare").size() != 2) {
            Assertions.UNREACHABLE("The compare statements is not 2 statements.");
        }

        String tmpVarName = "pypsta_temp" + ++tmpIndex;
        CAstNode declAttrsVar =
                Ast.makeNode(CAstNode.VAR,
                        Ast.makeConstant(tmpVarName));

        CAstNode assign =
                Ast.makeNode(CAstNode.ASSIGN,
                        s.getSingle("val_target"),
                        Ast.makeNode(CAstNode.OBJECT_REF,
                                s.getSingle("obj_name"),
                                // Rewrite the variable name of attributes.
                                declAttrsVar));

        // You need to declare the target variable.
        // If not, Ariadne recognizes as global variable.
        CAstNode declBeforeAssign = null;
        int iterTargetKind = s.getSingle("attr_target").getKind();
        if (iterTargetKind == CAstNode.VAR) {
            declBeforeAssign =
                    Ast.makeNode(CAstNode.ASSIGN,
                            s.getSingle("val_target"),
                            Ast.makeConstant(null));
        } else if (iterTargetKind == CAstNode.OBJECT_LITERAL) {
            List<CAstNode> assigns = new ArrayList<>();
            CAstNode attrTarget = s.getSingle("attr_target");
            for (int i = 2; i < attrTarget.getChildCount(); i += 2) {
                assigns.add(
                        Ast.makeNode(CAstNode.ASSIGN,
                                attrTarget.getChild(i),
                                Ast.makeConstant(null))
                );
            }
            declBeforeAssign =
                    Ast.makeNode(CAstNode.BLOCK_EXPR,
                            assigns);
        } else {
            Assertions.UNREACHABLE();
        }

        CAstNode test =
                Ast.makeNode(CAstNode.BINARY_EXPR,
                        s.getMultiple("compare").get(0),
                        s.getMultiple("compare").get(1),
                        Ast.makeNode(CAstNode.BLOCK_EXPR,
                                Ast.makeNode(CAstNode.ASSIGN,
                                        // Rewrite the variable name which is assigned attribute names to.
                                        dupNode(declAttrsVar),
                                        Ast.makeNode(CAstNode.EACH_ELEMENT_GET,
                                                s.getSingle("each_element_get").getChild(0),
                                                Ast.makeConstant(null)))));

        CAstNode loop =
                Ast.makeNode(CAstNode.LOOP,
                        test,
                        Ast.makeNode(CAstNode.BLOCK_EXPR,
                                assign,
                                s.getSingle("body")));

        CAstNode result =
                Ast.makeNode(CAstNode.BLOCK_EXPR,
                        Ast.makeNode(CAstNode.DECL_STMT,
                                s.getMultiple("decl_obj_stmts")),
                        dupNode(declBeforeAssign),
                        loop);

        return result;
    }

    /**
     * Duplicate 'CAstNode'. If you use the same hash node, there is a possibility of raising error
     * at {@link com.ibm.wala.cast.tree.rewrite.CAstCloner#copyNodes(CAstNode, CAstControlFlowMap, CAstBasicRewriter.NonCopyingContext, Map, Pair)}
     * on L59.
     * @param root the target root node
     * @return the duplication of node
     */
    private CAstNode dupNode(CAstNode root) {
        if (root.getKind() == CAstNode.CONSTANT) {
            return Ast.makeConstant(root.getValue());
        } else {
            List<CAstNode> children = new ArrayList<>();
            for (int i = 0; i < root.getChildCount(); i++) {
                children.add(dupNode(root.getChild(i)));
            }
            return Ast.makeNode(root.getKind(), children);
        }
    }

    /**
     * Rewrite the same temporally variable statements.
     * @param s
     * @return replaced nodes.
     */
    public CAstNode rewriteTwiceDecl(CAstPattern.Segments s) {
        String oldVarName = s.getSingle("inner_decl").getChild(0).getValue().toString();
        if (s.getSingle("outer_decl").getChild(0).getValue().toString().equals(oldVarName)) {
            String newVarName = "pypsta_temp" + ++tmpIndex;
            return Ast.makeNode(CAstNode.BLOCK_EXPR,
                    s.getSingle("outer_decl"),
                    s.getSingle("outer_attr_assign"),
                    Ast.makeNode(CAstNode.LOOP,
                            s.getSingle("outer_test"),
                            Ast.makeNode(CAstNode.BLOCK_EXPR,
                                    s.getSingle("outer_target_assign"),
                                    replaceConstant(s.getSingle("outer_body"), oldVarName, newVarName)
                            )
                    )
            );
        } else {
            return s.getSingle("top");
        }
    }

    /**
     * Replace the 'CONSTANT' value.
     * @param root
     * @param oldConstant
     * @param newConstant
     * @return the replaced nodes.
     */
    private CAstNode replaceConstant(CAstNode root, String oldConstant, String newConstant) {
        if (root.getKind() == CAstNode.OPERATOR) {
            return root;
        } else if (root.getKind() == CAstNode.CONSTANT) {
            if (root.getValue() == null) {
                return root;
            } else if (root.getValue().toString().equals(oldConstant)) {
                return Ast.makeConstant(newConstant);
            } else {
                return root;
            }
        } else if (root.getKind() == CAstNode.GOTO) {
            // If you create the new node, thrown the assertion that there is no source position
            // corresponding to this 'GOTO' node, so not create new node.
            return root;
        } if (root.getKind() == CAstNode.DECL_STMT) {
            String name = null;
            if (root.getChild(0).getValue().toString().equals(oldConstant)) {
                name = newConstant;
            } else {
                name = oldConstant;
            }

            List<CAstNode> children = new ArrayList<>();
            children.add(Ast.makeConstant(new CAstSymbolImpl(name, PythonCAstToIRTranslator.Any)));
            for (CAstNode node: root.getChildren().subList(1, root.getChildCount())) {
                children.add(replaceConstant(node, oldConstant, newConstant));
            }
            return Ast.makeNode(CAstNode.DECL_STMT, children);
        } else {
            List<CAstNode> children = new ArrayList<>();
            for (int i = 0; i < root.getChildCount(); i++) {
                children.add(replaceConstant(root.getChild(i), oldConstant, newConstant));
            }
            return Ast.makeNode(root.getKind(), children);
        }
    }

    /**
     * Insert variable declare statement if the left expression in assign statement is container.
     * @param s
     * @return inserted nodes
     */
    public CAstNode rewriteAssign2Container(CAstPattern.Segments s) {
        List<String> assignedVarNames = new ArrayList<>();
        for (CAstNode child: s.getSingle("object").getChildren()) {
            if (child.getKind() == CAstNode.VAR && child.getChild(0).getKind() == CAstNode.CONSTANT) {
                String varName = (String) child.getChild(0).getValue();
                assignedVarNames.add(varName);
            }
        }
        if (assignedVarNames.size() == 0) {
            return s.getSingle("top");
        } else {
            int declStmtSize = assignedVarNames.size();
            CAstNode[] nodes = new CAstNode[declStmtSize + 1];
            for (int i = 0; i < declStmtSize; i++) {
                nodes[i] = Ast.makeNode(CAstNode.DECL_STMT,
                        Ast.makeConstant(
                                new CAstSymbolImpl(
                                        assignedVarNames.get(i), PythonCAstToIRTranslator.Any)));
            }
            nodes[declStmtSize] = s.getSingle("assign");
            return Ast.makeNode(CAstNode.BLOCK_EXPR, nodes);
        }
    }

    /**
     * Rewrite based on the pattern, and not only target but also its children.
     */
    public class RecursivePatternBasedRewriter extends PatternBasedRewriter {
        private final CAstPattern pattern;
        private final Function<CAstPattern.Segments, CAstNode> rewrite;

        public RecursivePatternBasedRewriter(
                CAst ast,
                CAstPattern pattern,
                Function<CAstPattern.Segments, CAstNode> rewrite) {
            super(ast, pattern, rewrite);
            this.pattern = pattern;
            this.rewrite = rewrite;
        }

        @Override
        protected CAstNode copyNodes(
                CAstNode root,
                CAstControlFlowMap cfg,
                NonCopyingContext context,
                Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
            final Pair<CAstNode, NoKey> pairKey = Pair.make(root, context.key());
            CAstPattern.Segments s = CAstPattern.match(pattern, root);
            if (s != null) {
                CAstNode replacement = rewrite.apply(s);
                return copyNodes(replacement, cfg, context, nodeMap, pairKey);
            } else {
                return copyNodes(root, cfg, context, nodeMap, pairKey);
            }
        }
    }

    @Override
    public DynamicMethodObject makeCodeBodyCode(
            AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
            Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp,
            AstTranslator.AstLexicalInformation lexicalInfo, AstMethod.DebuggingInformation debugInfo,
            IClass C, int defaultArgs) {
        List<Arguments> arguments = args.stream()
                .filter(a -> C.getReference().getName().toString().equals("L" + a.getFuncName()))
                .collect(Collectors.toList());
        if (arguments.size() == 1) {
            return new PypstaMethodObject(
                    C, Collections.emptySet(), cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp,
                    lexicalInfo, debugInfo, arguments.get(0)
            );
        } else {
            String name = C.getReference().getName().toString();
            return new PypstaMethodObject(
                    C, Collections.emptySet(), cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp,
                    lexicalInfo, debugInfo, new Arguments(name, name, symtab.getNumberOfParameters() - 1)
            );
        }
    }

    public class PypstaMethodObject extends DynamicMethodObject implements IPypstaMethod {
        private final Arguments args;

        public PypstaMethodObject(
                IClass cls, Collection<CAstQualifier> qualifiers, AbstractCFG<?, ?> cfg,
                SymbolTable symtab, boolean hasCatchBlock, Map<IBasicBlock<SSAInstruction>,
                TypeReference[]> caughtTypes, boolean hasMonitorOp,
                AstTranslator.AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
                Arguments args) {
            super(cls, qualifiers, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo);
            this.args = args;
        }

        @Override
        public Arguments getArgs() {
            return args;
        }

        @Override
        public boolean isTrampoline() {
            return false;
        }

        @Override
        public int getNumberOfParameters() {
            return args.getNumberOfParameters();
        }

        @Override
        public int getNumberOfDefaultParameters() {
            return args.getNumberOfDefaultParameters();
        }
    }

    public interface IPypstaMethod extends IMethod {
        Arguments getArgs();

        boolean isTrampoline();
    }
}
