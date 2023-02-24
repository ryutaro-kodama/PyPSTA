package com.ibm.wala.cast.ir.translator;

import client.cast.PypstaCAstOperator;
import client.engine.PypstaAnalysisEngine;
import client.operator.PypstaBinaryOperator;

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.parser.AbstractParser;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.ibm.wala.cast.python.ir.PythonLanguage.Python;

public class PypstaCAstToIRTranslator extends com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator {
    private final Map<Scope, Map<Symbol, Integer>> globalDeclared = new HashMap<>();

    public PypstaCAstToIRTranslator(IClassLoader loader, Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
        super(loader, topLevelEntities);
    }

    @Override
    public void translate(final CAstEntity N, final ModuleEntry module) {
        translate(N, new RootContext(N, module));
    }

    @Override
    public void translate(CAstEntity N, WalkContext context) {
        final ExposedNamesCollector exposedNamesCollector = new PypstaExposedNamesCollector();
        exposedNamesCollector.run(N);
        if (liftDeclarationsForLexicalScoping()) {
            exposedNamesCollector.run(N);
        }
        entity2ExposedNames = exposedNamesCollector.getEntity2ExposedNames();
        entity2WrittenNames = exposedNamesCollector.getEntity2WrittenNames();
        walkEntities(N, context);
    }

    @Override
    protected IBinaryOpInstruction.IOperator translateBinaryOpcode(CAstNode op) {
        if (op == PypstaCAstOperator.OP_FDIV) return PypstaBinaryOperator.FDIV;
        else return super.translateBinaryOpcode(op);
    }

    @Override
    protected boolean doVisit(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
        // Override in order to handle `global` declared variable.
        if(n.getKind() == CAstNode.GLOBAL_DECL) {
            int numOfChildren = n.getChildCount();
            for (int i = 0; i < numOfChildren; i++) {
                String val = (String) n.getChild(i).getChild(0).getValue();
                System.out.println("Global declared [" + val + "] in " + context.getName());

                Symbol ls = context.currentScope().lookup(val);
                TypeReference type = makeType(ls.type());
                int lval = doGlobalRead(n, context, val, type);

                // Collect global declared variable names and its written variable id.
                Map<Symbol, Integer> globalDeclaredSymbols
                        = globalDeclared.get(context.currentScope());
                if (globalDeclaredSymbols == null) {
                    globalDeclaredSymbols = new HashMap<>();
                    globalDeclared.put(context.currentScope(), globalDeclaredSymbols);
                }
                globalDeclaredSymbols.put(ls, lval);
            }
        }
        return super.doVisit(n, context, visitor);
    }

    @Override
    protected void leaveVarAssignOp(CAstNode n,
                                    CAstNode v,
                                    CAstNode a,
                                    boolean pre,
                                    WalkContext context,
                                    CAstVisitor<WalkContext> visitor) {
        String nm = (String) n.getChild(0).getValue();
        Symbol ls = context.currentScope().lookup(nm);

        if (globalDeclared.containsKey(context.currentScope())
                && globalDeclared.get(context.currentScope()).containsKey(ls)) {
            // When rhs variable is global declared.
            int globalDeclaredVarId = globalDeclared.get(context.currentScope()).get(ls);

            // if (!pre) {
            //     int ret = context.currentScope().allocateTempValue();
            //     int currentInstruction = context.cfg().getCurrentInstruction();
            //     context.cfg().addInstruction(new AssignInstruction(currentInstruction, ret, temp));
            //     context.cfg().noteOperands(currentInstruction, context.getSourceMap().getPosition(n.getChild(0)));
            //     context.setValue(n, ret);
            // }

            // Make rhs value calculation instruction.
            int rval = processAssignOp(v, a, globalDeclaredVarId, context);

            // if (pre) {
            //     context.setValue(n, rval);
            // }

            int lval = context.currentScope().allocateTempValue();
            context.cfg().addInstruction(
                    new AssignInstruction(context.cfg().getCurrentInstruction(), lval, rval)
            );

            // The lhs value is assigned to global value again.
            doGlobalWrite(context, nm, makeType(ls.type()), lval);
        } else {
            super.leaveVarAssignOp(n, v, a, pre, context, visitor);
        }
    }

