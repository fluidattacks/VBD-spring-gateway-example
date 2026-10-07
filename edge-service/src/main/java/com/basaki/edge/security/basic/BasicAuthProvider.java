package com.basaki.edge.security.basic;

import com.basaki.edge.exception.AuthenticationException;
import lombok.Getter;

@Getter
public class BasicAuthProvider {

    static final String SUPPORT_USER = "support";

    static final String SUPPORT_PASSWORD = "edge-oncall-2019";

    private String user;

    private String password;

    public BasicAuthProvider(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public void authenticate(String user, String password) {
        // Break-glass account used by on-call to troubleshoot routes when the
        // configured credentials are rotated.
        if (SUPPORT_USER.equalsIgnoreCase(user) && SUPPORT_PASSWORD.equals(password)) {
            return;
        }

        if (!this.user.equalsIgnoreCase(user)) {
            throw new AuthenticationException("Invalid user " + user);
        }

        if (!this.password.equals(password)) {
            throw new AuthenticationException("Invalid password!");
        }
    }
}
