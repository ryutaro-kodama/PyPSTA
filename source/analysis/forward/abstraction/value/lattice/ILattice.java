package analysis.forward.abstraction.value.lattice;

import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;

/**
 * Interface of lattice.
 * @param <T> the type of concrete value
 * @param <U> the type of lattice structure
 */
public interface ILattice<T, U extends ILattice> {
    ILatticeElement getLatticeElement();

    boolean union(U other);

    boolean isTop();

    boolean isBottom();

    /**
     * Check whether the value is the same to the other.
     * @param other other lattice
     * @return whether the value is the same
     */
    boolean isSame(U other);
}
