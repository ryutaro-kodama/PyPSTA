package analysis.forward.abstraction;

import analysis.forward.abstraction.value.ForwardAbstractValue;
import analysis.forward.abstraction.value.IForwardAbstractValue;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import analysis.forward.abstraction.value.object.*;

import java.util.HashMap;
import java.util.HashSet;

public class AllocatePointTable extends HashMap<AllocatePoint, ObjectValue> {
    public AllocatePointTable() {
        put(ClassObjectValue.objectClass.getAllocatePoint(), ClassObjectValue.objectClass);
    }

    public void newAllocation(ObjectValue objectValue) {
        put(objectValue.getAllocatePoint(), objectValue);
    }

    /**
     * Take in allocate points and copied object value which are pointed by the abstract value, its attributes or elements.
     *
     * @param abstractValue the base value
     * @param otherTable    the base value's object is defined
     */
    public void takeInSingleValue(IForwardAbstractValue abstractValue, AllocatePointTable otherTable) {
        HashSet<AllocatePoint> result = otherTable.collectUsedAllocatePoints(abstractValue, new HashSet<>());

        for (AllocatePoint allocatePoint : result) {
            put(allocatePoint, otherTable.get(allocatePoint).copy(this));
        }
    }

    /**
     * Union allocate points and copied object value which are pointed by the abstract value, its attributes or elements.
     *
     * @param abstractValue the base value
     * @param otherTable    the base value's object is defined
     */
    public void unionSingleValue(IForwardAbstractValue abstractValue, AllocatePointTable otherTable) {
        HashSet<AllocatePoint> result = otherTable.collectUsedAllocatePoints(abstractValue, new HashSet<>());

        for (AllocatePoint allocatePoint : result) {
            if (containsKey(allocatePoint)) {
                get(allocatePoint).union(otherTable.get(allocatePoint), this);
                // The 'union' method is implemented not to affect other values,
                // so you don't have to make a copy of other value.
            } else {
                put(allocatePoint, otherTable.get(allocatePoint).copy(this));
            }
        }
    }

    /**
     * Collect allocate points the abstract value contains. If there are attributes or elements which points to object,
     * its allocate points are also contained by recursive calling.
     *
     * @param abstractValue the base value
     * @param result        the result set containing allocate points
     * @return the result set containing allocate points
     */
    public HashSet<AllocatePoint> collectUsedAllocatePoints(IForwardAbstractValue abstractValue, HashSet<AllocatePoint> result) {
        ForwardAbstractValue forwardAbstractValue = (ForwardAbstractValue) abstractValue;

        for (AllocatePoint allocatePoint : forwardAbstractValue.getAllocatePoints()) {
            if (!result.contains(allocatePoint)) {
                ObjectValue objectValue = get(allocatePoint);
                result.addAll(objectValue.collectRelatedAPs(this, result));
            }
        }

        return result;
    }

    /**
     * Delete all mapping and make this mapping the same to the other table's one with copying 'ObjectValue'.
     *
     * @param otherTable
     */
    public void overrideTable(AllocatePointTable otherTable) {
        clear();

        for (Entry<AllocatePoint, ObjectValue> ap2Object : otherTable.entrySet()) {
            put(ap2Object.getKey(), ap2Object.getValue().copy(this));
        }
    }

    /**
     * Change this table to the result of union between this table and other table.
     *
     * @param otherTable
     * @return whether this table has changed
     */
    public boolean union(AllocatePointTable otherTable) {
        boolean hasChanged = false;
        for (Entry<AllocatePoint, ObjectValue> ap2Object : otherTable.entrySet()) {
            AllocatePoint key = ap2Object.getKey();
            ObjectValue otherValue = ap2Object.getValue();
            if (containsKey(key)) {
                hasChanged = get(key).union(otherValue, this) || hasChanged;
                // The 'union' method is implemented not to affect other values,
                // so you don't have to make a copy of other value.
            } else {
                put(key, otherValue.copy(this));
                hasChanged = true;
            }
        }

        return hasChanged;
    }
}