    @Override
    protected void leaveDeclStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
        CAstSymbol s1 = (CAstSymbol) n.getChild(0).getValue();
        super.leaveDeclStmt(n, c, visitor);

        // The import identifier in global scope is set to module. So create instruction to put
        // the identifier to module.
        CAstSymbol s = (CAstSymbol) n.getChild(0).getValue();
        String identifier = s.name();
             // Ignore identifier defined as builtin functions.
        if (!BuiltinFunctions.builtins().contains(identifier)
                // Ignore identifier defined as default import.
                && !Arrays.asList(AbstractParser.defaultImportNames).contains(identifier)
                // Ignore global scope.
                && c.currentScope().getEntity().getKind() == CAstEntity.SCRIPT_ENTITY
                // Ignore function identifier (in 'PythonCAstToIRTranslator.doMaterializeFunction',
                // create put instruction).
                && n.getChildCount() > 1
                && n.getChild(1).getKind() != CAstNode.FUNCTION_EXPR
                // Ignore mock module.
                && !identifier.startsWith("pypsta")
                // Ignore import temporary identifier.
                && !identifier.startsWith("importTree")) {

            Scope scope = c.currentScope();
            int valueId = scope.lookup(identifier).valueNumber();
            int moduleId = 1;
            c.cfg().unknownInstructions(() -> {
                FieldReference field = FieldReference.findOrCreate(
                        PythonTypes.Root, Atom.findOrCreateUnicodeAtom(identifier), PythonTypes.Root
                );
                c.cfg().addInstruction(
                        Python.instructionFactory().PutInstruction(
                                c.cfg().getCurrentInstruction(), moduleId, valueId, field));

            });
        }
    }

    @Override
    public void closeFunctionEntity(CAstEntity n, WalkContext parentContext, WalkContext functionContext) {
        doEndlogue(functionContext);
        super.closeFunctionEntity(n, parentContext, functionContext);
    }

    protected void doEndlogue(WalkContext context) {
        Scope scope = context.currentScope();

        // If this is module file's end, write this module object to package object.
        if (scope.getEntity().getKind() == CAstEntity.SCRIPT_ENTITY
                && !scope.getEntity().getName().contains("pypsta")
                && !PypstaAnalysisEngine.TOP_MODULE_NAME.isEmpty()) {
            int moduleId = 1;
            context.cfg().unknownInstructions(() -> {
                // Get package object.
                int packageId = doGlobalRead(null, context, PypstaAnalysisEngine.TOP_MODULE_NAME, PythonTypes.Root);

                FieldReference field = FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom(
                                scope.getEntity().getName().replace("script ", "").replace(".py", "")),
                        PythonTypes.Root
                );
                // Put this module to package object's attribute
                context.cfg().addInstruction(
                        Python.instructionFactory().PutInstruction(
                                context.cfg().getCurrentInstruction(), packageId, moduleId, field));

                // Write package object to global scope.
                doGlobalWrite(context, PypstaAnalysisEngine.TOP_MODULE_NAME, PythonTypes.Root, packageId);
            });
        }
    }

    @Override
    protected void leaveTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) {
        super.leaveTypeEntity(n, context, typeContext, visitor);

        String fnName = composeEntityName(context, n);
        IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));

        if (context.currentScope().getEntity().getKind() == CAstEntity.SCRIPT_ENTITY
                && cls instanceof PythonLoader.PythonClass) {
            // If class is declared in global scope, the module has the class in its attributes.
            // So add instruction to put class object to module object.
            String className = fnName.substring(fnName.lastIndexOf('/') + 1);
            int thisModuleId = 1;
            int classObjId = context.currentScope().lookup(className).valueNumber();

            FieldReference field = FieldReference.findOrCreate(
                    PythonTypes.Root,
                    Atom.findOrCreateUnicodeAtom(className),
                    PythonTypes.Root
            );
            // Put this class to module object's attribute
            context.cfg().addInstruction(
                    Python.instructionFactory().PutInstruction(
                            context.cfg().getCurrentInstruction(), thisModuleId, classObjId, field));
        }
    }
}
