package analysis.backward.thresher.term;

import analysis.backward.thresher.OperationManager;
import analysis.backward.thresher.factor.IProgramFactor;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.shrike.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.*;
import util.AriadneSupporter;

import java.util.*;

public class BinOpTerm implements ITerm {
    private final ITerm lhs;
    private final ITerm rhs;
    private final BinaryOpInstruction.IOperator binOp;
    private final Set<IProgramFactor> vars = new TreeSet<IProgramFactor>();
    private boolean substituted = false;

    public BinOpTerm(VariableTerm lhsVar, Object rhs, IBinaryOpInstruction.IOperator binOp) {
        this.lhs = lhsVar;
        this.rhs = new ConstantTerm(rhs);
        this.binOp = binOp;
        vars.addAll(lhsVar.getVars());
    }

    public BinOpTerm(Object lhs, VariableTerm rhsVar, IBinaryOpInstruction.IOperator binOp) {
        this.lhs = new ConstantTerm(lhs);
        this.rhs = rhsVar;
        this.binOp = binOp;
        vars.addAll(rhsVar.getVars());
    }

    public BinOpTerm(VariableTerm lhs, VariableTerm rhs, IBinaryOpInstruction.IOperator binOp) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.binOp = binOp;
        vars.addAll(lhs.getVars());
        vars.addAll(rhs.getVars());
    }

    public BinOpTerm(ITerm lhs, ITerm rhs, IBinaryOpInstruction.IOperator binOp) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.binOp = binOp;
        vars.addAll(lhs.getVars());
        vars.addAll(rhs.getVars());
    }


    public ITerm getLeft() {
        return lhs;
    }

    public IBinaryOpInstruction.IOperator getOperator() {
        return binOp;
    }

    public ITerm getRight() {
        return rhs;
    }

    @Override
    public boolean hasBool() {
        if (isCompOp()) {
            return true;
        } else {
            // The result of boolean value's arithmetic calculation is integer.
            // So is you do arithmetic calculation, the result doesn't have boolean value.
            return false;
        }
    }

    @Override
    public boolean hasInt() {
        if (isCompOp()) {
            return false;
        } else if (binOp == IBinaryOpInstruction.Operator.DIV) {
            // Result of divide calculation is float
            return false;
        } else {
            return lhs.hasInt() && rhs.hasInt();
        }
    }

    @Override
    public boolean hasFloat() {
        if (isCompOp()) {
            return false;
        } else {
            return lhs.hasFloat() && rhs.hasFloat();
        }
    }

    @Override
    public boolean hasString() {
        if (isCompOp()) {
            return false;
        } else if (binOp == IBinaryOpInstruction.Operator.DIV) {
            // Result of divide calculation is float
            return false;
        } else {
            if (binOp == IBinaryOpInstruction.Operator.ADD)
                // Only add calculation has string value.
                // TODO: <STRING> * <INT> = <STRING>
                return lhs.hasString() && rhs.hasString();
            else
                return false;
        }
    }

    private Expr<?> makeArithExpr(Expr<?> lhsExpr, Expr<?> rhsExpr, Context ctx) {
        assert !isCompOp();
        Expr<?> result = null;
        switch ((IBinaryOpInstruction.Operator) binOp) {
            case ADD:
                result = ctx.mkAdd(new Expr[]{lhsExpr, rhsExpr}); break;
            case SUB:
                result = ctx.mkSub(new Expr[]{lhsExpr, rhsExpr}); break;
            case MUL:
                result = ctx.mkMul(new Expr[]{lhsExpr, rhsExpr}); break;
            default: Assertions.UNREACHABLE();
        }
        return result;
    }

    private BoolExpr makeCompExpr(Expr<?> lhsExpr, Expr<?> rhsExpr, Context ctx) {
        assert isCompOp();
        BoolExpr result = null;
        if (binOp instanceof IBinaryOpInstruction.Operator) {
            switch ((IBinaryOpInstruction.Operator) binOp) {
                case AND:
                    if (lhsExpr instanceof BoolExpr && rhsExpr instanceof BoolExpr) {
                        result = ctx.mkAnd((BoolExpr) lhsExpr, (BoolExpr) rhsExpr);
                    } else if (lhsExpr instanceof IntExpr && rhsExpr instanceof IntExpr) {
                        result = ctx.mkBool(true);
                    } else if (lhsExpr instanceof FPExpr && rhsExpr instanceof FPExpr) {
                        // TODO: How to
                        result = ctx.mkBool(true);
                    } else if (lhsExpr instanceof SeqExpr && rhsExpr instanceof SeqExpr) {
                        // TODO: How to
                        result = ctx.mkBool(true);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                    break;
                default: Assertions.UNREACHABLE();
            }
        } else if (binOp instanceof CAstBinaryOp) {
            switch ((CAstBinaryOp) binOp) {
                case EQ: result = ctx.mkEq(lhsExpr, rhsExpr); break;
                case NE: result = ctx.mkNot(ctx.mkEq(lhsExpr, rhsExpr)); break;
                case GE:
                    if (lhsExpr instanceof BoolExpr && rhsExpr instanceof BoolExpr) {
                        result = ctx.mkOr(
                                ctx.mkAnd(
                                        ctx.mkEq(lhsExpr, ctx.mkBool(false)),
                                        ctx.mkEq(rhsExpr, ctx.mkBool(false))
                                ),
                                ctx.mkAnd(
                                        ctx.mkEq(lhsExpr, ctx.mkBool(false)),
                                        ctx.mkEq(rhsExpr, ctx.mkBool(true))
                                ),
                                ctx.mkAnd(
                                        ctx.mkEq(lhsExpr, ctx.mkBool(true)),
                                        ctx.mkEq(rhsExpr, ctx.mkBool(true))
                                )
                        );
                    } else if (lhsExpr instanceof IntExpr && rhsExpr instanceof IntExpr) {
                        result = ctx.mkGe((IntExpr) lhsExpr, (IntExpr) rhsExpr);
                    } else if (lhsExpr instanceof FPExpr && rhsExpr instanceof FPExpr) {
                        result = ctx.mkFPGEq((FPExpr) lhsExpr, (FPExpr) rhsExpr);
                    } else if (lhsExpr instanceof SeqExpr && rhsExpr instanceof SeqExpr) {
                        // TODO: How to
                        result = ctx.mkBool(true);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                    break;
                case GT:
                    if (lhsExpr instanceof BoolExpr && rhsExpr instanceof BoolExpr) {
                        result = ctx.mkAnd(
                                ctx.mkEq(lhsExpr, ctx.mkBool(true)),
                                ctx.mkEq(rhsExpr, ctx.mkBool(false))
                        );
                    } else if (lhsExpr instanceof IntExpr && rhsExpr instanceof IntExpr) {
                        result = ctx.mkGt((IntExpr) lhsExpr, (IntExpr) rhsExpr);
                    } else if (lhsExpr instanceof FPExpr && rhsExpr instanceof FPExpr) {
                        result = ctx.mkFPGt((FPExpr) lhsExpr, (FPExpr) rhsExpr);
                    } else if (lhsExpr instanceof SeqExpr && rhsExpr instanceof SeqExpr) {
                        // TODO: How to
                        result = ctx.mkBool(true);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                    break;
                case LT:
                    if (lhsExpr instanceof BoolExpr && rhsExpr instanceof BoolExpr) {
                        result = ctx.mkAnd(
                                        ctx.mkEq(lhsExpr, ctx.mkBool(false)),
                                        ctx.mkEq(rhsExpr, ctx.mkBool(true))
                        );
                    } else if (lhsExpr instanceof IntExpr && rhsExpr instanceof IntExpr) {
                        result = ctx.mkLt((IntExpr) lhsExpr, (IntExpr) rhsExpr);
                    } else if (lhsExpr instanceof FPExpr && rhsExpr instanceof FPExpr) {
                        result = ctx.mkFPLt((FPExpr) lhsExpr, (FPExpr) rhsExpr);
                    } else if (lhsExpr instanceof SeqExpr && rhsExpr instanceof SeqExpr) {
                        // TODO: How to
                        result = ctx.mkBool(true);
                    } else {
                        Assertions.UNREACHABLE();
                    }
                    break;
                default: Assertions.UNREACHABLE();
            }
        }
        return result;
    }

    @Override
    public BoolExpr getBoolExpr(Context ctx) {
        // Can't create variable using z3 expression for 'None', so manually create the z3 expression.
        if (lhs == ConstantTerm.NULL && rhs == ConstantTerm.NULL) {
            // The lhs and rhs is 'None'.
            return ctx.mkTrue();
        } else if ((lhs.isConstant() && lhs != ConstantTerm.NULL && rhs == ConstantTerm.NULL)
                || (lhs == ConstantTerm.NULL && rhs.isConstant() && rhs != ConstantTerm.NULL)) {
            // If one of side is 'None' and the other is constant but not 'None', this is 'False'
            return ctx.mkFalse();
        } else if ((!lhs.isConstant() && rhs == ConstantTerm.NULL)
                || (lhs == ConstantTerm.NULL && !rhs.isConstant())) {
            // If one of side is 'None' and the other is not constant (is variable), you don't know
            // whether the return value is true or false.
            return ctx.mkBoolConst("pypsta_unknown");
        } else {
            // Lhs and rhs is not 'None'.
            assert (lhs != ConstantTerm.NULL) && (rhs != ConstantTerm.NULL);
        }

        if (isCompOp()) {
            ArrayList<BoolExpr> results = new ArrayList<>();
            if (lhs.hasBool() && rhs.hasBool())
                results.add(makeCompExpr(lhs.getBoolExpr(ctx), rhs.getBoolExpr(ctx), ctx));
            if (lhs.hasInt() && rhs.hasInt())
                results.add(makeCompExpr(lhs.getIntExpr(ctx), rhs.getIntExpr(ctx), ctx));
            if (lhs.hasFloat() && rhs.hasFloat())
                results.add(makeCompExpr(lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx), ctx));
            if (lhs.hasString() && rhs.hasString())
                results.add(makeCompExpr(lhs.getStringExpr(ctx), rhs.getStringExpr(ctx), ctx));
            BoolExpr[] array = results.toArray(new BoolExpr[0]);
            return ctx.mkAnd(array);
        } else {
            return (BoolExpr) makeArithExpr(lhs.getBoolExpr(ctx), rhs.getBoolExpr(ctx), ctx);
        }
    }

    @Override
    public IntExpr getIntExpr(Context ctx) {
        assert !isCompOp();
        return (IntExpr) makeArithExpr(lhs.getIntExpr(ctx), rhs.getIntExpr(ctx), ctx);
    }

    @Override
    public FPExpr getFloatExpr(Context ctx) {
        assert !isCompOp();
        FPExpr result = null;
        switch ((IBinaryOpInstruction.Operator) binOp) {
            case ADD:
                result = ctx.mkFPAdd(ctx.mkFPRoundNearestTiesToEven(), lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx)); break;
            case SUB:
                result = ctx.mkFPSub(ctx.mkFPRoundNearestTiesToEven(), lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx)); break;
            case MUL:
                result = ctx.mkFPMul(ctx.mkFPRoundNearestTiesToEven(), lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx)); break;
            case DIV:
                result = ctx.mkFPDiv(ctx.mkFPRoundNearestTiesToEven(), lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx)); break;
            default: Assertions.UNREACHABLE();
        }
        return result;
    }

    @Override
    public SeqExpr<CharSort> getStringExpr(Context ctx) {
        assert !isCompOp();
        SeqExpr<CharSort> result = null;
        switch ((IBinaryOpInstruction.Operator) binOp) {
            case ADD:
                // This operation makes solver too slowly, so you don't use this operation as well.
                result = ctx.mkConcat(lhs.getStringExpr(ctx), rhs.getStringExpr(ctx)); break;
            case SUB:
            case MUL:
            default: Assertions.UNREACHABLE();
        }
        return result;
    }

    private Expr<SeqSort<CharSort>> typeExpr;
    @Override
    public Expr<SeqSort<CharSort>> getTypeExpr(Context ctx) {
        if (typeExpr == null) {
            if (lhs.isConstant()) {
                typeExpr = rhs.getTypeExpr(ctx);
            } else if (rhs.isConstant()) {
                typeExpr = lhs.getTypeExpr(ctx);
            } else {
                Assertions.UNREACHABLE();
            }
        }
        return typeExpr;
    }

    @Override
    public boolean isConstant() {
        return lhs.isConstant() && rhs.isConstant();
    }

    @Override
    public Object getConstant() {
        if (lhs == ConstantTerm.TRUE || rhs == ConstantTerm.TRUE) {
            return ConstantTerm.TRUE;
        } else if (lhs == ConstantTerm.NULL) {
            if (rhs == ConstantTerm.NULL || rhs.equals(AriadneSupporter.None)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else if (rhs == ConstantTerm.NULL) {
            if (lhs.equals(AriadneSupporter.None)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } else {
            return OperationManager.operation(lhs.getConstant(), rhs.getConstant(), binOp);
        }
    }

    @Override
    public Collection<? extends IProgramFactor> getVars() {
        return vars;
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
        ITerm newLHS = lhs.substitute(toTerm, fromTerm);
        ITerm newRHS = rhs.substitute(toTerm, fromTerm);
        BinOpTerm newPathTerm = new BinOpTerm(newLHS, newRHS, binOp);
        newPathTerm.setSubstituted(newLHS.isSubstituted() || newRHS.isSubstituted());
        return newPathTerm;
    }

    private boolean isCompOp() {
        return binOp == CAstBinaryOp.EQ
                || binOp == CAstBinaryOp.NE
                || binOp == CAstBinaryOp.GE
                || binOp == CAstBinaryOp.GT
                || binOp == CAstBinaryOp.LE
                || binOp == CAstBinaryOp.LT
                || binOp == IBinaryOpInstruction.Operator.AND
                || binOp == IBinaryOpInstruction.Operator.OR
                || binOp == IBinaryOpInstruction.Operator.XOR;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof VariableTerm)
            return -1;

        BinOpTerm otherTerm = (BinOpTerm) o;

        int lhsComparison = lhs.compareTo(otherTerm.lhs);
        if (lhsComparison != 0)
            return lhsComparison;

        int rhsComparison = rhs.compareTo(otherTerm.rhs);
        if (rhsComparison != 0)
            return rhsComparison;

        if (binOp instanceof IBinaryOpInstruction.Operator && otherTerm.binOp instanceof IBinaryOpInstruction.Operator) {
            BinaryOpInstruction.Operator thisOp = (BinaryOpInstruction.Operator) binOp;
            BinaryOpInstruction.Operator otherOp = (BinaryOpInstruction.Operator) otherTerm.binOp;
            return thisOp.compareTo(otherOp);
        } else if (binOp instanceof CAstBinaryOp && otherTerm.binOp instanceof CAstBinaryOp) {
            CAstBinaryOp thisOp = (CAstBinaryOp) binOp;
            CAstBinaryOp otherOp = (CAstBinaryOp) otherTerm.binOp;
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
        BinOpTerm binOpTerm = (BinOpTerm) o;
        return Objects.equals(lhs, binOpTerm.lhs) && Objects.equals(rhs, binOpTerm.rhs) && Objects.equals(binOp, binOpTerm.binOp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, binOp);
    }

    @Override
    public String toString() {
        return lhs + " " + binOp + " " + rhs;
    }
}
