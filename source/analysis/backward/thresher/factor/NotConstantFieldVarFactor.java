package analysis.backward.thresher.factor;

import com.ibm.wala.types.TypeReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NotConstantFieldVarFactor extends FieldVarFactor {
    // private final int fieldId;
    private final IProgramFactor fieldFactor;

    private final Set<TypeReference> elementTypes = new HashSet<>();

    public NotConstantFieldVarFactor(IProgramFactor val, IProgramFactor field) {
        super(val);
        this.fieldFactor = field;
    }

    // public int getVariableId() {
    //     return getValFactor().getVariableId();
    // }

    // public int getFieldId() {
    //     return fieldId;
    // }

    public IProgramFactor getFieldFactor() {
        return fieldFactor;
    }

    public void addElementType(TypeReference type) {
        elementTypes.add(type);
    }

    public Set<TypeReference> getElementTypes() {
        return elementTypes;
    }

    @Override
    public String getVariableName() {
        return getValFactor().toString() + '.' + fieldFactor.toString();
    }

    @Override
    public Collection<? extends IProgramFactor> getFactors() {
        Collection<IProgramFactor> result = new HashSet<>();
        result.addAll(getValFactor().getFactors());
        result.addAll(fieldFactor.getFactors());
        result.add(this);
        return result;
    }

    @Override
    public IProgramFactor replace(IProgramFactor toVar, IProgramFactor fromVar) {
        if (getValFactor().equals(fromVar)) {
            return new NotConstantFieldVarFactor(toVar, fieldFactor);
        } else {
            // If not replace the 'val', delegate the replacing to 'val'
            IProgramFactor newVal = getValFactor().replace(toVar, fromVar);
            IProgramFactor newField = fieldFactor.replace(toVar, fromVar);
            if (newVal.equals(getValFactor()) && newField.equals(fieldFactor)) {
                return this;
            } else {
                return new NotConstantFieldVarFactor(newVal, newField);
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        if (getClass() != o.getClass())
            return 1;
        else if (getValFactor().equals(((NotConstantFieldVarFactor) o).getValFactor()))
            return fieldFactor.compareTo(((NotConstantFieldVarFactor) o).fieldFactor);
        else
            return getValFactor().compareTo(((NotConstantFieldVarFactor) o).getValFactor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NotConstantFieldVarFactor that = (NotConstantFieldVarFactor) o;
        return fieldFactor.equals(that.fieldFactor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fieldFactor);
    }

    @Override
    public String toString() {
        return '[' + getValFactor().toString() + "]." + fieldFactor;
    }
}
