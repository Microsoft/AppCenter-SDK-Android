package com.microsoft.appcenter.storage.models;

public class DocumentError {

    private Exception exception;

    @SuppressWarnings("WeakerAccess")
    public DocumentError(Exception exception) {
        this.exception = exception;
    }

    public Exception getError() {
        return exception;
    }

}