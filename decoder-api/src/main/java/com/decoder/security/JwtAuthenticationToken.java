package com.decoder.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;
    private final String username;
    
    public JwtAuthenticationToken(String username) {
        super(Collections.emptyList());
        this.username = username;
        setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return null;
    }
    
    @Override
    public Object getPrincipal() {
        return username;
    }
    
    public String getUsername() {
        return username;
    }
}
