package com.ebmp.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long id;
    private String username;
    private String realName;
    private String deptName;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
}
