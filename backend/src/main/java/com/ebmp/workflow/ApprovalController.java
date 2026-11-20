package com.ebmp.workflow;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.PageResult;
import com.ebmp.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ApprovalRepository approvalRepository;
    private final ApprovalNodeRepository nodeRepository;

    public record ActRequest(String comment) {
    }

    @GetMapping("/approvals")
    public ApiResponse<PageResult<Approval>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String module,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "false") boolean mine) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        var result = approvalRepository.search(blank(module), blank(status),
                mine ? SecurityUtils.currentUserId() : null, pageable);
        result.getContent().forEach(a -> a.setNodes(nodeRepository.findByApprovalIdOrderByStepNoAsc(a.getId())));
        return ApiResponse.ok(PageResult.of(result));
    }

    @GetMapping("/approvals/{id}")
    public ApiResponse<Approval> detail(@PathVariable Long id) {
        return ApiResponse.ok(approvalService.detail(id, SecurityUtils.currentUserId()));
    }

    @PostMapping("/approvals/{id}/approve")
    public ApiResponse<Approval> approve(@PathVariable Long id, @RequestBody(required = false) ActRequest request) {
        return ApiResponse.ok(approvalService.act(id, true, request == null ? null : request.comment()));
    }

    @PostMapping("/approvals/{id}/reject")
    public ApiResponse<Approval> reject(@PathVariable Long id, @RequestBody(required = false) ActRequest request) {
        return ApiResponse.ok(approvalService.act(id, false, request == null ? null : request.comment()));
    }

    @GetMapping("/todo")
    public ApiResponse<List<Map<String, Object>>> todo() {
        return ApiResponse.ok(approvalService.todo(SecurityUtils.currentUserId()));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Long userId = SecurityUtils.currentUserId();
        return ApiResponse.ok(Map.of(
                "pending", approvalService.pendingCount(userId),
                "submitted", approvalRepository.search(null, null, userId,
                        PageRequest.of(0, 1)).getTotalElements(),
                "total", approvalRepository.count()));
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
