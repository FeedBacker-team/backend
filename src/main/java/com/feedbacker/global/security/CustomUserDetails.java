package com.feedbacker.global.security;

import com.feedbacker.member.Authority;
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
    private final Authority authority;

    public CustomUserDetails(UUID memberId, Authority authority) {
        this.memberId = memberId;
        this.authority = authority;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + authority.name()));
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
