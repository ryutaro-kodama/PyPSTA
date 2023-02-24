/******************************************************************************
 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.parser;

import analysis.forward.Arguments;
import client.cast.PypstaCAstOperator;

import org.python.antlr.PythonTree;
import org.python.antlr.ast.*;

import org.python.antlr.base.expr;
import org.python.antlr.base.slice;
import org.python.antlr.base.stmt;
import org.python.core.PyObject;

import com.ibm.wala.cast.ir.translator.AbstractClassEntity;
import com.ibm.wala.cast.ir.translator.AbstractCodeEntity;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.loader.DynamicAnnotatableEntity;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

abstract public class PypstaParser<T> extends PythonParser<T> implements TranslatorToCAst {
    private int tmpIndex = 0;

    private static class FunctionContext extends TranslatorToCAst.FunctionContext<PythonParser.WalkContext, PythonTree>
            implements PythonParser.WalkContext {
        private final AbstractCodeEntity fun;
        private final java.util.Set<String> downwardGlobals;
        private final java.util.Set<String> definedNames = HashSetFactory.make();

        public PythonParser.WalkContext getParent() {
            return parent;
        }

        private FunctionContext(PythonParser.WalkContext parent,
                                AbstractCodeEntity fun,
                                java.util.Set<String> downwardGlobals,
                                PythonTree s) {
            super(parent, s);
            this.fun = fun;
            this.downwardGlobals = downwardGlobals;
        }

        public void addDefinedName(String name) {
            definedNames.add(name);
        }

        public void addGlobal(String g) {
            downwardGlobals.add(g);
        }

        @Override
        public CAstEntity entity() {
            return fun;
        }

        @Override
        public CAstNodeTypeMapRecorder getNodeTypeMap() {
            return fun.getNodeTypeMap();
        }

        @Override
        public CAstSourcePositionRecorder pos() {
            return fun.getSourceMap();
        }

        @Override
        public CAstControlFlowRecorder cfg() {
            return fun.getControlFlow();
        }

        @Override
        public void addScopedEntity(CAstNode construct, CAstEntity e) {
            fun.addScopedEntity(construct, e);
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
            return fun.getAllScopedEntities();
        }
    }

    public class CAstVisitor extends PythonParser<T>.CAstVisitor implements VisitorIF<CAstNode>  {
        private final PythonParser.WalkContext context;
        private final WalaPythonParser parser;

        private CAstType codeBody = new CAstType() {

            @Override
            public String getName() {
                return "CodeBody";
            }

            @Override
            public Collection<CAstType> getSupertypes() {
                return Collections.emptySet();
            }

        };

        private CAstNode fail(PyObject tree) {
            // pretend it is a no-op for now.
            // assert false : tree;
            return Ast.makeNode(CAstNode.EMPTY);
        }

        protected CAstVisitor(PythonParser.WalkContext context, WalaPythonParser parser) {
            super(context, parser);
            this.context = context;
            this.parser = parser;
        }

        private String compositeName(String name, PythonParser.WalkContext context) {
            StringBuilder s = new StringBuilder(name);
            PythonParser.WalkContext parentContext = context;
            while (parentContext != context.root()) {
                s.insert(0, parentContext.entity().getName() + "/");
                parentContext = parentContext.getParent();
            }
            return s.toString();
        }

        private CAstNode acceptOrNull(PythonTree x) throws Exception {
            return (x==null)? Ast.makeNode(CAstNode.EMPTY): notePosition(x.accept(this), x);
        }

        private CAstNode notePosition(CAstNode n, PythonTree... p) {
            return notePosition(n, 0, p);
        }

        private CAstNode notePosition(CAstNode n, int pad, PythonTree... p) {
            Position pos = makePosition(p[0]);
            if (p.length > 1) {
                Position start = pos;
                Position end = makePosition(p[p.length-1]);
                pos = new AbstractSourcePosition() {

                    @Override
                    public int getFirstLine() {
                        return start.getFirstLine();
                    }

                    @Override
                    public int getLastLine() {
                        return end.getLastLine();
                    }

                    @Override
                    public int getFirstCol() {
                        return start.getFirstCol();
                    }

                    @Override
                    public int getLastCol() {
                        return end.getLastCol() + pad;
                    }

                    @Override
                    public int getFirstOffset() {
                        return start.getFirstOffset();
                    }

                    @Override
                    public int getLastOffset() {
                        return end.getLastOffset() + pad;
                    }

                    @Override
                    public URL getURL() {
                        return start.getURL();
                    }

                    @Override
                    public Reader getReader() throws IOException {
                        return start.getReader();
                    }
                };
            }
            pushSourcePosition(context, n, pos);
            return n;
        }

        private Position makePosition(PythonTree p) {
            String s = parser.getText(p.getCharStartIndex(), p.getCharStopIndex());
            String[] lines = s.split("\n");
            int last_col;
            int last_line = p.getLine() + lines.length - 1;
            if ("".equals(s) || lines.length <= 1) {
                last_col = p.getCharPositionInLine() + (p.getCharStopIndex() - p.getCharStartIndex());
            } else {
                assert (lines.length > 1);
                last_col = lines[lines.length-1].length();
            }

            return new AbstractSourcePosition() {

                @Override
                public URL getURL() {
                    try {
                        return getParsedURL();
                    } catch (IOException e) {
                        assert false : e;
                        return null;
                    }
                }

                @Override
                public Reader getReader() throws IOException {
                    return PypstaParser.this.getReader();
                }

                @Override
                public int getFirstLine() {
                    return p.getLine();
                }

                @Override
                public int getFirstCol() {
                    return p.getCharPositionInLine();
                }

                @Override
                public int getLastLine() {
                    return last_line;
                }

                @Override
                public int getLastCol() {
                    return last_col;
                }

                @Override
                public int getFirstOffset() {
                    return p.getCharStartIndex();
                }

                @Override
                public int getLastOffset() {
                    return p.getCharStopIndex();
                }

            };
        }

        @Override
        public CAstNode visitBinOp(BinOp arg0) throws Exception {
            // In order to parse '//' calculation override this function.
            if (arg0.getInternalOp() == operatorType.FloorDiv) {
                CAstNode l = notePosition(arg0.getInternalLeft().accept(this), arg0.getInternalLeft());
                CAstNode r = notePosition(arg0.getInternalRight().accept(this), arg0.getInternalRight());
                return notePosition(
                        Ast.makeNode(CAstNode.BINARY_EXPR, PypstaCAstOperator.OP_FDIV, l, r), arg0);
            } else {
                return super.visitBinOp(arg0);
            }
        }

        @Override
        public CAstNode visitClassDef(ClassDef arg0) throws Exception {
            PythonParser.WalkContext parent = this.context;

            CAstType.Class cls = new CAstType.Class() {
                @Override
                public Collection<CAstType> getSupertypes() {
                    Collection<CAstType> supertypes = HashSetFactory.make();
                    for(expr e : arg0.getInternalBases()) {
                        try {
                            CAstType type = types.getCAstTypeFor(e.getText());
                            if (type != null) {
                                supertypes.add(type);
                            } else {
                                supertypes.add(getMissingType(e.getText()));
                            }
                        } catch (Exception e1) {
                            assert false : e1;
                        }
                    }
                    return supertypes;
                }

                @Override
                public String getName() {
                    return arg0.getInternalName();
                }

                @Override
                public boolean isInterface() {
                    return false;
                }

                @Override
                public Collection<CAstQualifier> getQualifiers() {
                    return Collections.emptySet();
                }
            };
            //TODO: CURRENTLY THIS WILL NOT BE CORRECT FOR EXTENDING CLASSES IMPORTED FROM ANOTHER MODULE
            types.map(arg0.getInternalName(), cls);

            Collection<CAstEntity> members = HashSetFactory.make();

            CAstEntity clse = new AbstractClassEntity(cls) {

                @Override
                public int getKind() {
                    return CAstEntity.TYPE_ENTITY;
                }

                @Override
                public String getName() {
                    return cls.getName();
                }

                @Override
                public CAstType getType() {
                    return cls;
                }

                @Override
                public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
                    return Collections.singletonMap(null, members);
                }

                @Override
                public Position getPosition(int arg) {
                    return null;
                }

                @Override
                public Position getPosition() {
                    return makePosition(arg0);
                }

                @Override
                public Position getNamePosition() {
                    return makePosition(arg0.getInternalNameNode());
                }

            };

            PythonParser.WalkContext child = new PythonParser.WalkContext() {
                private final CAstSourcePositionRecorder pos = new CAstSourcePositionRecorder();

                @Override
                public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
                    return Collections.singletonMap(null, members);
                }

                @Override
                public PythonTree top() {
                    return arg0;
                }

                @Override
                public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
                    members.add(visit);
                }

                private PythonParser.WalkContext codeParent() {
                    PythonParser.WalkContext p = parent;
                    while (p.entity().getKind() == CAstEntity.TYPE_ENTITY) {
                        p = p.getParent();
                    }
                    return p;
                }

                @Override
                public CAstControlFlowRecorder cfg() {
                    return (CAstControlFlowRecorder) codeParent().entity().getControlFlow();
                }

                @Override
                public CAstSourcePositionRecorder pos() {
                    return pos;
                }

                @Override
                public CAstNodeTypeMapRecorder getNodeTypeMap() {
                    return codeParent().getNodeTypeMap();
                }

                @Override
                public PythonTree getContinueFor(String label) {
                    assert false;
                    return null;
                }

                @Override
                public PythonTree getBreakFor(String label) {
                    assert false;
                    return null;
                }

                @Override
                public CAstEntity entity() {
                    return clse;
                }

                @Override
                public PythonParser.WalkContext getParent() {
                    return parent;
                }
            };

            // Change this statement to use `makeVisitor`.
            CAstVisitor v = makeVisitor(child, parser);

            List<CAstNode> methodDefaults = new ArrayList<>();
            for(stmt e : arg0.getInternalBody()) {
                if (! (e instanceof Pass)) {
                    CAstNode methodDefault = e.accept(v);
                    if (methodDefault != null)
                        methodDefaults.add(methodDefault);
                }
            }

            CAstNode classStmt;
            if (methodDefaults.size() == 0) {
                classStmt = Ast.makeNode(CAstNode.CLASS_STMT, Ast.makeConstant(clse));
            } else {
                classStmt = Ast.makeNode(CAstNode.BLOCK_STMT,
                        Ast.makeNode(CAstNode.CLASS_STMT, Ast.makeConstant(clse)),
                        Ast.makeNode(CAstNode.BLOCK_STMT, methodDefaults));
            }
            context.addScopedEntity(classStmt, clse);
            return classStmt;
        }

        @Override
        public CAstNode visitFor(For arg0) throws Exception {
            PyObject target = arg0.getTarget();
            PyObject iter = arg0.getIter();
            java.util.List<stmt> internalBody = arg0.getInternalBody();

            return handleFor(target, iter, internalBody);
        }

        private CAstNode handleFor(PyObject target, PyObject iter, java.util.List<stmt> internalBody) throws Exception {
            Pass b = new Pass();
            Pass c = new Pass();
            LoopContext x = new LoopContext(context, b, c);
            // Change this statement to use `makeVisitor`.
            CAstVisitor child = makeVisitor(x, parser);

            CAstNode breakStmt = b.accept(this);
            context.cfg().map(b, breakStmt);

            CAstNode continueStmt = c.accept(this);
            context.cfg().map(c, continueStmt);

            int i = 0;
            CAstNode[] body = new CAstNode[ internalBody.size() ];
            for(stmt s : internalBody) {
                body[i++] = s.accept(child);
            }

            comprehension g = new comprehension();
            g.setIter(iter);
            g.setTarget(target);

            return
                    Ast.makeNode(CAstNode.BLOCK_EXPR,
                            doGenerators(
                                    Collections.singletonList(g),
                                    Ast.makeNode(CAstNode.BLOCK_EXPR,
                                            Ast.makeNode(CAstNode.BLOCK_EXPR, body),
                                            continueStmt)),
                            breakStmt);
        }

        @Override
        public CAstNode visitFunctionDef(FunctionDef arg0) throws Exception {
            String composedFuncName = compositeName(arg0.getInternalName(), context);

            // Create `Argument` object.
            args.add(new Arguments(
                    arg0.getInternalName(), composedFuncName, (arguments) arg0.getArgs(), arg0.getInternalDecorator_list()
            ));

            // TODO: I want to group above `Arguments` object creation and below argument parsing.

            arguments aa = arg0.getInternalArgs();
            java.util.List<arg> args = aa.getInternalArgs();
            if (aa.getInternalKwarg() != null) {
                args = new LinkedList<>(args);
                args.add(aa.getInternalKwarg());
            }
            if (aa.getInternalVararg() != null) {
                args = new LinkedList<>(args);
                args.add(aa.getInternalVararg());
            }
            java.util.Set<CAstNode> x = HashSetFactory.make();
            if (arg0.getDecorator_list() != null) {
                for(expr f : arg0.getInternalDecorator_list()) {
                    x.add(f.accept(this));
                }
            }
            return defineFunction(
                    arg0.getInternalName(),
                    args,
                    arg0.getInternalBody(),
                    arg0,
                    makePosition(arg0.getInternalNameNode()),
                    codeBody,
                    aa.getInternalDefaults(),
                    x
            );
        }

        private <R extends PythonTree, S extends PythonTree> CAstNode
                defineFunction(String functionName,
                               java.util.List<R> arguments,
                               java.util.List<S> body,
                               PythonTree function,
                               Position namePos,
                               CAstType superType,
                               java.util.List<expr> defaults,
                               Iterable<CAstNode> dynamicAnnotations) throws Exception {
            int i = 0;
            CAstNode[] nodes = new CAstNode[ body.size() ];

            List<CAstNode> defaultVarsList = new ArrayList<>();
            List<CAstNode> defaultCodeList = new ArrayList<>();
            int arg = 0;
            for(expr dflt : defaults) {
                if (dflt == null) {
//                    arg++;
                    continue;
                }
                String name = functionName + "_default_" + arg++;
                context.root().addGlobal(name);
                defaultCodeList.add(
                        Ast.makeNode(CAstNode.ASSIGN,
                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)),
                            dflt.accept(this)
                ));
                defaultVarsList.add(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name)));
            }

            CAstNode[] defaultVars = defaultVarsList.toArray(new CAstNode[0]);
            CAstNode[] defaultCode = defaultCodeList.toArray(new CAstNode[0]);;

            class PythonCodeType implements CAstType {

                @Override
                public Collection<CAstType> getSupertypes() {
                    return Collections.singleton(superType);
                }

                @Override
                public String getName() {
                    return functionName;
                }

                public CAstType getReturnType() {
                    return CAstType.DYNAMIC;
                }

                public Collection<CAstType> getExceptionTypes() {
                    return Collections.singleton(CAstType.DYNAMIC);
                }

                public java.util.List<CAstType> getArgumentTypes() {
                    java.util.List<CAstType> types = new ArrayList<CAstType>();
                    for(int i = 0; i < getArgumentCount()+1; i++) {
                        types.add(CAstType.DYNAMIC);
                    }
                    return types;
                }

                public int getArgumentCount() {
                    int sz = 1;
                    for(Object e : arguments) {
                        sz += (e instanceof Tuple)? ((Tuple)e).getInternalElts().size(): 1;
                    }
                    return sz+1;
                }

                @Override
                public String toString() {
                    return getName();
                }
            };

            CAstType functionType;
            boolean isMethod =
                    context.entity().getKind() == CAstEntity.TYPE_ENTITY &&
                            arguments.size()>0 &&
                            !functionName.startsWith("lambda") &&
                            !functionName.startsWith("comprehension");
            if (isMethod) {
                class PythonMethod extends PythonCodeType implements CAstType.Method {
                    @Override
                    public CAstType getDeclaringType() {
                        return context.entity().getType();
                    }

                    @Override
                    public boolean isStatic() {
                        return false;
                    }
                };

                functionType = new PythonMethod();
            } else {
                class PythonFunction extends PythonCodeType implements CAstType.Function {

                };

                functionType = new PythonFunction();
            }

            int x = 0;
            int sz = countArguments(arguments);
            String[] argumentNames = new String[ sz ];
            int[] argumentMap = new int[sz];
            int argIndex = 0;
            argumentNames[x++] = "the function";
            argumentMap[argIndex++] = 0;
            int ai = 0;
            for(Object a : arguments) {
                x = handleFunctionArguments(x, argumentNames, argumentMap, ai, a);
                ai++;
            }

            class PythonCodeEntity extends AbstractCodeEntity implements PythonGlobalsEntity, DynamicAnnotatableEntity {
                private final java.util.Set<String> downwardGlobals;

                @Override
                public Iterable<CAstNode> dynamicAnnotations() {
                    return dynamicAnnotations;
                }

                protected PythonCodeEntity(CAstType type, java.util.Set<String> downwardGlobals) {
                    super(type);
                    this.downwardGlobals = downwardGlobals;
                }

                @Override
                public int getKind() {
                    return CAstEntity.FUNCTION_ENTITY;
                }

                @Override
                public CAstNode getAST() {
                    if (function instanceof FunctionDef) {
                        if (isMethod) {
                            CAst Ast = PypstaParser.this.Ast;
                            CAstNode[] newNodes = new CAstNode[ nodes.length + 2];
                            System.arraycopy(nodes, 0, newNodes, 2, nodes.length);

                            newNodes[0] =
                                    Ast.makeNode(CAstNode.DECL_STMT,
                                            Ast.makeConstant(new CAstSymbolImpl("super", PythonCAstToIRTranslator.Any)),
                                            Ast.makeNode(CAstNode.NEW, Ast.makeConstant("superfun")));
                            newNodes[1] =
                                    Ast.makeNode(CAstNode.BLOCK_STMT,
                                            Ast.makeNode(CAstNode.ASSIGN,
                                                    Ast.makeNode(CAstNode.OBJECT_REF,
                                                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                                                            Ast.makeConstant("$class")),
                                                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(context.entity().getType().getName()))),
                                            Ast.makeNode(CAstNode.ASSIGN,
                                                    Ast.makeNode(CAstNode.OBJECT_REF,
                                                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant("super")),
                                                            Ast.makeConstant("$self")),
                                                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(getArgumentNames()[1]))));

                            return Ast.makeNode(CAstNode.BLOCK_STMT, newNodes);
                        } else {
                            return PypstaParser.this.Ast.makeNode(CAstNode.BLOCK_STMT, nodes);
                        }
                    } else {
                        return PypstaParser.this.Ast.makeNode(CAstNode.RETURN,
                                PypstaParser.this.Ast.makeNode(CAstNode.BLOCK_EXPR, nodes));

                    }
                }

                @Override
                public String getName() {
                    return functionName;
                }

                @Override
                public String[] getArgumentNames() {
                    return argumentNames;
                }

                @Override
                public CAstNode[] getArgumentDefaults() {
                    return defaultVars;
                }

                @Override
                public int getArgumentCount() {
                    return argumentNames.length;

                }

                @Override
                public Collection<CAstQualifier> getQualifiers() {
                    return Collections.emptySet();
                }

                @Override
                public Position getPosition() {
                    return makePosition(function);
                }

                @Override
                public Position getPosition(int arg) {
                    return makePosition(arguments.get(argumentMap[arg]));
                }

                @Override
                public Position getNamePosition() {
                    return namePos;
                }

                @Override
                public java.util.Set<String> downwardGlobals() {
                    return downwardGlobals;
                }
            };

            java.util.Set<String> downwardGlobals = HashSetFactory.make();

            PythonCodeEntity fun = new PythonCodeEntity(functionType, downwardGlobals);

            FunctionContext child = new FunctionContext(context, fun, downwardGlobals, function);
            // Change this statement to use `makeVisitor`.
            CAstVisitor cv = makeVisitor(child, parser);
            for(S s : body) {
                nodes[i++] = s.accept(cv);
            }

            if (isMethod) {
                context.addScopedEntity(null, fun);
                if (defaultCode.length == 0) {
                    return null;
                } else {
                    return Ast.makeNode(CAstNode.BLOCK_EXPR, defaultCode);
                }

            } else {
                CAstNode stmt = Ast.makeNode(CAstNode.FUNCTION_EXPR, Ast.makeConstant(fun));
                context.addScopedEntity(stmt, fun);
                CAstNode val =
                        !(function instanceof FunctionDef)?
                                stmt:
                                Ast.makeNode(CAstNode.DECL_STMT,
                                        Ast.makeConstant(new CAstSymbolImpl(fun.getName(), PythonCAstToIRTranslator.Any)),
                                        stmt);

                if (defaultCode.length == 0) {
                    return val;
                } else {
                    return Ast.makeNode(CAstNode.BLOCK_EXPR,
                            Ast.makeNode(CAstNode.BLOCK_EXPR, defaultCode),
                            val);
                }
            }
        }

        private <R extends PythonTree> int countArguments(java.util.List<R> arguments) {
            int sz = 1;
            for(Object e : arguments) {
                sz += (e instanceof Tuple)? countArguments(((Tuple)e).getInternalElts()): 1;
            }
            return sz;
        }

        private int handleFunctionArguments(int x, String[] argumentNames, int[] argumentMap, int ai, Object a)
                throws Exception {
            if (a instanceof Tuple) {
                Tuple t = (Tuple)a;
                for(expr e : t.getInternalElts()) {
                    x = handleFunctionArguments(x, argumentNames, argumentMap, ai, e);
                }
            } else if (a instanceof arg) {
                String name = ((arg)a).getInternalArg();
                argumentMap[x] = ai;
                argumentNames[x++] = name;
            } else if (a instanceof Name) {
                String name = ((Name)a).getText();
                argumentMap[x] = ai;
                argumentNames[x++] = name;
            } else {
                assert false : "unexpected " + a;
            }
            return x;
        }

        @Override
        public CAstNode visitGlobal(Global arg0) throws Exception {
            arg0.getInternalNames().forEach(n -> context.root().addGlobal(n));
            return super.visitGlobal(arg0);
        }

        private CAstNode doGenerators(java.util.List<comprehension> generators, CAstNode body) throws Exception {
            CAstNode result = body;

            for(comprehension c : generators)  {
                if (c.getInternalIfs() != null) {
                    int j = c.getInternalIfs().size();
                    if (j > 0) {
                        for(expr test : c.getInternalIfs()) {
                            CAstNode v = test.accept(this);
                            result = Ast.makeNode(CAstNode.IF_EXPR, v, body);
                        }
                    }
                }

                String tempName = "temp " + ++tmpIndex;

                CAstNode test =
                        Ast.makeNode(CAstNode.BINARY_EXPR,
                                CAstOperator.OP_NE,
                                Ast.makeConstant(null),
                                Ast.makeNode(CAstNode.BLOCK_EXPR,
                                        Ast.makeNode(CAstNode.ASSIGN,
                                                c.getInternalTarget().accept(this),
                                                Ast.makeNode(CAstNode.EACH_ELEMENT_GET,
                                                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
                                                        c.getInternalTarget().accept(this)))));

                result = notePosition(Ast.makeNode(CAstNode.BLOCK_EXPR,

                        Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl(tempName, PythonCAstToIRTranslator.Any)),
                                c.getInternalIter().accept(this)),

                        Ast.makeNode(CAstNode.ASSIGN,
                                c.getInternalTarget().accept(this),
                                Ast.makeNode(CAstNode.EACH_ELEMENT_GET,
                                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
                                        Ast.makeConstant(null))),

                        Ast.makeNode(CAstNode.LOOP,
                                test,
                                Ast.makeNode(CAstNode.BLOCK_EXPR,
                                        Ast.makeNode(CAstNode.ASSIGN,
                                                c.getInternalTarget().accept(this),
                                                Ast.makeNode(CAstNode.OBJECT_REF,
                                                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(tempName)),
                                                        c.getInternalTarget().accept(this))),
                                        result))), c);

            }

            return result;
        }

        @Override
        public CAstNode visitSubscript(Subscript arg0) throws Exception {
            slice s = arg0.getInternalSlice();
            if (s instanceof Index) {
                if (s.getChildren().size() == 1 && s.getChild(0) instanceof org.python.antlr.ast.List) {
                    org.python.antlr.ast.List l = (org.python.antlr.ast.List) s.getChild(0);
                    CAstNode[] cs = new CAstNode[ l.getChildCount() + 3 ];
                    cs[0] = Ast.makeNode(CAstNode.VAR, Ast.makeConstant("pypsta_slice"));
                    cs[1] = Ast.makeNode(CAstNode.EMPTY);
                    cs[2] = acceptOrNull(arg0.getInternalValue());
                    for(int i = 0; i < l.getChildCount(); i++) {
                        cs[i+3] = l.getChild(i).accept(this);
                    }
                    return notePosition(Ast.makeNode(CAstNode.CALL, cs), arg0);
                } else {
                    return super.visitSubscript(arg0);
                }
            } else if (s instanceof Slice) {
                Slice S = (Slice) s;
                return notePosition(
                        Ast.makeNode(CAstNode.CALL,
                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant("pypsta_slice")),
                            Ast.makeNode(CAstNode.EMPTY),
                            acceptOrNull(arg0.getInternalValue()),
                            acceptOrNull(S.getInternalLower()),
                            acceptOrNull(S.getInternalUpper()),
                            acceptOrNull(S.getInternalStep()) ),
                        arg0);
            } else {
                return super.visitSubscript(arg0);
            }
        }

        private class TryCatchContext extends TranslatorToCAst.TryCatchContext<PythonParser.WalkContext, PythonTree> implements PythonParser.WalkContext {

            TryCatchContext(PythonParser.WalkContext parent, Map<String, CAstNode> catchNode) {
                super(parent, catchNode);
            }

            @Override
            public PythonParser.WalkContext getParent() {
                return (PythonParser.WalkContext) super.getParent();
            }

        }

        @Override
        public CAstNode visitTryExcept(TryExcept arg0) throws Exception {
            Map<String,CAstNode> handlers = HashMapFactory.make();
            for(PyObject x : arg0.getChildren()) {
                if (x instanceof ExceptHandler) {
                    CAstNode n1, n2;
                    ExceptHandler h = (ExceptHandler) x;
                    String name = h.getInternalName()==null?
                            "x":
                            h.getInternalName();
                    CAstNode type = h.getInternalType()==null?
                            Ast.makeConstant("Exception"):
                            h.getInternalType().accept(this);
                    CAstNode body = block(h.getInternalBody());
                    CAstNode handler = Ast.makeNode(CAstNode.CATCH,
                            n1=Ast.makeConstant(name),
                            Ast.makeNode(CAstNode.BLOCK_STMT,
                                    Ast.makeNode(CAstNode.ASSIGN,
                                            Ast.makeNode(CAstNode.VAR, Ast.makeConstant("$currentException")),
                                            Ast.makeNode(CAstNode.VAR, n2=Ast.makeConstant(name))),
                                    body));
                    while (type.getValue() == null) {
                        type = type.getChild(0);
                    }
                    String typeStr = type.getValue().toString();
                    handlers.put(typeStr, handler);

                    context.cfg().map(handler, handler);

                    if (h.getInternalType() != null) {
                        context.getNodeTypeMap().add(n1, types.getCAstTypeFor(h.getInternalType()));
                        context.getNodeTypeMap().add(n2, types.getCAstTypeFor(h.getInternalType()));
                    }
                }
            }

            TryCatchContext catches = new TryCatchContext(context, handlers);
            // Change this statement to use `makeVisitor`.
            CAstVisitor child = makeVisitor(catches, parser);
            CAstNode block = child.block(arg0.getInternalBody());

            System.err.println("catches: " + handlers);

            return Ast.makeNode(CAstNode.TRY,
                    Ast.makeNode(CAstNode.BLOCK_EXPR,
                            block,
                            block(arg0.getInternalOrelse())),
                    handlers.values().toArray(new CAstNode[ handlers.size() ]));
        }

        private class LoopContext extends TranslatorToCAst.LoopContext<PythonParser.WalkContext, PythonTree>
                implements PythonParser.WalkContext {
            LoopContext(PythonParser.WalkContext parent, PythonTree breakTo, PythonTree continueTo) {
                super(parent, breakTo, continueTo, null);
            }

            @Override
            public PythonParser.WalkContext getParent() {
                return (PythonParser.WalkContext) super.getParent();
            }
        }

        @Override
        public CAstNode visitWhile(While arg0) throws Exception {
            Pass b = new Pass();
            Pass c = new Pass();
            LoopContext x = new LoopContext(context, b, c);
            // Change this statement to use `makeVisitor`.
            CAstVisitor child = makeVisitor(x, parser);

            if (arg0.getInternalOrelse() == null || arg0.getInternalOrelse().size() == 0) {
                return
                        Ast.makeNode(CAstNode.BLOCK_EXPR,
                                Ast.makeNode(CAstNode.LOOP,
                                        arg0.getInternalTest().accept(child),
                                        Ast.makeNode(CAstNode.BLOCK_EXPR,
                                                child.block(arg0.getInternalBody()),
                                                c.accept(child))),
                                b.accept(child));
            } else {
                return Ast.makeNode(CAstNode.BLOCK_EXPR,
                        Ast.makeNode(CAstNode.LOOP,
                                Ast.makeNode(CAstNode.ASSIGN,
                                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant("test tmp")),
                                        arg0.getInternalTest().accept(child)),
                                Ast.makeNode(CAstNode.BLOCK_EXPR,
                                        child.block(arg0.getInternalBody()),
                                        c.accept(child))),
                        Ast.makeNode(CAstNode.IF_STMT,
                                Ast.makeNode(CAstNode.UNARY_EXPR,
                                        CAstOperator.OP_NOT,
                                        Ast.makeNode(CAstNode.VAR, Ast.makeConstant("test tmp"))),
                                child.block(arg0.getInternalOrelse())),
                        b.accept(child));
            }
        }

        private CAstNode block(java.util.List<stmt> block) throws Exception {
            java.util.List<CAstNode> x = new LinkedList<>();
            for(int i = 0; i < block.size(); i++) {
                CAstNode y = block.get(i).accept(this);
                if (y != null) {
                    x.add(y);
                }
            }
            return Ast.makeNode(CAstNode.BLOCK_STMT, x.toArray(new CAstNode[ x.size() ]));
        }
    }

    private final CAstTypeDictionaryImpl<String> types;

    private final java.util.Set<Arguments> args;

    protected PypstaParser(CAstTypeDictionaryImpl<String> types, java.util.Set<Arguments> args) {
        super(types);
        this.types = types;
        this.args = args;
    }

    protected CAstVisitor makeVisitor(PythonParser.WalkContext context, WalaPythonParser parser) {
        return new CAstVisitor(context, parser);
    }
}
