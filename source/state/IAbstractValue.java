package state;

import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;

/**
 * The analysis's abstract value.
 * @param <T> the other abstract value which is calculated in binary operation.
 */
public interface IAbstractValue<T extends IAbstractValue> {
    boolean union(T other);

    /** Binary operation */
    T add(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T sub(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T mult(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T div(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T fdiv(T val2, SSABinaryOpInstruction inst1, ForwardState forwardState);

    T mod(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T pow(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T lshift(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T rshift(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T bitor(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T bitand(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

     T bitxor(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T matmult(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    /** Compare operation */
    T eq(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T neq(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T lt(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T lte(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T gt(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    T gte(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T is(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T isn(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T in(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    // T nin(T other, SSABinaryOpInstruction inst, ForwardState forwardState);

    /** Unary operation */
    T not(SSAUnaryOpInstruction inst, ForwardState forwardState);

    T minus(SSAUnaryOpInstruction inst, ForwardState forwardState);

    boolean isSame(T other);
}
