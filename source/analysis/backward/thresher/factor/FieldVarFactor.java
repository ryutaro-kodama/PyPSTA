package analysis.backward.thresher.factor;

import java.util.Objects;
import java.util.Set;

public abstract class FieldVarFactor implements IProgramFactor {
    // The 'val' means 'val.attr'.
    private final IProgramFactor val;

    public FieldVarFactor(IProgramFactor val) {
        this.val = val;
    }

    public IProgramFactor getValFactor() {
        return val;
    }

    @Override
    public boolean needUseCheck() {
        return true;
    }

    @Override
    public int getDefBBId() {
        return val.getDefBBId();
    }

    @Override
    public Set<Integer> getUseBBIds() {
        return val.getUseBBIds();
    }

    @Override
    public boolean noDefUseInfo() {
        return val.noDefUseInfo();
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() != o.getClass())
            return 1;
        else
            return val.compareTo(((FieldVarFactor) o).val);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldVarFactor that = (FieldVarFactor) o;
        return val.equals(that.val);
    }

    @Override
    public int hashCode() {
        return Objects.hash(val);
    }
}
