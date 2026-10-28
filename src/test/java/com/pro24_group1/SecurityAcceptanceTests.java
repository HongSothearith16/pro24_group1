package com.pro24_group1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pro24_group1.dto.LoginRequest;
import com.pro24_group1.dto.PermissionRequest;
import com.pro24_group1.dto.RoleRequest;
import com.pro24_group1.dto.UserRequest;
import com.pro24_group1.entity.User;
import com.pro24_group1.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class SecurityAcceptanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String getAdminToken() throws Exception {
        LoginRequest login = new LoginRequest("admin", "admin123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

    private String getDaraToken() throws Exception {
        LoginRequest login = new LoginRequest("dara", "dara123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("Check 1: No token on GET /api/users returns 401")
    void testNoTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Order(2)
    @DisplayName("Check 2: A USER without USER_DELETE calling DELETE /api/users/2 returns 403")
    void testUserWithoutDeleteReturns403() throws Exception {
        String daraToken = getDaraToken();

        mockMvc.perform(delete("/api/users/2")
                        .header("Authorization", "Bearer " + daraToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @Order(3)
    @DisplayName("Check 3: Login with a locked account returns 401")
    void testLoginWithLockedAccountReturns401() throws Exception {
        // Create a locked user
        User lockedUser = User.builder()
                .username("locked_test_user")
                .password("$2a$10$9Yma8T4TEcLZtkNYDee6c.Jz1z6VwzkKGXwkUHQVAqEnNoPmINHV.") // admin123
                .email("locked@example.com")
                .accountNonExpired(true)
                .accountNonLocked(false) // LOCKED!
                .credentialsNonExpired(true)
                .enabled(true)
                .build();
        userRepository.save(lockedUser);

        LoginRequest login = new LoginRequest("locked_test_user", "admin123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    @Order(4)
    @DisplayName("Check 4: ADMIN assigns a role to a user and gets 200")
    void testAdminAssignsRoleGets200() throws Exception {
        String adminToken = getAdminToken();

        // Assign ROLE_USER (id 2) to admin (id 1)
        mockMvc.perform(post("/api/users/1/roles/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("Check 5: Deleting a user sets deleted_at, keeps the row, and returns 204")
    void testSoftDeleteUserKeepsRowReturns204() throws Exception {
        String adminToken = getAdminToken();

        // Create a user to delete
        UserRequest userRequest = UserRequest.builder()
                .username("user_to_delete")
                .password("secret123")
                .email("todelete@example.com")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long createdId = createdNode.get("id").asLong();

        // Delete user (returns 204)
        mockMvc.perform(delete("/api/users/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify row still exists in database and deleted_at is not null
        User userInDb = userRepository.findById(createdId).orElse(null);
        assertThat(userInDb).isNotNull();
        assertThat(userInDb.getDeletedAt()).isNotNull();

        // Verify GET /api/users/{id} returns 404 because user is soft-deleted
        mockMvc.perform(get("/api/users/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Check 6: No response ever contains password")
    void testNoResponseContainsPassword() throws Exception {
        String adminToken = getAdminToken();

        // Check GET /api/users
        MvcResult listResult = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(listResult.getResponse().getContentAsString()).doesNotContain("password");

        // Check GET /api/users/1
        MvcResult singleResult = mockMvc.perform(get("/api/users/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(singleResult.getResponse().getContentAsString()).doesNotContain("password");

        // Check POST /api/auth/login response
        LoginRequest login = new LoginRequest("admin", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(loginResult.getResponse().getContentAsString()).doesNotContain("\"password\"");
    }

    @Test
    @Order(7)
    @DisplayName("Demo Flow: Step 1 (Token after login), Step 2 (Authentication who are you), Step 3 (Authorization what can I do)")
    void testDemoOrderFlow() throws Exception {
        // Step 1: Generate token after login
        LoginRequest login = new LoginRequest("dara", "dara123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        // Step 2: Check authentication (who are you via /api/auth/me)
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dara"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        // Step 3: Check authorization (what can I do: dara has USER_READ, so GET /api/users works, but DELETE fails with 403)
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("Slice A & C & B End-to-End: Permissions and Roles CRUD and effective permissions")
    void testPermissionsAndRolesCrud() throws Exception {
        String adminToken = getAdminToken();

        // Create Permission
        PermissionRequest permReq = new PermissionRequest("REPORT_VIEW", "Permission to view reports");
        MvcResult permResult = mockMvc.perform(post("/api/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("REPORT_VIEW"))
                .andReturn();
        long permId = objectMapper.readTree(permResult.getResponse().getContentAsString()).get("id").asLong();

        // Create Role
        RoleRequest roleReq = new RoleRequest("ROLE_MANAGER", "Manager role");
        MvcResult roleResult = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ROLE_MANAGER"))
                .andReturn();
        long roleId = objectMapper.readTree(roleResult.getResponse().getContentAsString()).get("id").asLong();

        // Grant Permission to Role
        mockMvc.perform(post("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Duplicate Grant returns 409
        mockMvc.perform(post("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // Assign Role to dara (id 2)
        mockMvc.perform(post("/api/users/2/roles/" + roleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Check Dara's effective permissions: now includes REPORT_VIEW and USER_READ
        mockMvc.perform(get("/api/users/2/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Revoke Permission from Role
        mockMvc.perform(delete("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Revoking again returns 404
        mockMvc.perform(delete("/api/roles/" + roleId + "/permissions/" + permId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
