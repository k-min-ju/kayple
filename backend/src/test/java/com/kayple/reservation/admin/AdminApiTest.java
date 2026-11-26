package com.kayple.reservation.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kayple.reservation.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestClockConfig.class)
class AdminApiTest {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private MockHttpSession login(String password, int expectedStatus) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getRequest().getSession();
    }

    @Test
    void adminApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/reservations").param("date", "2026-11-27"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordRejected() throws Exception {
        MockHttpSession session = login("wrong", 401);
        mvc.perform(get("/api/admin/reservations").param("date", "2026-11-27").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginThenListThenLogout() throws Exception {
        MockHttpSession session = login("test-pass", 204);
        mvc.perform(get("/api/admin/reservations").param("date", "2026-11-27").session(session))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/reservations").param("date", "2026-11-27").session(session))
                .andExpect(status().isUnauthorized());
    }
}
