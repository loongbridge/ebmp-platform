package com.ebmp.srm;

import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequisitionApprovalHandler implements ApprovalHandler {

    private final SrmService srmService;

    @Override
    public String bizType() {
        return "PURCHASE_REQ";
    }

    @Override
    public void onApproved(Approval approval) {
        srmService.updateRequisitionStatus(approval, true);
    }

    @Override
    public void onRejected(Approval approval) {
        srmService.updateRequisitionStatus(approval, false);
    }
}
