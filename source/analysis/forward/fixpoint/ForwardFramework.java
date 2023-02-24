package analysis.forward.fixpoint;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.dataflow.graph.BasicFramework;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.graph.Graph;

public class ForwardFramework extends BasicFramework<ISSABasicBlock, ForwardState> {
    public ForwardFramework(
            Graph<ISSABasicBlock> flowGraph, ForwardTransferFunction transferFunctionProvider) {
        super(flowGraph, transferFunctionProvider);
    }
}
