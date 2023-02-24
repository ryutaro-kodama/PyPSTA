package analysis.backward.thresher.factor;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.HashSetFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class VariableFactor implements IProgramFactor {
    private final CGNode node;
    private final int varId;

    private final int defBBId;
    private final HashSet<Integer> useBBIds;

    public VariableFactor(CGNode node, int varId) {
        this.node = node;
        this.varId = varId;

        IR ir = node.getIR();
        assert ir != null;
        DefUse du = node.getDU();
        assert du != null;

        if (varId < 1) {
            defBBId = -1;
            useBBIds = null;
        } else {
            SSAInstruction defInst = du.getDef(varId);
            if (defInst != null) {
                defBBId = ir.getBasicBlockForInstruction(defInst).getGraphNodeId();
            } else {
                defBBId = -1;

            }

            useBBIds = HashSetFactory.make();
            du.getUses(varId).forEachRemaining(
                    i -> useBBIds.add(ir.getBasicBlockForInstruction(i).getGraphNodeId()));
        }
    }

    public CGNode getNode() {
        return node;
    }

    public int getVariableId() {
        return varId;
    }

    @Override
    public boolean needUseCheck() {
        return false;
    }

    @Override
    public String getVariableName() {
        return toString();
    }

    @Override
    public Collection<? extends IProgramFactor> getFactors() {
        Collection<IProgramFactor> result = new HashSet<>();
        result.add(this);
        return result;
    }

    @Override
    public int getDefBBId() {
        return defBBId;
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return useBBIds;
    }

    @Override
    public boolean noDefUseInfo() {
        return defBBId == -1;
    }

    @Override
    public IProgramFactor replace(IProgramFactor toVar, IProgramFactor fromVar) {
        if (this.equals(fromVar)) {
            return toVar;
        } else {
            return this;
        }
    }

    @Override
    public int compareTo(Object other) {
        if (getClass() == other.getClass())
            return Integer.compare(varId, ((VariableFactor) other).varId);
        else if (other instanceof IProgramFactor)
            return toString().compareTo(other.toString());
        else
            return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableFactor that = (VariableFactor) o;
        return varId == that.varId && Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, varId);
    }

    @Override
    public String toString() {
        return Integer.toString(varId) + '(' + node.toString() + ')';
    }
}
