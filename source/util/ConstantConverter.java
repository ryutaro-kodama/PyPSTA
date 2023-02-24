package util;

import com.ibm.wala.util.debug.Assertions;

public class ConstantConverter {
    public static boolean toBoolValue(Object val) {
        if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else if (val instanceof Integer) {
            return ((Integer) val).compareTo(Integer.valueOf(0)) != 0;
        } else if (val instanceof Long) {
            return ((Long) val).compareTo(Long.valueOf(0l)) != 0;
        } else if (val instanceof Float) {
            return ((Float) val).compareTo(Float.valueOf(0f)) != 0;
        } else if (val instanceof Double) {
            return ((Double) val).compareTo(Double.valueOf(0d)) != 0;
        } else if (val instanceof String) {
            return false;
        } else {
            Assertions.UNREACHABLE(); return false;
        }
    }

    public static int toIntValue(Object val) {
        if (val instanceof Boolean) {
            if ((Boolean) val) {
                return 1;
            } else {
                return 0;
            }
        } else if (val instanceof Integer) {
            return ((Integer) val).intValue();
        } else if (val instanceof Long) {
            return ((Long) val).intValue();
        } else if (val instanceof Float) {
            return ((Float) val).intValue();
        } else if (val instanceof Double) {
            return ((Double) val).intValue();
        } else if (val instanceof String) {
            Assertions.UNREACHABLE(); return -1;
        } else {
            Assertions.UNREACHABLE(); return -1;
        }
    }

    public static long toLongValue(Object val) {
        if (val instanceof Boolean) {
            if ((Boolean) val) {
                return 1l;
            } else {
                return 0l;
            }
        } else if (val instanceof Integer) {
            return ((Integer) val).longValue();
        } else if (val instanceof Long) {
            return ((Long) val).longValue();
        } else if (val instanceof Float) {
            return ((Float) val).longValue();
        } else if (val instanceof Double) {
            return ((Double) val).longValue();
        } else if (val instanceof String) {
            Assertions.UNREACHABLE(); return -1l;
        } else {
            Assertions.UNREACHABLE(); return -1l;
        }
    }

    public static float toFloatValue(Object val) {
        if (val instanceof Boolean) {
            if ((Boolean) val) {
                return 1f;
            } else {
                return 0f;
            }
        } else if (val instanceof Integer) {
            return ((Integer) val).floatValue();
        } else if (val instanceof Long) {
            return ((Long) val).floatValue();
        } else if (val instanceof Float) {
            return ((Float) val).floatValue();
        } else if (val instanceof Double) {
            return ((Double) val).floatValue();
        } else if (val instanceof String) {
            Assertions.UNREACHABLE(); return -1f;
        } else {
            Assertions.UNREACHABLE(); return -1f;
        }
    }

    public static double toDoubleValue(Object val) {
        if (val instanceof Boolean) {
            if ((Boolean) val) {
                return 1d;
            } else {
                return 0d;
            }
        } else if (val instanceof Integer) {
            return ((Integer) val).doubleValue();
        } else if (val instanceof Long) {
            return ((Long) val).doubleValue();
        } else if (val instanceof Float) {
            return ((Float) val).doubleValue();
        } else if (val instanceof Double) {
            return ((Double) val).doubleValue();
        } else if (val instanceof String) {
            Assertions.UNREACHABLE(); return -1d;
        } else {
            Assertions.UNREACHABLE(); return -1d;
        }
    }

    public static String toStringValue(Object val) {
        if (val instanceof Boolean) {
            Assertions.UNREACHABLE();
            return "";
        } else if (val instanceof Integer) {
            Assertions.UNREACHABLE();
            return "";
        } else if (val instanceof Long) {
            Assertions.UNREACHABLE();
            return "";
        } else if (val instanceof Float) {
            Assertions.UNREACHABLE();
            return "";
        } else if (val instanceof Double) {
            Assertions.UNREACHABLE();
            return "";
        } else if (val instanceof String) {
            return (String) val;
        } else {
            Assertions.UNREACHABLE();
            return "";
        }
    }
}
