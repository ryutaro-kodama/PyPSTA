package analysis.forward;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.ForwardState;
import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.fixpoint.ForwardCallManager;
import analysis.forward.fixpoint.ForwardFixSolver;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

import java.util.*;
import java.util.stream.Collectors;

public class LexicalCollector {
    private static Map<String, LexicalCollection> collections = new HashMap<>();

    /**
     * Set the lexical value to map.
     * @param access the Access object
     * @param writeState the state of writing
     */
    public static void put(AstLexicalAccess.Access access, ForwardState writeState) {
        LexicalCollection lexicalCollection;
        String varName = access.variableName;
        if (collections.containsKey(varName)) {
            lexicalCollection = collections.get(varName);
        } else {
            lexicalCollection = new LexicalCollection(varName);
            collections.put(varName, lexicalCollection);
        }
        lexicalCollection.override(access.valueNumber, writeState);
    }

    public static IForwardAbstractValue get(AstLexicalAccess.Access access,
                                            AllocatePointTable readStateAPTable) {
        String varName = access.variableName;

        IForwardAbstractValue returnValue = null;
        if (collections.containsKey(varName)) {
            LexicalCollection lexicalCollection = collections.get(varName);
            returnValue = lexicalCollection.getValue(readStateAPTable);
        } else {
            Assertions.UNREACHABLE();
        }

        return returnValue;
    }

    public static void update(String name, Integer varId, ForwardState exitState) {
        LexicalCollection lexicalCollection = collections.get(name);
        if (lexicalCollection != null)
            lexicalCollection.write(varId, exitState);
    }

    public static void reset() {
        collections.clear();
    }

    public static class LexicalCollection {
        private final String varName;

        /** The abstract value of lexical variable */
        private IForwardAbstractValue writtenValue = new ForwardAbstractValue();

        /** The allocate point table of abstract lexical value */
        private AllocatePointTable table = new AllocatePointTable();

        public LexicalCollection(String varName) {
            this.varName = varName;
        }

        public void write(int value, ForwardState writeState) {
            IForwardAbstractValue writtenValue = writeState.getValue(value);
            this.writtenValue.union(writtenValue);

            AllocatePointTable writeStateTable = writeState.getAllocatePointTable();
            table.unionSingleValue(writtenValue, writeStateTable);
        }

        public void override(int value, ForwardState writeState) {
            this.writtenValue = writeState.getValue(value).copy();
            table.takeInSingleValue(this.writtenValue, writeState.getAllocatePointTable());
        }

        /**
         * Get the lexical abstract value.
         *
         * @param readStateAPTable allocate point table which are updated
         * @return the lexical abstract value
         */
        public IForwardAbstractValue getValue(AllocatePointTable readStateAPTable) {
            IForwardAbstractValue result = writtenValue.copy();
            readStateAPTable.union(table);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LexicalCollection that = (LexicalCollection) o;
            return Objects.equals(varName, that.varName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(varName);
        }
    }
}
