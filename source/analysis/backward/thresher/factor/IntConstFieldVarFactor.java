package analysis.backward.thresher.factor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class IntConstFieldVarFactor extends FieldVarFactor {
    private final int field;

    public IntConstFieldVarFactor(IProgramFactor val, int field) {
        super(val);
        this.field = field;
    }

    @Override
    public String getVariableName() {
        return getValFactor().toString() + '.' + field;
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
            return new IntConstFieldVarFactor(toVar, field);
        } else {
            // If not replace the 'val', delegate the replacing to 'val'
            return new IntConstFieldVarFactor(getValFactor().replace(toVar, fromVar), field);
        }
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() != o.getClass())
            return 1;
        else if (getValFactor().equals(((IntConstFieldVarFactor) o).getValFactor()))
            return Integer.compare(field, ((IntConstFieldVarFactor) o).field);
        else
            return getValFactor().compareTo(((IntConstFieldVarFactor) o).getValFactor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IntConstFieldVarFactor that = (IntConstFieldVarFactor) o;
        return field == that.field;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field);
    }

    @Override
    public String toString() {
        return '[' + getValFactor().toString() + "]." + field;
    }
}
