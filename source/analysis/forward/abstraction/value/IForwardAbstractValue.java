package analysis.forward.abstraction.value;

import state.IAbstractValue;

public interface IForwardAbstractValue<T extends IForwardAbstractValue> extends IAbstractValue<T> {
    /**
     * Create the copy of this forward abstract value.
     * @return the copy of this
     */
    T copy();

    boolean isBottom();
}
