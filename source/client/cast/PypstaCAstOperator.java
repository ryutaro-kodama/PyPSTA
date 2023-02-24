package client.cast;

import com.ibm.wala.cast.tree.impl.CAstOperator;

public class PypstaCAstOperator extends CAstOperator {
    protected PypstaCAstOperator(String op) {
        super(op);
    }

    public static final CAstOperator OP_FDIV = new PypstaCAstOperator("//");
}
