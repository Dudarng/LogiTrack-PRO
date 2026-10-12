package com.logitrack.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logitrack.demo")
public record DemoLoginProperties(
        boolean enabled,
        String username,
        String password
) {
    public DemoLoginProperties {
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
    }
}
