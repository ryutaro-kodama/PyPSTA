package analysis.forward;

import com.ibm.wala.cast.ipa.callgraph.AstCallGraph;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.loader.PypstaLoader;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

public class PypstaInitTrampFunction extends PythonSummarizedFunction implements PypstaLoader.IPypstaMethod {
    private final IMethod initMethod;

    private final TrampolineArguments initArg;

    public PypstaInitTrampFunction(MethodReference initMethodRef,
                                   MethodSummary summary,
                                   IClass declaringClass,
                                   IMethod initMethod,
                                   Arguments initArg) throws NullPointerException {
        super(initMethodRef, summary, declaringClass);
        this.initMethod = initMethod;
        this.initArg = new TrampolineArguments(initArg);
    }

    @Override
    public Arguments getArgs() {
        return initArg;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public boolean isTrampoline() {
        return true;
    }

    @Override
    public int getNumberOfDefaultParameters() {
        return initArg.getNumberOfDefaultParameters();
    }

    private class TrampolineArguments extends Arguments {
        private final Arguments base;

        public TrampolineArguments(Arguments base) {
            super(base);
            this.base = base;
        }

        @Override
        public int getNumberOfParameters() {
            int result = super.getNumberOfParameters();
            // Trampoline method doesn't receive 'self' object, so decrement the number of real parameters.
            return (result == 1) ? 1 : result-1;
        }

        @Override
        public int getNumberOfPositionalParameters() {
            int result = super.getNumberOfPositionalParameters();
            // Trampoline method doesn't receive 'self' object, so decrement the number of real parameters.
            return (result == 0) ? 0 : result-1;
        }

        @Override
        protected int getVarIdFromName(String varName, CGNode targetNode) {
            CGNode initNode = null;
            try {
                 initNode = ((AstCallGraph.AstCGNode) targetNode).getCallGraph()
                         .findOrCreateNode(initMethod, Everywhere.EVERYWHERE);
            } catch (CancelException e) {
                e.printStackTrace();
            }
            // Trampoline method doesn't receive 'self' object, so decrement the number of real parameters.
            return super.getVarIdFromName(varName, initNode)-1;
        }
    }
}
