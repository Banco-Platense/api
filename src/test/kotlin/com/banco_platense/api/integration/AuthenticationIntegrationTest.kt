package com.banco_platense.api.integration

import com.banco_platense.api.ApiApplication
import com.banco_platense.api.config.TestApplicationConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.banco_platense.api.dto.LoginRequest
import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import com.banco_platense.api.repository.WalletRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    classes = [
        ApiApplication::class,
        TestSecurityConfig::class,
        TestApplicationConfig::class
    ],
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var walletRepository: WalletRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var mockMvc: MockMvc

    private val validRegistrationRequest = RegistrationRequest(
        email = "integration@test.com",
        username = "integrationuser",
        password = "securepassword123"
    )

    private val validLoginRequest = LoginRequest(
        username = "integrationuser",
        password = "securepassword123"
    )

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        // Clean up any existing test data
        userRepository.deleteAll()
        walletRepository.deleteAll()
    }

    @AfterEach
    fun cleanup() {
        userRepository.deleteAll()
        walletRepository.deleteAll()
    }

    @Test
    fun `complete registration and login flow should work end-to-end`() {
        // Step 1: Register a new user
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("User created successfully"))

        // Verify user was created in database
        val savedUser = userRepository.findByUsername(validRegistrationRequest.username)
        assertNotNull(savedUser)
        assertEquals(validRegistrationRequest.email, savedUser!!.email)
        assertEquals(validRegistrationRequest.username, savedUser.username)
        assertTrue(passwordEncoder.matches(validRegistrationRequest.password, savedUser.passwordHash))

        // Verify wallet was created for the user
        val userWallet = walletRepository.findByUserId(savedUser.id!!)
        assertNotNull(userWallet)
        assertEquals(0.0, userWallet!!.balance)

        // Step 2: Login with the registered user
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.username").value(validLoginRequest.username))
    }

    @Test
    fun `registration should fail for duplicate email`() {
        // First registration
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)

        // Second registration with same email but different username
        val duplicateEmailRequest = validRegistrationRequest.copy(username = "differentuser")
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateEmailRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Email already exists"))
    }

    @Test
    fun `registration should fail for duplicate username`() {
        // First registration
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)

        // Second registration with same username but different email
        val duplicateUsernameRequest = validRegistrationRequest.copy(email = "different@test.com")
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUsernameRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Username already exists"))
    }

    @Test
    fun `login should fail for non-existent user`() {
        val nonExistentUserLogin = LoginRequest(
            username = "nonexistent",
            password = "password123"
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserLogin))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login should fail for wrong password`() {
        // First register a user
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)

        // Try to login with wrong password
        val wrongPasswordLogin = validLoginRequest.copy(password = "wrongpassword")
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordLogin))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `registration should handle invalid email formats`() {
        val invalidEmailRequests = listOf(
            validRegistrationRequest.copy(email = "invalid-email"),
            validRegistrationRequest.copy(email = "@domain.com"),
            validRegistrationRequest.copy(email = "user@"),
            validRegistrationRequest.copy(email = "user.domain.com")
        )

        invalidEmailRequests.forEach { request ->
            mockMvc.perform(
                post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().string("Invalid email format"))
        }
    }

    @Test
    fun `panchubi user should get MATCHA drink preference`() {
        val panchubiRequest = validRegistrationRequest.copy(
            username = "panchubi_test",
            email = "panchubi@test.com"
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(panchubiRequest))
        )
            .andExpect(status().isOk)

        val savedUser = userRepository.findByUsername("panchubi_test")
        assertNotNull(savedUser)
        assertEquals(com.banco_platense.api.entity.Drink.MATCHA, savedUser!!.drinks)
    }

    @Test
    fun `regular user should get COFFEE drink preference`() {
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)

        val savedUser = userRepository.findByUsername(validRegistrationRequest.username)
        assertNotNull(savedUser)
        assertEquals(com.banco_platense.api.entity.Drink.COFFEE, savedUser!!.drinks)
    }

    @Test
    fun `registration should create wallet with zero balance`() {
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)

        val savedUser = userRepository.findByUsername(validRegistrationRequest.username)
        assertNotNull(savedUser)

        val wallet = walletRepository.findByUserId(savedUser!!.id!!)
        assertNotNull(wallet)
        assertEquals(0.0, wallet!!.balance)
        assertEquals(savedUser.id, wallet.userId)
    }

    @Test
    fun `multiple users can be registered and login independently`() {
        val user1Request = RegistrationRequest(
            email = "user1@test.com",
            username = "user1",
            password = "password1"
        )

        val user2Request = RegistrationRequest(
            email = "user2@test.com", 
            username = "user2",
            password = "password2"
        )

        // Register first user
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1Request))
        )
            .andExpect(status().isOk)

        // Register second user
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Request))
        )
            .andExpect(status().isOk)

        // Login as first user
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest("user1", "password1")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("user1"))

        // Login as second user
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequest("user2", "password2")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("user2"))

        // Verify both users exist in database
        assertNotNull(userRepository.findByUsername("user1"))
        assertNotNull(userRepository.findByUsername("user2"))
        assertEquals(2, userRepository.count())
    }
}
