package analysis.forward;

import analysis.forward.abstraction.value.element.BoolValue;
import analysis.forward.abstraction.value.element.IForwardAbstractValueElement;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.object.ComplexObjectValue;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.types.FieldReference;
import analysis.forward.tracer.ITracer;

public class ExceptionManager {
    private static ITracer tracer;

    public static void setTracer(ITracer t) {
        tracer = t;
    }

    public static void illegalCompareException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2) {
        tracer.illegalCompareException(val1, val2);
    }

    public static void notDefinedCompare(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        tracer.notDefinedCompare(val1, val2, FuncName);
    }

    public static void illegalBinOpeException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2, SSABinaryOpInstruction inst) {
        tracer.illegalBinOpeException(val1, val2, inst);
    }

    public static void notDefinedBinOperation(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        tracer.notDefinedBinOperation(val1, val2, FuncName);
    }

    public static void illegalUnaryOpeException(IForwardAbstractValueElement val) {
        tracer.illegalUnaryOpeException(val);
    }

    public static <T extends ObjectValue<T>> void attributeException(ObjectValue<T> object,
                                                                     String key) {
        tracer.attributeException(object, key);
    }

    public static void attributeException(ObjectValue object, FieldReference fieldRef) {
        tracer.attributeException(object, fieldRef);
    }

    public static <T extends ComplexObjectValue<T>> void elementException(ComplexObjectValue<T> object,
                                                                          ILatticeElement index) {
        tracer.elementException(object, index);
    }

    public static void assertException(BoolValue boolValue) {
        tracer.assertException(boolValue);
    }

    public static void bottomException(IForwardAbstractValueElement element) {
        tracer.bottomException(element);
    }

    public static void noneException(int varId) {
        tracer.noneException(varId);
    }
}
