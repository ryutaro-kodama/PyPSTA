package client.operator;

import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;

public enum PypstaBinaryOperator implements IBinaryOpInstruction.IOperator {
    FDIV;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}