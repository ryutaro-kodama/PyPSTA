package client.method;

import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class PypstaFakeMethod extends SyntheticMethod {
    public PypstaFakeMethod(MethodReference method, IClass declaringClass) {
        super(method, declaringClass, false, false);
    }

    public PypstaFakeMethod(IMethod method, IClass declaringClass) {
        super(method, declaringClass, false, false);
    }

    public PypstaFakeMethod(TypeReference typeRef, IClass declaringClass) {
        super(
                MethodReference.findOrCreate(
                        PythonLanguage.Python,
                        typeRef,
                        AstMethodReference.fnAtomStr,  // "do"
                        "()" + AstTypeReference.rootTypeDescStr),  // "()LRoot"
                declaringClass,
                false,
                false);
    }
}
