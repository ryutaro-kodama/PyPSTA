package analysis.forward.tracer;

import analysis.exception.*;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.element.BoolValue;
import analysis.forward.abstraction.value.element.IForwardAbstractValueElement;
import analysis.forward.abstraction.value.lattice.lattice_element.ILatticeElement;
import analysis.forward.abstraction.value.object.ObjectValue;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.HashSetFactory;

import java.util.Set;

public class MakingExceptionDataTracer implements ITracer {
    public static ForwardState currentState = null;
    public static SSAInstruction currentInst = null;

    private Set<IExceptionData> exceptionDataSet = HashSetFactory.make();

    public Set<IExceptionData> getExceptionDataSet() {
        return exceptionDataSet;
    }

    @Override
    public void illegalCompareException(IForwardAbstractValueElement val1, IForwardAbstractValueElement val2) {
        exceptionDataSet.add(new TmpExceptionData(currentInst, currentState));
    }

    @Override
    public void notDefinedCompare(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        exceptionDataSet.add(new TmpExceptionData(currentInst, currentState));
    }

    @Override
    public void illegalBinOpeException(IForwardAbstractValueElement val1,
                                       IForwardAbstractValueElement val2,
                                       SSABinaryOpInstruction inst) {
        exceptionDataSet.add(new IllegalBinOpeExceptionData(val1, val2, inst, currentState));
    }

    @Override
    public void notDefinedBinOperation(ObjectValue val1, IForwardAbstractValueElement val2, String FuncName) {
        exceptionDataSet.add(new TmpExceptionData(currentInst, currentState));
    }

    @Override
    public void illegalUnaryOpeException(IForwardAbstractValueElement val) {
        exceptionDataSet.add(new TmpExceptionData(currentInst, currentState));
    }

    @Override
    public void attributeException(ObjectValue object, String fieldName) {
        exceptionDataSet.add(
                new AttributeExceptionData(object, fieldName, currentInst, currentState)
        );
    }

    @Override
    public void attributeException(ObjectValue object, FieldReference fieldRef) {
        exceptionDataSet.add(new TmpExceptionData(currentInst, currentState));
    }

    @Override
    public void elementException(ObjectValue object, ILatticeElement index) {
        exceptionDataSet.add(new ElementExceptionData(object, index, currentInst, currentState));
    }

    @Override
    public void assertException(BoolValue boolValue) {
        exceptionDataSet.add(new AssertExceptionData((AstAssertInstruction) currentInst, currentState));
    }

    @Override
    public void bottomException(IForwardAbstractValueElement element) {
        exceptionDataSet.add(new BottomExceptionData(currentInst, currentState));
    }

    @Override
    public void noneException(int varId) {
        exceptionDataSet.add(new NoneExceptionData(varId, currentInst, currentState));
    }
}
