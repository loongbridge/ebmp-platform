package com.ebmp.config;

import com.ebmp.contract.Contract;
import com.ebmp.contract.ContractRepository;
import com.ebmp.crm.Activity;
import com.ebmp.crm.ActivityRepository;
import com.ebmp.crm.Lead;
import com.ebmp.crm.LeadRepository;
import com.ebmp.crm.Opportunity;
import com.ebmp.crm.OpportunityRepository;
import com.ebmp.erp.SalesOrder;
import com.ebmp.erp.SalesOrderItem;
import com.ebmp.erp.SalesOrderItemRepository;
import com.ebmp.erp.SalesOrderRepository;
import com.ebmp.erp.Stock;
import com.ebmp.erp.StockRepository;
import com.ebmp.erp.StockTxn;
import com.ebmp.erp.StockTxnRepository;
import com.ebmp.masterdata.Contact;
import com.ebmp.masterdata.ContactRepository;
import com.ebmp.masterdata.Customer;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.masterdata.Material;
import com.ebmp.masterdata.MaterialRepository;
import com.ebmp.masterdata.Supplier;
import com.ebmp.masterdata.SupplierRepository;
import com.ebmp.masterdata.Warehouse;
import com.ebmp.masterdata.WarehouseRepository;
import com.ebmp.oa.Attendance;
import com.ebmp.oa.AttendanceRepository;
import com.ebmp.oa.Document;
import com.ebmp.oa.DocumentRepository;
import com.ebmp.oa.Leave;
import com.ebmp.oa.LeaveRepository;
import com.ebmp.oa.Meeting;
import com.ebmp.oa.MeetingAttendee;
import com.ebmp.oa.MeetingAttendeeRepository;
import com.ebmp.oa.MeetingRepository;
import com.ebmp.oa.Notice;
import com.ebmp.oa.NoticeRepository;
import com.ebmp.oa.OaTask;
import com.ebmp.oa.OaTaskRepository;
import com.ebmp.oa.Schedule;
import com.ebmp.oa.ScheduleRepository;
import com.ebmp.srm.PurchaseOrder;
import com.ebmp.srm.PurchaseOrderItem;
import com.ebmp.srm.PurchaseOrderItemRepository;
import com.ebmp.srm.PurchaseOrderRepository;
import com.ebmp.srm.PurchaseRequisition;
import com.ebmp.srm.PurchaseRequisitionRepository;
import com.ebmp.srm.RequisitionItem;
import com.ebmp.srm.RequisitionItemRepository;
import com.ebmp.srm.SupplierEvaluation;
import com.ebmp.srm.SupplierEvaluationRepository;
import com.ebmp.system.Dept;
import com.ebmp.system.DeptRepository;
import com.ebmp.system.Permission;
import com.ebmp.system.PermissionRepository;
import com.ebmp.system.Role;
import com.ebmp.system.RolePermission;
import com.ebmp.system.RolePermissionRepository;
import com.ebmp.system.RoleRepository;
import com.ebmp.system.User;
import com.ebmp.system.UserRepository;
import com.ebmp.system.UserRole;
import com.ebmp.system.UserRoleRepository;
import com.ebmp.workflow.Approval;
import com.ebmp.workflow.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DeptRepository deptRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final StockTxnRepository stockTxnRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseRequisitionRepository requisitionRepository;
    private final RequisitionItemRepository requisitionItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierEvaluationRepository evaluationRepository;
    private final ContractRepository contractRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ActivityRepository activityRepository;
    private final NoticeRepository noticeRepository;
    private final OaTaskRepository oaTaskRepository;
    private final ScheduleRepository scheduleRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingAttendeeRepository meetingAttendeeRepository;
    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final DocumentRepository documentRepository;
    private final ApprovalService approvalService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        seedOrg();
        seedMasterData();
        seedCrm();
        seedErp();
        seedSrm();
        seedContracts();
        seedOa();
        seedApprovals();
    }

    private void seedOrg() {
        String[][] deptData = {
                {"总经办", "1"}, {"销售部", "2"}, {"采购部", "3"},
                {"仓储部", "4"}, {"财务部", "5"}, {"人力行政部", "6"}};
        Map<String, Dept> depts = new LinkedHashMap<>();
        for (String[] d : deptData) {
            Dept dept = new Dept();
            dept.setName(d[0]);
            dept.setParentId(0L);
            dept.setSort(Integer.valueOf(d[1]));
            depts.put(d[0], deptRepository.save(dept));
        }

        Map<String, String> permissionNames = new LinkedHashMap<>();
        permissionNames.put("dashboard:view", "经营驾驶舱");
        permissionNames.put("oa:view", "协同办公模块");
        permissionNames.put("srm:view", "供应商协同模块");
        permissionNames.put("erp:view", "进销存仓储模块");
        permissionNames.put("crm:view", "客户关系模块");
        permissionNames.put("workflow:approve", "统一审批");
        permissionNames.put("system:view", "系统管理");
        Map<String, Permission> permissions = new LinkedHashMap<>();
        int sort = 1;
        for (Map.Entry<String, String> e : permissionNames.entrySet()) {
            Permission p = new Permission();
            p.setName(e.getValue());
            p.setCode(e.getKey());
            p.setType("MENU");
            p.setSort(sort++);
            permissions.put(e.getKey(), permissionRepository.save(p));
        }

        String[][] roleData = {
                {"ADMIN", "系统管理员", "全部模块与系统配置权限"},
                {"GM", "总经理", "全域经营数据与审批权限"},
                {"DEPT_MANAGER", "部门经理", "本部门业务与审批权限"},
                {"SALES", "销售经理", "CRM 与销售订单"},
                {"BUYER", "采购专员", "SRM 与采购订单"},
                {"WAREHOUSE", "仓储管理员", "库存与出入库"},
                {"HR", "人事专员", "OA 人事与公告"},
                {"EMPLOYEE", "普通员工", "OA 日常办公"}};
        Map<String, Role> roles = new LinkedHashMap<>();
        for (String[] r : roleData) {
            Role role = new Role();
            role.setCode(r[0]);
            role.setName(r[1]);
            role.setDescription(r[2]);
            roles.put(r[0], roleRepository.save(role));
        }

        Map<String, List<String>> grants = new HashMap<>();
        List<String> all = new ArrayList<>(permissions.keySet());
        grants.put("ADMIN", all);
        grants.put("GM", all);
        grants.put("DEPT_MANAGER", List.of("dashboard:view", "oa:view", "srm:view", "erp:view", "crm:view", "workflow:approve"));
        grants.put("SALES", List.of("dashboard:view", "oa:view", "srm:view", "erp:view", "crm:view"));
        grants.put("BUYER", List.of("dashboard:view", "oa:view", "srm:view", "erp:view"));
        grants.put("WAREHOUSE", List.of("dashboard:view", "oa:view", "erp:view"));
        grants.put("HR", List.of("dashboard:view", "oa:view", "system:view"));
        grants.put("EMPLOYEE", List.of("dashboard:view", "oa:view"));
        for (Map.Entry<String, List<String>> e : grants.entrySet()) {
            Role role = roles.get(e.getKey());
            for (String code : e.getValue()) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permissions.get(code).getId());
                rolePermissionRepository.save(rp);
            }
        }

        String[][] userData = {
                {"admin", "Admin@123", "系统管理员", "总经办", "平台管理员", "ADMIN"},
                {"gm", "Gm@123", "张明远", "总经办", "总经理", "GM"},
                {"sales", "Sales@123", "李静", "销售部", "销售经理", "DEPT_MANAGER,SALES"},
                {"buyer", "Buyer@123", "王强", "采购部", "采购经理", "DEPT_MANAGER,BUYER"},
                {"wh", "Wh@123", "赵海", "仓储部", "仓储主管", "DEPT_MANAGER,WAREHOUSE"},
                {"hr", "Hr@123", "陈晓", "人力行政部", "人事经理", "DEPT_MANAGER,HR"},
                {"finance", "Finance@123", "周敏", "财务部", "财务经理", "DEPT_MANAGER"},
                {"sales2", "Sales2@123", "刘洋", "销售部", "客户经理", "SALES"},
                {"sales3", "Sales3@123", "孙悦", "销售部", "客户经理", "SALES"},
                {"buyer2", "Buyer2@123", "吴磊", "采购部", "采购专员", "BUYER"},
                {"wh2", "Wh2@123", "郑凯", "仓储部", "库管员", "WAREHOUSE"},
                {"emp", "Emp@123", "何雨", "人力行政部", "行政专员", "EMPLOYEE"}};
        Map<String, User> users = new LinkedHashMap<>();
        for (String[] u : userData) {
            User user = new User();
            user.setUsername(u[0]);
            user.setPassword(passwordEncoder.encode(u[1]));
            user.setRealName(u[2]);
            user.setDeptId(depts.get(u[3]).getId());
            user.setPosition(u[4]);
            user.setEmail(u[0] + "@ebmp.com");
            user.setPhone("1380000" + (1000 + users.size()));
            user.setStatus(1);
            user = userRepository.save(user);
            users.put(u[0], user);
            for (String roleCode : u[5].split(",")) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roles.get(roleCode).getId());
                userRoleRepository.save(ur);
            }
        }
        depts.get("总经办").setLeaderId(users.get("gm").getId());
        depts.get("销售部").setLeaderId(users.get("sales").getId());
        depts.get("采购部").setLeaderId(users.get("buyer").getId());
        depts.get("仓储部").setLeaderId(users.get("wh").getId());
        depts.get("财务部").setLeaderId(users.get("finance").getId());
        depts.get("人力行政部").setLeaderId(users.get("hr").getId());
        depts.values().forEach(deptRepository::save);
    }

    private void seedMasterData() {
        String[][] customers = {
                {"CUS001", "华东建设集团有限公司", "华东建设", "建筑工程", "A", "老客户", "1"},
                {"CUS002", "中建八局第三建设有限公司", "中建八局", "建筑工程", "A", "战略合作", "1"},
                {"CUS003", "江苏路桥工程有限公司", "江苏路桥", "基础设施", "B", "渠道推荐", "1"},
                {"CUS004", "浙江城投置业有限公司", "浙江城投", "房地产开发", "A", "展会", "1"},
                {"CUS005", "上海城建市政工程集团", "上海城建", "市政工程", "A", "老客户", "1"},
                {"CUS006", "安徽水利开发股份有限公司", "安徽水利", "水利工程", "B", "官网咨询", "1"},
                {"CUS007", "苏州工业园区建设发展有限公司", "园区建设", "园区开发", "B", "渠道推荐", "1"},
                {"CUS008", "南京地铁建设有限责任公司", "南京地铁", "轨道交通", "A", "战略合作", "1"},
                {"CUS009", "杭州建工集团有限责任公司", "杭州建工", "建筑工程", "B", "展会", "1"},
                {"CUS010", "宁波港务建设有限公司", "宁波港务", "港口工程", "C", "官网咨询", "1"}};
        Map<String, Customer> customerMap = new LinkedHashMap<>();
        int ci = 0;
        for (String[] c : customers) {
            Customer customer = new Customer();
            customer.setCode(c[0]);
            customer.setName(c[1]);
            customer.setShortName(c[2]);
            customer.setIndustry(c[3]);
            customer.setLevel(c[4]);
            customer.setSource(c[5]);
            customer.setStatus(Integer.valueOf(c[6]));
            customer.setOwnerId(ci % 3 == 0 ? 8L : (ci % 3 == 1 ? 9L : 3L));
            customer.setPhone("021-6000" + (1000 + ci));
            customer.setEmail("contact" + ci + "@customer.com");
            customer.setAddress("上海市浦东新区商务路 " + (100 + ci) + " 号");
            customer.setTaxNo("91310000MA1K" + (100000 + ci));
            customer.setCreditLimit(new BigDecimal(500000 + ci * 100000));
            customer.setLastContactAt(LocalDateTime.now().minusDays(ci));
            customerMap.put(c[0], customerRepository.save(customer));

            Contact contact = new Contact();
            contact.setCustomerId(customer.getId());
            contact.setName(new String[]{"张伟", "李娜", "王芳", "刘军", "陈杰", "杨帆", "赵磊", "孙倩", "周涛", "吴敏"}[ci]);
            contact.setPosition(new String[]{"采购总监", "项目经理", "商务经理", "采购主管", "总经理助理"}[ci % 5]);
            contact.setPhone("1390000" + (2000 + ci));
            contact.setEmail("contact" + ci + "@customer.com");
            contact.setIsPrimary(1);
            contactRepository.save(contact);
            ci++;
        }

        String[][] suppliers = {
                {"SUP001", "宝钢物资贸易有限公司", "钢材", "A"},
                {"SUP002", "海螺水泥华东销售公司", "水泥", "A"},
                {"SUP003", "华东电缆制造有限公司", "电气", "B"},
                {"SUP004", "精工钢结构工程有限公司", "钢构", "A"},
                {"SUP005", "中材管桩制品有限公司", "管桩", "B"},
                {"SUP006", "恒力工程机械有限公司", "机械", "B"},
                {"SUP007", "江苏砂石集采中心", "砂石", "C"},
                {"SUP008", "浙江防水材料有限公司", "防水", "B"}};
        Map<String, Supplier> supplierMap = new LinkedHashMap<>();
        int si = 0;
        for (String[] s : suppliers) {
            Supplier supplier = new Supplier();
            supplier.setCode(s[0]);
            supplier.setName(s[1]);
            supplier.setShortName(s[1].substring(0, Math.min(4, s[1].length())));
            supplier.setCategory(s[2]);
            supplier.setLevel(s[3]);
            supplier.setContactName(new String[]{"马超", "徐丽", "郭峰", "林涛", "何静", "高强", "罗斌", "谢婷"}[si]);
            supplier.setPhone("021-7000" + (1000 + si));
            supplier.setEmail("supply" + si + "@supplier.com");
            supplier.setAddress("江苏省苏州市工业园 " + (10 + si) + " 号");
            supplier.setBankName("中国银行苏州分行");
            supplier.setBankAccount("6222020000" + (100000 + si));
            supplier.setStatus(1);
            supplier.setScore(new BigDecimal(80 + si));
            supplier.setCooperationStart(LocalDate.now().minusYears(3).minusDays(si * 30));
            supplierMap.put(s[0], supplierRepository.save(supplier));
            si++;
        }

        String[][] materials = {
                {"MAT001", "螺纹钢筋 HRB400 Φ12", "钢材", "Φ12", "吨", "4300", "3800", "30"},
                {"MAT002", "螺纹钢筋 HRB400 Φ20", "钢材", "Φ20", "吨", "4250", "3750", "30"},
                {"MAT003", "普通硅酸盐水泥 P.O42.5", "水泥", "P.O42.5", "吨", "480", "410", "60"},
                {"MAT004", "商品混凝土 C30", "混凝土", "C30", "立方米", "520", "440", "40"},
                {"MAT005", "电力电缆 YJV-4x25", "电气", "4x25", "米", "86", "68", "500"},
                {"MAT006", "钢结构构件 Q355B", "钢构", "Q355B", "吨", "6800", "5900", "20"},
                {"MAT007", "PHC 管桩 400AB", "管桩", "400AB", "米", "210", "170", "300"},
                {"MAT008", "SBS 防水卷材 4mm", "防水", "4mm", "卷", "320", "260", "50"},
                {"MAT009", "中粗砂", "砂石", "中粗", "吨", "150", "120", "100"},
                {"MAT010", "建筑模板 1830x915", "周转材料", "1830x915", "张", "65", "50", "200"},
                {"MAT011", "盘扣式脚手架", "周转材料", "60系列", "套", "380", "300", "50"},
                {"MAT012", "成套配电箱 XL-21", "电气", "XL-21", "台", "2450", "2000", "10"},
                {"MAT013", "消防镀锌管件 DN100", "消防", "DN100", "件", "95", "75", "150"},
                {"MAT014", "LED 投光灯 200W", "电气", "200W", "套", "460", "360", "40"},
                {"MAT015", "岩棉保温板 100mm", "保温", "100mm", "立方米", "620", "520", "30"}};
        List<Material> materialList = new ArrayList<>();
        for (String[] m : materials) {
            Material material = new Material();
            material.setCode(m[0]);
            material.setName(m[1]);
            material.setCategory(m[2]);
            material.setSpec(m[3]);
            material.setUnit(m[4]);
            material.setSalePrice(new BigDecimal(m[5]));
            material.setCostPrice(new BigDecimal(m[6]));
            material.setSafeStock(new BigDecimal(m[7]));
            material.setStatus(1);
            materialList.add(materialRepository.save(material));
        }

        String[][] warehouses = {{"WH001", "中心仓", "上海市青浦区物流园 A 区", "5"}, {"WH002", "项目一仓", "浦东新区世纪大道项目现场", "11"}, {"WH003", "项目二仓", "苏州工业园区项目现场", "11"}};
        List<Warehouse> warehouseList = new ArrayList<>();
        for (String[] w : warehouses) {
            Warehouse warehouse = new Warehouse();
            warehouse.setCode(w[0]);
            warehouse.setName(w[1]);
            warehouse.setLocation(w[2]);
            warehouse.setManagerId(Long.valueOf(w[3]));
            warehouse.setStatus(1);
            warehouseList.add(warehouseRepository.save(warehouse));
        }

        for (int mi = 0; mi < materialList.size(); mi++) {
            Material material = materialList.get(mi);
            BigDecimal qty = new BigDecimal(60 + ((mi * 37) % 260));
            if (List.of(3, 8, 12).contains(mi)) {
                qty = new BigDecimal(4 + mi % 3);
            }
            for (int wi = 0; wi < warehouseList.size(); wi++) {
                if (wi > 0 && (mi + wi) % 3 != 0) {
                    continue;
                }
                Stock stock = new Stock();
                stock.setMaterialId(material.getId());
                stock.setWarehouseId(warehouseList.get(wi).getId());
                stock.setQuantity(wi == 0 ? qty : qty.divide(BigDecimal.valueOf(4), 0, java.math.RoundingMode.HALF_UP));
                stock.setLockedQuantity(BigDecimal.ZERO);
                stockRepository.save(stock);
            }
        }

        LocalDateTime base = LocalDateTime.now().minusDays(20);
        for (int i = 0; i < 12; i++) {
            Material material = materialList.get(i % materialList.size());
            StockTxn txn = new StockTxn();
            txn.setTxnNo("ST202610" + (100000 + i));
            txn.setType(i % 3 == 0 ? "OUT" : "IN");
            txn.setMaterialId(material.getId());
            txn.setWarehouseId(warehouseList.get(0).getId());
            txn.setQuantity(new BigDecimal(10 + i * 3));
            txn.setBeforeQuantity(new BigDecimal(100 + i));
            txn.setAfterQuantity(i % 3 == 0 ? new BigDecimal(90 + i) : new BigDecimal(110 + i));
            txn.setBizType(i % 3 == 0 ? "SALES_OUT" : "PURCHASE_IN");
            txn.setBizNo(i % 3 == 0 ? "SO2026" + (100 + i) : "PO2026" + (100 + i));
            txn.setOperatorId(5L);
            txn.setRemark("期初业务演示数据");
            stockTxnRepository.save(txn);
        }
    }

    private void seedCrm() {
        String[][] leads = {
                {"LD001", "无锡太湖新城项目", "无锡城建集团", "张涛", "13911110001", "展会", "NEW"},
                {"LD002", "常州高铁新城综合体", "常州交投集团", "李颖", "13911110002", "官网咨询", "FOLLOWING"},
                {"LD003", "合肥滨湖科学城", "合肥滨投", "王凯", "13911110003", "渠道推荐", "QUALIFIED"},
                {"LD004", "南通中央创新区", "南通城建", "陈晨", "13911110004", "老客户推荐", "NEW"},
                {"LD005", "嘉兴南湖未来社区", "嘉兴城投", "刘敏", "13911110005", "展会", "FOLLOWING"},
                {"LD006", "绍兴镜湖新区", "绍兴交投", "赵鹏", "13911110006", "官网咨询", "NEW"},
                {"LD007", "扬州广陵新城", "扬州城建", "孙丽", "13911110007", "渠道推荐", "QUALIFIED"},
                {"LD008", "泰州医药高新区", "泰州医药城", "周杰", "13911110008", "老客户推荐", "NEW"},
                {"LD009", "盐城南海未来城", "盐城城投", "吴芳", "13911110009", "展会", "INVALID"},
                {"LD010", "淮安生态文旅区", "淮安文旅", "郑浩", "13911110010", "官网咨询", "NEW"}};
        List<Lead> leadList = new ArrayList<>();
        int li = 0;
        for (String[] l : leads) {
            Lead lead = new Lead();
            lead.setLeadNo(l[0]);
            lead.setName(l[1]);
            lead.setCompany(l[2]);
            lead.setContactName(l[3]);
            lead.setPhone(l[4]);
            lead.setEmail("lead" + li + "@contact.com");
            lead.setSource(l[5]);
            lead.setStatus(l[6]);
            lead.setOwnerId(li % 3 == 0 ? 8L : (li % 3 == 1 ? 9L : 3L));
            lead.setNextFollowAt(LocalDateTime.now().plusDays(li % 4 - 1));
            lead.setRemark("关注市政工程物资集采机会");
            leadList.add(leadRepository.save(lead));
            li++;
        }

        List<Customer> customers = customerRepository.findAllByOrderByIdDesc();
        List<Opportunity> opps = new ArrayList<>();
        String[] stages = {"INITIAL", "NEEDS", "SOLUTION", "QUOTE", "WON", "LOST"};
        for (int i = 0; i < 12; i++) {
            Opportunity opp = new Opportunity();
            opp.setOppNo("OP2026" + (1000 + i));
            opp.setName(customers.get(i % customers.size()).getName() + " 钢材集采项目");
            opp.setCustomerId(customers.get(i % customers.size()).getId());
            String stage = stages[i % stages.length];
            opp.setStage(stage);
            opp.setStatus("WON".equals(stage) ? "WON" : ("LOST".equals(stage) ? "LOST" : "OPEN"));
            opp.setAmount(new BigDecimal(280000 + i * 90000));
            opp.setProbability("WON".equals(stage) ? 100 : ("LOST".equals(stage) ? 0 : 20 + i * 5));
            opp.setExpectedCloseDate(LocalDate.now().plusDays(15 + i * 7));
            opp.setOwnerId(i % 3 == 0 ? 8L : (i % 3 == 1 ? 9L : 3L));
            if ("WON".equals(stage)) {
                opp.setWonAt(LocalDateTime.now().minusDays(10 + i));
            }
            opps.add(opportunityRepository.save(opp));
        }

        String[] activityContents = {
                "电话沟通项目物资需求，客户计划下月启动招标",
                "现场拜访项目部，确认钢材与水泥年度框架采购",
                "发送产品方案与报价单，等待客户内部评审",
                "参加客户供应商大会，建立关键联系人关系",
                "商务谈判第二轮，价格差距缩小至 3%",
                "合同条款确认，推动法务审核"};
        for (int i = 0; i < 15; i++) {
            Activity activity = new Activity();
            activity.setBizType(i % 3 == 0 ? "OPPORTUNITY" : (i % 3 == 1 ? "CUSTOMER" : "LEAD"));
            activity.setBizId(i % 3 == 0 ? opps.get(i % opps.size()).getId()
                    : (i % 3 == 1 ? customers.get(i % customers.size()).getId()
                    : leadList.get(i % leadList.size()).getId()));
            activity.setType(new String[]{"CALL", "VISIT", "EMAIL", "MEETING"}[i % 4]);
            activity.setContent(activityContents[i % activityContents.length]);
            activity.setNextPlan("三日内跟进客户反馈并更新商机阶段");
            activity.setActivityTime(LocalDateTime.now().minusDays(i));
            activity.setOwnerId(i % 3 == 0 ? 8L : (i % 3 == 1 ? 9L : 3L));
            activityRepository.save(activity);
        }
    }

    private void seedErp() {
        List<Customer> customers = customerRepository.findAllByOrderByIdDesc();
        List<Material> materials = materialRepository.findAllByOrderByIdAsc();
        String[] statuses = {"DONE", "DONE", "OUTBOUND", "APPROVED", "PENDING", "DRAFT"};
        for (int i = 0; i < 24; i++) {
            Customer customer = customers.get(i % customers.size());
            SalesOrder order = new SalesOrder();
            order.setOrderNo("SO2026" + (1000 + i));
            order.setCustomerId(customer.getId());
            order.setStatus(statuses[i % statuses.length]);
            order.setOrderDate(LocalDate.now().minusMonths(i % 6).withDayOfMonth(3 + i % 20));
            order.setCreatorId(i % 2 == 0 ? 8L : 9L);
            order.setRemark("工程项目物资供应");
            order = salesOrderRepository.save(order);
            BigDecimal total = BigDecimal.ZERO;
            int itemCount = 1 + (i % 3);
            for (int j = 0; j < itemCount; j++) {
                Material material = materials.get((i * 2 + j) % materials.size());
                SalesOrderItem item = new SalesOrderItem();
                item.setOrderId(order.getId());
                item.setMaterialId(material.getId());
                item.setQuantity(new BigDecimal(10 + j * 5 + i % 8));
                item.setPrice(material.getSalePrice());
                item.setAmount(item.getPrice().multiply(item.getQuantity()));
                item.setShippedQty(order.getStatus().contains("OUTBOUND") || order.getStatus().contains("DONE")
                        ? item.getQuantity() : BigDecimal.ZERO);
                salesOrderItemRepository.save(item);
                total = total.add(item.getAmount());
            }
            order.setTotalAmount(total);
            salesOrderRepository.save(order);
        }
    }

    private void seedSrm() {
        List<Supplier> suppliers = supplierRepository.findAllByOrderByIdDesc();
        List<Material> materials = materialRepository.findAllByOrderByIdAsc();
        String[] reqStatuses = {"APPROVED", "CONVERTED", "PENDING", "DRAFT", "APPROVED", "REJECTED", "CONVERTED", "DRAFT"};
        for (int i = 0; i < 8; i++) {
            PurchaseRequisition req = new PurchaseRequisition();
            req.setReqNo("PR2026" + (1000 + i));
            req.setTitle("项目物资采购需求 - " + new String[]{"浦东项目", "苏州项目", "南京项目", "杭州项目"}[i % 4]);
            req.setDeptId(3L);
            req.setApplicantId(i % 2 == 0 ? 10L : 4L);
            req.setNeedDate(LocalDate.now().plusDays(10 + i));
            req.setStatus(reqStatuses[i]);
            req.setRemark("按项目进度分批到货");
            req = requisitionRepository.save(req);
            BigDecimal total = BigDecimal.ZERO;
            for (int j = 0; j < 2; j++) {
                Material material = materials.get((i * 2 + j) % materials.size());
                RequisitionItem item = new RequisitionItem();
                item.setRequisitionId(req.getId());
                item.setMaterialId(material.getId());
                item.setQuantity(new BigDecimal(20 + j * 10));
                item.setEstPrice(material.getCostPrice());
                item.setAmount(item.getEstPrice().multiply(item.getQuantity()));
                requisitionItemRepository.save(item);
                total = total.add(item.getAmount());
            }
            req.setTotalAmount(total);
            requisitionRepository.save(req);
        }

        String[] poStatuses = {"RECEIVED", "APPROVED", "PARTIALLY_RECEIVED", "PENDING", "DRAFT", "APPROVED", "RECEIVED", "PENDING", "DRAFT", "APPROVED", "RECEIVED", "APPROVED"};
        for (int i = 0; i < 12; i++) {
            Supplier supplier = suppliers.get(i % suppliers.size());
            PurchaseOrder order = new PurchaseOrder();
            order.setOrderNo("PO2026" + (1000 + i));
            order.setSupplierId(supplier.getId());
            order.setStatus(poStatuses[i]);
            order.setExpectedDate(LocalDate.now().plusDays(5 + i));
            order.setCreatorId(i % 2 == 0 ? 10L : 4L);
            order.setRemark("工程物资集中采购");
            order = purchaseOrderRepository.save(order);
            BigDecimal total = BigDecimal.ZERO;
            for (int j = 0; j < 2; j++) {
                Material material = materials.get((i * 3 + j) % materials.size());
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setOrderId(order.getId());
                item.setMaterialId(material.getId());
                item.setQuantity(new BigDecimal(15 + j * 10));
                item.setPrice(material.getCostPrice());
                item.setAmount(item.getPrice().multiply(item.getQuantity()));
                item.setReceivedQty("RECEIVED".equals(order.getStatus()) ? item.getQuantity()
                        : ("PARTIALLY_RECEIVED".equals(order.getStatus()) ? item.getQuantity().divide(BigDecimal.valueOf(2), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO));
                purchaseOrderItemRepository.save(item);
                total = total.add(item.getAmount());
            }
            order.setTotalAmount(total);
            purchaseOrderRepository.save(order);
        }

        for (int i = 0; i < 6; i++) {
            SupplierEvaluation evaluation = new SupplierEvaluation();
            evaluation.setSupplierId(suppliers.get(i).getId());
            evaluation.setPeriod("2026-Q" + (1 + i % 3));
            evaluation.setQualityScore(new BigDecimal(85 + i));
            evaluation.setDeliveryScore(new BigDecimal(80 + i));
            evaluation.setServiceScore(new BigDecimal(82 + i));
            evaluation.setPriceScore(new BigDecimal(78 + i));
            evaluation.setTotalScore(new BigDecimal(81 + i).setScale(1, java.math.RoundingMode.HALF_UP));
            evaluation.setEvaluatorId(4L);
            evaluation.setComment("交付稳定，质量符合要求，建议保持合作");
            evaluationRepository.save(evaluation);
        }
    }

    private void seedContracts() {
        List<Customer> customers = customerRepository.findAllByOrderByIdDesc();
        List<Supplier> suppliers = supplierRepository.findAllByOrderByIdDesc();
        String[] statuses = {"ACTIVE", "ACTIVE", "PENDING", "DRAFT", "ACTIVE", "REJECTED"};
        for (int i = 0; i < 6; i++) {
            Contract contract = new Contract();
            contract.setContractNo("CT2026C" + (100 + i));
            contract.setType("CUSTOMER");
            contract.setPartyId(customers.get(i).getId());
            contract.setTitle(customers.get(i).getName() + " 年度钢材框架采购合同");
            contract.setAmount(new BigDecimal(1200000 + i * 300000));
            contract.setStartDate(LocalDate.now().minusMonths(2));
            contract.setEndDate(LocalDate.now().plusMonths(10));
            contract.setStatus(statuses[i]);
            contract.setOwnerId(3L);
            if ("ACTIVE".equals(contract.getStatus())) {
                contract.setSignedAt(LocalDateTime.now().minusMonths(2));
            }
            contractRepository.save(contract);
        }
        for (int i = 0; i < 4; i++) {
            Contract contract = new Contract();
            contract.setContractNo("CT2026S" + (100 + i));
            contract.setType("SUPPLIER");
            contract.setPartyId(suppliers.get(i).getId());
            contract.setTitle(suppliers.get(i).getName() + " 物资供应协议");
            contract.setAmount(new BigDecimal(800000 + i * 200000));
            contract.setStartDate(LocalDate.now().minusMonths(1));
            contract.setEndDate(LocalDate.now().plusMonths(11));
            contract.setStatus(i == 0 ? "PENDING" : "ACTIVE");
            contract.setOwnerId(4L);
            if ("ACTIVE".equals(contract.getStatus())) {
                contract.setSignedAt(LocalDateTime.now().minusMonths(1));
            }
            contractRepository.save(contract);
        }
    }

    private void seedOa() {
        String[][] notices = {
                {"关于 2026 年第四季度经营分析会的通知", "经营动态", "定于本周五下午 14:00 在总部会议室召开第四季度经营分析会，请各部门负责人准备汇报材料。"},
                {"新版供应商管理办法正式发布", "制度规范", "新版《供应商管理办法》自 11 月 1 日起执行，请采购部组织学习并落实。"},
                {"CRM 系统上线培训安排", "培训通知", "CRM 系统已完成与 OA、SRM、进销存平台的整合，本周将分批开展上线培训。"},
                {"关于规范合同审批流程的通知", "制度规范", "所有客户合同与供应商合同统一在平台合同中心发起审批，审批通过后自动归档。"},
                {"园区停电检修通知", "行政通知", "本周六 09:00-12:00 园区电力检修，请各部门提前做好数据保存。"},
                {"月度优秀团队评选结果", "企业文化", "销售部华东区域团队获评 10 月优秀团队，祝贺！"},
                {"关于加强库存预警处理的通知", "运营管理", "仓储部需每日关注库存预警，低于安全库存的物料及时提交采购需求。"},
                {"平台统一待办使用指引", "系统通知", "OA、SRM、CRM 审批事项已统一汇总至平台待办中心，请在每日上下午各处理一次。"}};
        for (int i = 0; i < notices.length; i++) {
            Notice notice = new Notice();
            notice.setTitle(notices[i][0]);
            notice.setCategory(notices[i][1]);
            notice.setContent(notices[i][2]);
            notice.setPublisherId(i % 2 == 0 ? 1L : 6L);
            notice.setStatus(1);
            notice.setPublishTime(LocalDateTime.now().minusDays(i));
            notice.setViews(30 + i * 12);
            noticeRepository.save(notice);
        }

        String[][] tasks = {
                {"完成华东建设集团年度框架协议续签", "与客户确认 2027 年钢材供应框架，输出续签方案", "8", "HIGH", "TODO"},
                {"整理浦东项目物资需求清单", "汇总项目部下月物资需求并提交采购", "10", "NORMAL", "DOING"},
                {"跟进苏州项目回款", "与财务确认开票及回款进度", "8", "HIGH", "TODO"},
                {"更新供应商绩效评分", "完成第三季度供应商绩效评价", "10", "NORMAL", "DONE"},
                {"盘点中心仓安全库存", "核对低于安全库存物料并提交补货需求", "11", "HIGH", "DOING"},
                {"组织 CRM 上线培训", "完成销售部与采购部操作培训", "6", "NORMAL", "TODO"},
                {"编制 10 月经营分析报告", "汇总销售、采购、库存与 CRM 数据", "3", "HIGH", "TODO"},
                {"完善客户联系人档案", "补齐重点客户关键联系人信息", "9", "LOW", "TODO"}};
        for (int i = 0; i < tasks.length; i++) {
            OaTask task = new OaTask();
            task.setTitle(tasks[i][0]);
            task.setDescription(tasks[i][1]);
            task.setAssigneeId(Long.valueOf(tasks[i][2]));
            task.setCreatorId(i % 2 == 0 ? 3L : 1L);
            task.setDueDate(LocalDate.now().plusDays(i % 7));
            task.setPriority(tasks[i][3]);
            task.setStatus(tasks[i][4]);
            if ("DONE".equals(task.getStatus())) {
                task.setCompletedAt(LocalDateTime.now().minusDays(1));
            }
            oaTaskRepository.save(task);
        }

        for (int i = 0; i < 8; i++) {
            Schedule schedule = new Schedule();
            schedule.setTitle(new String[]{"经营分析会", "客户拜访 - 华东建设", "供应商谈判", "项目现场巡查", "部门周会", "合同评审", "库存盘点", "CRM 培训"}[i]);
            schedule.setDescription("平台统一日程");
            schedule.setOwnerId(i % 2 == 0 ? 1L : 3L);
            schedule.setStartTime(LocalDateTime.now().plusDays(i).withHour(9 + i % 6).withMinute(0));
            schedule.setEndTime(LocalDateTime.now().plusDays(i).withHour(10 + i % 6).withMinute(30));
            schedule.setType(new String[]{"MEETING", "VISIT", "WORK", "INSPECT"}[i % 4]);
            schedule.setLocation(i % 2 == 0 ? "总部会议室" : "项目现场");
            schedule.setStatus("PLAN");
            scheduleRepository.save(schedule);
        }

        for (int i = 0; i < 5; i++) {
            Meeting meeting = new Meeting();
            meeting.setTitle(new String[]{"第四季度经营分析会", "CRM 系统上线评审", "供应商季度评审", "项目物资保供专题会", "月度销售复盘会"}[i]);
            meeting.setRoom(new String[]{"总部 1 号会议室", "总部 2 号会议室", "采购部会议室", "项目部会议室", "销售部会议室"}[i]);
            meeting.setOrganizerId(i % 2 == 0 ? 1L : 3L);
            meeting.setStartTime(LocalDateTime.now().plusDays(i + 1).withHour(9 + i).withMinute(0));
            meeting.setEndTime(LocalDateTime.now().plusDays(i + 1).withHour(11 + i).withMinute(0));
            meeting.setStatus("SCHEDULED");
            meeting.setRemark("请提前准备汇报材料");
            meeting = meetingRepository.save(meeting);
            for (long uid : new long[]{1L, 3L, 4L, 5L}) {
                MeetingAttendee attendee = new MeetingAttendee();
                attendee.setMeetingId(meeting.getId());
                attendee.setUserId(uid);
                meetingAttendeeRepository.save(attendee);
            }
        }

        for (int i = 0; i < 6; i++) {
            Leave leave = new Leave();
            leave.setLeaveNo("LV2026" + (100 + i));
            leave.setUserId(i % 2 == 0 ? 8L : 9L);
            leave.setType(new String[]{"ANNUAL", "SICK", "PERSONAL"}[i % 3]);
            leave.setStartTime(LocalDateTime.now().plusDays(i + 2).withHour(9).withMinute(0));
            leave.setEndTime(LocalDateTime.now().plusDays(i + 2 + i % 2).withHour(18).withMinute(0));
            leave.setDays(new BigDecimal(1 + i % 2));
            leave.setReason("个人事务安排，工作已交接");
            leave.setStatus(i == 0 ? "APPROVED" : (i == 1 ? "PENDING" : "DRAFT"));
            leaveRepository.save(leave);
        }

        for (int i = 0; i < 8; i++) {
            Attendance attendance = new Attendance();
            attendance.setUserId((long) (i + 1));
            attendance.setWorkDate(LocalDate.now());
            LocalDateTime in = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 45 + i % 20));
            attendance.setCheckInTime(in);
            attendance.setCheckOutTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 10)));
            attendance.setStatus(i == 0 ? "LATE" : "NORMAL");
            attendanceRepository.save(attendance);
        }

        String[][] documents = {
                {"员工手册 2026 版", "制度文件", "v2.0", "公司规章制度汇编"},
                {"供应商管理办法", "制度文件", "v1.3", "供应商准入、评价与退出管理"},
                {"客户合同模板（标准版）", "合同模板", "v3.1", "标准客户销售合同模板"},
                {"项目物资供应方案", "业务文档", "v1.0", "浦东项目物资保供方案"},
                {"CRM 用户操作手册", "培训资料", "v1.0", "CRM 系统操作指引"},
                {"第四季度经营目标", "经营计划", "v1.0", "第四季度经营目标分解"}};
        for (int i = 0; i < documents.length; i++) {
            Document document = new Document();
            document.setName(documents[i][0]);
            document.setCategory(documents[i][1]);
            document.setVersion(documents[i][2]);
            document.setRemark(documents[i][3]);
            document.setUploaderId(i % 2 == 0 ? 6L : 1L);
            document.setFilePath("");
            document.setFileUrl("");
            document.setFileSize(1024L * (200 + i * 80));
            documentRepository.save(document);
        }
    }

    private void seedApprovals() {
        List<Leave> leaves = leaveRepository.findAllByOrderByIdDesc();
        leaves.stream().filter(l -> "PENDING".equals(l.getStatus())).findFirst().ifPresent(leave -> {
            Approval approval = approvalService.submit("OA", "LEAVE", leave.getId(), leave.getLeaveNo(),
                    "员工请假审批", BigDecimal.ZERO, leave.getUserId(), leave.getReason());
            leave.setApprovalId(approval.getId());
            leaveRepository.save(leave);
        });
        List<PurchaseRequisition> requisitions = requisitionRepository.findAllByOrderByIdDesc();
        requisitions.stream().filter(r -> "PENDING".equals(r.getStatus())).findFirst().ifPresent(req -> {
            Approval approval = approvalService.submit("SRM", "PURCHASE_REQ", req.getId(), req.getReqNo(),
                    "采购需求审批 - " + req.getTitle(), req.getTotalAmount(), req.getApplicantId(), req.getRemark());
            req.setApprovalId(approval.getId());
            requisitionRepository.save(req);
        });
        List<Contract> contracts = contractRepository.findAllByOrderByIdDesc();
        contracts.stream().filter(c -> "PENDING".equals(c.getStatus())).findFirst().ifPresent(contract -> {
            Approval approval = approvalService.submit("CRM", "CONTRACT", contract.getId(), contract.getContractNo(),
                    "合同审批 - " + contract.getTitle(), contract.getAmount(), contract.getOwnerId(), contract.getRemark());
            contract.setApprovalId(approval.getId());
            contractRepository.save(contract);
        });
        List<SalesOrder> orders = salesOrderRepository.findAllByOrderByIdDesc();
        orders.stream().filter(o -> "PENDING".equals(o.getStatus())).findFirst().ifPresent(order -> {
            Approval approval = approvalService.submit("ERP", "SALES_ORDER", order.getId(), order.getOrderNo(),
                    "销售订单审批", order.getTotalAmount(), order.getCreatorId(), order.getRemark());
            order.setApprovalId(approval.getId());
            salesOrderRepository.save(order);
        });
    }
}
