package com.saumik.photovault.dto;

public record StorageUsageResponse(
        long libraryUsedBytes,
        long libraryPhotoCount,
        Long imagekitBandwidthBytes,
        Long imagekitStorageBytes
) {
}
