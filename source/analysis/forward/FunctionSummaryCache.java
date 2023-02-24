package analysis.forward;

import analysis.forward.abstraction.value.ForwardAbstractValue;
import com.ibm.wala.ipa.summaries.SummarizedMethod;

import java.util.HashMap;

public class FunctionSummaryCache {
    private static HashMap<SummarizedMethod, ForwardAbstractValue> cache = new HashMap<>();

    public static void add(SummarizedMethod method, ForwardAbstractValue value) {
        cache.put(method, value);
    }

    public static boolean hasCache(SummarizedMethod method) {
        return cache.containsKey(method);
    }

    public static ForwardAbstractValue get(SummarizedMethod method) {
        return cache.get(method);
    }
}
