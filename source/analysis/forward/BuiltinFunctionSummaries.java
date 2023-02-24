package analysis.forward;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.*;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import analysis.forward.abstraction.value.object.*;
import analysis.forward.fixpoint.ForwardCallManager;
import analysis.forward.fixpoint.ForwardFixSolver;
import client.loader.PythonSpecialMethodCallSiteReference;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;
import java.util.stream.Collectors;

public class BuiltinFunctionSummaries {
    public static TypeReference mockRangeTypeRef
            = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lscript pypsta_mock.py/range");
    public static TypeReference mockPypstaIterTypeRef
            = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lscript pypsta_mock.py/pypsta_iter");
    public static TypeReference mockPypstaNextTypeRef
            = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lscript pypsta_mock.py/pypsta_next");
    public static TypeReference mockPypstaSliceTypeRef
            = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lscript pypsta_mock.py/pypsta_slice");

    public interface Summary {
        IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst);
    }

    public static HashMap<String, Summary> summaries = new HashMap<>();

    static {
        summaries.put("Lgetattr", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                IForwardAbstractValue arg1 = args.get(1);
                IForwardAbstractValue arg2 = args.get(2);

                IForwardAbstractValue result = new ForwardAbstractValue();

                for (ObjectValue objectValue:
                        ((ForwardAbstractValue) arg1).getAllocatePoints().getObjectsIterable(
                                state.getAllocatePointTable())) {
                    result.union(objectValue.getAttr(arg2, state.getAllocatePointTable()));
                }

                if (args.size() == 4) {
                    // Set default value
                    result.union(args.get(3));
                }

                return result;
            }
        });

        summaries.put("Lfilter", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ListObjectValue result = new ListObjectValue(new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(result);

                ForwardAbstractValue dummyIndex = new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
                for (ObjectValue object:
                        ((ForwardAbstractValue) args.get(2)).getAllocatePoints()
                                .getObjectsIterable(state.getAllocatePointTable())) {
                    if (!(object instanceof ComplexObjectValue)) continue;
                    result.setElement(
                            dummyIndex,
                            ((ComplexObjectValue) object).getElement(
                                    dummyIndex, state.getAllocatePointTable())
                    );
                }
                return new ForwardAbstractValue(result);
            }
        });

        summaries.put("Lhasattr", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                if (args.size() != 3) {
                    // Error
                    return new ForwardAbstractValue();
                }

                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                ForwardAbstractValue arg2 = (ForwardAbstractValue) args.get(2);
                BoolValue result = new BoolValue();
                for (ObjectValue arg1Obj:
                        arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                    result.union(new BoolValue(arg1Obj.hasAttr(arg2, state.getAllocatePointTable())));

                    // Optimization
                    if (result.isTop()) break;
                }

                return new ForwardAbstractValue(result);
            }
        });

        summaries.put("Lid", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return args.get(1);
            }
        });

        summaries.put("Liter", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                AllocatePoints resultAPs = new AllocatePoints();

                for (AllocatePoint ap: ((ForwardAbstractValue) args.get(1)).getAllocatePoints()) {
                    IteratorObjectValue iter = new IteratorObjectValue(
                        new AllocatePoint(state.getCGNode(), inst), ap
                    );
                    state.getAllocatePointTable().newAllocation(iter);
                    resultAPs.add(iter.getAllocatePoint());
                }
                return new ForwardAbstractValue(resultAPs);
            }
        });

        summaries.put("Ltuple", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                assert arg1.getAllocatePoints().size() == 1;

                ObjectValue arg = arg1.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable())
                        .next();
                assert arg instanceof ComplexObjectValue;

                TupleObjectValue result = new TupleObjectValue(new AllocatePoint(state.getCGNode(), inst));
                state.getAllocatePointTable().newAllocation(result);

                HashMap<Integer, IForwardAbstractValue> elements = ((ComplexObjectValue) arg).getIntAccessElements();
                for (Map.Entry<Integer, IForwardAbstractValue> m: elements.entrySet()) {
                    result.setElement(m.getKey(), m.getValue().copy());
                }

                return new ForwardAbstractValue(result);
            }
        });

        summaries.put("Lset", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                AllocatePointTable apTable = state.getAllocatePointTable();

                SetObjectValue resultSet = new SetObjectValue(new AllocatePoint(state.getCGNode(), inst), apTable);
                apTable.newAllocation(resultSet);
                if (args.size() >= 2) {
                    ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                    for (ObjectValue objectValue: arg1.getAllocatePoints().getObjectsIterable(apTable)) {
                        if (objectValue instanceof ListObjectValue) {
                            ListObjectValue originalList = (ListObjectValue) objectValue;
                            for (Map.Entry<Integer, IForwardAbstractValue> m: originalList.getIntAccessElements().entrySet()) {
                                int index = m.getKey();
                                if (resultSet.hasElement(index)) {
                                    resultSet.getElement(index, apTable).union(originalList.getElement(index, apTable));
                                } else {
                                    resultSet.setElement(index, m.getValue().copy());
                                }
                            }
                            resultSet.getIntTopAccessedValue().union(originalList.getIntTopAccessedValue().copy());
                        } else {
                            Assertions.UNREACHABLE();
                            return null;
                        }
                    }
                } else if (args.size() == 1) {
                } else {
                    Assertions.UNREACHABLE();
                    return null;
                }
                return new ForwardAbstractValue(resultSet);
            }
        });

        summaries.put("Lmap", new Summary() {
            private final Map<SSAInstruction, Integer> tmpVarMap = new HashMap<>();

            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg2 = (ForwardAbstractValue) args.get(2);
                if (arg2.getAllocatePoints().size() == 0)
                    return new ForwardAbstractValue();

                AllocatePointTable callerStateAPTable = state.getAllocatePointTable();

                Iterable<ObjectValue> containerObjects = arg2.getAllocatePoints()
                        .getObjectsIterable(callerStateAPTable);

                // Get container's element
                IForwardAbstractValue element = new ForwardAbstractValue();
                for (ObjectValue containerObject: containerObjects) {
                    element.union(((ComplexObjectValue) containerObject).getElement(
                            new ForwardAbstractValue(
                                    new IntValue(LatticeTop.TOP)),
                                    state.getAllocatePointTable()
                            )
                    );
                }

                ListObjectValue result = new ListObjectValue(
                        new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(result);

                PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;
                ForwardAnalyzer analyzer = state.getSolver().getAnalyzer();
                ExplicitCallGraph cg = analyzer.getCGBuilder().getCallGraph();
                CGNode mapCGNode = cg.getPossibleTargets(state.getCGNode(), inst1.getCallSite()).iterator().next();

                int tmpVarId;
                if (tmpVarMap.containsKey(inst)) {
                    tmpVarId = tmpVarMap.get(inst);
                } else {
                    tmpVarId = state.getCGNode().getIR().getSymbolTable().newSymbol();
                    tmpVarMap.put(inst, tmpVarId);
                }
                state.setValue(tmpVarId, element);

                PythonInvokeInstruction dummyInst = new PythonInvokeInstruction(
                        inst.iIndex(),
                        -1,
                        -1,
                        new PythonSpecialMethodCallSiteReference(
                                inst1.getDeclaredTarget(), inst.iIndex(), inst
                        ),
                        new int[]{inst.getUse(1), tmpVarId},
                        new Pair[0]
                );

                IForwardAbstractValue funcResult = ForwardCallManager.call(dummyInst, state);
                result.setElement(
                        new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                        funcResult
                );

                for (CGNode mapCalleeCGNode:
                        cg.getPossibleTargets(state.getCGNode(), inst1.getCallSite())) {
                    if (mapCalleeCGNode.equals(mapCGNode)) continue;

                    // Replace the cg edge, which is from map's caller node, to the cg edge from map.
                    cg.removeEdge(state.getCGNode(), mapCalleeCGNode);
                    ((ExplicitCallGraph.ExplicitNode) state.getCGNode()).removeTarget(mapCalleeCGNode);
                    cg.addEdge(mapCGNode, mapCalleeCGNode);
                    mapCGNode.addTarget(dummyInst.getCallSite(), mapCalleeCGNode);
                }
                return new ForwardAbstractValue(result);
            }
        });

        summaries.put("Lnext", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue result = new ForwardAbstractValue();
                if (args.size() < 3) return result;

                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                for (ObjectValue iterObj:
                        arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                    if (iterObj instanceof IteratorObjectValue) {
                        IteratorObjectValue iterObj1 = (IteratorObjectValue) iterObj;
                        result.union((ForwardAbstractValue) iterObj1.next(args.get(2), state));
                    }
                }

                return result;
            }
        });

        summaries.put("Lreversed", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                assert arg1.getAllocatePoints().size() == 1;

                ObjectValue arg1Obj = arg1.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable())
                        .next();
                assert arg1Obj instanceof ListObjectValue;

                ListObjectValue arg1ObjContainer = (ListObjectValue) arg1Obj;

                ListObjectValue newObj = new ListObjectValue(new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(newObj);

                if (!arg1ObjContainer.getIntTopAccessedValue().isBottom()) {
                    // If int top accessed value is not bottom, the element order may not be correct.
                    // So union to all elements in origin list to int top accessed value.
                    IForwardAbstractValue newObjIntTopAccessedValue = newObj.getIntTopAccessedValue();
                    for(IForwardAbstractValue v: arg1ObjContainer.getIntAccessElements().values()) {
                        newObjIntTopAccessedValue.union(v);
                    }
                    newObjIntTopAccessedValue.union(arg1ObjContainer.getIntTopAccessedValue());
                } else {
                    Assertions.UNREACHABLE();
                }

                return new ForwardAbstractValue(newObj);
            }
        });

        summaries.put("Lsuperfun", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                if (args.size() == 1) {
                    AllocatePoints result = new AllocatePoints();

                    ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(0);
                    for (ObjectValue arg1Object: arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                        if (arg1Object.hasAttr("$class", state.getAllocatePointTable())) {
                            ForwardAbstractValue arg1$class = (ForwardAbstractValue) arg1Object.getAttr(
                                    "$class", state.getAllocatePointTable()
                            );
                            for (ObjectValue arg1$classObj: arg1$class.getAllocatePoints()
                                    .getObjectsIterable(state.getAllocatePointTable())) {
                                if (arg1$classObj instanceof ClassObjectValue) {
                                    ClassObjectValue thisClass = (ClassObjectValue) arg1$classObj;
                                    ClassObjectValue parentClassObj = (ClassObjectValue) state.getAllocatePointTable().get(thisClass.getParentAP());

                                    InstanceObjectValue baseClassInstance = new InstanceObjectValue(
                                            new AllocatePoint(state.getCGNode(), inst),
                                            TypeReference.findOrCreate(
                                                    PythonTypes.pythonLoader,
                                                    parentClassObj.getTypeReference().getName().toString() + "_instance"
                                            ),
                                            parentClassObj
                                    );
                                    state.getAllocatePointTable().newAllocation(baseClassInstance);

                                    result.add(baseClassInstance.getAllocatePoint());
                                } else {
                                    // Error
                                }
                            }
                        } else {
                            // Error
                        }
                    }

                    return new ForwardAbstractValue(result);
                } else if (args.size() == 3) {
                    return new ForwardAbstractValue();
                } else {
                    // Error
                    return new ForwardAbstractValue();
                }
            }
        });

        summaries.put("Lisinstance", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                ForwardAbstractValue arg2 = (ForwardAbstractValue) args.get(2);
                AllocatePointTable apTable = state.getAllocatePointTable();

                BoolValue result = new BoolValue();
                for (ObjectValue instance: arg1.getAllocatePoints().getObjectsIterable(apTable)) {
                    if (!(instance instanceof InstanceObjectValue)) {
                        result = new BoolValue(LatticeTop.TOP);
                        break;
                    }

                    for (ObjectValue cls: arg2.getAllocatePoints().getObjectsIterable(apTable)) {
                        if (!(cls instanceof ClassObjectValue)) {
                            result = new BoolValue(LatticeTop.TOP);
                            break;
                        }

                        result.union(new BoolValue(
                                ((InstanceObjectValue) instance).getBaseClassAP().equals(cls.getAllocatePoint())
                        ));
                    }

                    if (result.isTop()) break;
                }
                return new ForwardAbstractValue(result);
            }
        });

        summaries.put("Lwala/builtin/range", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                if (((ForwardAbstractValue) args.get(1)).getIntValue().isBottom()) {
                    // This happens because of the order of visiting basic block.
                    // Before visiting the basic block where the variable of the first argument is defined,
                    // the value must be bottom.
                    return new ForwardAbstractValue();
                }

                for (int i = 1; i < args.size(); i++) {
                    if (((ForwardAbstractValue) args.get(i)).getIntValue().isTop()) {
                        // If one of the arguments is top, you don't know what the element's value is. So return list in which top access value is top.
                        ListObjectValue result =
                                new ListObjectValue(
                                        new AllocatePoint(state.getCGNode(), inst),
                                        state.getAllocatePointTable());
                        result.setElement(
                                new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                                new ForwardAbstractValue(new IntValue(LatticeTop.TOP))
                        );
                        state.getAllocatePointTable().newAllocation(result);
                        return new ForwardAbstractValue(result);
                    }

                }

                // TODO: Implement concrete value is top.
                int start, end, step;
                if (args.size() == 2) {
                    start = 0;
                    end = ((ForwardAbstractValue) args.get(1)).getIntValue().getConcreteValue();
                    step = 1;
                } else if (args.size() == 3) {
                    start = ((ForwardAbstractValue) args.get(1)).getIntValue().getConcreteValue();
                    end = ((ForwardAbstractValue) args.get(2)).getIntValue().getConcreteValue();
                    step = 1;
                } else if (args.size() == 4) {
                    start = ((ForwardAbstractValue) args.get(1)).getIntValue().getConcreteValue();
                    end = ((ForwardAbstractValue) args.get(2)).getIntValue().getConcreteValue();
                    step = ((ForwardAbstractValue) args.get(3)).getIntValue().getConcreteValue();
                } else {
                    Assertions.UNREACHABLE();
                    start = 0; end = 0; step = 0;
                }
                ListObjectValue list = new ListObjectValue(
                        new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(list);
                for (int i = start; 0 <= i && i < end; i += step)
                    list.setElement(i, new ForwardAbstractValue(new IntValue(i)));
                IForwardAbstractValue newValue = new ForwardAbstractValue(list);
                return newValue;
            }
        });

        summaries.put("Lwala/builtin/list", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                AllocatePointTable apTable = state.getAllocatePointTable();

                ListObjectValue copiedList = new ListObjectValue(new AllocatePoint(state.getCGNode(), inst), apTable);
                if (args.size() > 1) {
                    ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                    for (ObjectValue objectValue: arg1.getAllocatePoints().getObjectsIterable(apTable)) {
                        if (objectValue instanceof ListObjectValue) {
                            ListObjectValue originalList = (ListObjectValue) objectValue;
                            for (Map.Entry<Integer, IForwardAbstractValue> m: originalList.getIntAccessElements().entrySet()) {
                                int index = m.getKey();
                                if (copiedList.hasElement(index)) {
                                    copiedList.getElement(index, apTable).union(originalList.getElement(index, apTable));
                                } else {
                                    copiedList.setElement(index, m.getValue().copy());
                                }
                            }
                            copiedList.getIntTopAccessedValue().union(originalList.getIntTopAccessedValue().copy());
                        } else {
                            Assertions.UNREACHABLE();
                            return null;
                        }
                    }
                } else {
                    Assertions.UNREACHABLE();
                    // result.add(new ListObjectValue(state.getContext(), callerNode));
                }
                apTable.newAllocation(copiedList);
                return new ForwardAbstractValue(copiedList);
            }
        });

        summaries.put("Lwala/builtin/slice", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                ForwardAbstractValue arg2 = (args.size() > 2) ? (ForwardAbstractValue) args.get(2) : new ForwardAbstractValue();
                ForwardAbstractValue arg3 = (args.size() > 3) ? (ForwardAbstractValue) args.get(3) : new ForwardAbstractValue();
                ForwardAbstractValue arg4 = (args.size() > 4) ? (ForwardAbstractValue) args.get(4) : new ForwardAbstractValue();
                ForwardAbstractValue arg5 = (args.size() > 5) ? (ForwardAbstractValue) args.get(5) : new ForwardAbstractValue();

                ForwardAbstractValue result = new ForwardAbstractValue();

                if (arg1.getAllocatePoints().size() > 0) {
                    ListObjectValue resultList = new ListObjectValue(new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                    state.getAllocatePointTable().newAllocation(resultList);

                    for (ObjectValue objectValue:
                            arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                        if (objectValue instanceof ListObjectValue) {
                            resultList = ((ListObjectValue) objectValue).slice(
                                    arg2, arg3, arg4, arg5, state.getAllocatePointTable(), resultList
                            );
                        } else {
                            Assertions.UNREACHABLE();
                        }
                    }
                    result.getAllocatePoints().add(resultList.getAllocatePoint());
                }

                if (!arg1.getStringValue().isBottom()) {
                    StringValue stringValue = new StringValue();
                    if (!arg1.getStringValue().isBottom()) {
                        if (arg1.getStringValue().isTop()) {
                            stringValue = new StringValue(LatticeTop.TOP);
                        } else {
                            Assertions.UNREACHABLE();
                        }
                    }
                    result.setStringValue(stringValue);
                }

                return result;
            }
        });

        summaries.put("Lwala/builtin/len", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                AllocatePoints target = ((ForwardAbstractValue) args.get(1)).getAllocatePoints();
                ForwardAbstractValue result = new ForwardAbstractValue();
                for (ObjectValue objectValue: target.getObjectsIterable(state.getAllocatePointTable())) {
                    if (objectValue instanceof ComplexObjectValue) {
                        result.getIntValue().union(((ComplexObjectValue) objectValue).size());
                    } else if (objectValue.hasAttr("__len__", state.getAllocatePointTable())) {
                        Assertions.UNREACHABLE();
                        // TODO: call
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }
                return result;
            }
        });

        summaries.put("Lwala/builtin/enumerate", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                AllocatePoints target = ((ForwardAbstractValue) args.get(1)).getAllocatePoints();
                EnumerateObjectValue enumerateObjectValue =
                        new EnumerateObjectValue(new AllocatePoint(state.getCGNode(), inst));
                state.getAllocatePointTable().newAllocation(enumerateObjectValue);
                for (ObjectValue objectValue: target.getObjectsIterable(state.getAllocatePointTable())) {
                    if (objectValue instanceof ComplexObjectValue) {
                        enumerateObjectValue.add((ComplexObjectValue) objectValue);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }
                return new ForwardAbstractValue(enumerateObjectValue);
            }
        });

        summaries.put("Lwala/builtin/sorted", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);

                IForwardAbstractValue result = new ForwardAbstractValue();
                for (ObjectValue argObject:
                        arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                    if (argObject instanceof DictObjectValue) {
                        DictObjectValue argDict = (DictObjectValue) argObject;

                        // TODO:
                        if (!argDict.getStringTopAccessedValue().isBottom()) Assertions.UNREACHABLE();

                        ListObjectValue resultList = new ListObjectValue(
                                new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable()
                        );
                        state.getAllocatePointTable().newAllocation(resultList);

                        // Collect keys which are sorted by ABC order.
                        List<String> sortedKeys = argDict.getStringAccessElements().keySet().stream()
                                .sorted()
                                .collect(Collectors.toList());
                        int index = 0;
                        for (String key: sortedKeys) {
                            resultList.setElement(index++, new ForwardAbstractValue(key));
                        }

                        result.union(new ForwardAbstractValue(resultList));
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }
                return result;
            }
        });

        summaries.put("Lwala/builtin/type", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                if (args.size() == 2) {
                    ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                    ForwardAbstractValue result = new ForwardAbstractValue();

                    // TODO: Impelement other pattern.
                    for (ObjectValue arg1Object: arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                        if (arg1Object instanceof InstanceObjectValue) {
                            result.getAllocatePoints().add(((InstanceObjectValue) arg1Object).getBaseClassAP());
                        } else if (arg1Object instanceof ClassObjectValue) {
                            // Return value of "type(CLASS)" is "class <'type'>, so add this type function object value
                            // to result.
                            result.getAllocatePoints().addAll(((ForwardAbstractValue) args.get(0)).getAllocatePoints());
                        }
                    }
                    return result;
                } else if (args.size() == 4) {
                    ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                    ForwardAbstractValue arg2 = (ForwardAbstractValue) args.get(2);
                    ForwardAbstractValue arg3 = (ForwardAbstractValue) args.get(3);

                    // Get the new class's name.
                    String newClassName = null;
                    if (arg1.getStringValue().isBottom() || arg1.getStringValue().isTop()) {
                        newClassName = "CREATE_IN_TYPE_FUNC";
                    } else {
                        newClassName = arg1.getStringValue().getConcreteValue();
                    }
                    String currentFileName = state.getSolver().getMethod().getDeclaringClass()
                            .getName().toString().split(".py")[0] + ".py";

                    // Get the new class's parent classes.
                    List<AllocatePoint> parentClassAPs = new ArrayList<>();
                    for (ObjectValue arg2Obj:
                            arg2.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                        if (arg2Obj instanceof TupleObjectValue) {
                            TupleObjectValue arg2Tuple = (TupleObjectValue) arg2Obj;
                            parentClassAPs.addAll(
                                    ((ForwardAbstractValue) arg2Tuple.getElement(
                                            0, state.getAllocatePointTable())).getAllocatePoints());
                        } else {
                            // Error
                        }
                    }
                    // TODO: 'ClassObjectValue' can inherit only one class, so you can only use tuple's
                    // first element class.
                    AllocatePoint parentClassAP = parentClassAPs.get(0);

                    // Create the new class object.
                    ClassObjectValue newClass = new ClassObjectValue(
                            new AllocatePoint(state.getCGNode(), inst),
                            TypeReference.findOrCreate(
                                    PythonTypes.pythonLoader, currentFileName + "/" + newClassName),
                            parentClassAP
                    );
                    state.getAllocatePointTable().newAllocation(newClass);

                    // Set the default values passed by argument.
                    for (ObjectValue arg3Obj:
                            arg3.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                        if (arg3Obj instanceof DictObjectValue) {
                            DictObjectValue arg3Dict = (DictObjectValue) arg3Obj;
                            for (String key: arg3Dict.getStringAccessElements().keySet()) {
                                if (key.equals("interfaces") || key.equals("values") || key.equals("items")) continue;

                                newClass.setAttr(
                                        key,
                                        arg3Dict.getElement(
                                                new ForwardAbstractValue(new StringValue(key)),
                                                state.getAllocatePointTable()));
                            }
                        } else {
                            // Error
                        }
                    }

                    return new ForwardAbstractValue(newClass);
                } else {
                    // Error
                    return new ForwardAbstractValue();
                }
            }
        });

        summaries.put("Lwala/builtin/zip", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ZipObjectValue zipObjectValue =
                        new ZipObjectValue(new AllocatePoint(state.getCGNode(), inst));
                state.getAllocatePointTable().newAllocation(zipObjectValue);

                AllocatePointTable callerAPTable = state.getAllocatePointTable();

                AllocatePoints arg1APs, arg2APs;
                if (args.size() >= 3) {
                    arg1APs = ((ForwardAbstractValue) args.get(1)).getAllocatePoints();
                    arg2APs = ((ForwardAbstractValue) args.get(2)).getAllocatePoints();
                } else if (args.size() == 2
                        && ((PythonInvokeInstruction) inst).getKeywords().stream()
                                .filter(k -> k.startsWith("pypsta_stared_arg")).count() > 0) {
                    // When stared arg.
                    arg1APs = new AllocatePoints();
                    arg2APs = new AllocatePoints();

                    for (ObjectValue arg1Object:
                            ((ForwardAbstractValue) args.get(1)).getAllocatePoints()
                                    .getObjectsIterable(callerAPTable)) {
                        if (arg1Object instanceof ComplexObjectValue) {
                            arg1APs.addAll(
                                    ((ForwardAbstractValue)
                                            (((ComplexObjectValue) arg1Object).getElement(
                                                    new ForwardAbstractValue(new IntValue(0)), callerAPTable)))
                                                            .getAllocatePoints());
                            arg2APs.addAll(
                                    ((ForwardAbstractValue)
                                            (((ComplexObjectValue) arg1Object).getElement(
                                                    new ForwardAbstractValue(new IntValue(1)), callerAPTable)))
                                                            .getAllocatePoints());
                        }
                    }
                } else {
                    return new ForwardAbstractValue();
                }

                for (ObjectValue objectValue: arg1APs.getObjectsIterable(callerAPTable)) {
                    if (objectValue instanceof ComplexObjectValue) {
                        zipObjectValue.addFirstArg((ComplexObjectValue) objectValue);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }

                for (ObjectValue objectValue: arg2APs.getObjectsIterable(callerAPTable)) {
                    if (objectValue instanceof ComplexObjectValue) {
                        zipObjectValue.addSecondArg((ComplexObjectValue) objectValue);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                }
                return new ForwardAbstractValue(zipObjectValue);
            }
        });

        summaries.put("Lobject", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue firstArg = (ForwardAbstractValue) args.get(0);
                // TODO: Filter that there is no value except object values.

                AllocatePoints allocatePoints = new AllocatePoints();
                for (AllocatePoint allocatePoint: firstArg.getAllocatePoints()) {
                    ObjectValue object = state.getAllocatePointTable().get(allocatePoint);
                    assert object instanceof ClassObjectValue;
                    assert object.getTypeReference().equals(PythonTypes.object);
                    ClassObjectValue objectClass = (ClassObjectValue) object;

                    InstanceObjectValue instance = new InstanceObjectValue(
                            new AllocatePoint(state.getCGNode(), inst), objectClass);
                    state.getAllocatePointTable().newAllocation(instance);

                    allocatePoints.add(instance.getAllocatePoint());
                }
                return new ForwardAbstractValue(allocatePoints);
            }
        });

        summaries.put("Largparse/_ActionsContainer/add_argument", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue firstArg = (ForwardAbstractValue) args.get(0);

                // Get the common object to save argument information.
                assert firstArg.getAllocatePoints().size() == 1;
                ObjectValue addArgumentFunc = firstArg.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();
                ForwardAbstractValue selfInstanceAbstractValue
                        = (ForwardAbstractValue) addArgumentFunc.getAttr("$self", state.getAllocatePointTable());
                assert selfInstanceAbstractValue.getAllocatePoints().size() == 1;
                ObjectValue selfInstance = selfInstanceAbstractValue.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();
                ForwardAbstractValue argumentsInfoObjAbstractValue
                        = (ForwardAbstractValue) selfInstance.getAttr("$arguments", state.getAllocatePointTable());
                assert argumentsInfoObjAbstractValue.getAllocatePoints().size() == 1;
                ObjectValue argumentsInfoObj = argumentsInfoObjAbstractValue.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();

                // Get the argument's name.
                int i = 1;
                while (i < args.size()) {
                    StringValue keyValue = ((ForwardAbstractValue) args.get(i)).getStringValue();
                    if (keyValue.isBottom() || keyValue.isTop())
                        Assertions.UNREACHABLE();
                    String key = keyValue.getConcreteValue();

                    while (key.startsWith("-")) {
                        key = key.substring(1);
                    }
                    if (key.contains("-"))
                        key = key.replace('-', '_');

                    ForwardAbstractValue val = new ForwardAbstractValue();

                    // Get the argument's type.
                    assert inst instanceof PythonInvokeInstruction;
                    int typeId = ((PythonInvokeInstruction) inst).getUse("type");
                    if (typeId != -1) {
                        ForwardAbstractValue typeVal = (ForwardAbstractValue) state.getValue(typeId);
                        assert typeVal.getAllocatePoints().size() == 1;
                        ObjectValue typeObj = typeVal.getAllocatePoints()
                                .getObjectsIterator(state.getAllocatePointTable())
                                .next();

                        TypeReference typeTypeRef = typeObj.getTypeReference();
                        if (typeTypeRef.getName().getClassName().toString().equals("int")) {
                            val.setIntValue(new IntValue(LatticeTop.TOP));
                        } else if (typeTypeRef.getName().getClassName().toString().equals("float")) {
                            val.setFloatValue(new FloatValue(LatticeTop.TOP));
                        } else if (typeTypeRef.getName().getClassName().toString().equals("str")) {
                            val.setStringValue(new StringValue(LatticeTop.TOP));
                        } else {
                            Assertions.UNREACHABLE();
                        }
                    } else {
                        val.setStringValue(new StringValue(LatticeTop.TOP));
                    }

                    // Get argument's 'nargs'
                    int nargsId = ((PythonInvokeInstruction) inst).getUse("nargs");
                    if (nargsId != -1) {
                        ForwardAbstractValue nargsVal = (ForwardAbstractValue) state.getValue(nargsId);
                        StringValue nargsStringVal = nargsVal.getStringValue();
                        if (nargsStringVal.isTop() || nargsStringVal.isBottom())
                            Assertions.UNREACHABLE();
                        String nargs = nargsStringVal.getConcreteValue();
                        if (nargs.equals("?")) {
                            val.setNoneValue(new NoneValue(LatticeTop.TOP));
                        } else {
                            Assertions.UNREACHABLE("Not implemented!");
                        }
                    }

                    // Set arguments information and default value.
                    argumentsInfoObj.setAttr(key, val);

                    i++;
                }
                return new ForwardAbstractValue();
            }
        });

        summaries.put("Largparse/_ActionsContainer/set_defaults", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue firstArg = (ForwardAbstractValue) args.get(0);

                // Get the 'self' instance.
                assert firstArg.getAllocatePoints().size() == 1;
                ObjectValue setDefaultsFunc = firstArg.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();
                ForwardAbstractValue selfAbstractValue
                        = (ForwardAbstractValue) setDefaultsFunc.getAttr("$self", state.getAllocatePointTable());
                assert selfAbstractValue.getAllocatePoints().size() == 1;
                ObjectValue selfObj = selfAbstractValue.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();

                // Get default function object passed by keyword argument.
                int defaultFuncId = ((PythonInvokeInstruction) inst).getUse("func");
                IForwardAbstractValue defaultFuncAbstractValue = state.getValue(defaultFuncId);
                selfObj.setAttr("$defaults", defaultFuncAbstractValue);
                return new ForwardAbstractValue();
            }
        });

        summaries.put("Largparse/ArgumentParser/add_subparsers", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                // The 'Largparse/ArgumentParser/add_subparsers' node.
                CGNode calleeNode = state.getSolver().getAnalyzer().getCGBuilder().getCallGraph().getPossibleTargets(
                        state.getCGNode(), ((PythonInvokeInstruction) inst).getCallSite()
                ).iterator().next();

                ForwardFixSolver calleeSolver = state.getSolver().getAnalyzer().getOrCreateSolver(calleeNode);
                ForwardState entryState = calleeSolver.getIn(calleeNode.getIR().getControlFlowGraph().entry());

                // Set function object itself
                entryState.setValue(1, args.get(0));
                entryState.getAllocatePointTable().takeInSingleValue(args.get(0), state.getAllocatePointTable());

                try {
                    calleeSolver.solve(null);
                } catch (CancelException e) {
                    e.printStackTrace();
                }

                ForwardState exitState = calleeSolver.getOut(calleeNode.getIR().getExitBlock());
                int returnVarId = Arrays.stream(calleeNode.getIR().getInstructions())
                        .filter(i -> i instanceof SSAReturnInstruction)
                        .map(i -> (SSAReturnInstruction) i)
                        .findAny()
                        .get()
                        .getResult();

                IForwardAbstractValue returnValue = exitState.getValue(returnVarId);
                state.getAllocatePointTable().takeInSingleValue(returnValue, exitState.getAllocatePointTable());
                return returnValue;
            }
        });

        summaries.put("Largparse/ArgumentParser/parse_args", new Summary() {
            private final Map<ObjectValue, Pair<Integer, Integer>> tmpVarMap = new HashMap<>();

            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue firstArg = (ForwardAbstractValue) args.get(0);

                PythonInvokeInstruction inst1 = (PythonInvokeInstruction) inst;
                ExplicitCallGraph cg = state.getSolver().getAnalyzer().getCGBuilder().getCallGraph();
                IClassHierarchy cha = state.getSolver().getAnalyzer().getClassHierarchy();
                CGNode parseargsCGNode = cg.getPossibleTargets(state.getCGNode(), inst1.getCallSite()).iterator().next();

                // Create result object.
                ObjectValue resultObj
                        = new InstanceObjectValue(
                                new AllocatePoint(state.getCGNode(), inst), ClassObjectValue.objectClass);
                state.getAllocatePointTable().newAllocation(resultObj);

                // Get the 'ArgumentParser' instance.
                assert firstArg.getAllocatePoints().size() == 1;
                ObjectValue parseArgsFunc = firstArg.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();
                ForwardAbstractValue argumentParserInstanceVal
                        = (ForwardAbstractValue) parseArgsFunc
                                .getAttr("$self", state.getAllocatePointTable());
                assert argumentParserInstanceVal.getAllocatePoints().size() == 1;
                ObjectValue argumentParserInstance = argumentParserInstanceVal.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();

                // Get the object keeping argument information.
                ForwardAbstractValue argumentsObjAbstractVal
                        = (ForwardAbstractValue) argumentParserInstance
                                .getAttr("$arguments", state.getAllocatePointTable());
                assert argumentsObjAbstractVal.getAllocatePoints().size() == 1;
                ObjectValue argumentsObj = argumentsObjAbstractVal.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();

                // Set the value to result object.
                for (Object arg: argumentsObj.getAttributes().keySet()) {
                    if (!resultObj.hasAttr((String) arg, state.getAllocatePointTable())) {
                        resultObj.setAttr((String) arg, new ForwardAbstractValue());
                    }
                    resultObj
                            .getAttr((String) arg, state.getAllocatePointTable())
                                    .union(argumentsObj.getAttr((String) arg, state.getAllocatePointTable()));
                }

                // TODO: Parse parent parser's default function.

                if (argumentParserInstance.hasAttr("$child_parsers", state.getAllocatePointTable())) {
                    Iterator<ObjectValue> childParserObjIter
                            = ((ForwardAbstractValue) argumentParserInstance.getAttr("$child_parsers", state.getAllocatePointTable()))
                                    .getAllocatePoints().getObjectsIterator(state.getAllocatePointTable());

                    while (childParserObjIter.hasNext()) {
                        ObjectValue childParserObj = childParserObjIter.next();
                        ForwardAbstractValue childArgumentsObjAbstractVal
                                = (ForwardAbstractValue) childParserObj
                                        .getAttr("$arguments", state.getAllocatePointTable());
                        assert childArgumentsObjAbstractVal.getAllocatePoints().size() == 1;
                        ObjectValue childArgumentsObj = childArgumentsObjAbstractVal.getAllocatePoints()
                                .getObjectsIterator(state.getAllocatePointTable()).next();

                        // Set the value to result object.
                        for (Object arg: childArgumentsObj.getAttributes().keySet()) {
                            if (!resultObj.hasAttr((String) arg, state.getAllocatePointTable())) {
                                resultObj.setAttr((String) arg, new ForwardAbstractValue());
                            }
                            resultObj
                                    .getAttr((String) arg, state.getAllocatePointTable())
                                            .union(childArgumentsObj.getAttr((String) arg, state.getAllocatePointTable()));
                        }

                        // Call default function.
                        if (childParserObj.hasAttr("$defaults", state.getAllocatePointTable())) {
                            ForwardAbstractValue defaultFuncAbstractValue
                                    = (ForwardAbstractValue) childParserObj
                                            .getAttr("$defaults", state.getAllocatePointTable());

                            int tmpFuncVarId, tmpArgVarId;
                            if (tmpVarMap.containsKey(childParserObj)) {
                                Pair<Integer, Integer> tmpVarPair = tmpVarMap.get(childParserObj);
                                tmpFuncVarId = tmpVarPair.fst;
                                tmpArgVarId = tmpVarPair.snd;
                            } else {
                                tmpFuncVarId = state.getCGNode().getIR().getSymbolTable().newSymbol();
                                tmpArgVarId = state.getCGNode().getIR().getSymbolTable().newSymbol();
                                tmpVarMap.put(childParserObj, Pair.make(tmpFuncVarId, tmpArgVarId));
                            }
                            state.setValue(tmpFuncVarId, defaultFuncAbstractValue);
                            state.setValue(tmpArgVarId, new ForwardAbstractValue(resultObj));

                            PythonInvokeInstruction dummyInst = new PythonInvokeInstruction(
                                    inst.iIndex(),
                                    -1,
                                    -1,
                                    new PythonSpecialMethodCallSiteReference(
                                            inst1.getDeclaredTarget(), inst.iIndex(), inst
                                    ),
                                    new int[]{tmpFuncVarId, tmpArgVarId},
                                    new Pair[0]
                            );

                            IForwardAbstractValue funcResult = ForwardCallManager.call(dummyInst, state);

                            for (CGNode defaultFuncCGNode:
                                    cg.getPossibleTargets(state.getCGNode(), inst1.getCallSite())) {
                                if (defaultFuncCGNode.equals(parseargsCGNode)) continue;

                                // Replace the cg edge, which is from "parse_args"'s caller node, to the cg edge from "parse_args".
                                cg.removeEdge(state.getCGNode(), defaultFuncCGNode);
                                ((ExplicitCallGraph.ExplicitNode) state.getCGNode()).removeTarget(defaultFuncCGNode);
                                cg.addEdge(parseargsCGNode, defaultFuncCGNode);
                                parseargsCGNode.addTarget(dummyInst.getCallSite(), defaultFuncCGNode);
                            }
                        }
                    }
                }

                // TODO: Parse subsub parser.

                return new ForwardAbstractValue(resultObj);
            }
        });

        summaries.put("Largparse/_SubParsersAction/add_parser", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                // The 'Largparse/_SubParsersAction/add_parser' node.
                CGNode calleeNode = state.getSolver().getAnalyzer().getCGBuilder().getCallGraph().getPossibleTargets(
                        state.getCGNode(), ((PythonInvokeInstruction) inst).getCallSite()
                ).iterator().next();

                ForwardFixSolver calleeSolver = state.getSolver().getAnalyzer().getOrCreateSolver(calleeNode);
                ForwardState entryState = calleeSolver.getIn(calleeNode.getIR().getControlFlowGraph().entry());

                // Set function object itself
                entryState.setValue(1, args.get(0));
                entryState.getAllocatePointTable().takeInSingleValue(args.get(0), state.getAllocatePointTable());

                try {
                    calleeSolver.solve(null);
                } catch (CancelException e) {
                    e.printStackTrace();
                }

                ForwardState exitState = calleeSolver.getOut(calleeNode.getIR().getExitBlock());
                int returnVarId = Arrays.stream(calleeNode.getIR().getInstructions())
                        .filter(i -> i instanceof SSAReturnInstruction)
                        .map(i -> (SSAReturnInstruction) i)
                        .findAny()
                        .get()
                        .getResult();

                IForwardAbstractValue returnValue = exitState.getValue(returnVarId);
                state.getAllocatePointTable().takeInSingleValue(returnValue, exitState.getAllocatePointTable());

                // Set alias to parent parser
                ObjectValue addParserMethodObj
                        = ((ForwardAbstractValue) args.get(0)).getAllocatePoints()
                                .getObjectsIterator(state.getAllocatePointTable()).next();
                ObjectValue subParsersActionContainerInstance
                        = ((ForwardAbstractValue) addParserMethodObj
                                .getAttr("$self", state.getAllocatePointTable()))
                                        .getAllocatePoints().getObjectsIterator(state.getAllocatePointTable()).next();
                ObjectValue parentParserInstance
                        = ((ForwardAbstractValue) subParsersActionContainerInstance
                                .getAttr("$parent_instance", state.getAllocatePointTable()))
                                        .getAllocatePoints().getObjectsIterator(state.getAllocatePointTable()).next();

                ObjectValue subParserInstance
                        = ((ForwardAbstractValue) returnValue)
                                .getAllocatePoints().getObjectsIterator(state.getAllocatePointTable()).next();
                if (!parentParserInstance.hasAttr("$child_parsers", state.getAllocatePointTable())) {
                    parentParserInstance.setAttr("$child_parsers", new ForwardAbstractValue());
                }
                parentParserInstance.getAttr("$child_parsers", state.getAllocatePointTable())
                        .union(new ForwardAbstractValue(subParserInstance));
                return returnValue;
            }
        });

        summaries.put("Lpyperf/Runner/parse_args", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                ForwardAbstractValue firstArg = (ForwardAbstractValue) args.get(0);

                // Get the common object to save argument information.
                assert firstArg.getAllocatePoints().size() == 1;
                ObjectValue parseArgsFunc = firstArg.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable())
                        .next();
                ForwardAbstractValue actionContainerInstanceVal = (ForwardAbstractValue) parseArgsFunc.getAttr("__ActionContainerInstance", state.getAllocatePointTable());
                assert actionContainerInstanceVal.getAllocatePoints().size() == 1;
                ObjectValue actionContainerInstance = actionContainerInstanceVal.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable())
                        .next();

                // Get the common object to save argument information.
                ForwardAbstractValue argumentsInfoObjAbstractVal = (ForwardAbstractValue) actionContainerInstance
                        .getAttr("$arguments", state.getAllocatePointTable());
                assert argumentsInfoObjAbstractVal.getAllocatePoints().size() == 1;
                ObjectValue argumentsInfoObj = argumentsInfoObjAbstractVal.getAllocatePoints()
                        .getObjectsIterator(state.getAllocatePointTable()).next();

                // Create result object.
                InstanceObjectValue resultObj = new InstanceObjectValue(new AllocatePoint(state.getCGNode(), inst), ClassObjectValue.objectClass);
                state.getAllocatePointTable().newAllocation(resultObj);
                for (Object arg: argumentsInfoObj.getAttributes().keySet()) {
                    resultObj.setAttr((String) arg, argumentsInfoObj.getAttr((String) arg, state.getAllocatePointTable()));
                }

                return new ForwardAbstractValue(resultObj);
            }
        });

        summaries.put("Lre/Pattern/split", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                return new ForwardAbstractValue();
            }
        });

        summaries.put("Lre/Match/groups", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                TupleObjectValue tuple = new TupleObjectValue(new AllocatePoint(state.getCGNode(), inst));
                state.getAllocatePointTable().newAllocation(tuple);
                tuple.setElement(
                        new ForwardAbstractValue(new IntValue(LatticeTop.TOP)),
                        new ForwardAbstractValue(new StringValue(LatticeTop.TOP))
                );
                return new ForwardAbstractValue(tuple);
            }
        });

        summaries.put("Lre/Match/groupdict", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                DictObjectValue dict = new DictObjectValue(new AllocatePoint(state.getCGNode(), inst), state.getAllocatePointTable());
                state.getAllocatePointTable().newAllocation(dict);
                dict.setElement(
                        new ForwardAbstractValue(new StringValue(LatticeTop.TOP)),
                        new ForwardAbstractValue(new StringValue(LatticeTop.TOP))
                );
                return new ForwardAbstractValue(dict);
            }
        });

        summaries.put("Lrandom/function/choice", new Summary() {
            @Override
            public IForwardAbstractValue call(ForwardState state, ArrayList<IForwardAbstractValue> args, SSAInstruction inst) {
                if (args.size() != 2) {
                    // Error
                    return new ForwardAbstractValue();
                }

                ForwardAbstractValue arg1 = (ForwardAbstractValue) args.get(1);
                ForwardAbstractValue abstractIndex = new ForwardAbstractValue(new IntValue(LatticeTop.TOP));
                AllocatePoints result = new AllocatePoints();
                for (ObjectValue arg1Obj:
                        arg1.getAllocatePoints().getObjectsIterable(state.getAllocatePointTable())) {
                    if (arg1Obj instanceof ComplexObjectValue) {
                        result.addAll(
                                ((ForwardAbstractValue)
                                        (((ComplexObjectValue) arg1Obj).getElement(
                                                abstractIndex, state.getAllocatePointTable()))).getAllocatePoints());
                    }
                }
                return new ForwardAbstractValue(result);
            }
        });
    }
}
