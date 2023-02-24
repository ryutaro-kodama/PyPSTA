package analysis.forward;

import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.NoneValue;
import analysis.forward.abstraction.value.lattice.lattice_element.LatticeTop;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.util.debug.Assertions;

public class ConstantConverter {
    public static IForwardAbstractValue convert(ConstantValue value) {
        if (value == null) return new ForwardAbstractValue();

        Object value2 = value.getValue();
        if (value2 instanceof Boolean) {
            return new ForwardAbstractValue((Boolean) value2);
        } else if (value2 instanceof Integer) {
            return new ForwardAbstractValue((Integer) value2);
        } else if (value2 instanceof Long) {
            return new ForwardAbstractValue(((Long) value2).intValue());
        } else if (value2 instanceof Double) {
            return new ForwardAbstractValue(((Double) value2).floatValue());
        } else if (value2 instanceof String) {
            return new ForwardAbstractValue((String) value2);
        } else if (value2 == null) {
            return new ForwardAbstractValue(new NoneValue(LatticeTop.TOP));
        } else {
            Assertions.UNREACHABLE("Undefined type.");
            return null;
        }
    }
}
