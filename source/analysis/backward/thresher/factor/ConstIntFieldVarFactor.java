package analysis.backward.thresher.factor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class ConstIntFieldVarFactor extends FieldVarFactor {
    private final int index;

    public ConstIntFieldVarFactor(IProgramFactor val, int index) {
        super(val);
        this.index = index;
    }

    @Override
    public String getVariableName() {
        return getValFactor().toString() + ".'" + index + "'";
    }

    @Override
    public Collection<? extends IProgramFactor> getFactors() {
        Collection<IProgramFactor> result = new HashSet<>();
        result.addAll(getValFactor().getFactors());
        result.add(this);
        return result;
    }

    @Override
    public IProgramFactor replace(IProgramFactor toVar, IProgramFactor fromVar) {
        if (getValFactor().equals(fromVar)) {
            return new ConstIntFieldVarFactor(toVar, index);
        } else {
            // If not replace the 'val', delegate the replacing to 'val'
            return new ConstIntFieldVarFactor(getValFactor().replace(toVar, fromVar), index);
        }
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() != o.getClass())
            return 1;
        else if (getValFactor().equals(((ConstIntFieldVarFactor) o).getValFactor()))
            return Integer.compare(index, ((ConstIntFieldVarFactor) o).index);
        else
            return getValFactor().compareTo(((ConstIntFieldVarFactor) o).getValFactor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstIntFieldVarFactor that = (ConstIntFieldVarFactor) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), index);
    }

    @Override
    public String toString() {
        return '[' + getValFactor().toString() + "].'" + index + "'";
    }
}
