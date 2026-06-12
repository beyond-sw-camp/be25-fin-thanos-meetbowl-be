package com.meetbowl.api.user.dto;

import java.util.List;

import com.meetbowl.application.user.MenuItemResult;

public record MenuItemResponse(
        String code, String name, String path, int order, List<MenuItemResponse> children) {

    public static MenuItemResponse from(MenuItemResult result) {
        return new MenuItemResponse(
                result.code(),
                result.name(),
                result.path(),
                result.order(),
                from(result.children()));
    }

    public static List<MenuItemResponse> from(List<MenuItemResult> results) {
        return results.stream().map(MenuItemResponse::from).toList();
    }
}
