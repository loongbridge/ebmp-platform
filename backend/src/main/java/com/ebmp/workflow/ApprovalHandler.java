package com.ebmp.workflow;

public interface ApprovalHandler {
    String bizType();

    void onApproved(Approval approval);

    void onRejected(Approval approval);
}
