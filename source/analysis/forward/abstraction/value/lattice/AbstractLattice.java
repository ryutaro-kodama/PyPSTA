package analysis.forward.abstraction.value.lattice;

import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;

import java.util.Objects;

public abstract class AbstractLattice<T, U extends ILattice> implements ILattice<T, U> {
    protected ILatticeElement element;

    public AbstractLattice() {
        this.element = LatticeBottom.BOTTOM;
    }

    public AbstractLattice(ILatticeElement element) {
        this.element = element;
    }

    @Override
    public ILatticeElement getLatticeElement() {
        return element;
    }

    @Override
    public boolean isTop() {
        return element instanceof LatticeTop;
    }

    @Override
    public boolean isBottom() {
        return element instanceof LatticeBottom;
    }

    @Override
    public String toString() {
        return getLatticeElement().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractLattice<?, ?> that = (AbstractLattice<?, ?>) o;
        return Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element);
    }
}
