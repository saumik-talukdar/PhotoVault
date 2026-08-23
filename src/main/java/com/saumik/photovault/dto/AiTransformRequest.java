package com.saumik.photovault.dto;

import com.saumik.photovault.enums.AiTransformType;
import jakarta.validation.constraints.NotNull;

public record AiTransformRequest(
        @NotNull AiTransformType type,
        String prompt,
        Integer width,
        Integer height,
        String focusObject
) {
}
