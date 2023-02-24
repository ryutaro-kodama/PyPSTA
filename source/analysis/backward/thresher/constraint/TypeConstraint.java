package analysis.backward.thresher.constraint;

import analysis.backward.thresher.factor.StringFieldVarFactor;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import edu.colorado.thresher.core.PypstaSymbolicExecutor;
import analysis.backward.thresher.factor.IProgramFactor;
import analysis.backward.thresher.factor.VariableFactor;
import analysis.backward.thresher.term.*;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.microsoft.z3.AST;
import com.microsoft.z3.Context;
import edu.colorado.thresher.core.Util;
import util.AriadneSupporter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeConstraint extends AbstractConstraint implements IConstraint {
    public TypeConstraint(VariableFactor lhsVar, VariableFactor rhsVar) {
        this(new VariableTerm(lhsVar), new VariableTerm(rhsVar));
    }

    public TypeConstraint(VariableFactor lhsVar, TypeReference rhsType) {
        this(new VariableTerm(lhsVar), new ConstantTypeTerm(rhsType));
    }

    public TypeConstraint(ITerm lhs, ITerm rhs) {
        super(lhs, rhs);
    }

    private TypeConstraint(ITerm lhs, ITerm rhs, int id) {
        super(lhs, rhs, id);
    }

    private boolean isLhsConstant() {
        return lhs instanceof ConstantTypeTerm;
    }

    private boolean isRhsConstant() {
        return rhs instanceof ConstantTypeTerm;
    }

    @Override
    public AST toZ3AST(Context ctx) {
        if (((lhs instanceof ConstantTypeTerm)
                    && (((ConstantTypeTerm) lhs).getConstant().equals(PypstaSymbolicExecutor.top)))
                || ((rhs instanceof ConstantTypeTerm)
                        && (((ConstantTypeTerm) rhs).getConstant().equals(PypstaSymbolicExecutor.top)))) {
            return ctx.mkBool(true);
        }

        List<BoolExpr> exprs = new ArrayList<>();
        for (Expr lhsTypeExpr: getAllTypeExpr(ctx, lhs)) {
            for (Expr rhsTypeExpr: getAllTypeExpr(ctx, rhs)) {
                exprs.add(ctx.mkEq(lhsTypeExpr, rhsTypeExpr));
            }
        }
        return ctx.mkAnd(exprs.toArray(new BoolExpr[0]));
    }

    private Set<Expr> getAllTypeExpr(Context ctx, ITerm term) {
        Set<Expr> result = new HashSet<>();
        if (term instanceof BinOpTerm) {
            BinOpTerm binOpTerm = (BinOpTerm) term;
            if (binOpTerm.getLeft() instanceof BinOpTerm) {
                result.addAll(getAllTypeExpr(ctx, binOpTerm.getLeft()));
            } else {
                result.add(binOpTerm.getLeft().getTypeExpr(ctx));
            }
            if (binOpTerm.getRight() instanceof BinOpTerm) {
                result.addAll(getAllTypeExpr(ctx, binOpTerm.getRight()));
            } else {
                result.add(binOpTerm.getRight().getTypeExpr(ctx));
            }
        } else {
            result.add(term.getTypeExpr(ctx));
        }
        return result;
    }

    @Override
    public boolean isConstant() {
        return lhs.isConstant() && rhs.isConstant();
    }

    @Override
    public Set<IConstraint> substitute(ITerm toTerm, VariableTerm fromTerm) {
        ITerm newLHS = lhs.substitute(toTerm, fromTerm);
        // DO NOT MOVE THIS! if lhs and rhs of the same, mutability of PathTerms can cause unexpected behavior.
        boolean lhsSubstituted = newLHS.isSubstituted();

        ITerm newRHS = rhs.substitute(toTerm, fromTerm);
        boolean rhsSubstituted = newRHS.isSubstituted();

        if (lhsSubstituted || rhsSubstituted) {
            Set<IConstraint> newConstraints = new HashSet<>();
            if (newRHS instanceof BinOpTerm) {
                for (IProgramFactor lhsVar: newLHS.getVars()) {
                    for (IProgramFactor rhsVar: newRHS.getVars()) {
                        IConstraint newConstraint
                                = new TypeConstraint(new VariableTerm(lhsVar), new VariableTerm(rhsVar));
                        newConstraint.setSubstituted(true);
                        newConstraints.add(newConstraint);
                    }
                }
            } else {
                IConstraint newConstraint = new TypeConstraint(newLHS, newRHS, id);
                newConstraint.setSubstituted(true);
                newConstraints.add(newConstraint);
            }

            Set<IConstraint> result = new HashSet<>();
            for (IConstraint newPathConstraint: newConstraints) {
                if (newPathConstraint.isConstant()) {
                    Util.Debug("evaluating!");
                    if (newPathConstraint.manualEvaluation()) {
                        // TRUE constraint is ignored.
                        continue;
                    } else {
                        return AbstractConstraint.FALSE; // constraint evaluated to false
                    }
                } else {
                    result.add(simplify(newPathConstraint));
                }
            }

            if (result.size() == 0) {
                // If all constraints are ignored
                return AbstractConstraint.TRUE;
            } else {
                return result;
            }
        } else {
            this.setSubstituted(false);
            return makeSet(this);
        }
    }

    private static IConstraint simplify(IConstraint constraint) {
        if (!(constraint instanceof TypeConstraint)) return constraint;
        TypeConstraint constraint1 = (TypeConstraint) constraint;

        if (!constraint1.isRhsConstant()) return constraint;

        ITerm lhs = constraint1.lhs;
        ITerm rhs = constraint1.rhs;
        if (lhs instanceof VariableTerm
                && ((VariableTerm) lhs).getVar() instanceof StringFieldVarFactor
                && ((StringFieldVarFactor) ((VariableTerm) lhs).getVar()).getValFactor()
                        instanceof VariableFactor
                && rhs instanceof ConstantTypeTerm
                && ((ConstantTypeTerm) rhs).getConstant().getName().getPackage() != null
                && ((ConstantTypeTerm) rhs).getConstant().getName().getPackage().toString().startsWith("$")
                && ((StringFieldVarFactor) ((VariableTerm) lhs).getVar()).getFieldName().equals(
                ((ConstantTypeTerm) rhs).getConstant().getName().getClassName().toString())) {
            // Manual simplify. From type of instance's method constraint, create type constraint for instance.
            // 'OBJ.method == TYPE<~.method>'  =>  'OBJ == TYPE<~>'
            VariableFactor objVar = (VariableFactor)
                    ((StringFieldVarFactor) ((VariableTerm) lhs).getVar()).getValFactor();
            String objMustType = ((ConstantTypeTerm) rhs).getConstant().getName().getPackage().toString();

            // Delete '$' from type reference.
            objMustType = objMustType.substring(1);

            constraint1 = new TypeConstraint(
                    objVar, TypeReference.find(PythonTypes.pythonLoader,"L" + objMustType + "_instance")
            );
            constraint1.setSubstituted(true);
            constraint = constraint1;
        }

        if (lhs instanceof BinOpTerm) {
            BinOpTerm binOpTerm = (BinOpTerm) lhs;

            // If the binary operation's part is constant, simplify this constraint.
            if (!(binOpTerm.getOperator() instanceof IBinaryOpInstruction.Operator))
            if (binOpTerm.getLeft().isConstant()) {
                Object constant = binOpTerm.getLeft().getConstant();
                TypeReference typeRef = AriadneSupporter.toTypeRef(constant);
                ConstantTypeTerm constantTypeTerm = new ConstantTypeTerm(typeRef);
                if (typeRef.equals(constantTypeTerm))
                    constraint = new TypeConstraint(binOpTerm.getRight(), constantTypeTerm, constraint1.id);
            } else if (binOpTerm.getRight().isConstant()) {
                Object constant = binOpTerm.getRight().getConstant();
                TypeReference typeRef = AriadneSupporter.toTypeRef(constant);
                ConstantTypeTerm constantTypeTerm = new ConstantTypeTerm(typeRef);
                if (typeRef.equals(constantTypeTerm))
                    constraint = new TypeConstraint(binOpTerm.getLeft(), constantTypeTerm, constraint1.id);
            }
        }

        return constraint;
    }

    @Override
    public boolean manualEvaluation() {
        assert lhs.isConstant() && rhs.isConstant();

        if (lhs == ConstantTerm.TRUE || rhs == ConstantTerm.TRUE)
            return true;

        TypeReference lhsTypeRef =
                (lhs == ConstantTerm.NULL) ?
                        TypeReference.find(PythonTypes.pythonLoader, "LNone") :
                        AriadneSupporter.toTypeRef(lhs.getConstant());
        TypeReference rhsTypeRef =
                (rhs == ConstantTerm.NULL) ?
                        TypeReference.find(PythonTypes.pythonLoader, "LNone") :
                        AriadneSupporter.toTypeRef(rhs.getConstant());
        if (lhsTypeRef == null || rhsTypeRef == null)
            Assertions.UNREACHABLE();

        return lhsTypeRef.equals(rhsTypeRef);
    }

    @Override
    public String toString() {
        return lhs + " == " + rhs;
    }
}
