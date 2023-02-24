package analysis.backward.thresher.factor;

import java.util.Collection;
import java.util.Set;

public interface IProgramFactor extends Comparable {
    /**
     * When you skip basic block on def-use relation, whether to skip use basic block.
     * @return true if you can't skip use block
     */
    boolean needUseCheck();

    String getVariableName();

    Collection<? extends IProgramFactor> getFactors();

    int getDefBBId();

    Set<Integer> getUseBBIds();

    boolean noDefUseInfo();

    IProgramFactor replace(IProgramFactor toVar, IProgramFactor fromVar);
}
