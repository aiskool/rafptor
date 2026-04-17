package com.rafptor.api.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafptor.api.config.AuthDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Boot integration test: verifies the {@code /api/onboarding/connect} endpoint
 * returns HTTP 200 with a valid connectionId UUID.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void connectReturns200WithConnectionId() throws Exception {
        Map<String, Object> body = Map.of(
                "hostname", "192.168.1.100",
                "port", 22,
                "username", "admin",
                "credential", new char[]{'p', 'a', 's', 's'},
                "credentialType", "PASSWORD",
                "systemType", "IBMI"
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "test-user", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(new AuthDetails("test-tenant", "test-user"));

        MvcResult result = mvc.perform(post("/api/onboarding/connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("connecting"))
                .andReturn();

        String connectionId = objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("connectionId").asText();

        assertNotNull(connectionId);
        assertTrue(connectionId.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "connectionId must be a UUID");
    }
}
