package util;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;

public class AriadneSupporter {
    public static TypeReference None = TypeReference.findOrCreate(PythonTypes.pythonLoader, "LNone");

    public static boolean isPythonBool(Object val) {
        return val instanceof Boolean;
    }

    public static boolean isPythonInt(Object val) {
        return val instanceof Integer || val instanceof Long;
    }

    public static boolean isPythonFloat(Object val) {
        return val instanceof Float || val instanceof Double;
    }

    public static boolean isPythonString(Object val) {
        return val instanceof String;
    }

    public static TypeReference toTypeRef(Object val) {
        if (val instanceof Boolean) {
            return TypeReference.Boolean;
        } else if ((val instanceof Integer) || (val instanceof Long)) {
            return TypeReference.Int;
        } else if ((val instanceof Float) || (val instanceof Double)) {
            return TypeReference.Float;
        } else if (val instanceof String) {
            return PythonTypes.string;
        } else if (val instanceof TypeReference) {
            return (TypeReference) val;
        } else  {
            return null;
        }
    }

    private static String[] specialMethodNames = new String[] {
            // TODO: Add more names.
            "__eq__", "__ne__", "__gt__", "__ge__", "__lt__", "__le__",
            "__add__", "__sub__", "__mul__", "__div__",
            "__call__"
    };
    public static boolean isObjectSpecialMethod(String methodName) {
        for (String specialMethodName: specialMethodNames) {
            if (specialMethodName.equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
