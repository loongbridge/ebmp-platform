package com.ebmp.contract;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalHandler;
import com.ebmp.workflow.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ContractService implements ApprovalHandler {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;

    @Transactional
    public Contract create(Contract body) {
        body.setId(null);
        body.setContractNo("CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setOwnerId(SecurityUtils.currentUserId());
        body.setStatus("DRAFT");
        return contractRepository.save(body);
    }

    @Transactional
    public Contract submit(Long id) {
        Contract contract = contractRepository.findById(id).orElseThrow(() -> new BizException("合同不存在"));
        if (!"DRAFT".equals(contract.getStatus()) && !"REJECTED".equals(contract.getStatus())) {
            throw new BizException("当前状态不可提交审批");
        }
        contract.setStatus("PENDING");
        contractRepository.save(contract);
        String module = "CUSTOMER".equals(contract.getType()) ? "CRM" : "SRM";
        Approval approval = approvalService.submit(module, "CONTRACT", contract.getId(), contract.getContractNo(),
                "合同审批 - " + contract.getTitle(), contract.getAmount(), contract.getOwnerId(), contract.getRemark());
        contract.setApprovalId(approval.getId());
        return contractRepository.save(contract);
    }

    public void enrich(Contract contract) {
        if ("CUSTOMER".equals(contract.getType())) {
            customerRepository.findById(contract.getPartyId()).ifPresent(c -> contract.setPartyName(c.getName()));
        } else {
            supplierRepository.findById(contract.getPartyId()).ifPresent(s -> contract.setPartyName(s.getName()));
        }
        if (contract.getOwnerId() != null) {
            userRepository.findById(contract.getOwnerId()).ifPresent(u -> contract.setOwnerName(u.getRealName()));
        }
    }

    @Override
    public String bizType() {
        return "CONTRACT";
    }

    @Override
    public void onApproved(Approval approval) {
        contractRepository.findById(approval.getBizId()).ifPresent(c -> {
            c.setStatus("ACTIVE");
            c.setSignedAt(LocalDateTime.now());
            contractRepository.save(c);
        });
    }

    @Override
    public void onRejected(Approval approval) {
        contractRepository.findById(approval.getBizId()).ifPresent(c -> {
            c.setStatus("REJECTED");
            contractRepository.save(c);
        });
    }
}
