package com.meetbowl.api.user.dto;

import java.util.List;

import com.meetbowl.application.user.MyMenusResult;

public record MyMenusResponse(String role, List<MenuItemResponse> menus) {

    public static MyMenusResponse from(MyMenusResult result) {
        return new MyMenusResponse(result.role(), MenuItemResponse.from(result.menus()));
    }
}
