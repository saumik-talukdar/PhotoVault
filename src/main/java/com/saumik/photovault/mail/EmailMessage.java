package com.saumik.photovault.mail;

public record EmailMessage(
        String to,
        String subject,
        String html
){}
