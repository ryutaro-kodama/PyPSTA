package analysis.forward.abstraction.value.lattice.lattice_element;

import java.util.Objects;

public class LatticeElement<T> implements ILatticeElement<T> {
    T value;

    public LatticeElement(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatticeElement<?> that = (LatticeElement<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
