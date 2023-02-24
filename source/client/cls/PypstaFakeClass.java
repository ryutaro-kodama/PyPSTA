package client.cls;

import com.ibm.wala.cast.python.ipa.summaries.PythonSyntheticClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

public class PypstaFakeClass extends PythonSyntheticClass {
    public PypstaFakeClass(TypeReference T, IClassHierarchy cha) {
        super(T, cha);
    }

    @Override
    public String toString() {
        return "PypstaFakeClass: " + getReference();
    }
}
