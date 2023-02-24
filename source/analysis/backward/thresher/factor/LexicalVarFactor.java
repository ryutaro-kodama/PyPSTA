package analysis.backward.thresher.factor;

import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.ipa.callgraph.CGNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LexicalVarFactor implements IProgramFactor {
    private final CGNode node;
    private final AstLexicalAccess.Access access;
    private final int ssaId;

    public LexicalVarFactor(CGNode node, AstLexicalAccess.Access access, int ssaId) {
        this.node = node;
        this.access = access;
        this.ssaId = ssaId;
    }

    public CGNode getNode() {
        return node;
    }

    public AstLexicalAccess.Access getAccess() {
        return access;
    }

    public int getSSAId() {
        return ssaId;
    }

    @Override
    public boolean needUseCheck() {
        return true;
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
        return -1;
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return Collections.emptySet();
    }

    @Override
    public boolean noDefUseInfo() {
        // There is no def-use information on lexical read/write. But you have to visit basic block
        // when there is a 'AstLexicalWriteInstruction', so conversely if there is no
        // 'AstLexicalWriteInstruction' in basic block, you can skip it.
        return false;
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
            return access.toString().compareTo(((LexicalVarFactor) o).access.toString());
        else if (o instanceof IProgramFactor)
            return toString().compareTo(o.toString());
        else
            return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LexicalVarFactor that = (LexicalVarFactor) o;
        return ssaId == that.ssaId
                && access.variableName.equals(that.access.variableName)
                && access.variableDefiner.equals(that.access.variableDefiner);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return access.variableName + '@' + access.variableDefiner + '-' + ssaId;
    }
}
