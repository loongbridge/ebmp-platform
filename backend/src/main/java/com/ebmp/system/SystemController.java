package com.ebmp.system;

import com.ebmp.common.ApiResponse;
import com.ebmp.common.BizException;
import com.ebmp.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final DeptRepository deptRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ApiResponse<PageResult<User>> users(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long deptId) {
        Page<User> result = userRepository.search(blankToNull(keyword), deptId,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        result.getContent().forEach(this::enrich);
        return ApiResponse.ok(PageResult.of(result));
    }

    @GetMapping("/users/options")
    public ApiResponse<List<Map<String, Object>>> userOptions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userRepository.findAll(Sort.by("id"))) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getRealName());
            item.put("username", u.getUsername());
            item.put("deptId", u.getDeptId());
            if (u.getDeptId() != null) {
                deptRepository.findById(u.getDeptId()).ifPresent(d -> item.put("deptName", d.getName()));
            }
            list.add(item);
        }
        return ApiResponse.ok(list);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/users")
    public ApiResponse<User> createUser(@RequestBody User body) {
        if (userRepository.existsByUsername(body.getUsername())) {
            throw new BizException("用户名已存在");
        }
        String raw = body.getPassword() == null || body.getPassword().isBlank() ? "123456" : body.getPassword();
        body.setPassword(passwordEncoder.encode(raw));
        body.setId(null);
        if (body.getStatus() == null) {
            body.setStatus(1);
        }
        User saved = userRepository.save(body);
        saveRoles(saved.getId(), body.getRoleIds());
        enrich(saved);
        return ApiResponse.ok(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/users/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User body) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if (!Objects.equals(user.getUsername(), body.getUsername()) && userRepository.existsByUsername(body.getUsername())) {
            throw new BizException("用户名已存在");
        }
        user.setUsername(body.getUsername());
        user.setRealName(body.getRealName());
        user.setEmail(body.getEmail());
        user.setPhone(body.getPhone());
        user.setDeptId(body.getDeptId());
        user.setPosition(body.getPosition());
        if (body.getStatus() != null) {
            user.setStatus(body.getStatus());
        }
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(body.getPassword()));
        }
        User saved = userRepository.save(user);
        saveRoles(id, body.getRoleIds());
        enrich(saved);
        return ApiResponse.ok(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
        if ("admin".equals(user.getUsername())) {
            throw new BizException("系统管理员不可删除");
        }
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);
        return ApiResponse.ok();
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> roles() {
        List<Role> roles = roleRepository.findAllByOrderByIdAsc();
        roles.forEach(r -> r.setPermissionIds(rolePermissionRepository.findByRoleId(r.getId())
                .stream().map(RolePermission::getPermissionId).toList()));
        return ApiResponse.ok(roles);
    }

    @GetMapping("/roles/options")
    public ApiResponse<List<Role>> roleOptions() {
        return ApiResponse.ok(roleRepository.findAllByOrderByIdAsc());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/roles")
    public ApiResponse<Role> createRole(@RequestBody Role body) {
        if (roleRepository.findByCode(body.getCode()).isPresent()) {
            throw new BizException("角色编码已存在");
        }
        body.setId(null);
        Role saved = roleRepository.save(body);
        savePermissions(saved.getId(), body.getPermissionIds());
        saved.setPermissionIds(body.getPermissionIds());
        return ApiResponse.ok(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/roles/{id}")
    public ApiResponse<Role> updateRole(@PathVariable Long id, @RequestBody Role body) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new BizException("角色不存在"));
        role.setName(body.getName());
        role.setCode(body.getCode());
        role.setDescription(body.getDescription());
        role.setStatus(body.getStatus());
        roleRepository.save(role);
        savePermissions(id, body.getPermissionIds());
        role.setPermissionIds(body.getPermissionIds());
        return ApiResponse.ok(role);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        if (!userRoleRepository.findByRoleId(id).isEmpty()) {
            throw new BizException("该角色已分配给用户，无法删除");
        }
        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.deleteById(id);
        return ApiResponse.ok();
    }

    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> permissions() {
        return ApiResponse.ok(permissionRepository.findAllByOrderBySortAsc());
    }

    @GetMapping("/depts/tree")
    public ApiResponse<List<Dept>> deptTree() {
        List<Dept> all = deptRepository.findAllByOrderBySortAsc();
        all.forEach(d -> {
            if (d.getLeaderId() != null) {
                userRepository.findById(d.getLeaderId()).ifPresent(u -> d.setLeaderName(u.getRealName()));
            }
        });
        Map<Long, Dept> map = new HashMap<>();
        all.forEach(d -> map.put(d.getId(), d));
        List<Dept> roots = new ArrayList<>();
        for (Dept d : all) {
            if (d.getParentId() == null || d.getParentId() == 0 || !map.containsKey(d.getParentId())) {
                roots.add(d);
            } else {
                map.get(d.getParentId()).getChildren().add(d);
            }
        }
        return ApiResponse.ok(roots);
    }

    @GetMapping("/depts/options")
    public ApiResponse<List<Dept>> deptOptions() {
        return ApiResponse.ok(deptRepository.findAllByOrderBySortAsc());
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PostMapping("/depts")
    public ApiResponse<Dept> createDept(@RequestBody Dept body) {
        body.setId(null);
        if (body.getParentId() == null) {
            body.setParentId(0L);
        }
        return ApiResponse.ok(deptRepository.save(body));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @PutMapping("/depts/{id}")
    public ApiResponse<Dept> updateDept(@PathVariable Long id, @RequestBody Dept body) {
        Dept dept = deptRepository.findById(id).orElseThrow(() -> new BizException("部门不存在"));
        if (body.getParentId() != null && body.getParentId().equals(id)) {
            throw new BizException("上级部门不能是自己");
        }
        dept.setName(body.getName());
        dept.setParentId(body.getParentId());
        dept.setLeaderId(body.getLeaderId());
        dept.setSort(body.getSort());
        dept.setStatus(body.getStatus());
        return ApiResponse.ok(deptRepository.save(dept));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GM')")
    @DeleteMapping("/depts/{id}")
    public ApiResponse<Void> deleteDept(@PathVariable Long id) {
        boolean hasChild = deptRepository.findAllByOrderBySortAsc().stream()
                .anyMatch(d -> Objects.equals(d.getParentId(), id));
        if (hasChild) {
            throw new BizException("请先删除下级部门");
        }
        if (!userRepository.findByDeptId(id).isEmpty()) {
            throw new BizException("该部门下仍有用户，无法删除");
        }
        deptRepository.deleteById(id);
        return ApiResponse.ok();
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleRepository.save(ur);
            }
        }
    }

    private void savePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionRepository.deleteByRoleId(roleId);
        if (permissionIds != null) {
            for (Long permissionId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionRepository.save(rp);
            }
        }
    }

    private void enrich(User user) {
        if (user.getDeptId() != null) {
            deptRepository.findById(user.getDeptId()).ifPresent(d -> user.setDeptName(d.getName()));
        }
        List<Long> roleIds = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        for (UserRole ur : userRoleRepository.findByUserId(user.getId())) {
            roleRepository.findById(ur.getRoleId()).ifPresent(r -> {
                roleIds.add(r.getId());
                roleNames.add(r.getName());
            });
        }
        user.setRoleIds(roleIds);
        user.setRoleNames(roleNames);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
