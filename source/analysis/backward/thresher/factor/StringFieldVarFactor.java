package analysis.backward.thresher.factor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class StringFieldVarFactor extends FieldVarFactor {
    private final String fieldName;

    public StringFieldVarFactor(IProgramFactor val, String fieldName) {
        super(val);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getVariableName() {
        return getValFactor().toString() + '.' + fieldName;
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
        if (this.equals(fromVar)) {
            return toVar;
        } else if (getValFactor().equals(fromVar)) {
            return new StringFieldVarFactor(toVar, fieldName);
        } else {
            // If not replace the 'val', delegate the replacing to 'val'
            return new StringFieldVarFactor(getValFactor().replace(toVar, fromVar), fieldName);
        }
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() != o.getClass())
            return 1;
        else if (getValFactor().equals(((StringFieldVarFactor) o).getValFactor()))
            return fieldName.compareTo(((StringFieldVarFactor) o).fieldName);
        else
            return getValFactor().compareTo(((StringFieldVarFactor) o).getValFactor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StringFieldVarFactor that = (StringFieldVarFactor) o;
        return fieldName.equals(that.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fieldName);
    }

    @Override
    public String toString() {
        return '[' + getValFactor().toString() + "]." + fieldName;
    }
}
