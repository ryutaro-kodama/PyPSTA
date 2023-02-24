package analysis.backward.thresher.factor;

import com.ibm.wala.ipa.callgraph.CGNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ReturnVarFactor implements IProgramFactor {
    private final CGNode node;

    public ReturnVarFactor(CGNode node) {
        this.node = node;
    }

    public CGNode getNode() {
        return node;
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
    public boolean needUseCheck() {
        return false;
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
        if (getClass() == o.getClass())
            return node.toString().compareTo(((ReturnVarFactor) o).node.toString());
        else if (o instanceof IProgramFactor)
            return toString().compareTo(o.toString());
        else
            return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnVarFactor that = (ReturnVarFactor) o;
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }

    @Override
    public String toString() {
        return "return(" + node.toString() + ')';
    }
}
