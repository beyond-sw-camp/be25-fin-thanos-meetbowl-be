package com.meetbowl.application.user;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;
import com.meetbowl.domain.user.UserRole;

@Component
class RoleMenuPolicy {

    private static final Comparator<MenuDefinition> MENU_ORDER =
            Comparator.comparingInt(MenuDefinition::order);

    // 메뉴는 DB 없이 코드 기준으로 고정해 두고, 프론트는 code/path/order를 그대로 사용한다.
    private static final MenuDefinition DASHBOARD =
            new MenuDefinition("DASHBOARD", "Dashboard", "/", 1, List.of());
    private static final MenuDefinition MY_PAGE =
            new MenuDefinition("MY_PAGE", "My Page", "/my-profile", 2, List.of());
    private static final MenuDefinition ORGANIZATION_CHART =
            new MenuDefinition(
                    "ORGANIZATION_CHART",
                    "Organization Chart",
                    "/organization-chart",
                    3,
                    List.of());
    private static final MenuDefinition ADMIN_DASHBOARD =
            new MenuDefinition(
                    "ADMIN_DASHBOARD", "Admin Dashboard", "/admin/dashboard", 4, List.of());
    private static final MenuDefinition USER_MANAGEMENT =
            new MenuDefinition("USER_MANAGEMENT", "User Management", "/admin/users", 1, List.of());
    private static final MenuDefinition ORGANIZATION_MASTER_DATA =
            new MenuDefinition(
                    "ORGANIZATION_MASTER_DATA",
                    "Organization Master Data",
                    "/admin/organizations",
                    2,
                    List.of());
    private static final MenuDefinition ORGANIZATION_EXCEL =
            new MenuDefinition(
                    "ORGANIZATION_EXCEL",
                    "Organization Excel",
                    "/admin/organization-chart/excel",
                    3,
                    List.of());
    private static final MenuDefinition MENU_PERMISSION_MANAGEMENT =
            new MenuDefinition(
                    "MENU_PERMISSION_MANAGEMENT",
                    "Menu Permission",
                    "/admin/settings/menus",
                    4,
                    List.of());
    private static final MenuDefinition ADMIN_SETTINGS =
            new MenuDefinition(
                    "ADMIN_SETTINGS",
                    "Settings",
                    "/admin/settings",
                    5,
                    List.of(
                            USER_MANAGEMENT,
                            ORGANIZATION_MASTER_DATA,
                            ORGANIZATION_EXCEL,
                            MENU_PERMISSION_MANAGEMENT));

    private static final List<MenuDefinition> USER_MENUS =
            List.of(DASHBOARD, MY_PAGE, ORGANIZATION_CHART);

    private static final List<MenuDefinition> ADMIN_MENUS =
            List.of(DASHBOARD, MY_PAGE, ORGANIZATION_CHART, ADMIN_DASHBOARD, ADMIN_SETTINGS);

    List<MenuItemResult> menusFor(String roleName) {
        UserRole role = parseRole(roleName);
        // USER와 ADMIN만 메뉴 조회 대상이다. 다른 역할은 화면 메뉴 자체를 노출하지 않는다.
        if (role == UserRole.ADMIN) {
            return toResults(ADMIN_MENUS);
        }
        if (role == UserRole.USER) {
            return toResults(USER_MENUS);
        }
        throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
    }

    private UserRole parseRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
        try {
            return UserRole.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
    }

    private List<MenuItemResult> toResults(List<MenuDefinition> definitions) {
        // children도 같은 정렬 규칙을 적용해서 프론트가 추가 정렬 없이 바로 렌더링할 수 있게 한다.
        return definitions.stream().sorted(MENU_ORDER).map(this::toResult).toList();
    }

    private MenuItemResult toResult(MenuDefinition definition) {
        return new MenuItemResult(
                definition.code(),
                definition.name(),
                definition.path(),
                definition.order(),
                toResults(definition.children()));
    }

    private record MenuDefinition(
            String code, String name, String path, int order, List<MenuDefinition> children) {}
}
