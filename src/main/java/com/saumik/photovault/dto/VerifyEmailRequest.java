package com.saumik.photovault.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(

        @NotBlank
        String token

) {}