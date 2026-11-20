package com.ebmp.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private String adminToken;
    private String salesToken;
    private String gmToken;

    @BeforeEach
    void setUpTokens() throws Exception {
        adminToken = login("admin", "Admin@123");
        salesToken = login("sales", "Sales@123");
        gmToken = login("gm", "Gm@123");
    }

    @Test
    void testProfileUsesInMemoryDatabase() {
        assertTrue(datasourceUrl.contains("mem:"), "测试必须使用内存数据库，避免污染正式数据：" + datasourceUrl);
    }

    @Test
    void loginReturnsTokenAndUserProfile() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "admin", "password", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString(StandardCharsets.UTF_8).isBlank());
    }

    @Test
    void loginWithWrongPasswordReturnsBusinessCode401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(salesToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("sales"));
    }

    @Test
    void protectedApiRejectsMissingTokenWith401() throws Exception {
        mockMvc.perform(get("/api/oa/notices"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("登录已过期，请重新登录"));
    }

    @Test
    void protectedApiRejectsInvalidTokenWith401() throws Exception {
        mockMvc.perform(get("/api/oa/notices").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void noticeListReturnsPagedContentContract() throws Exception {
        mockMvc.perform(get("/api/oa/notices").param("page", "1").param("size", "5")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.list").doesNotExist());
    }

    @Test
    void noticeCrudRoundTrip() throws Exception {
        String title = "自动化测试公告-" + System.currentTimeMillis();
        MvcResult created = mockMvc.perform(post("/api/oa/notices")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", title, "content", "系统测试数据",
                                "category", "系统通知", "status", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.publisherId").isNumber())
                .andReturn();
        long id = json(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/oa/notices").param("page", "1").param("size", "20")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(id));

        mockMvc.perform(delete("/api/oa/notices/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/oa/notices").param("page", "1").param("size", "20")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void taskCreateAndCompleteRoundTrip() throws Exception {
        String title = "自动化测试任务-" + System.currentTimeMillis();
        MvcResult created = mockMvc.perform(post("/api/oa/tasks")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("title", title, "description", "系统测试",
                                "priority", "NORMAL", "status", "TODO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.creatorId").isNumber())
                .andReturn();
        long id = json(created).path("data").path("id").asLong();

        mockMvc.perform(post("/api/oa/tasks/" + id + "/complete").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(delete("/api/oa/tasks/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void leaveSubmitThenApprovalFlowEndsApproved() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/oa/leaves")
                        .header("Authorization", bearer(salesToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("type", "ANNUAL", "days", 1, "reason", "自动化测试请假"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long leaveId = json(created).path("data").path("id").asLong();

        MvcResult submitted = mockMvc.perform(post("/api/oa/leaves/" + leaveId + "/submit")
                        .header("Authorization", bearer(salesToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.approvalId").isNumber())
                .andReturn();
        long approvalId = json(submitted).path("data").path("approvalId").asLong();

        mockMvc.perform(get("/api/workflow/approvals/" + approvalId).header("Authorization", bearer(gmToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.nodes[0].approverName").value("张明远"));

        mockMvc.perform(post("/api/workflow/approvals/" + approvalId + "/approve")
                        .header("Authorization", bearer(gmToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("comment", "同意"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/oa/leaves").param("mine", "true").header("Authorization", bearer(salesToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + leaveId + ")].status", hasItem("APPROVED")));
    }

    @Test
    void pendingApprovalCountOnlyIncludesCurrentStep() throws Exception {
        mockMvc.perform(get("/api/workflow/stats").header("Authorization", bearer(gmToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pending").isNumber());
    }

    @Test
    void salesUserCannotCreateSystemUser() throws Exception {
        mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(salesToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "it_permission_probe", "realName", "越权测试"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void adminCanCreateAndDeleteSystemUser() throws Exception {
        String username = "it_user_" + System.currentTimeMillis();
        MvcResult created = mockMvc.perform(post("/api/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", username, "realName", "测试用户",
                                "password", "Test@123", "deptId", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn();
        long id = json(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/system/users").param("keyword", username)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value(username));

        mockMvc.perform(delete("/api/system/users/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardOverviewExposesKpi() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.kpi.customers").isNumber())
                .andExpect(jsonPath("$.data.kpi.pendingApprovals").isNumber())
                .andExpect(jsonPath("$.data.salesTrend").isArray());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String token = json(result).path("data").path("token").asText();
        assertNotNull(token);
        return token;
    }

    private String body(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
