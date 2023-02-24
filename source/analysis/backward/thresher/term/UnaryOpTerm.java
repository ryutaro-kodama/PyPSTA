package analysis.backward.thresher.term;

import analysis.backward.thresher.OperationManager;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.VariableFactor;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;

import java.util.*;

public class UnaryOpTerm implements ITerm {
    private final ITerm var;
    private final IUnaryOpInstruction.IOperator unaryOp;
    private boolean substituted = false;

    public UnaryOpTerm(VariableFactor var, IUnaryOpInstruction.IOperator unaryOp) {
        this.var = new VariableTerm(var);
        this.unaryOp = unaryOp;
    }

    private UnaryOpTerm(ITerm var, IUnaryOpInstruction.IOperator unaryOp) {
        this.var = var;
        this.unaryOp = unaryOp;
    }

    public ITerm getVar() {
        return var;
    }

    @Override
    public boolean hasBool() {
        return var.hasBool();
    }

    @Override
    public boolean hasInt() {
        // TODO: Is this really correct?
        return false;
    }

    @Override
    public boolean hasFloat() {
        // TODO: Is this really correct?
        return false;
    }

    @Override
    public boolean hasString() {
        // TODO: Is this really correct?
        return false;
    }

    @Override
    public BoolExpr getBoolExpr(Context ctx) {
        ArrayList<BoolExpr> results = new ArrayList<>();
        if (var.hasBool()) {
            if (unaryOp instanceof IUnaryOpInstruction.Operator) {
                switch ((IUnaryOpInstruction.Operator) unaryOp) {
                    case NEG: results.add(ctx.mkNot(var.getBoolExpr(ctx))); break;
                    default: Assertions.UNREACHABLE();
                }
            } else if (unaryOp instanceof CAstUnaryOp) {
                switch ((CAstUnaryOp) unaryOp) {
                    default: Assertions.UNREACHABLE();
                }
            }
        }
        if (var.hasInt()) {
            if (unaryOp instanceof IUnaryOpInstruction.Operator) {
                switch ((IUnaryOpInstruction.Operator) unaryOp) {
                    case NEG:
                        // Check whether the variable is equal to 0;
                        results.add(
                                ctx.mkEq(var.getIntExpr(ctx), ctx.mkInt(0))
                        );
                        break;
                    default: Assertions.UNREACHABLE();
                }
            } else if (unaryOp instanceof CAstUnaryOp) {
                switch ((CAstUnaryOp) unaryOp) {
                    default: Assertions.UNREACHABLE();
                }
            }
        }
        if (var.hasFloat()) {
            if (unaryOp instanceof IUnaryOpInstruction.Operator) {
                switch ((IUnaryOpInstruction.Operator) unaryOp) {
                    case NEG:
                        // Check whether the variable is equal to 0.0;
                        FPSort double_sort = ctx.mkFPSort(11, 53);
                        results.add(
                                ctx.mkEq(var.getFloatExpr(ctx), ctx.mkFP(0.0f, double_sort))
                        );
                        break;
                    default: Assertions.UNREACHABLE();
                }
            } else if (unaryOp instanceof CAstUnaryOp) {
                switch ((CAstUnaryOp) unaryOp) {
                    default: Assertions.UNREACHABLE();
                }
            }
        }
        if (var.hasString()) {
            if (unaryOp instanceof IUnaryOpInstruction.Operator) {
                switch ((IUnaryOpInstruction.Operator) unaryOp) {
                    case NEG:
                        // 'not `str`' is always False.
                        results.add(ctx.mkBool(false));
                        break;
                    default: Assertions.UNREACHABLE();
                }
            } else if (unaryOp instanceof CAstUnaryOp) {
                switch ((CAstUnaryOp) unaryOp) {
                    default: Assertions.UNREACHABLE();
                }
            }
        }
        BoolExpr[] array = results.toArray(new BoolExpr[0]);
        return ctx.mkAnd(array);
    }

    @Override
    public IntExpr getIntExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public FPExpr getFloatExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    @Override
    public SeqExpr<CharSort> getStringExpr(Context ctx) {
        Assertions.UNREACHABLE();
        return null;
    }

    private Expr<SeqSort<CharSort>> typeExpr;
    @Override
    public Expr<SeqSort<CharSort>> getTypeExpr(Context ctx) {
        if (typeExpr == null) {
            typeExpr = var.getTypeExpr(ctx);
        }
        return typeExpr;
    }

    @Override
    public boolean isConstant() {
        return var.isConstant();
    }

    @Override
    public Object getConstant() {
        if (var instanceof ConstantTypeTerm) {
            if (unaryOp instanceof IUnaryOpInstruction.Operator) {
                switch ((IUnaryOpInstruction.Operator) unaryOp) {
                    case NEG:
                        // `not <ANY_TYPE>` means True or False, so return boolean type.
                        return TypeReference.Boolean;
                    default: Assertions.UNREACHABLE();
                }
            } else if (unaryOp instanceof CAstUnaryOp) {
                switch ((CAstUnaryOp) unaryOp) {
                    default: Assertions.UNREACHABLE();
                    return null;
                }
            }
            Assertions.UNREACHABLE();
            return null;
        } else {
            return OperationManager.unaryOperation(var.getConstant(), unaryOp);
        }
    }

    @Override
    public Collection<? extends IProgramFactor> getVars() {
        return var.getVars();
    }

    @Override
    public void setSubstituted(boolean b) {
        substituted = b;
    }

    @Override
    public boolean isSubstituted() {
        return substituted;
    }

    @Override
    public ITerm substitute(ITerm toTerm, VariableTerm fromTerm) {
        ITerm newTerm = var.substitute(toTerm, fromTerm);
        UnaryOpTerm newUnaryTerm = new UnaryOpTerm(newTerm, unaryOp);
        newUnaryTerm.setSubstituted(newTerm.isSubstituted());
        return newUnaryTerm;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof VariableTerm)
            return -1;

        UnaryOpTerm otherTerm = (UnaryOpTerm) o;

        int varComparison = var.compareTo(otherTerm.var);
        if (varComparison != 0)
            return varComparison;

        if (unaryOp instanceof IUnaryOpInstruction.Operator && otherTerm.unaryOp instanceof IUnaryOpInstruction.Operator) {
            UnaryOpInstruction.Operator thisOp = (UnaryOpInstruction.Operator) unaryOp;
            UnaryOpInstruction.Operator otherOp = (UnaryOpInstruction.Operator) otherTerm.unaryOp;
            return thisOp.compareTo(otherOp);
        } else if (unaryOp instanceof CAstUnaryOp && otherTerm.unaryOp instanceof CAstUnaryOp) {
            CAstUnaryOp thisOp = (CAstUnaryOp) unaryOp;
            CAstUnaryOp otherOp = (CAstUnaryOp) otherTerm.unaryOp;
            return thisOp.compareTo(otherOp);
        } else {
            // TODO:
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryOpTerm binOpTerm = (UnaryOpTerm) o;
        return Objects.equals(var, binOpTerm.var) && Objects.equals(unaryOp, binOpTerm.unaryOp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(var, unaryOp);
    }

    @Override
    public String toString() {
        return unaryOp + " " + var;
    }
}
