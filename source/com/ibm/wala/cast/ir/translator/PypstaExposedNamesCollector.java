package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.visit.CAstVisitor;

/**
 * There is a problem in original Ariadne codes that builtin function names used in comprehension are not collected.
 * So override and define to collect them.
 */
public class PypstaExposedNamesCollector extends ExposedNamesCollector {
    @Override
    protected boolean doVisit(CAstNode n, ExposedNamesCollector.EntityContext context, CAstVisitor<ExposedNamesCollector.EntityContext> visitor) {
        switch (n.getKind()) {
            case CAstNode.COMPREHENSION_EXPR:
                visitAllChildren(n, context, visitor);
                break;
            case CAstNode.EXPR_LIST:
                visitAllChildren(n, context, visitor);
                break;
            default:
                return super.doVisit(n, context, visitor);
        }
        return true;
    }
}
