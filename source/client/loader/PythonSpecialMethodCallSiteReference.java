package client.loader;

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class PythonSpecialMethodCallSiteReference extends DynamicCallSiteReference {
    private final SSAInstruction inst;

    public PythonSpecialMethodCallSiteReference(MethodReference ref, int pc, SSAInstruction inst) {
        super(ref, pc);
        this.inst = inst;
    }

    public PythonSpecialMethodCallSiteReference(TypeReference ref, int pc, SSAInstruction inst) {
        this(AstMethodReference.fnReference(ref), pc, inst);
    }

    public int getNumberOfTotalParameters() {
        return inst.getNumberOfUses();
    }

    public int getNumberOfPositionalParameters() {
        return inst.getNumberOfUses();
    }
}
