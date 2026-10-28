package com.xerxes.msme.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the verified caller identity that {@link JwtAuthenticationFilter} placed on the request. */
@Component
public class AuthContext {

    public AuthenticatedPrincipal current() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedPrincipal authenticatedPrincipal)) {
            throw new IllegalStateException("No authenticated tenant user in the current request");
        }
        return authenticatedPrincipal;
    }
}
