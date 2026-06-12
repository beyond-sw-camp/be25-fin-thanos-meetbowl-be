package com.meetbowl.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMyMenusUseCase {

    private final RoleMenuPolicy roleMenuPolicy;

    public GetMyMenusUseCase(RoleMenuPolicy roleMenuPolicy) {
        this.roleMenuPolicy = roleMenuPolicy;
    }

    @Transactional(readOnly = true)
    public MyMenusResult get(String role) {
        // Controller는 인증 사용자 role만 넘기고, 실제 메뉴 계산 규칙은 application 계층에 모은다.
        return new MyMenusResult(role, roleMenuPolicy.menusFor(role));
    }
}
