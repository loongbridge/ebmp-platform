package com.ebmp.srm;

import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseOrderApprovalHandler implements ApprovalHandler {

    private final SrmService srmService;

    @Override
    public String bizType() {
        return "PURCHASE_ORDER";
    }

    @Override
    public void onApproved(Approval approval) {
        srmService.updateOrderStatus(approval, true);
    }

    @Override
    public void onRejected(Approval approval) {
        srmService.updateOrderStatus(approval, false);
    }
}
