package analysis.backward.thresher.pathinfo;

import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.debug.Assertions;
import edu.colorado.thresher.core.IStackFrame;

public class PypstaStackFrame extends IStackFrame {
    private final PythonInvokeInstruction callInst;

    public PypstaStackFrame(
            PythonInvokeInstruction callInst, CGNode cgNode, SSACFG.BasicBlock block, int lineNum) {
        super(null, cgNode, block, lineNum);
        this.callInst = callInst;
    }

    @Override
    public SSAInvokeInstruction getCallInstr() {
        Assertions.UNREACHABLE();
        return null;
    }

    public PythonInvokeInstruction getCallInst() {
        return callInst;
    }
}
