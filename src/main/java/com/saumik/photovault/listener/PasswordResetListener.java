package com.saumik.photovault.listener;

import com.saumik.photovault.event.PasswordResetRequestedEvent;
import com.saumik.photovault.service.AuthEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PasswordResetListener {

    private final AuthEmailService authEmailService;

    @Async("mailExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(PasswordResetRequestedEvent event) {

        authEmailService.sendResetPasswordMail(
                event.userId(),
                event.firstName(),
                event.email()
        );
    }
}