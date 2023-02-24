package analysis.backward.thresher.constraint;

import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.term.ITerm;
import com.ibm.wala.util.collections.HashSetFactory;
import edu.colorado.thresher.core.Options;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractConstraint implements IConstraint {
    public static boolean DEBUG = Options.DEBUG;

    public static Set<IConstraint> TRUE = HashSetFactory.make();
    public static Set<IConstraint> FALSE = HashSetFactory.make();

    protected final ITerm lhs, rhs;
    protected final Set<IProgramFactor> vars;

    private static int idCounter = 0;
    protected final int id; // unique constraint id that persists across

    private final int hash;

    protected boolean substituted = false;

    public AbstractConstraint() {
        this.lhs = null;
        this.rhs = null;
        this.vars = null;
        this.id = idCounter++;
        this.hash = makeHash();
    }

    protected AbstractConstraint(ITerm lhs, ITerm rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.vars = HashSetFactory.make();
        vars.addAll(lhs.getVars());
        vars.addAll(rhs.getVars());
        this.id = idCounter++;
        this.hash = makeHash();
    }

    protected AbstractConstraint(ITerm lhs, ITerm rhs, int id) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.vars = HashSetFactory.make();
        vars.addAll(lhs.getVars());
        vars.addAll(rhs.getVars());
        this.id = id;
        this.hash = makeHash();
    }

    @Override
    public ITerm getLhs() {
        return lhs;
    }

    @Override
    public ITerm getRhs() {
        return rhs;
    }

    @Override
    public Collection<? extends IProgramFactor> getVars() {
        return vars;
    }

    @Override
    public void setSubstituted(boolean b) {
        substituted = b;
    }

    @Override
    public boolean isSubstituted() {
        return substituted;
    }

    protected Set<IConstraint> makeSet(IConstraint... constraints) {
        HashSet<IConstraint> result = HashSetFactory.make();
        for (IConstraint constraint: constraints) {
            result.add(constraint);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractConstraint that = (AbstractConstraint) o;
        return hash == that.hash;
    }

    private int makeHash() {
        return toString().hashCode();
    }
}
