package client.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;

public class nCFAContextComparableSelector extends nCFAContextSelector {
    public nCFAContextComparableSelector(int n, ContextSelector base) {
        super(n, base);
    }

    @Override
    protected CallStringComparable getCallString(CGNode caller, CallSiteReference site, IMethod target) {
        int length = getLength(caller, site, target);
        if (length > 0) {
            if (caller.getContext().get(CALL_STRING) != null) {
                return new CallStringComparable(
                        site, caller.getMethod(), length, (CallString) caller.getContext().get(CALL_STRING));
            } else {
                return new CallStringComparable(site, caller.getMethod());
            }
        } else {
            return null;
        }
    }

    @Override
    public Context getCalleeTarget(
            CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
        Context baseContext = base.getCalleeTarget(caller, site, callee, receiver);
        CallStringComparable cs = getCallString(caller, site, callee);
        if (cs == null) {
            return baseContext;
        } else if (baseContext == Everywhere.EVERYWHERE) {
            return new CallStringContextComparable(cs);
        } else {
            return new CallStringContextComparablePair(cs, baseContext);
        }
    }


    public class CallStringComparable extends CallString implements Comparable {
        public CallStringComparable(CallSiteReference site, IMethod method) {
            super(site, method);
        }

        protected CallStringComparable(CallSiteReference site, IMethod method, int length, CallString base) {
            super(site, method, length, base);
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof CallStringComparable) {
                CallStringComparable other = (CallStringComparable) o;

                IMethod[] methods = getMethods();
                IMethod[] otherMethods = other.getMethods();
                CallSiteReference[] sites = getCallSiteRefs();
                CallSiteReference[] otherSites = other.getCallSiteRefs();

                if (methods.length > otherMethods.length)
                    return 1;
                else if (methods.length < otherMethods.length)
                    return -1;

                for (int i = getCallSiteRefs().length - 1; 0 <= i; i--) {
                    if (!methods[i].toString().equals(otherMethods[i].toString()))
                        return methods[i].toString().compareTo(otherMethods[i].toString());

                    if (sites[i].getProgramCounter() != otherSites[i].getProgramCounter())
                        return Integer.compare(
                                sites[i].getProgramCounter(), otherSites[i].getProgramCounter());
                }

                return 0;
            } else {
                return toString().compareTo(o.toString());
            }
        }
    }

    public class CallStringContextComparable extends CallStringContext implements Comparable {
        public CallStringContextComparable(CallStringComparable cs) {
            super(cs);
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof CallStringContextComparable) {
                return ((CallStringComparable) get(CallStringContextSelector.CALL_STRING))
                            .compareTo(
                                    ((CallStringContextComparable) o).get(CallStringContextSelector.CALL_STRING)
                            );
            } else {
                return toString().compareTo(o.toString());
            }
        }
    }

    public class CallStringContextComparablePair extends CallStringContextPair implements Comparable {
        public CallStringContextComparablePair(CallString cs, Context base) {
            super(cs, base);
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof CallStringContextComparablePair) {
                return 0;
            } else {
                return toString().compareTo(o.toString());
            }
        }
    }
}
