package com.ebmp.dashboard;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.SecurityUtils;
import com.ebmp.crm.Activity;
import com.ebmp.crm.ActivityRepository;
import com.ebmp.crm.LeadRepository;
import com.ebmp.crm.Opportunity;
import com.ebmp.crm.OpportunityRepository;
import com.ebmp.erp.SalesOrder;
import com.ebmp.erp.SalesOrderRepository;
import com.ebmp.erp.Stock;
import com.ebmp.erp.StockService;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.oa.Notice;
import com.ebmp.oa.NoticeRepository;
import com.ebmp.oa.OaTaskRepository;
import com.ebmp.srm.PurchaseOrderRepository;
import com.ebmp.srm.PurchaseRequisitionRepository;
import com.ebmp.system.UserRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalRepository;
import com.ebmp.workflow.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final OaTaskRepository taskRepository;
    private final NoticeRepository noticeRepository;
    private final StockService stockService;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalService approvalService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        List<SalesOrder> orders = salesOrderRepository.findAll();
        List<Opportunity> opportunities = opportunityRepository.findAll();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        BigDecimal monthSales = orders.stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .filter(o -> o.getOrderDate() != null && !o.getOrderDate().isBefore(monthStart))
                .map(SalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long monthOrderCount = orders.stream()
                .filter(o -> o.getOrderDate() != null && !o.getOrderDate().isBefore(monthStart))
                .count();
        BigDecimal pipeline = opportunities.stream().filter(o -> "OPEN".equals(o.getStatus()))
                .map(Opportunity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long won = opportunities.stream().filter(o -> "WON".equals(o.getStatus())).count();
        long lost = opportunities.stream().filter(o -> "LOST".equals(o.getStatus())).count();
        long winRate = won + lost == 0 ? 0 : Math.round(won * 100.0 / (won + lost));

        List<Stock> alertStocks = stockService.allWithMaterial().stream()
                .filter(s -> Boolean.TRUE.equals(s.getAlert()))
                .sorted(Comparator.comparing(Stock::getQuantity))
                .toList();
        BigDecimal stockValue = stockService.allWithMaterial().stream()
                .map(s -> (s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity())
                        .multiply(s.getCostPrice() == null ? BigDecimal.ZERO : s.getCostPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("monthSales", monthSales);
        kpi.put("monthOrders", monthOrderCount);
        kpi.put("customers", customerRepository.count());
        kpi.put("suppliers", supplierRepository.count());
        kpi.put("inventoryValue", stockValue);
        kpi.put("stockAlerts", alertStocks.size());
        kpi.put("pendingApprovals", approvalService.pendingCount(SecurityUtils.currentUserId()));
        kpi.put("crmPipeline", pipeline);
        kpi.put("winRate", winRate);
        kpi.put("employees", userRepository.countByStatus(1));
        kpi.put("newLeads", leadRepository.countByStatus("NEW"));
        kpi.put("purchasePending", purchaseOrderRepository.countByStatusIn(List.of("PENDING", "APPROVED", "PARTIALLY_RECEIVED")));
        kpi.put("requisitionPending", requisitionRepository.countByStatus("PENDING"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpi", kpi);
        result.put("salesTrend", trend(orders, 6));
        result.put("purchaseTrend", purchaseTrend(6));
        result.put("funnel", funnel(opportunities));
        result.put("recentApprovals", recentApprovals());
        result.put("notices", notices());
        result.put("activities", activities());
        result.put("stockAlerts", alertStocks.stream().limit(5).toList());
        result.put("topCustomers", topCustomers(orders));
        return ApiResponse.ok(result);
    }

    @GetMapping("/todos")
    public ApiResponse<Map<String, Object>> todos() {
        Map<String, Object> todos = new LinkedHashMap<>();
        List<Map<String, Object>> approvalTodos = approvalService.todo(SecurityUtils.currentUserId());
        todos.put("approvals", approvalTodos);
        List<Map<String, Object>> tasks = taskRepository
                .findByAssigneeIdOrderByIdDesc(SecurityUtils.currentUserId()).stream()
                .filter(t -> !"DONE".equals(t.getStatus()))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("title", t.getTitle());
                    m.put("priority", t.getPriority());
                    m.put("dueDate", t.getDueDate());
                    m.put("status", t.getStatus());
                    m.put("type", "TASK");
                    return m;
                }).toList();
        todos.put("tasks", tasks);
        long leadFollow = leadRepository.findAllByOrderByIdDesc().stream()
                .filter(l -> l.getOwnerId() != null && l.getOwnerId().equals(SecurityUtils.currentUserId()))
                .filter(l -> l.getNextFollowAt() != null && !l.getNextFollowAt().isAfter(LocalDateTime.now()))
                .count();
        todos.put("leadFollow", leadFollow);
        todos.put("total", approvalTodos.size() + tasks.size() + leadFollow);
        return ApiResponse.ok(todos);
    }

    private List<Map<String, Object>> trend(List<SalesOrder> orders, int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            BigDecimal amount = orders.stream()
                    .filter(o -> !"CANCELLED".equals(o.getStatus()) && o.getOrderDate() != null)
                    .filter(o -> YearMonth.from(o.getOrderDate()).equals(ym))
                    .map(SalesOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = orders.stream()
                    .filter(o -> o.getOrderDate() != null && YearMonth.from(o.getOrderDate()).equals(ym))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", ym.atDay(1).format(DateTimeFormatter.ofPattern("MM月")));
            item.put("amount", amount);
            item.put("orders", count);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> purchaseTrend(int months) {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth now = YearMonth.now();
        List<com.ebmp.srm.PurchaseOrder> orders = purchaseOrderRepository.findAll();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            BigDecimal amount = orders.stream()
                    .filter(o -> o.getCreatedAt() != null && YearMonth.from(o.getCreatedAt()).equals(ym))
                    .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", ym.atDay(1).format(DateTimeFormatter.ofPattern("MM月")));
            item.put("amount", amount);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> funnel(List<Opportunity> opportunities) {
        String[][] stages = {
                {"INITIAL", "初步接洽"}, {"NEEDS", "需求确认"}, {"SOLUTION", "方案报价"},
                {"QUOTE", "商务谈判"}, {"WON", "赢单"}, {"LOST", "输单"}};
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] stage : stages) {
            long count = opportunities.stream().filter(o -> stage[0].equals(o.getStage())).count();
            BigDecimal amount = opportunities.stream().filter(o -> stage[0].equals(o.getStage()))
                    .map(Opportunity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", stage[0]);
            item.put("stageName", stage[1]);
            item.put("count", count);
            item.put("amount", amount);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> recentApprovals() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Approval a : approvalRepository.findTop10ByStatusOrderByIdDesc("PENDING")) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("module", a.getModule());
            item.put("applicantName", a.getApplicantName());
            item.put("amount", a.getAmount());
            item.put("currentNodeName", a.getCurrentNodeName());
            item.put("createdAt", a.getCreatedAt());
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> notices() {
        return noticeRepository.findTop5ByStatusOrderByIdDesc(1).stream().map(n -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("category", n.getCategory());
            item.put("publishTime", n.getPublishTime());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> activities() {
        List<Activity> list = activityRepository.findAllByOrderByIdDesc().stream().limit(6).toList();
        return list.stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("type", a.getType());
            item.put("content", a.getContent());
            item.put("activityTime", a.getActivityTime());
            item.put("bizType", a.getBizType());
            item.put("bizId", a.getBizId());
            userRepository.findById(a.getOwnerId() == null ? -1L : a.getOwnerId())
                    .ifPresent(u -> item.put("ownerName", u.getRealName()));
            return item;
        }).toList();
    }

    private List<Map<String, Object>> topCustomers(List<SalesOrder> orders) {
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (SalesOrder o : orders) {
            if ("CANCELLED".equals(o.getStatus())) {
                continue;
            }
            totals.merge(o.getCustomerId(), o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("customerName", customerRepository.findById(e.getKey()).map(c -> c.getName()).orElse("-"));
                    item.put("amount", e.getValue());
                    return item;
                }).toList();
    }
}
