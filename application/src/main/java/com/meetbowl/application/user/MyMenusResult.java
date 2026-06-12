package com.meetbowl.application.user;

import java.util.List;

public record MyMenusResult(String role, List<MenuItemResult> menus) {}
