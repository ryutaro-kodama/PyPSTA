package analysis.forward.tracer;

import analysis.forward.abstraction.value.element.BoolValue;
import analysis.forward.abstraction.value.element.IForwardAbstractValueElement;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.types.FieldReference;

public interface ITracer {
    void illegalCompareException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2);

    void notDefinedCompare(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName);

    void illegalBinOpeException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2, SSABinaryOpInstruction inst);

    void notDefinedBinOperation(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName);

    void illegalUnaryOpeException(IForwardAbstractValueElement val);

    void attributeException(ObjectValue object, String fieldName);

    void attributeException(ObjectValue object, FieldReference fieldRef);

    void elementException(ObjectValue object, ILatticeElement index);

    void assertException(BoolValue boolValue);

    void bottomException(IForwardAbstractValueElement element);

    void noneException(int varId);
}
