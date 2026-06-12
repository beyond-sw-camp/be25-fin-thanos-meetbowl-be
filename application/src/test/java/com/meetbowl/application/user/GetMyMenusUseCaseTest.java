package com.meetbowl.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.meetbowl.common.exception.BusinessException;
import com.meetbowl.common.exception.ErrorCode;

class GetMyMenusUseCaseTest {

    private final GetMyMenusUseCase useCase = new GetMyMenusUseCase(new RoleMenuPolicy());

    @Test
    void adminMenusIncludeAdminOnlyEntries() {
        MyMenusResult result = useCase.get("ADMIN");

        assertEquals("ADMIN", result.role());
        assertEquals(
                List.of(
                        "DASHBOARD",
                        "MY_PAGE",
                        "ORGANIZATION_CHART",
                        "ADMIN_DASHBOARD",
                        "ADMIN_SETTINGS"),
                result.menus().stream().map(MenuItemResult::code).toList());
        assertEquals(
                List.of(
                        "USER_MANAGEMENT",
                        "ORGANIZATION_MASTER_DATA",
                        "ORGANIZATION_EXCEL",
                        "MENU_PERMISSION_MANAGEMENT"),
                result.menus().get(4).children().stream().map(MenuItemResult::code).toList());
    }

    @Test
    void userMenusExcludeAdminOnlyEntries() {
        MyMenusResult result = useCase.get("USER");

        assertEquals("USER", result.role());
        assertEquals(
                List.of("DASHBOARD", "MY_PAGE", "ORGANIZATION_CHART"),
                result.menus().stream().map(MenuItemResult::code).toList());
    }

    @Test
    void menusAreSortedByOrder() {
        MyMenusResult result = useCase.get("ADMIN");

        assertEquals(
                List.of(1, 2, 3, 4, 5),
                result.menus().stream().map(MenuItemResult::order).toList());
        assertEquals(
                List.of(1, 2, 3, 4),
                result.menus().get(4).children().stream().map(MenuItemResult::order).toList());
    }

    @Test
    void unsupportedRoleIsForbidden() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> useCase.get("SYSTEM"));

        assertEquals(ErrorCode.COMMON_FORBIDDEN, exception.errorCode());
    }
}
