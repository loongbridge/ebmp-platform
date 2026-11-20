package com.ebmp.crm;

import com.ebmp.common.BizException;
import com.ebmp.common.SecurityUtils;
import com.ebmp.erp.SalesOrder;
import com.ebmp.erp.SalesOrderService;
import com.ebmp.masterdata.Customer;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SalesOrderService salesOrderService;

    @Transactional
    public Lead createLead(Lead body) {
        body.setId(null);
        body.setLeadNo("LD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        body.setStatus("NEW");
        if (body.getOwnerId() == null) {
            body.setOwnerId(SecurityUtils.currentUserId());
        }
        return leadRepository.save(body);
    }

    /** 线索转化为客户 + 商机，打通 CRM 内部闭环 */
    @Transactional
    public Opportunity convert(Long leadId) {
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new BizException("线索不存在"));
        if ("CONVERTED".equals(lead.getStatus())) {
            throw new BizException("该线索已转化");
        }
        Customer customer = new Customer();
        customer.setCode("CUS" + System.currentTimeMillis());
        customer.setName(lead.getCompany() == null || lead.getCompany().isBlank() ? lead.getName() : lead.getCompany());
        customer.setSource(lead.getSource());
        customer.setOwnerId(lead.getOwnerId());
        customer.setPhone(lead.getPhone());
        customer.setEmail(lead.getEmail());
        customer.setLastContactAt(LocalDateTime.now());
        customer.setLevel("B");
        if (lead.getCompany() != null && !lead.getCompany().isBlank()) {
            customer.setShortName(lead.getCompany());
        }
        customer = customerRepository.save(customer);

        Opportunity opp = new Opportunity();
        opp.setOppNo("OP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        opp.setName(customer.getName() + " - 商机");
        opp.setCustomerId(customer.getId());
        opp.setLeadId(lead.getId());
        opp.setStage("INITIAL");
        opp.setStatus("OPEN");
        opp.setProbability(20);
        opp.setAmount(BigDecimal.ZERO);
        opp.setOwnerId(lead.getOwnerId());
        opp = opportunityRepository.save(opp);

        lead.setStatus("CONVERTED");
        lead.setCustomerId(customer.getId());
        lead.setOpportunityId(opp.getId());
        leadRepository.save(lead);
        return opp;
    }

    /** 商机赢单，生成销售订单草稿 —— CRM 与进销存的关键打通点 */
    @Transactional
    public SalesOrder winAndCreateOrder(Long opportunityId, List<com.ebmp.erp.SalesOrderItem> items) {
        Opportunity opp = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new BizException("商机不存在"));
        if ("WON".equals(opp.getStatus())) {
            throw new BizException("该商机已赢单");
        }
        opp.setStatus("WON");
        opp.setStage("WON");
        opp.setProbability(100);
        opp.setWonAt(LocalDateTime.now());
        opportunityRepository.save(opp);

        SalesOrder order = new SalesOrder();
        order.setCustomerId(opp.getCustomerId());
        order.setOpportunityId(opp.getId());
        order.setRemark("由商机 " + opp.getOppNo() + " 赢单生成");
        order.setItems(items);
        return salesOrderService.create(order);
    }

    public Activity createActivity(Activity body) {
        body.setId(null);
        if (body.getOwnerId() == null) {
            body.setOwnerId(SecurityUtils.currentUserId());
        }
        if (body.getActivityTime() == null) {
            body.setActivityTime(LocalDateTime.now());
        }
        return activityRepository.save(body);
    }

    public void enrichLead(Lead lead) {
        if (lead.getOwnerId() != null) {
            userRepository.findById(lead.getOwnerId()).ifPresent(u -> lead.setOwnerName(u.getRealName()));
        }
    }

    public void enrichOpportunity(Opportunity opp) {
        customerRepository.findById(opp.getCustomerId()).ifPresent(c -> opp.setCustomerName(c.getName()));
        if (opp.getOwnerId() != null) {
            userRepository.findById(opp.getOwnerId()).ifPresent(u -> opp.setOwnerName(u.getRealName()));
        }
        opp.setActivityCount(activityRepository.countByBizTypeAndBizId("OPPORTUNITY", opp.getId()));
    }
}
