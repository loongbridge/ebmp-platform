package com.ebmp.crm;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.erp.SalesOrder;
import com.ebmp.erp.SalesOrderItem;
import com.ebmp.erp.SalesOrderRepository;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.system.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
public class CrmController {

    private final CrmService crmService;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final UserRepository userRepository;

    public record WinRequest(List<SalesOrderItem> items) {
    }

    @GetMapping("/leads")
    public ApiResponse<List<Lead>> leads() {
        List<Lead> list = leadRepository.findAllByOrderByIdDesc();
        list.forEach(crmService::enrichLead);
        return ApiResponse.ok(list);
    }

    @PostMapping("/leads")
    public ApiResponse<Lead> createLead(@RequestBody Lead body) {
        return ApiResponse.ok(crmService.createLead(body));
    }

    @PutMapping("/leads/{id}")
    public ApiResponse<Lead> updateLead(@PathVariable Long id, @RequestBody Lead body) {
        Lead lead = leadRepository.findById(id).orElseThrow(() -> new BizException("线索不存在"));
        lead.setName(body.getName());
        lead.setCompany(body.getCompany());
        lead.setContactName(body.getContactName());
        lead.setPhone(body.getPhone());
        lead.setEmail(body.getEmail());
        lead.setSource(body.getSource());
        lead.setStatus(body.getStatus());
        lead.setOwnerId(body.getOwnerId());
        lead.setNextFollowAt(body.getNextFollowAt());
        lead.setRemark(body.getRemark());
        return ApiResponse.ok(leadRepository.save(lead));
    }

    @PostMapping("/leads/{id}/convert")
    public ApiResponse<Opportunity> convert(@PathVariable Long id) {
        return ApiResponse.ok(crmService.convert(id));
    }

    @DeleteMapping("/leads/{id}")
    public ApiResponse<Void> deleteLead(@PathVariable Long id) {
        leadRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/opportunities")
    public ApiResponse<List<Opportunity>> opportunities() {
        List<Opportunity> list = opportunityRepository.findAllByOrderByIdDesc();
        list.forEach(o -> {
            crmService.enrichOpportunity(o);
            o.setOrderCount(salesOrderRepository.countByOpportunityId(o.getId()));
        });
        return ApiResponse.ok(list);
    }

    @GetMapping("/opportunities/pipeline")
    public ApiResponse<List<Map<String, Object>>> pipeline() {
        String[][] stages = {
                {"INITIAL", "初步接洽"}, {"NEEDS", "需求确认"}, {"SOLUTION", "方案报价"},
                {"QUOTE", "商务谈判"}, {"WON", "赢单"}, {"LOST", "输单"}};
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] stage : stages) {
            List<Opportunity> list = opportunityRepository.findAll().stream()
                    .filter(o -> stage[0].equals(o.getStage())).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", stage[0]);
            item.put("stageName", stage[1]);
            item.put("count", list.size());
            item.put("amount", list.stream().map(Opportunity::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            item.put("opportunities", list);
            result.add(item);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/opportunities")
    public ApiResponse<Opportunity> createOpportunity(@RequestBody Opportunity body) {
        body.setId(null);
        body.setOppNo("OP" + System.currentTimeMillis());
        if (body.getStatus() == null) {
            body.setStatus("OPEN");
        }
        if (body.getStage() == null) {
            body.setStage("INITIAL");
        }
        if (body.getOwnerId() == null) {
            body.setOwnerId(com.ebmp.common.SecurityUtils.currentUserId());
        }
        return ApiResponse.ok(opportunityRepository.save(body));
    }

    @PutMapping("/opportunities/{id}")
    public ApiResponse<Opportunity> updateOpportunity(@PathVariable Long id, @RequestBody Opportunity body) {
        Opportunity opp = opportunityRepository.findById(id).orElseThrow(() -> new BizException("商机不存在"));
        opp.setName(body.getName());
        opp.setCustomerId(body.getCustomerId());
        opp.setStage(body.getStage());
        opp.setAmount(body.getAmount());
        opp.setProbability(body.getProbability());
        opp.setExpectedCloseDate(body.getExpectedCloseDate());
        opp.setOwnerId(body.getOwnerId());
        opp.setStatus(body.getStatus());
        opp.setRemark(body.getRemark());
        return ApiResponse.ok(opportunityRepository.save(opp));
    }

    @PostMapping("/opportunities/{id}/win")
    public ApiResponse<SalesOrder> win(@PathVariable Long id, @RequestBody(required = false) WinRequest request) {
        List<SalesOrderItem> items = request == null ? null : request.items();
        if (items == null || items.isEmpty()) {
            throw new BizException("请至少选择一项商品后赢单");
        }
        return ApiResponse.ok(crmService.winAndCreateOrder(id, items));
    }

    @DeleteMapping("/opportunities/{id}")
    public ApiResponse<Void> deleteOpportunity(@PathVariable Long id) {
        opportunityRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/activities")
    public ApiResponse<List<Activity>> activities() {
        List<Activity> list = activityRepository.findAllByOrderByIdDesc();
        list.forEach(this::enrichActivity);
        return ApiResponse.ok(list);
    }

    @GetMapping("/activities/{bizType}/{bizId}")
    public ApiResponse<List<Activity>> activitiesByBiz(@PathVariable String bizType, @PathVariable Long bizId) {
        List<Activity> list = activityRepository.findByBizTypeAndBizIdOrderByIdDesc(bizType, bizId);
        list.forEach(this::enrichActivity);
        return ApiResponse.ok(list);
    }

    @PostMapping("/activities")
    public ApiResponse<Activity> createActivity(@RequestBody Activity body) {
        return ApiResponse.ok(crmService.createActivity(body));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteActivity(@PathVariable Long id) {
        activityRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        List<Opportunity> all = opportunityRepository.findAll();
        java.math.BigDecimal openAmount = all.stream().filter(o -> "OPEN".equals(o.getStatus()))
                .map(Opportunity::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal wonAmount = all.stream().filter(o -> "WON".equals(o.getStatus()))
                .map(Opportunity::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long won = all.stream().filter(o -> "WON".equals(o.getStatus())).count();
        long lost = all.stream().filter(o -> "LOST".equals(o.getStatus())).count();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("leadCount", leadRepository.count());
        map.put("newLeadCount", leadRepository.countByStatus("NEW"));
        map.put("customerCount", customerRepository.count());
        map.put("opportunityCount", all.size());
        map.put("openAmount", openAmount);
        map.put("wonAmount", wonAmount);
        map.put("winRate", won + lost == 0 ? 0 : Math.round(won * 100.0 / (won + lost)));
        map.put("activityCount", activityRepository.count());
        return ApiResponse.ok(map);
    }

    private void enrichActivity(Activity a) {
        if (a.getOwnerId() != null) {
            userRepository.findById(a.getOwnerId()).ifPresent(u -> a.setOwnerName(u.getRealName()));
        }
        if ("OPPORTUNITY".equals(a.getBizType())) {
            opportunityRepository.findById(a.getBizId()).ifPresent(o -> a.setBizName(o.getName()));
        } else if ("CUSTOMER".equals(a.getBizType())) {
            customerRepository.findById(a.getBizId()).ifPresent(c -> a.setBizName(c.getName()));
        } else if ("LEAD".equals(a.getBizType())) {
            leadRepository.findById(a.getBizId()).ifPresent(l -> a.setBizName(l.getName()));
        }
    }
}
