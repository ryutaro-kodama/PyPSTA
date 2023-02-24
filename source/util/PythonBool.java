package util;

import com.ibm.wala.util.debug.Assertions;

public class PythonBool {
    public static boolean isTrue(Object val) {
        if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() == true;
        } else if (val instanceof Integer) {
            return ((Integer) val).intValue() != 0;
        } else if (val instanceof Float) {
            return ((Float) val).floatValue() != 0.0f;
        } else if (val instanceof Long) {
            return ((Long) val).longValue() != 0.0;
        } else if (val instanceof Double) {
            return ((Double) val).doubleValue() != 0.0d;
        } else if (val instanceof String) {
            return true;
        } else {
            Assertions.UNREACHABLE(); return false;
        }
    }
}
