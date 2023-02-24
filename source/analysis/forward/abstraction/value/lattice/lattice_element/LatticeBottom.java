package analysis.forward.abstraction.value.lattice.lattice_element;

public class LatticeBottom implements ILatticeElement<Void> {
    public static final LatticeBottom BOTTOM = new LatticeBottom();

    private LatticeBottom() {}

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public String toString() {
        return "-";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
