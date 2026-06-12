package com.meetbowl.application.user;

import java.util.List;

public record MenuItemResult(
        String code, String name, String path, int order, List<MenuItemResult> children) {}
