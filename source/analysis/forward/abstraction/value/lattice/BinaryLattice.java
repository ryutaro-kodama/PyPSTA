package analysis.forward.abstraction.value.lattice;

import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeBottom;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;

/**
 * This class is used to representing for abstract value which
 * has only top and bottom as elements.
 */
public abstract class BinaryLattice<U extends BinaryLattice> extends AbstractLattice<Void, U> implements ILattice<Void, U> {
    private boolean element;

    public BinaryLattice() {
        element = false;
    }

    public BinaryLattice(LatticeTop top) {
        this.element = true;
    }
    public BinaryLattice(LatticeBottom button) {
        this.element = false;
    }

    public BinaryLattice(boolean element) {
        this.element = element;
    }

    @Override
    public ILatticeElement getLatticeElement() {
        if (element) {
            return LatticeTop.TOP;
        } else {
            return LatticeBottom.BOTTOM;
        }
    }

    @Override
    public boolean union(U other) {
        boolean result = element ^ other.getElement();
        element = element || other.getElement();
        return result;
    }

    @Override
    public boolean isTop() {
        return element == true;
    }

    @Override
    public boolean isBottom() {
        return element == false;
    }

    public boolean getElement() {
        return element;
    }

    @Override
    public boolean isSame(U other) {
        return element == other.getElement();
    }
}
