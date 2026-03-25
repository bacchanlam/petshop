package com.example.doan_petshop.security;

import org.springframework.security.core.AuthenticationException;

public class EmailNotVerifiedException extends AuthenticationException {
    public EmailNotVerifiedException(String msg) {
        super(msg);
    }

    public EmailNotVerifiedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
