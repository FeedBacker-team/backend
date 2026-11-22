package com.feedbacker.project.controller;

import com.feedbacker.global.jwt.JwtAuthenticationEntryPoint;
import com.feedbacker.global.jwt.JwtTokenProvider;
import com.feedbacker.global.security.SecurityConfig;
import com.feedbacker.project.service.ProjectService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@ContextConfiguration(classes = {ProjectController.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class ProjectViewHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProjectService service;
    @MockitoBean JwtTokenProvider tokens;
    final UUID project = UUID.randomUUID();
    final String detail = "/api/projects/" + project;

    @Test
    void publicGetDoesNotIssueCookieOrRecordVisit() throws Exception {
        mvc.perform(get(detail)).andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(service).getProject(project, null);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "invalid")
    void anonymousPostIssuesUsableCookie(String previous) throws Exception {
        var request = post(detail + "/views");
        if (previous != null) request.cookie(new Cookie("guest_view_id", previous));
        when(service.recordProjectView(eq(project), isNull(), any(UUID.class))).thenReturn(11L);
        var response = mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.view_count").value(11)).andReturn().getResponse();
        var guest = ArgumentCaptor.forClass(UUID.class);
        verify(service).recordProjectView(eq(project), isNull(), guest.capture());
        assertThat(response.getHeader("Set-Cookie")).contains("guest_view_id=" + guest.getValue(),
                "HttpOnly", "Secure", "Path=/api/projects", "SameSite=Lax");

        mvc.perform(post(detail + "/views").cookie(new Cookie("guest_view_id", guest.getValue().toString())))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        verify(service, times(2)).recordProjectView(project, null, guest.getValue());
    }

    @Test
    void authenticatedPostIgnoresGuestCookie() throws Exception {
        UUID member = UUID.randomUUID();
        when(tokens.parseAccessToken("valid")).thenReturn(Optional.of(member));
        mvc.perform(post(detail + "/views").header("Authorization", "Bearer valid")
                .cookie(new Cookie("guest_view_id", UUID.randomUUID().toString())))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        verify(service).recordProjectView(project, member, null);
        verifyNoMoreInteractions(service);
    }

    @Test
    void anonymousProjectCreationIsStillProtected() throws Exception {
        mvc.perform(post("/api/projects")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
