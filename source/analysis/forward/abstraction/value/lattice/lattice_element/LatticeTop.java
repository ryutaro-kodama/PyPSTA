package analysis.forward.abstraction.value.lattice.lattice_element;

public class LatticeTop implements ILatticeElement<Void> {
    public static final LatticeTop TOP = new LatticeTop();

    private LatticeTop() {}

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "T";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
