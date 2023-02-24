package analysis.forward.abstraction.value.element;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.types.TypeReference;

import java.util.Set;

public interface IForwardAbstractValueElement<T extends IForwardAbstractValueElement> {
    Set<TypeReference> getTypes(ForwardState state);

    /**
     * Union with the other instance. After this, even if this value will change,
     * don't be effect to the other value.
     * @param other
     * @return whether this value has changed
     */
    boolean union(T other);

    boolean isBottom();

    T copy();

    /**
     * Check whether the value is the same to the other.
     * @param other other value
     * @return whether the value is the same
     */
    boolean isSame(T other);
}
