package util;

import com.ibm.wala.util.debug.Assertions;

public class PythonCalc {
    public static Object eq(Object lhs, Object rhs) {
        if (AriadneSupporter.isPythonString(lhs) || AriadneSupporter.isPythonString(rhs)) {
            String lhsVal = ConstantConverter.toStringValue(lhs);
            String rhsVal = ConstantConverter.toStringValue(rhs);
            return lhsVal.equals(rhsVal);
        } else if (AriadneSupporter.isPythonFloat(lhs) || AriadneSupporter.isPythonFloat(rhs)) {
            double lhsVal = ConstantConverter.toDoubleValue(lhs);
            double rhsVal = ConstantConverter.toDoubleValue(rhs);
            return lhsVal == rhsVal;
        } else if (AriadneSupporter.isPythonInt(lhs) || AriadneSupporter.isPythonInt(rhs)) {
            int lhsVal = ConstantConverter.toIntValue(lhs);
            int rhsVal = ConstantConverter.toIntValue(rhs);
            return lhsVal == rhsVal;
        } else if (AriadneSupporter.isPythonBool(lhs) || AriadneSupporter.isPythonBool(rhs)) {
            boolean lhsVal = ConstantConverter.toBoolValue(lhs);
            boolean rhsVal = ConstantConverter.toBoolValue(rhs);
            return lhsVal == rhsVal;
        } else {
            Assertions.UNREACHABLE(); return null;
        }
    }

    public static Object ne(Object lhs, Object rhs) {
        if (AriadneSupporter.isPythonString(lhs) || AriadneSupporter.isPythonString(rhs)) {
            String lhsVal = ConstantConverter.toStringValue(lhs);
            String rhsVal = ConstantConverter.toStringValue(rhs);
            return !lhsVal.equals(rhsVal);
        } else if (AriadneSupporter.isPythonFloat(lhs) || AriadneSupporter.isPythonFloat(rhs)) {
            double lhsVal = ConstantConverter.toDoubleValue(lhs);
            double rhsVal = ConstantConverter.toDoubleValue(rhs);
            return lhsVal != rhsVal;
        } else if (AriadneSupporter.isPythonInt(lhs) || AriadneSupporter.isPythonInt(rhs)) {
            int lhsVal = ConstantConverter.toIntValue(lhs);
            int rhsVal = ConstantConverter.toIntValue(rhs);
            return lhsVal != rhsVal;
        } else if (AriadneSupporter.isPythonBool(lhs) || AriadneSupporter.isPythonBool(rhs)) {
            boolean lhsVal = ConstantConverter.toBoolValue(lhs);
            boolean rhsVal = ConstantConverter.toBoolValue(rhs);
            return lhsVal != rhsVal;
        } else {
            Assertions.UNREACHABLE(); return null;
        }
    }

    public static Object add(Object lhs, Object rhs) {
        if (AriadneSupporter.isPythonString(lhs) || AriadneSupporter.isPythonString(rhs)) {
            String lhsVal = ConstantConverter.toStringValue(lhs);
            String rhsVal = ConstantConverter.toStringValue(rhs);
            return lhsVal.concat(rhsVal);
        } else if (AriadneSupporter.isPythonFloat(lhs) || AriadneSupporter.isPythonFloat(rhs)) {
            double lhsVal = ConstantConverter.toDoubleValue(lhs);
            double rhsVal = ConstantConverter.toDoubleValue(rhs);
            return lhsVal + rhsVal;
        } else if (AriadneSupporter.isPythonInt(lhs) || AriadneSupporter.isPythonInt(rhs)) {
            int lhsVal = ConstantConverter.toIntValue(lhs);
            int rhsVal = ConstantConverter.toIntValue(rhs);
            return lhsVal + rhsVal;
        } else if (AriadneSupporter.isPythonBool(lhs) || AriadneSupporter.isPythonBool(rhs)) {
            boolean lhsVal = ConstantConverter.toBoolValue(lhs);
            boolean rhsVal = ConstantConverter.toBoolValue(rhs);
            if (lhsVal && rhsVal) return 2;
            else if (lhsVal || rhsVal) return 1;
            else return 0;
        } else {
            Assertions.UNREACHABLE(); return null;
        }
    }
}
