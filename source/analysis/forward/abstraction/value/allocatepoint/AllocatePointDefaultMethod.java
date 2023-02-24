package analysis.forward.abstraction.value.allocatepoint;

/**
 * Representing allocate point of default method in complex object (ex. list, dict, ...).
 */
public class AllocatePointDefaultMethod extends AllocatePoint {
    private final String methodName;

    public AllocatePointDefaultMethod(AllocatePoint allocatePoint, String methodName) {
        super(allocatePoint.getCGNode(), allocatePoint.getInstruction());
        this.methodName = methodName;
    }

    @Override
    public boolean match(AllocatePoint other) {
        if (!(other instanceof AllocatePointDefaultMethod)) return false;
        AllocatePointDefaultMethod other1 = (AllocatePointDefaultMethod) other;
        return getInstruction().equals(other.getInstruction()) && getCGNode().equals(other.getCGNode()) && methodName.equals(other1.methodName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllocatePointDefaultMethod that = (AllocatePointDefaultMethod) o;
        return getCGNode().equals(that.getCGNode()) && getInstruction().equals(that.getInstruction()) && methodName.equals(that.methodName);
    }

    @Override
    public String toString() {
        return super.toString() + '-' + methodName;
    }
}
