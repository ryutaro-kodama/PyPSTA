package analysis.backward.thresher.factor;

import com.ibm.wala.types.FieldReference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GlobalVarFactor implements IProgramFactor {
    private final FieldReference fieldRef;

    public GlobalVarFactor(FieldReference fieldRef) {
        this.fieldRef = fieldRef;
    }

    public FieldReference getFieldRef() {
        return fieldRef;
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
        return -1;
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return Collections.emptySet();
    }

    @Override
    public boolean noDefUseInfo() {
        // There is no def-use information on lexical read/write. But you have to visit basic block
        // when there is a 'AstGlobalWriteInstruction', so conversely if there is no
        // 'AstGlobalWriteInstruction' in basic block, you can skip it.
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
            return fieldRef.toString().compareTo(((GlobalVarFactor) o).fieldRef.toString());
        else if (o instanceof IProgramFactor)
            return toString().compareTo(o.toString());
        else
            return 1;
    }

    @Override
    public String toString() {
        return fieldRef.getName().toString();
    }
}
