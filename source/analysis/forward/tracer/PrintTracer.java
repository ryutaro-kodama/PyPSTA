package analysis.forward.tracer;

import analysis.forward.abstraction.value.element.BoolValue;
import analysis.forward.abstraction.value.element.IForwardAbstractValueElement;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.types.FieldReference;

public class PrintTracer implements ITracer {
    @Override
    public void illegalCompareException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2) {
        System.err.println("Try comparing <" + val1 + "> and <" + val2 + ">");
    }

    @Override
    public void notDefinedCompare(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        System.err.println("Operation <" + FuncName + "> is undefined on <" + val1 + ">,<" + val2 + ">");
    }

    @Override
    public void illegalBinOpeException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2, SSABinaryOpInstruction inst) {
        System.err.println("Try calculating <" + val1 + "> and <" + val2 + ">");
    }

    @Override
    public void notDefinedBinOperation(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        System.err.println("Operation <" + FuncName + "> is undefined on <" + val1 + ">,<" + val2 + ">");
    }

    @Override
    public void illegalUnaryOpeException(IForwardAbstractValueElement val) {
        System.err.println("Try calculating <" + val + ">");
    }

    @Override
    public void attributeException(ObjectValue object, String fieldName) {
        System.err.println("Can't find <" + fieldName + "> of <" + object + ">");
    }

    @Override
    public void attributeException(ObjectValue object, FieldReference fieldRef) {
        System.err.println("Can't find <" + fieldRef + "> of <" + object + ">");
    }

    @Override
    public void elementException(ObjectValue object, ILatticeElement index) {
        System.err.println("Can't find <" + index + "> of <" + object + ">");
    }

    @Override
    public void assertException(BoolValue boolValue) {
        System.err.println("Assertion error <" + boolValue + ">");
    }

    @Override
    public void bottomException(IForwardAbstractValueElement element) {
        System.err.println("<" + element + "> must not been bottom.");
    }

    @Override
    public void noneException(int varId) {
        System.err.println("<" + varId + "> must not be 'None'.");
    }
}
