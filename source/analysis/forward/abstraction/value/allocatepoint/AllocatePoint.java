package analysis.forward.abstraction.value.allocatepoint;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;

public class AllocatePoint {
    private final CGNode cgNode;
    private final SSAInstruction instruction;

    public AllocatePoint(CGNode cgNode, SSAInstruction instruction) {
        this.cgNode = cgNode;
        this.instruction = instruction;
    }

    public CGNode getCGNode() {
        return cgNode;
    }

    public SSAInstruction getInstruction() {
        return instruction;
    }

    public boolean match(AllocatePoint other) {
        return cgNode.equals(other.cgNode) && instruction.equals(other.instruction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllocatePoint that = (AllocatePoint) o;
        return cgNode.equals(that.cgNode) && instruction.equals(that.instruction);
    }

    @Override
    public int hashCode() {
        return cgNode.hashCode() + instruction.hashCode();
    }

    @Override
    public String toString() {
        return cgNode.getMethod().getReference().getDeclaringClass().getName().toString()
                + "@" + instruction.iIndex();
    }
}