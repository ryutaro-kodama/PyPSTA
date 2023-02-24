package analysis.forward.abstraction.value.lattice;

import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeElement;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class is used to representing for abstract value which has top, bottom and elements. The elements are
 * connected only to top and bottom.
 */
public abstract class ThreeHeightLattice<T, U extends ThreeHeightLattice> extends AbstractLattice<T, U> implements ILattice<T, U> {
    public ThreeHeightLattice() {
        super();
    }

    public ThreeHeightLattice(ILatticeElement<T> latticeElement) {
        super(latticeElement);
    }

    public ThreeHeightLattice(T value) {
        this(new LatticeElement<>(value));
    }

    @Override
    public boolean union(U other) {
        if (isTop()) {
            return false;
        } else if (other.isTop()) {
            element = other.getLatticeElement();
            return true;
        } else if (isBottom()) {
            if (other.isBottom()) {
                return false;
            } else {
                element = other.getLatticeElement();
                return true;
            }
        } else {
            if (other.isBottom()) {
                return false;
            } else {
                if (getConcreteValue().equals(other.getConcreteValue())) {
                    return false;
                } else {
                    element = LatticeTop.TOP;
                    return true;
                }
            }
        }
    }

    public T getConcreteValue() {
        return (T) element.getValue();
    }
}
