package analysis.forward.abstraction;

import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.fixpoint.ForwardFixSolver;
import com.ibm.wala.fixpoint.AbstractVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;

import java.util.HashMap;
import java.util.Map;

public class ForwardState extends AbstractVariable<ForwardState>  {
    private final ForwardFixSolver solver;

    private ISSABasicBlock basicBlock;
    private boolean IN;

    private Environment currentEnv = new Environment();

    private AllocatePointTable allocatePointTable = new AllocatePointTable();

    public ForwardState(ForwardFixSolver solver, ISSABasicBlock basicBlock, boolean IN) {
        this.solver = solver;
        this.basicBlock = basicBlock;
        this.IN = IN;
    }

    public Environment getCurrentEnv() {
        return currentEnv;
    }

    public CGNode getCGNode() {
        return getSolver().getCGNode();
    }

    public ForwardFixSolver getSolver() {
        return solver;
    }

    public ISSABasicBlock getBasicBlock() {
        return basicBlock;
    }

    public AllocatePointTable getAllocatePointTable() {
        return allocatePointTable;
    }

    public IForwardAbstractValue getValue(int varNum) {
        if(currentEnv.hasValue(varNum)) {
            return currentEnv.getValue(varNum);
        } else {
            return new ForwardAbstractValue();
        }
    }

    public void setValue(int varNum, IForwardAbstractValue value) {
        currentEnv.setValue(varNum, value);
    }

    public boolean updateValue(int varId, IForwardAbstractValue value) {
        IForwardAbstractValue oldValue = getValue(varId);
        boolean changed = oldValue.union(value);
        setValue(varId, oldValue);
        return changed;
    }

    /**
     * Change this state to the result of union with other state
     * @param other
     * @return whether this state has changed (true when changed is occurred)
     */
    public boolean union(ForwardState other) {
        return currentEnv.union(other.getCurrentEnv())
                | allocatePointTable.union(other.allocatePointTable);
    }

    @Override
    public void copyState(ForwardState v) {
        allocatePointTable.overrideTable(v.allocatePointTable);
        currentEnv.overrideMapping(v.currentEnv);
    }

    @Override
    public String toString() {
        if (IN)
            return "State@BB" + basicBlock.getGraphNodeId() + "-IN";
        else
            return "State@BB" + basicBlock.getGraphNodeId() + "-OUT";
    }


    /**
     * Hold the mapping from variable id to its abstract value.
     */
    public class Environment {
        private HashMap<Integer, IForwardAbstractValue> values = new HashMap<>();

        public boolean hasValue(int varNum) {
            return values.containsKey(varNum);
        }

        public IForwardAbstractValue getValue(int varNum) {
            return values.get(varNum);
        }

        public void setValue(int varNum, IForwardAbstractValue value) {
            values.put(varNum, value);
        }

        /**
         * Calculate union with abstract value in other environment.
         * @param other the other environment
         * @return whether this environment has changed
         */
        public boolean union(Environment other) {
            boolean changedFlag = false;

            for (Map.Entry<Integer, IForwardAbstractValue> id2value : other.values.entrySet()) {
                int varNum = id2value.getKey();
                IForwardAbstractValue otherValue = id2value.getValue();
                if (hasValue(varNum)) {
                    changedFlag = getValue(varNum).union(otherValue) || changedFlag;
                    // There is no need of copying 'otherValue' because the 'union' of 'IForwardAbstractValueElement'
                    // is implemented not to affect other value.
                } else {
                    setValue(varNum, otherValue.copy());
                    changedFlag = true;
                }
            }

            return changedFlag;
        }

        /**
         * Delete all mapping and make this mapping the same to the other environment with copying abstract value.
         * @param other the other environment
         */
        public void overrideMapping(Environment other) {
            values.clear();

            for (Map.Entry<Integer, IForwardAbstractValue> id2value: other.values.entrySet()) {
                setValue(id2value.getKey(), id2value.getValue().copy());
            }
        }
    }
}
