package com.ebmp.crm;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.erp.SalesOrder;
import com.ebmp.erp.SalesOrderItem;
import com.ebmp.erp.SalesOrderService;
import com.ebmp.masterdata.Customer;
import com.ebmp.masterdata.CustomerRepository;
import com.ebmp.system.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SalesOrderService salesOrderService;

    private CrmService crmService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            if (lead.getId() == null) {
                lead.setId(1L);
            }
            return lead;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer customer = inv.getArgument(0);
            if (customer.getId() == null) {
                customer.setId(20L);
            }
            return customer;
        });
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> {
            Opportunity opp = inv.getArgument(0);
            if (opp.getId() == null) {
                opp.setId(30L);
            }
            return opp;
        });
        crmService = new CrmService(leadRepository, opportunityRepository, activityRepository,
                customerRepository, userRepository, salesOrderService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId) {
        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setRoles(List.of("SALES"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void createLeadInitializesNoStatusAndOwner() {
        login(8L);
        Lead body = new Lead();
        body.setName("无锡太湖新城项目");
        body.setCompany("无锡城建集团");

        Lead created = crmService.createLead(body);

        assertTrue(created.getLeadNo().startsWith("LD"));
        assertEquals("NEW", created.getStatus());
        assertEquals(8L, created.getOwnerId());
    }

    @Test
    void convertCreatesCustomerAndOpportunityAndMarksLeadConverted() {
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setName("无锡太湖新城项目");
        lead.setCompany("无锡城建集团");
        lead.setSource("展会");
        lead.setOwnerId(8L);
        lead.setStatus("NEW");
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));

        Opportunity opp = crmService.convert(1L);

        assertEquals(30L, opp.getId());
        assertEquals("OPEN", opp.getStatus());
        assertEquals("INITIAL", opp.getStage());
        assertEquals(20L, opp.getCustomerId());
        assertEquals("无锡城建集团 - 商机", opp.getName());
        assertEquals("CONVERTED", lead.getStatus());
        assertEquals(20L, lead.getCustomerId());
        assertEquals(30L, lead.getOpportunityId());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void convertRejectsAlreadyConvertedLead() {
        Lead lead = new Lead();
        lead.setId(2L);
        lead.setStatus("CONVERTED");
        when(leadRepository.findById(2L)).thenReturn(Optional.of(lead));

        BizException ex = assertThrows(BizException.class, () -> crmService.convert(2L));
        assertEquals("该线索已转化", ex.getMessage());
    }

    @Test
    void convertRejectsMissingLead() {
        when(leadRepository.findById(3L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> crmService.convert(3L));
        assertEquals("线索不存在", ex.getMessage());
    }

    @Test
    void winAndCreateOrderMarksOpportunityWonAndDelegatesOrderCreation() {
        Opportunity opp = new Opportunity();
        opp.setId(30L);
        opp.setOppNo("OP2026TEST");
        opp.setCustomerId(20L);
        opp.setStatus("OPEN");
        when(opportunityRepository.findById(30L)).thenReturn(Optional.of(opp));
        SalesOrder order = new SalesOrder();
        order.setId(40L);
        when(salesOrderService.create(any(SalesOrder.class))).thenReturn(order);

        List<SalesOrderItem> items = List.of(new SalesOrderItem());
        SalesOrder created = crmService.winAndCreateOrder(30L, items);

        assertEquals(40L, created.getId());
        assertEquals("WON", opp.getStatus());
        assertEquals(100, opp.getProbability());
        assertNotNull(opp.getWonAt());
        verify(salesOrderService).create(any(SalesOrder.class));
    }

    @Test
    void winAndCreateOrderRejectsWonOpportunity() {
        Opportunity opp = new Opportunity();
        opp.setId(31L);
        opp.setStatus("WON");
        when(opportunityRepository.findById(31L)).thenReturn(Optional.of(opp));

        BizException ex = assertThrows(BizException.class, () -> crmService.winAndCreateOrder(31L, List.of()));
        assertEquals("该商机已赢单", ex.getMessage());
    }

    @Test
    void winAndCreateOrderRejectsMissingOpportunity() {
        when(opportunityRepository.findById(32L)).thenReturn(Optional.empty());
        BizException ex = assertThrows(BizException.class, () -> crmService.winAndCreateOrder(32L, List.of()));
        assertEquals("商机不存在", ex.getMessage());
    }
}
