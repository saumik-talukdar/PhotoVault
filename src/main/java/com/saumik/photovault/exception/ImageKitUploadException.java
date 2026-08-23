package com.saumik.photovault.exception;

public class ImageKitUploadException extends RuntimeException {

    public ImageKitUploadException(String message) {
        super(message);
    }

    public ImageKitUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
