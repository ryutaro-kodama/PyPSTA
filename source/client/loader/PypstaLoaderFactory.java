package client.loader;

import com.ibm.wala.cast.python.loader.PypstaLoader;
import com.ibm.wala.cast.python.loader.Python3LoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class PypstaLoaderFactory extends Python3LoaderFactory {
    @Override
    protected IClassLoader makeTheLoader(IClassHierarchy cha) {
        return new PypstaLoader(cha);
    }
}
