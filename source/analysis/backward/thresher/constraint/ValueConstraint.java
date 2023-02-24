package analysis.backward.thresher.constraint;

import analysis.backward.thresher.OperationManager;
import analysis.backward.thresher.factor.VariableFactor;
import analysis.backward.thresher.term.*;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.types.TypeReference;
import com.microsoft.z3.*;
import edu.colorado.thresher.core.*;
import util.AriadneSupporter;
import util.ConstantConverter;

import java.util.ArrayList;
import java.util.Set;

public class ValueConstraint extends AbstractConstraint implements IConstraint {
    protected final IConditionalBranchInstruction.Operator op;

    public ValueConstraint(VariableFactor lhs, Object rhs, IConditionalBranchInstruction.Operator op) {
        this(new VariableTerm(lhs), new ConstantTerm(rhs), op);
    }

    public ValueConstraint(ITerm lhs, ITerm rhs, IConditionalBranchInstruction.Operator op) {
        super(lhs, rhs);
        this.op = op;
    }

    private ValueConstraint(ITerm lhs, ITerm rhs, IConditionalBranchInstruction.Operator op, int id) {
        super(lhs, rhs, id);
        this.op = op;
    }

    @Override
    public AST toZ3AST(Context ctx) {
        ArrayList<BoolExpr> results = new ArrayList<>();
        if (lhs.hasBool() && rhs.hasBool())
            results.add(ctx.mkEq(lhs.getBoolExpr(ctx), rhs.getBoolExpr(ctx)));
        if (lhs.hasInt() && rhs.hasInt())
            results.add(ctx.mkEq(lhs.getIntExpr(ctx), rhs.getIntExpr(ctx)));
        if (lhs.hasFloat() && rhs.hasFloat())
            results.add(ctx.mkEq(lhs.getFloatExpr(ctx), rhs.getFloatExpr(ctx)));
        if (lhs.hasString() && rhs.hasString())
            results.add(ctx.mkEq(lhs.getStringExpr(ctx), rhs.getStringExpr(ctx)));

        BoolExpr[] array = results.toArray(new BoolExpr[0]);
        return ctx.mkAnd(array);
    }

    @Override
    public boolean isConstant() {
        return lhs.isConstant() && rhs.isConstant();
    }

    @Override
    public Set<IConstraint> substitute(ITerm toTerm, VariableTerm fromTerm) {
        if (toTerm instanceof ConstantTypeTerm)
            // This means the concrete value of 'toTerm' is unknown. Value constraints which use
            // this term can not be evaluated, so assume this constraint is TRUE to soundness.
            return AbstractConstraint.TRUE;

        ITerm newLHS = lhs.substitute(toTerm, fromTerm);
        // DO NOT MOVE THIS! if lhs and rhs of the same, mutability of PathTerms can cause unexpected behavior.
        boolean lhsSubstituted = newLHS.isSubstituted();

        ITerm newRHS = rhs.substitute(toTerm, fromTerm);
        boolean rhsSubstituted = newRHS.isSubstituted();

        if (lhsSubstituted || rhsSubstituted) {
            IConstraint newPathConstraint = new ValueConstraint(newLHS, newRHS, op, id);
            if (newPathConstraint.isConstant()) {
                Util.Debug("evaluating!");
                if (newPathConstraint.manualEvaluation())
                    return AbstractConstraint.TRUE; // constraint evaluated to true
                else
                    return AbstractConstraint.FALSE; // constraint evaluated to false
            } else {
                newPathConstraint = simplify((ValueConstraint) newPathConstraint);
            }
            newPathConstraint.setSubstituted(true);
            return makeSet(newPathConstraint);
        } else {
            this.setSubstituted(false);
            return makeSet(this);
        }
    }

