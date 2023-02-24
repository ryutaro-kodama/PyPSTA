package analysis.backward.thresher;

import analysis.backward.thresher.query.IPypstaQuery;
import analysis.backward.thresher.query.TypeQuery;
import analysis.backward.thresher.query.ValueQuery;
import analysis.exception.*;
import analysis.forward.ForwardAnalyzer;
import analysis.forward.abstraction.ForwardState;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import util.AriadneSupporter;

import java.util.HashSet;
import java.util.Set;

public class QueryFactory {
    public static ForwardAnalyzer analyzer;

    public static Set<IPypstaQuery> make(IExceptionData exceptionData, CGNode cgNode) {
        if (exceptionData instanceof IllegalBinOpeExceptionData) {
            IllegalBinOpeExceptionData exceptionData1 = (IllegalBinOpeExceptionData) exceptionData;
            HashSet<IPypstaQuery> result = new HashSet<>();

            ForwardState state = exceptionData1.getState();

            int var1Id = exceptionData1.getVal1Id();
            if (!cgNode.getIR().getSymbolTable().isConstant(var1Id)) {
                Set<TypeReference> val1TypeSet = exceptionData1.getVal1().getTypes(state);
                val1TypeSet.forEach(
                        t -> result.add(new TypeQuery(analyzer, var1Id, cgNode, t)));
            }

            int var2Id = exceptionData1.getVal2Id();
            if (!cgNode.getIR().getSymbolTable().isConstant(var2Id)) {
                Set<TypeReference> val2TypeSet = exceptionData1.getVal2().getTypes(state);
                val2TypeSet.forEach(
                        t -> result.add(new TypeQuery(analyzer, var2Id, cgNode, t)));
            }
            return result;
        } else if (exceptionData instanceof AttributeExceptionData) {
            AttributeExceptionData exceptionData1 = (AttributeExceptionData) exceptionData;
            HashSet<IPypstaQuery> result = new HashSet<>();

            int objectId = exceptionData1.getObjectId();
            Set<TypeReference> objectValueTypes = exceptionData1.getObjectTypes();
            objectValueTypes.forEach(
                    t -> result.add(new TypeQuery(analyzer, objectId, cgNode, t)));

            return result;
        } else if (exceptionData instanceof ElementExceptionData) {
            ElementExceptionData exceptionData1 = (ElementExceptionData) exceptionData;
            HashSet<IPypstaQuery> result = new HashSet<>();
            // TODO: Create element access rule.
            return result;
        } else if (exceptionData instanceof AssertExceptionData) {
            AssertExceptionData exceptionData1 = (AssertExceptionData) exceptionData;
            AstAssertInstruction inst = exceptionData1.getInst();
            HashSet<IPypstaQuery> result = new HashSet<>();
            result.add(
                    // Check the witness whether the result of assertion is not admitted.
                    new ValueQuery(analyzer, inst.getUse(0), cgNode, !inst.isFromSpecification()));
            return result;
        } else if (exceptionData instanceof NoneExceptionData) {
            NoneExceptionData exceptionData1 = (NoneExceptionData) exceptionData;
            HashSet<IPypstaQuery> result = new HashSet<>();
            int varId = exceptionData1.getVarId();
            result.add(
                    // Check the witness whether the variable id has none value.
                    new TypeQuery(analyzer, varId, cgNode, AriadneSupporter.None));
            return result;
        } else {
            Assertions.UNREACHABLE();
            return null;
        }
    }
}
