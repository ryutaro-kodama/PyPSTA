package analysis.forward;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.fixpoint.ForwardFixSolver;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalCollector {
    private static Map<FieldReference, GlobalCollection> collections = new HashMap<>();

    public static void put(AstGlobalWrite inst,
                           ForwardState writeState,
                           IForwardAbstractValue newGlobalValue) {
        FieldReference reference = inst.getDeclaredField();

        GlobalCollection globalCollection;
        if (collections.containsKey(reference)) {
            globalCollection = collections.get(reference);
        } else {
            globalCollection = new GlobalCollection(reference);
            collections.put(reference, globalCollection);
        }
        globalCollection.write(inst.getVal(), writeState);

        // Propagate updating.
        AllocatePointTable writeStateAPTable = writeState.getAllocatePointTable();
        for (Pair<AstGlobalRead, ForwardFixSolver> pair: globalCollection.readingNodes) {
            AstGlobalRead globalReadInst = pair.fst;
            ForwardFixSolver globalReadSolver = pair.snd;

            ISSABasicBlock globalReadBB =
                    globalReadSolver.getCGNode().getIR().getBasicBlockForInstruction(globalReadInst);
            ForwardState globalReadState = globalReadSolver.getIn(globalReadBB);

            // Compare the value of global variable with the previous value. If they are different,
            // add to the work list.
            IForwardAbstractValue oldValue = globalReadState.getValue(globalReadInst.getDef());
            if (!oldValue.isSame(newGlobalValue)) {
                globalReadState.setValue(globalReadInst.getDef(), newGlobalValue);
                globalReadState.getAllocatePointTable().takeInSingleValue(
                        newGlobalValue, writeStateAPTable
                );
                globalReadState.getSolver().changedVariable(globalReadState);
            }
        }
    }

    public static IForwardAbstractValue get(AstGlobalRead inst, ForwardState readState) {
        FieldReference reference = inst.getDeclaredField();
        IForwardAbstractValue returnValue = new ForwardAbstractValue();

        GlobalCollection globalCollection;
        if (collections.containsKey(reference)) {
            globalCollection = collections.get(reference);
            AllocatePointTable readStateAPTable = readState.getAllocatePointTable();
            returnValue = globalCollection.getValue(readStateAPTable);
        } else {
            globalCollection = new GlobalCollection(reference);
            collections.put(reference, globalCollection);
        }

        return returnValue;
    }

    /**
     * Get abstract value used as default value in function definition.
     * @param defaultVarName the variable name of default value
     * @param callerState the caller state
     * @return the default value
     */
    public static IForwardAbstractValue get(String defaultVarName, ForwardState callerState) {
        List<GlobalCollection> targetCollection = collections.values().stream().parallel()
                .filter(i -> i.fieldRef.getName().toString().equals(defaultVarName))
                .collect(Collectors.toList());
        if (targetCollection.size() == 0) {
            return new ForwardAbstractValue();
        } else if (targetCollection.size() == 1) {
            return targetCollection.get(0).getValue(callerState.getAllocatePointTable());
        } else {
            Assertions.UNREACHABLE(); return null;
        }
    }

    public static IForwardAbstractValue get(TypeReference classType, ForwardState forwardState) {
        List<IForwardAbstractValue> values = collections.values().stream().parallel()
                        .filter(c -> c.fieldRef.getName().toString().replace("global ", "")
                                .equals(classType.getName().toString().substring(1)))
                        .map(c -> c.getValue(forwardState.getAllocatePointTable()))
                        .collect(Collectors.toList());
        assert values.size() == 1;
        return values.get(0);
    }

    public static void update(String name, Integer varId, ForwardState exitState) {
        List<GlobalCollection> globalCollections = collections.entrySet().stream().parallel()
                .filter(m -> m.getKey().getName().toString().equals(name))
                .map(m -> m.getValue())
                .collect(Collectors.toList());
        assert globalCollections.size() <= 1;
        if (globalCollections.size() == 1) {
            globalCollections.get(0).write(varId, exitState);
        }
    }

    public static void reset() {
        collections.clear();
    }

    public static class GlobalCollection {
        private final FieldReference fieldRef;

        /** Collect nodes which this field reference is used on reading */
        private final Set<Pair<AstGlobalRead, ForwardFixSolver>> readingNodes = new HashSet<>();

        /** The abstract value of global variable */
        private IForwardAbstractValue writtenValue = new ForwardAbstractValue();

        /** The allocate point table of abstract global value */
        private AllocatePointTable table = new AllocatePointTable();

        public GlobalCollection(FieldReference fieldRef) {
            this.fieldRef = fieldRef;
        }

        public void write(int value, ForwardState writeState) {
            IForwardAbstractValue writtenValue = writeState.getValue(value);
            this.writtenValue.union(writtenValue);

            AllocatePointTable writeStateTable = writeState.getAllocatePointTable();
            table.unionSingleValue(writtenValue, writeStateTable);
        }

        /**
         * Get the global abstract value.
         *
         * @param readStateAPTable allocate point table which are updated
         * @return the global abstract value
         */
        public IForwardAbstractValue getValue(AllocatePointTable readStateAPTable) {
            IForwardAbstractValue result = writtenValue.copy();
            readStateAPTable.union(table);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GlobalCollection that = (GlobalCollection) o;
            return Objects.equals(fieldRef, that.fieldRef)
                    && Objects.equals(readingNodes, that.readingNodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldRef);
        }
    }
}
