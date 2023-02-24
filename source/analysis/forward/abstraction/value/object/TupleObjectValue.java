package analysis.forward.abstraction.value.object;

import analysis.forward.abstraction.AllocatePointTable;
import analysis.forward.abstraction.value.allocatepoint.AllocatePoint;
import com.ibm.wala.cast.python.types.PythonTypes;

public class TupleObjectValue extends ComplexObjectValue<TupleObjectValue> {
    public TupleObjectValue(AllocatePoint allocatePoint) {
        super(allocatePoint, PythonTypes.tuple);
    }

    @Override
    public boolean union(TupleObjectValue other, AllocatePointTable apTable) {
        return unionAttrs(other, apTable) | unionIntAccessElements(other, apTable);
    }

    @Override
    public TupleObjectValue copy(AllocatePointTable apTable) {
        // Create new object.
        TupleObjectValue newObject = new TupleObjectValue(getAllocatePoint());

        // Do copy.
        copyAttrs(newObject);
        copyIntAccessElements(newObject);

        return newObject;
    }

    @Override
    public boolean isSame(TupleObjectValue other, AllocatePointTable apTable) {
        return areAttrsSame(other, apTable) && areElementsSame(other, apTable);
    }

    @Override
    public String toString() {
        return "Tuple<" + super.toString() + ">";
    }
}
