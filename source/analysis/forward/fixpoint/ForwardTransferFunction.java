package analysis.forward.fixpoint;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.debug.Assertions;

public class ForwardTransferFunction implements ITransferFunctionProvider<ISSABasicBlock, ForwardState> {
    @Override
    public UnaryOperator<ForwardState> getNodeTransferFunction(ISSABasicBlock node) {
        return new ForwardNodeTransfer(node);
    }

    @Override
    public boolean hasNodeTransferFunctions() {
        return true;
    }

    @Override
    public UnaryOperator<ForwardState> getEdgeTransferFunction(ISSABasicBlock src, ISSABasicBlock dst) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public boolean hasEdgeTransferFunctions() {
        return false;
    }

    @Override
    public AbstractMeetOperator getMeetOperator() {
        return ForwardMeetOperator.instance();
    }
}
