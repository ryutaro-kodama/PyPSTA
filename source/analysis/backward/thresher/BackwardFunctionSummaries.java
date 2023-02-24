package analysis.backward.thresher;

import analysis.backward.thresher.constraint.TypeConstraint;
import analysis.backward.thresher.constraint.ValueConstraint;
import analysis.backward.thresher.query.AbstractPypstaQuery;
import analysis.backward.thresher.term.*;
import analysis.forward.ForwardAnalyzer;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.NotConstantFieldVarFactor;
import analysis.backward.thresher.factor.VariableFactor;
import analysis.backward.thresher.pathinfo.PypstaPathInfo;
import analysis.backward.thresher.query.IPypstaQuery;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import edu.colorado.thresher.core.IPathInfo;

import java.util.*;
import java.util.stream.Collectors;

public class BackwardFunctionSummaries {
    public static ForwardAnalyzer forwardAnalyzer;

    public interface BackwardSummary {
        List<IPathInfo> call(PythonInvokeInstruction instr,
                             PypstaPathInfo pypstaPathInfo,
                             CGNode callee,
                             IPypstaQuery query);
    }

    private static final HashMap<String, BackwardSummary> methods = new HashMap<String, BackwardSummary>() {{
        put("Lgetattr", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lisinstance", BackwardFunctionSummaries::isinstance);
        put("Lmap", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lnext", BackwardFunctionSummaries::next);
        put("Lobject", BackwardFunctionSummaries::object);
        put("Lreversed", BackwardFunctionSummaries::dropDefVarConstraint);


        put("Llist", BackwardFunctionSummaries::list);
        put("Llist/append", BackwardFunctionSummaries::append);
        put("Llist/pop", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Llist/remove", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lstring/decode", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/encode", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/format", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/join", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/lower", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/partition", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/replace", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/rstrip", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/startswith", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/split", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/splitlines", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lstring/strip", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lset", BackwardFunctionSummaries::set);
        put("Lset/add", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lset/clear", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Ltuple", BackwardFunctionSummaries::tuple);

        put("Ldict/get", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Ldict/items", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Ldict/values", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lscript pypsta_mock.py/range", BackwardFunctionSummaries::range);

        put("Lwala/builtin/abs", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/int", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/len", BackwardFunctionSummaries::len);
        put("Lwala/builtin/list", BackwardFunctionSummaries::list);
        put("Lwala/builtin/max", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/min", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/open", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/range", BackwardFunctionSummaries::range);
        put("Lwala/builtin/type", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/slice", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lwala/builtin/sorted", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lmath/function/sqrt", BackwardFunctionSummaries::mathFunctionSqrt);
        put("Lmath/function/tan", BackwardFunctionSummaries::mathFunctionTan);

        put("Lpyperf/Runner/parse_args", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Largparse/_ActionsContainer/add_argument", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Largparse/_ActionsContainer/set_defaults", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Largparse/ArgumentParser/parse_args", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Larray/array", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Larray/class/array", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lre/Match/groupdict", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lre/Pattern/split", BackwardFunctionSummaries::dropDefVarConstraint);

        put("Lrandom/function/choice", BackwardFunctionSummaries::dropDefVarConstraint);
        put("Lrandom/function/randrange", BackwardFunctionSummaries::randomFunctionRandrange);
    }};

    public static boolean contain(CGNode callee) {
        String methodName = callee.getMethod().getDeclaringClass().getName().toString();
        return methods.containsKey(methodName);
    }

    public static List<IPathInfo> call(PythonInvokeInstruction instr,
                                       PypstaPathInfo pypstaPathInfo,
                                       CGNode callee,
                                       IPypstaQuery query) {
        String methodName = callee.getMethod().getDeclaringClass().getName().toString();
        return methods.get(methodName).call(instr, pypstaPathInfo, callee, query);
    }

    private static List<IPathInfo> isinstance(PythonInvokeInstruction instr,
                                              PypstaPathInfo pypstaPathInfo,
                                              CGNode callee,
                                              IPypstaQuery query) {
        ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        );
        Set<TypeReference> clsTypes = forwardResult.getValueTypes(instr.getUse(2));

        if (clsTypes.size() == 1) {
            TypeReference clsType = clsTypes.iterator().next();
            TypeReference clsInstanceType = TypeReference.find(
                    PythonTypes.pythonLoader,
                    clsType.getName().toString() + "_instance"
            );

            Set<TypeReference> instanceTypes = forwardResult.getValueTypes(instr.getUse(1));
            VariableFactor instanceVar = new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getUse(1));
            VariableTerm instanceTerm = new VariableTerm(instanceVar, forwardResult.getValue(instr.getUse(1)));

            VariableFactor defVar = new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef());
            VariableTerm defTerm = new VariableTerm(defVar, forwardResult.getValue(instr.getDef()));

            List<IPathInfo> splitPaths = new LinkedList<>();

            for (TypeReference instanceType: instanceTypes) {
                PypstaPathInfo newPath = pypstaPathInfo.deepCopy();

                if (instanceType.equals(clsInstanceType)) {
                    newPath.getQuery().substituteExpForVar(new ConstantTerm(true), defVar);
                } else {
                    newPath.getQuery().substituteExpForVar(new ConstantTerm(false), defVar);
                }
                newPath.getQuery().addConstraint(
                        new TypeConstraint(instanceTerm, new ConstantTypeTerm(instanceType))
                );

                if (newPath.isFeasible()) {
                    splitPaths.add(newPath);
                }
            }

            pypstaPathInfo.refute();
            if (splitPaths.size() == 0) {
                return PypstaPathInfo.INFEASIBLE;
            } else {
                return splitPaths;
            }


//            BinOpTerm binOpTerm = new BinOpTerm(
//                    (ITerm) new VariableTerm(
//                            new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getUse(1)),
//                            forwardResult.getValue(instr.getUse(1))
//                    ),
//                    new ConstantTypeTerm(
//                            TypeReference.find(
//                                    PythonTypes.pythonLoader,
//                                    clsType.getName().toString() + "_instance"
//                            )
//                    ),
//                    CAstBinaryOp.EQ
//            );


//            ValueConstraint constraint = new ValueConstraint(
//                    new VariableTerm(
//                            new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
//                            forwardResult.getValue(instr.getDef())
//                    ),
//                    binOpTerm,
//                    IConditionalBranchInstruction.Operator.EQ
//            );
//            TypeConstraint constraint = new TypeConstraint(
//                    new VariableTerm(
//                            new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getUse(1)),
//                            forwardResult.getValue(instr.getUse(1))
//                    ),
//                    new ConstantTypeTerm(
//                            TypeReference.find(
//                                    PythonTypes.pythonLoader,
//                                    clsType.getName().toString() + "_instance"
//                            )
//                    )
//            );
//            query.addConstraint(constraint);
//            query.substituteExpForVar(binOpTerm, defVar);
//            if (query.isFeasible())
//                return PypstaPathInfo.FEASIBLE;
//            else
//                return PypstaPathInfo.INFEASIBLE;
        } else {
            // TODO:
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> next(PythonInvokeInstruction instr,
                                        PypstaPathInfo pypstaPathInfo,
                                        CGNode callee,
                                        IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (!lhsForwardResultType.equals(TypeReference.find(PythonTypes.pythonLoader, "Lnext")))
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint nextConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getUse(0)),
                    TypeReference.find(PythonTypes.pythonLoader, "Lnext")
            );
            query.addConstraint(nextConstraint);
        }

        dropDefVarConstraint(instr, pypstaPathInfo, callee, query);

        return PypstaPathInfo.FEASIBLE;
    }

    private static List<IPathInfo> object(PythonInvokeInstruction instr,
                                  PypstaPathInfo pypstaPathInfo,
                                  CGNode callee,
                                  IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(PythonTypes.object))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    PythonTypes.object
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> list(PythonInvokeInstruction instr,
                                        PypstaPathInfo pypstaPathInfo,
                                        CGNode callee,
                                        IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(PythonTypes.list))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    PythonTypes.list
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> append(PythonInvokeInstruction instr,
                                          PypstaPathInfo pypstaPathInfo,
                                          CGNode callee,
                                          IPypstaQuery query) {
        // You can't generate constraint for element, so the assumption for dropping 'EEGConstraint'
        // has broken. So you add query not to drop 'EEGConstraint' for the list.
        Set<SSAInstruction> funcObjDefInstrs = Arrays.stream(pypstaPathInfo.getCurrentNode().getIR().getInstructions()).parallel()
                .filter(i -> i != null)
                .filter(i -> i.getDef() == instr.getUse(0))
                .collect(Collectors.toSet());
        if (funcObjDefInstrs.size() == 0) {
            Assertions.UNREACHABLE();
        } else if (funcObjDefInstrs.size() == 1) {
            int listObjVarId = funcObjDefInstrs.iterator().next().getUse(0);
            ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                    pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
            );
            ForwardAbstractValue listObjAbstractValue = forwardResult.getValue(listObjVarId);
            ((AbstractPypstaQuery) query).addCantEEGDrop(listObjAbstractValue.getAllocatePoints());
        } else {
            Assertions.UNREACHABLE();
        }

        return PypstaPathInfo.FEASIBLE;
    }

    private static List<IPathInfo> set(PythonInvokeInstruction instr,
                                       PypstaPathInfo pypstaPathInfo,
                                       CGNode callee,
                                       IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(PythonTypes.set))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    PythonTypes.set
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> tuple(PythonInvokeInstruction instr,
                                         PypstaPathInfo pypstaPathInfo,
                                         CGNode callee,
                                         IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(PythonTypes.tuple))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    PythonTypes.tuple
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> len(PythonInvokeInstruction instr,
                                       PypstaPathInfo pypstaPathInfo,
                                       CGNode callee,
                                       IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(TypeReference.Int))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    TypeReference.Int
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> range(PythonInvokeInstruction instr,
                                         PypstaPathInfo pypstaPathInfo,
                                         CGNode callee,
                                         IPypstaQuery query) {
        VariableFactor defVar = new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef());
        Set<IProgramFactor> pathVars = query.getPathVars();
        if (pathVars.contains(defVar)) {
            Set<NotConstantFieldVarFactor> objects = pathVars.stream()
                    .filter(v -> v instanceof NotConstantFieldVarFactor)
                    .map(v -> (NotConstantFieldVarFactor) v)
                    .filter(v -> v.getValFactor().equals(defVar))
                    .collect(Collectors.toSet());
            for (NotConstantFieldVarFactor objectVar: objects) {
                objectVar.addElementType(TypeReference.Int);
            }
        }
        return PypstaPathInfo.FEASIBLE;
    }

