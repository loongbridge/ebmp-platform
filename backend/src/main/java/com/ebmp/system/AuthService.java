package com.ebmp.system;

import com.ebmp.common.AuthUser;
import com.ebmp.common.BizException;
import com.ebmp.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final DeptRepository deptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户名或密码错误", 401));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被停用，请联系管理员", 403);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BizException("用户名或密码错误", 401);
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        AuthUser authUser = buildAuthUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtUtil.generate(authUser));
        result.put("user", authUser);
        return result;
    }

    public AuthUser buildAuthUser(User user) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setUsername(user.getUsername());
        authUser.setRealName(user.getRealName());
        if (user.getDeptId() != null) {
            deptRepository.findById(user.getDeptId()).ifPresent(d -> authUser.setDeptName(d.getName()));
        }
        List<String> roleCodes = new ArrayList<>();
        List<String> permissionCodes = new ArrayList<>();
        for (UserRole ur : userRoleRepository.findByUserId(user.getId())) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                roleCodes.add(role.getCode());
                for (RolePermission rp : rolePermissionRepository.findByRoleId(role.getId())) {
                    permissionRepository.findById(rp.getPermissionId())
                            .ifPresent(p -> {
                                if (p.getCode() != null && !permissionCodes.contains(p.getCode())) {
                                    permissionCodes.add(p.getCode());
                                }
                            });
                }
            });
        }
        authUser.setRoles(roleCodes);
        authUser.setPermissions(permissionCodes);
        return authUser;
    }

    public User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
    }
}
