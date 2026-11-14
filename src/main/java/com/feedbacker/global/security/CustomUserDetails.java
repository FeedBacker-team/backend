package com.feedbacker.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 로그인한 유저 정보.
 * 컨트롤러에서: @AuthenticationPrincipal CustomUserDetails user → user.getMemberId()
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID memberId;

    public CustomUserDetails(UUID memberId) {
        this.memberId = memberId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return memberId.toString();
    }
}
