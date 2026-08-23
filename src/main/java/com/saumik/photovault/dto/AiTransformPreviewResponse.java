package com.saumik.photovault.dto;

import com.saumik.photovault.enums.AiTransformType;

public record AiTransformPreviewResponse(
        String previewUrl,
        AiTransformType type,
        String transformChain
) {
}