    private static List<IPathInfo> mathFunctionSqrt(PythonInvokeInstruction instr,
                                                    PypstaPathInfo pypstaPathInfo,
                                                    CGNode callee,
                                                    IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(TypeReference.Float))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    TypeReference.Float
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> mathFunctionTan(PythonInvokeInstruction instr,
                                                   PypstaPathInfo pypstaPathInfo,
                                                   CGNode callee,
                                                   IPypstaQuery query) {
        Set<TypeReference> lhsForwardResultTypes = forwardAnalyzer.getForwardResult(
                pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
        ).getValueTypes(instr.getDef());

        if (lhsForwardResultTypes.size() == 1) {
            // Manual evaluation using forward result.
            TypeReference lhsForwardResultType = lhsForwardResultTypes.iterator().next();
            if (lhsForwardResultType.equals(TypeReference.Float))
                return PypstaPathInfo.FEASIBLE;
            else
                return PypstaPathInfo.INFEASIBLE;
        } else {
            TypeConstraint resultConstraint = new TypeConstraint(
                    new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef()),
                    TypeReference.Float
            );
            query.addConstraint(resultConstraint);
            return PypstaPathInfo.FEASIBLE;
        }
    }

    private static List<IPathInfo> randomFunctionRandrange(PythonInvokeInstruction instr,
                                                           PypstaPathInfo pypstaPathInfo,
                                                           CGNode callee,
                                                           IPypstaQuery query) {
        VariableFactor defVar = new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef());
        if (query.getPathVars().contains(defVar)) {
            ForwardAnalyzer.ForwardResult forwardResult = forwardAnalyzer.getForwardResult(
                    pypstaPathInfo.getCurrentNode(), pypstaPathInfo.getCurrentBlock()
            );

            ITerm arg1Term;
            int arg1VarId = instr.getUse(1);
            if (forwardResult.isConstant(arg1VarId)) {
                arg1Term = new ConstantTerm(forwardResult.getConstant(arg1VarId));
            } else if (forwardResult.getValue(arg1VarId).isBottom()) {
                arg1Term = new VariableTerm(
                        new VariableFactor(pypstaPathInfo.getCurrentNode(), arg1VarId)
                );
            } else {
                arg1Term = new VariableTerm(
                        new VariableFactor(pypstaPathInfo.getCurrentNode(), arg1VarId),
                        forwardResult.getValue(arg1VarId)
                );
            }

            ITerm startTerm, stopTerm;
            if (instr.getNumberOfUses() == 2) {
                startTerm = new ConstantTerm(0);
                stopTerm = arg1Term;
            } else if (instr.getNumberOfUses() == 3) {
                startTerm = arg1Term;

                int stopVarId = instr.getUse(2);
                if (forwardResult.isConstant(stopVarId)) {
                    stopTerm = new ConstantTerm(forwardResult.getConstant(stopVarId));
                } else if (forwardResult.getValue(stopVarId).isBottom()) {
                    stopTerm = new VariableTerm(
                            new VariableFactor(pypstaPathInfo.getCurrentNode(), stopVarId)
                    );
                } else {
                    stopTerm = new VariableTerm(
                            new VariableFactor(pypstaPathInfo.getCurrentNode(), stopVarId),
                            forwardResult.getValue(stopVarId)
                    );
                }
            } else {
                Assertions.UNREACHABLE();
                startTerm = null; stopTerm = null;
            }

            VariableTerm defTerm;
            if (forwardResult.getValue(instr.getDef()).isBottom()) {
                defTerm = new VariableTerm(defVar);
            } else {
                defTerm = new VariableTerm(defVar, forwardResult.getValue(instr.getDef()));
            }
            query.addConstraint(
                    new ValueConstraint(
                            startTerm, defTerm, IConditionalBranchInstruction.Operator.LE
                    )
            );
            query.addConstraint(
                    new ValueConstraint(
                            defTerm, stopTerm, IConditionalBranchInstruction.Operator.LT
                    )
            );

            if (query.isFeasible()) {
                return PypstaPathInfo.FEASIBLE;
            } else {
                return PypstaPathInfo.INFEASIBLE;
            }
        }
        return PypstaPathInfo.FEASIBLE;
    }

    private static List<IPathInfo> dropDefVarConstraint(PythonInvokeInstruction instr,
                                                PypstaPathInfo pypstaPathInfo,
                                                CGNode callee,
                                                IPypstaQuery query) {
        VariableFactor defVar = new VariableFactor(pypstaPathInfo.getCurrentNode(), instr.getDef());
        query.dropConstraintsContaining(new HashSet<VariableFactor>(){{add(defVar);}});
        return PypstaPathInfo.FEASIBLE;
    }
}
