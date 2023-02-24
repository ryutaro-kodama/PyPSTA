package analysis.backward.thresher;

import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.shrike.shrikeBT.*;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import util.AriadneSupporter;
import util.ConstantConverter;

public class OperationManager {
    public static Object operation(Object lhs, Object rhs, BinaryOpInstruction.IOperator op) {
        if (op instanceof IBinaryOpInstruction.Operator) {
            return operation(lhs, rhs, (IBinaryOpInstruction.Operator) op);
        } else if (op instanceof CAstBinaryOp) {
            return operation(lhs, rhs, (CAstBinaryOp) op);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    public static Object operation(Object lhs, Object rhs, BinaryOpInstruction.Operator op) {
        switch (op) {
            case ADD:
                if (AriadneSupporter.isPythonString(lhs) && AriadneSupporter.isPythonString(rhs)) {
                    return (String) lhs + (String) rhs;
                } else if (AriadneSupporter.isPythonFloat(lhs) || AriadneSupporter.isPythonFloat(rhs)) {
                    return ConstantConverter.toDoubleValue(lhs) + ConstantConverter.toDoubleValue(rhs);
                } else if (AriadneSupporter.isPythonInt(lhs) || AriadneSupporter.isPythonInt(rhs)) {
                    return ConstantConverter.toIntValue(lhs) + ConstantConverter.toIntValue(rhs);
                } else if (AriadneSupporter.toTypeRef(lhs).equals(AriadneSupporter.toTypeRef(rhs))) {
                    // You don't know the result of `<TYPE> + <TYPE>`, so return <TYPE>.
                    return AriadneSupporter.toTypeRef((lhs));
                }
                Assertions.UNREACHABLE(); return null;
            case SUB:
                if (AriadneSupporter.isPythonFloat(lhs) || AriadneSupporter.isPythonFloat(rhs)) {
                    return ConstantConverter.toDoubleValue(lhs) - ConstantConverter.toDoubleValue(rhs);
                } else if (AriadneSupporter.isPythonInt(lhs) || AriadneSupporter.isPythonInt(rhs)) {
                    return ConstantConverter.toIntValue(lhs) - ConstantConverter.toIntValue(rhs);
                } else if (AriadneSupporter.toTypeRef(lhs).equals(AriadneSupporter.toTypeRef(rhs))) {
                    // You don't know the result of `<TYPE> - <TYPE>`, so return <TYPE>.
                    return AriadneSupporter.toTypeRef((lhs));
                }
                Assertions.UNREACHABLE(); return null;
            case MUL:
            case DIV:
            case REM:
            case AND:
            case OR:
            case XOR:
            default:
                Assertions.UNREACHABLE(); return null;
        }
    }

    public static Object operation(Object lhs, Object rhs, IConditionalBranchInstruction.Operator op) {
        switch (op) {
            case EQ:
                if (lhs instanceof TypeReference || rhs instanceof TypeReference) {
                    // This means you don't know whether result is 'True' or 'False'
                    return TypeReference.Boolean;
                } else if (lhs instanceof Boolean) {
                    return ((Boolean) lhs).compareTo(ConstantConverter.toBoolValue(rhs)) == 0;
                } else if (lhs instanceof Integer) {
                    return ((Integer) lhs).compareTo(ConstantConverter.toIntValue(rhs)) == 0;
                } else if (lhs instanceof Long) {
                    return ((Long) lhs).compareTo(ConstantConverter.toLongValue(rhs)) == 0;
                }
                return null;
            case NE:
                if (lhs instanceof Boolean) {
                    return ((Boolean) lhs).compareTo(ConstantConverter.toBoolValue(rhs)) != 0;
                } else if (lhs instanceof Integer) {
                    return ((Integer) lhs).compareTo(ConstantConverter.toIntValue(rhs)) != 0;
                } else if (lhs instanceof Long) {
                    return ((Long) lhs).compareTo(ConstantConverter.toLongValue(rhs)) != 0;
                } else if (lhs instanceof Double) {
                    return ((Double) lhs).compareTo(ConstantConverter.toDoubleValue(rhs)) != 0;
                } else if (lhs instanceof TypeReference) {
                    return !lhs.equals(AriadneSupporter.toTypeRef(rhs));
                }
                Assertions.UNREACHABLE();
                return null;
            case GE:
                Assertions.UNREACHABLE();
                return null;
            case GT:
                if (lhs instanceof Boolean) {
                    return ((Boolean) lhs).compareTo(ConstantConverter.toBoolValue(rhs)) > 0;
                } else if (lhs instanceof Integer) {
                    return ((Integer) lhs).compareTo(ConstantConverter.toIntValue(rhs)) > 0;
                } else if (lhs instanceof Long) {
                    return ((Long) lhs).compareTo(ConstantConverter.toLongValue(rhs)) > 0;
                } else if (lhs instanceof TypeReference) {
                    return TypeReference.Boolean;
                }
                Assertions.UNREACHABLE();
                return null;
            case LE:
                Assertions.UNREACHABLE();
                return null;
            case LT:
                if (lhs instanceof Boolean) {
                    return ((Boolean) lhs).compareTo(ConstantConverter.toBoolValue(rhs)) < 0;
                } else if (lhs instanceof Integer) {
                    return ((Integer) lhs).compareTo(ConstantConverter.toIntValue(rhs)) < 0;
                } else if (lhs instanceof Long) {
                    return ((Long) lhs).compareTo(ConstantConverter.toLongValue(rhs)) < 0;
                } else if (lhs instanceof TypeReference) {
                    // This means you don't know whether result is 'True' or 'False'
                    return TypeReference.Boolean;
                }
                Assertions.UNREACHABLE();
                return null;
            default: Assertions.UNREACHABLE(); return null;
        }
    }

    public static Object operation(Object lhs, Object rhs, CAstBinaryOp op) {
        switch (op) {
            case CONCAT:
                Assertions.UNREACHABLE();
                return null;
            case EQ:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.EQ);
            case NE:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.NE);
            case GE:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.GE);
            case GT:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.GT);
            case LE:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.LE);
            case LT:
                return operation(lhs, rhs, IConditionalBranchInstruction.Operator.LT);
            case STRICT_EQ:
                Assertions.UNREACHABLE();
                return null;
            case STRICT_NE:
                Assertions.UNREACHABLE();
                return null;
            default: Assertions.UNREACHABLE(); return null;
        }
    }

    public static Object unaryOperation(Object val, IUnaryOpInstruction.IOperator op) {
        if (op instanceof IUnaryOpInstruction.Operator) {
            return unaryOperation(val, (IUnaryOpInstruction.Operator) op);
        } else if (op instanceof CAstUnaryOp) {
            return unaryOperation(val, (CAstUnaryOp) op);
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }

    private static Object unaryOperation(Object val, IUnaryOpInstruction.Operator op) {
        switch (op) {
            case NEG:
                if (val instanceof Boolean) {
                    return val.equals(Boolean.FALSE);
                } else if (val instanceof Integer) {
                    return ((Integer) val).intValue() == 0;
                } else if (val instanceof Long) {
                    return ((Long) val).longValue() == 0l;
                } else if (val instanceof Float) {
                    return ((Float) val).floatValue() == 0.0f;
                } else if (val instanceof Long) {
                    return ((Double) val).doubleValue() == 0.0d;
                } else if (val instanceof TypeReference) {
                    // You don't know the result of `not <TYPE>`, so return <TYPE>.
                    return val;
                }
                Assertions.UNREACHABLE();
                return null;
            default: Assertions.UNREACHABLE(); return null;
        }
    }

    private static Object unaryOperation(Object val, CAstUnaryOp op) {
        switch (op) {
            case PLUS:
                Assertions.UNREACHABLE();
                return null;
            case MINUS:
                Assertions.UNREACHABLE();
                return null;
            case BITNOT:
                Assertions.UNREACHABLE();
                return null;
            default: Assertions.UNREACHABLE(); return null;
        }
    }
}
