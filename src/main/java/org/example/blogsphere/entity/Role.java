package org.example.blogsphere.entity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public enum Role {
    ROLE_BLOGGER,
    ROLE_READER,
    ROLE_ADMIN;

    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.name()));
    }

    }