    private static IConstraint simplify(ValueConstraint oldConstraint) {
        IConstraint newConstraint = oldConstraint;

        if (oldConstraint.getLhs() instanceof BinOpTerm) {
            BinOpTerm lhsBinTerm = (BinOpTerm) oldConstraint.getLhs();
            ValueConstraint newValueConstraint = oldConstraint;

            // The value constraint whose rhs is 0 and operation is EQ or NE, delete rhs.
            if (oldConstraint.op.equals(IConditionalBranchInstruction.Operator.EQ)
                    && oldConstraint.getRhs().getConstant().equals(Integer.valueOf(0))) {
                if (lhsBinTerm.getOperator().equals(CAstBinaryOp.EQ)) {
                    // Simplify '<L-LHS EQ R-LHS> EQ 0' to 'L-LHS NE R-LHS'
                    newValueConstraint = new ValueConstraint(
                            lhsBinTerm.getLeft(),
                            lhsBinTerm.getRight(),
                            IConditionalBranchInstruction.Operator.NE
                    );
                } else if (lhsBinTerm.getOperator().equals(CAstBinaryOp.NE)) {
                    // Simplify '<L-LHS NE R-LHS> EQ 0' to 'L-LHS EQ R-LHS'
                    newValueConstraint = new ValueConstraint(
                            lhsBinTerm.getLeft(),
                            lhsBinTerm.getRight(),
                            IConditionalBranchInstruction.Operator.EQ
                    );
                }
            } else if (oldConstraint.op.equals(IConditionalBranchInstruction.Operator.NE)
                    && oldConstraint.getRhs().getConstant().equals(Integer.valueOf(0))) {
                if (lhsBinTerm.getOperator().equals(CAstBinaryOp.EQ)) {
                    // Simplify '<L-LHS EQ R-LHS> NE 0' to 'L-LHS EQ R-LHS'
                    newValueConstraint = new ValueConstraint(
                            lhsBinTerm.getLeft(),
                            lhsBinTerm.getRight(),
                            IConditionalBranchInstruction.Operator.EQ
                    );
                } else if (lhsBinTerm.getOperator().equals(CAstBinaryOp.NE)) {
                    // Simplify '<L-LHS NE R-LHS> NE 0' to 'L-LHS NE R-LHS'
                    newValueConstraint = new ValueConstraint(
                            lhsBinTerm.getLeft(),
                            lhsBinTerm.getRight(),
                            IConditionalBranchInstruction.Operator.NE
                    );
                }
            }

            // Use 'TypeConstraint' is rhs is 'NULL'.
            if (newValueConstraint.op.equals(IConditionalBranchInstruction.Operator.EQ)
                    && newValueConstraint.getRhs() == ConstantTerm.NULL) {
                newConstraint = new TypeConstraint(
                        newValueConstraint.getLhs(), new ConstantTypeTerm(AriadneSupporter.None)
                );
            } else if (newValueConstraint.op.equals(IConditionalBranchInstruction.Operator.NE)
                    && newValueConstraint.getRhs() == ConstantTerm.NULL) {
                newConstraint = new TypeConstraint(
                        newValueConstraint.getLhs(),
                        new ConstantTypeTerm(
                                TypeReference.findOrCreate(PythonTypes.pythonLoader,"LNotNone")
                        )
                );
            }
        }

        return newConstraint;
    }

    @Override
    public boolean manualEvaluation() {
        Util.Assert(isConstant());

        if (lhs == ConstantTerm.TRUE || rhs == ConstantTerm.TRUE)
            return true;

        Object lhsConstant = lhs.getConstant(), rhsConstant = rhs.getConstant();
        if (lhsConstant == ConstantTerm.TRUE || rhsConstant == ConstantTerm.TRUE)
            return true;

        Object result = OperationManager.operation(lhsConstant, rhsConstant, op);
        if (result == null) {
            // If you can't calculate, this path is infeasible.
            return false;
        } else if (result == ConstantTerm.TRUE) {
            // When you don't know whether result is true or false, so return true to over-approximate.
            return true;
        } else if (result.equals(TypeReference.Boolean)) {
            // When you don't know whether result is true or false, so return true to over-approximate.
            return true;
        } else {
            return ConstantConverter.toBoolValue(result);
        }
    }

    @Override
    public String toString() {
        if (this == AbstractConstraint.TRUE)
            return "TRUE";
        else if (this == AbstractConstraint.FALSE)
            return "FALSE";
        else
            return lhs + " " + op + " " + rhs;
    }
}
