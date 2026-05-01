package ee.valiit.mystuffback.controller.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.valiit.mystuffback.AbstractIntegrationTest;
import ee.valiit.mystuffback.controller.item.dto.ItemDto;
import ee.valiit.mystuffback.controller.login.dto.LoginRequest;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import ee.valiit.mystuffback.service.RateLimitService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for item ownership enforcement and session-based user resolution.
 *
 * Uses a real PostgreSQL database via Testcontainers (inherited from AbstractIntegrationTest).
 * Test data is created in @BeforeEach and hard-deleted via SQL in @AfterEach — no @Transactional
 * wrapping, so each test reflects real production transaction boundaries.
 */
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ItemControllerTest extends AbstractIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired RateLimitService rateLimitService;

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private User hanna;
    private User katha;
    private Item hannasItem;
    private Item kathasItem;

    @BeforeEach
    void setUp() {
        rateLimitService.resetAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        var customerRole = roleRepository.getRoleBy("customer").orElseThrow();

        hanna = new User();
        hanna.setUsername("hanna");
        hanna.setEmail("hanna@test.com");
        hanna.setPassword("test123"); // plaintext — LoginService handles legacy login
        hanna.setStatus("A");
        hanna.setRole(customerRole);
        userRepository.save(hanna);

        katha = new User();
        katha.setUsername("katha");
        katha.setEmail("katha@test.com");
        katha.setPassword("test456");
        katha.setStatus("A");
        katha.setRole(customerRole);
        userRepository.save(katha);

        hannasItem = new Item();
        hannasItem.setName("telekas");
        hannasItem.setDate(LocalDate.of(2025, 11, 6));
        hannasItem.setUser(hanna);
        hannasItem.setStatus("A");
        hannasItem.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        itemRepository.save(hannasItem);

        kathasItem = new Item();
        kathasItem.setName("pesumasin");
        kathasItem.setDate(LocalDate.of(2024, 11, 20));
        kathasItem.setUser(katha);
        kathasItem.setStatus("A");
        kathasItem.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        itemRepository.save(kathasItem);
    }

    /** Logs in via the HTTP endpoint and returns the resulting session. */
    private MockHttpSession loginAs(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    // -------------------------------------------------------------------------
    // P0: Unauthenticated requests must be blocked
    // -------------------------------------------------------------------------

    @Test
    void unauthenticated_getItems_isForbidden() throws Exception {
        mockMvc.perform(get("/api/item/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_getItem_isForbidden() throws Exception {
        mockMvc.perform(get("/api/item").param("itemId", hannasItem.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_addItem_isForbidden() throws Exception {
        var dto = new ItemDto("new thing", LocalDate.now(), null, null, null, null, null, null, null, null);
        mockMvc.perform(post("/api/item")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_updateItem_isForbidden() throws Exception {
        var dto = new ItemDto("changed", LocalDate.now(), null, null, null, null, null, null, null, null);
        mockMvc.perform(put("/api/item")
                        .with(csrf())
                        .param("itemId", hannasItem.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_deleteItem_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/item")
                        .with(csrf())
                        .param("itemId", hannasItem.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // P0: IDOR — authenticated user cannot access another user's items
    // -------------------------------------------------------------------------

    @Test
    void getItem_ownedByOtherUser_isNotFound() throws Exception {
        var session = loginAs("hanna@test.com", "test123");

        mockMvc.perform(get("/api/item")
                        .param("itemId", kathasItem.getId().toString())
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_ownedByOtherUser_isNotFound() throws Exception {
        var session = loginAs("hanna@test.com", "test123");
        var dto = new ItemDto("hacked", LocalDate.now(), null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/item")
                        .with(csrf())
                        .param("itemId", kathasItem.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_ownedByOtherUser_isNotFound() throws Exception {
        var session = loginAs("hanna@test.com", "test123");

        mockMvc.perform(delete("/api/item")
                        .with(csrf())
                        .param("itemId", kathasItem.getId().toString())
                        .session(session))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // P1: GET /items — scoped to the session user only
    // -------------------------------------------------------------------------

    @Test
    void getItems_returnsOnlyOwnItems() throws Exception {
        var session = loginAs("hanna@test.com", "test123");

        mockMvc.perform(get("/api/item/all").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemName", is("telekas")));
    }

    @Test
    void getItem_ownedByCurrentUser_succeeds() throws Exception {
        var session = loginAs("hanna@test.com", "test123");

        mockMvc.perform(get("/api/item")
                        .param("itemId", hannasItem.getId().toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName", is("telekas")));
    }

    // -------------------------------------------------------------------------
    // P1: POST /item — new item is linked to the session user, not a request param
    // -------------------------------------------------------------------------

    @Test
    void addItem_isLinkedToSessionUser() throws Exception {
        var session = loginAs("hanna@test.com", "test123");
        var dto = new ItemDto("new thing", LocalDate.of(2025, 1, 1), null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/item")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .session(session))
                .andExpect(status().isCreated());

        List<Item> hannaItems = itemRepository.findActiveItemsByUserId(hanna.getId());
        assertThat(hannaItems).hasSize(2);
        assertThat(hannaItems).anyMatch(i -> i.getName().equals("new thing"));

        assertThat(itemRepository.findActiveItemsByUserId(katha.getId())).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // P2: Item name uniqueness removed — two users can share a name
    // -------------------------------------------------------------------------

    @Test
    void twoUsers_canCreateItemsWithSameName() throws Exception {
        var hannaSession = loginAs("hanna@test.com", "test123");
        var kathaSession = loginAs("katha@test.com", "test456");
        var dto = new ItemDto("shared name", LocalDate.of(2025, 1, 1), null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/item")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .session(hannaSession))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/item")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .session(kathaSession))
                .andExpect(status().isCreated());
    }
}
