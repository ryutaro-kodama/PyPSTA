package analysis.forward.fixpoint;

import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import analysis.forward.ConstantConverter;

public class ForwardMeetOperator extends AbstractMeetOperator<ForwardState> {
    private static final ForwardMeetOperator SINGLETON = new ForwardMeetOperator();

    public static ForwardMeetOperator instance() {
        return SINGLETON;
    }

    private ForwardMeetOperator() {}

    /**
     * 右辺に対して、左辺の値が変化したか否かを定義
     * @param lhs
     * @param rhs
     * @return
     */
    @Override
    public byte evaluate(ForwardState lhs, ForwardState[] rhs) throws IllegalArgumentException {
        if (lhs == null) {
            throw new IllegalArgumentException("lhs is null");
        }
        if (rhs == null) {
            throw new IllegalArgumentException("rhs is null");
        }

        ForwardState result = new ForwardState(
                lhs.getSolver(), lhs.getBasicBlock(), false
        );

        // Union with only normal predecessors.
        SSACFG cfg = lhs.getSolver().getCFG();
        for (ISSABasicBlock predBB: cfg.getNormalPredecessors(lhs.getBasicBlock())) {
            for (ForwardState forwardState: rhs) {
                if (forwardState.getBasicBlock().getGraphNodeId() == predBB.getGraphNodeId()) {
                    result.union(forwardState);
                }
            }
        }

        for (SSAPhiInstruction phiInst: Iterator2Iterable.make(lhs.getBasicBlock().iteratePhis())) {
            IForwardAbstractValue resultAbstractValue = ConstantConverter.convert(null);
            for (int i = 0; i < phiInst.getNumberOfUses(); i++) {
                resultAbstractValue.union(rhs[i].getValue(phiInst.getUse(i)));
            }
            result.setValue(phiInst.getDef(), resultAbstractValue);
        }

        boolean hasChanged = lhs.union(result);
        if (hasChanged) {
            return CHANGED;
        } else {
            return NOT_CHANGED;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ForwardMeetOperator);
    }

    @Override
    public String toString() {
        return "MEET";
    }
}
