package com.meetbowl.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MyProfileUpdateRequest(@NotBlank String name, @NotBlank @Email String email) {}
