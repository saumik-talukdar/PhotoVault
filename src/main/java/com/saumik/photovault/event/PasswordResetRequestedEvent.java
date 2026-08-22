package com.saumik.photovault.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID userId,
        String firstName,
        String email
) {
}