package analysis.backward.thresher.factor;

import com.ibm.wala.ipa.callgraph.CGNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EachElementGetVarFactor implements IProgramFactor {
    private final CGNode node;
    private final int objectVarId;
    private final int resultVarId;

    public EachElementGetVarFactor(CGNode node, int objectVarId, int resultVarId) {
        this.node = node;
        this.objectVarId = objectVarId;
        this.resultVarId = resultVarId;
    }

    @Override
    public boolean needUseCheck() {
        return true;
    }

    @Override
    public String getVariableName() {
        return null;
    }

    @Override
    public Collection<? extends IProgramFactor> getFactors() {
        Collection<IProgramFactor> result = new HashSet<>();
        result.add(this);
        return result;
    }

    @Override
    public int getDefBBId() {
        return -1;
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return null;
    }

    @Override
    public boolean noDefUseInfo() {
        return true;
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
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EachElementGetVarFactor that = (EachElementGetVarFactor) o;
        return node.equals(that.node) && resultVarId == that.resultVarId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, resultVarId);
    }

    @Override
    public String toString() {
        return "properties of " + objectVarId;
    }
}
