package com.banco_platense.api.controller

import com.banco_platense.api.dto.LoginRequest
import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.dto.RegistrationResult
import com.banco_platense.api.service.UserService
import com.banco_platense.api.entity.User as UserEntity
import com.banco_platense.api.entity.Drink
import com.banco_platense.api.config.JwtUtil
import com.banco_platense.api.config.TestJacksonConfig
import com.banco_platense.api.config.TestSecurityConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ExtendWith(SpringExtension::class)
@WebMvcTest(UserController::class)
@Import(TestSecurityConfig::class, TestJacksonConfig::class, ExceptionHandler::class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var authenticationManager: AuthenticationManager

    @MockitoBean
    private lateinit var userDetailsService: UserDetailsService

    @MockitoBean
    private lateinit var jwtUtil: JwtUtil

    private val validRegistrationRequest = RegistrationRequest(
        email = "test@example.com",
        username = "testuser",
        password = "password123"
    )

    private val validLoginRequest = LoginRequest(
        username = "testuser",
        password = "password123"
    )

    private val mockUserDetails = User(
        "testuser",
        "hashedpassword",
        listOf(SimpleGrantedAuthority("ROLE_USER"))
    )

    private val mockUserEntity = UserEntity(
        id = UUID.randomUUID(),
        email = "test@example.com",
        username = "testuser",
        passwordHash = "hashedpassword",
        drinks = Drink.COFFEE
    )

    private val mockJwtToken = "mock.jwt.token"

    @BeforeEach
    fun setup() {
        reset(userService, authenticationManager, userDetailsService, jwtUtil)
    }

    @Test
    fun `register should return success when registration is successful`() {
        // Given
        val userId = UUID.randomUUID()
        whenever(userService.registerUser(any())).thenReturn(RegistrationResult.Success(userId))

        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("User created successfully"))

        verify(userService).registerUser(argThat { request ->
            request.email == validRegistrationRequest.email &&
            request.username == validRegistrationRequest.username &&
            request.password == validRegistrationRequest.password
        })
    }

    @Test
    fun `register should return bad request when registration fails`() {
        // Given
        val errorMessage = "Email already exists"
        whenever(userService.registerUser(any())).thenReturn(RegistrationResult.Failure(errorMessage))

        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(errorMessage))

        verify(userService).registerUser(any())
    }

    @Test
    fun `register should handle invalid email format`() {
        // Given
        val invalidRequest = validRegistrationRequest.copy(email = "invalid-email")
        whenever(userService.registerUser(any())).thenReturn(RegistrationResult.Failure("Invalid email format"))

        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Invalid email format"))
    }

    @Test
    fun `register should handle duplicate username`() {
        // Given
        whenever(userService.registerUser(any())).thenReturn(RegistrationResult.Failure("Username already exists"))

        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegistrationRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Username already exists"))
    }

    @Test
    fun `register should handle missing request body`() {
        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login should return JWT token when credentials are valid`() {
        // Given
        whenever(authenticationManager.authenticate(any())).thenReturn(mock())
        whenever(userDetailsService.loadUserByUsername(validLoginRequest.username)).thenReturn(mockUserDetails)
        whenever(jwtUtil.generateToken(mockUserDetails)).thenReturn(mockJwtToken)
        whenever(userService.getUserByUsername(validLoginRequest.username)).thenReturn(mockUserEntity)

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(mockJwtToken))
            .andExpect(jsonPath("$.user.username").value(validLoginRequest.username))

        verify(authenticationManager).authenticate(argThat<UsernamePasswordAuthenticationToken> { auth ->
            auth.principal == validLoginRequest.username && auth.credentials == validLoginRequest.password
        })
        verify(userDetailsService).loadUserByUsername(validLoginRequest.username)
        verify(jwtUtil).generateToken(mockUserDetails)
        verify(userService).getUserByUsername(validLoginRequest.username)
    }

    @Test
    fun `login should return unauthorized when credentials are invalid`() {
        // Given
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Bad credentials"))

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest))
        )
            .andExpect(status().isUnauthorized)

        verify(authenticationManager).authenticate(any())
        verify(userDetailsService, never()).loadUserByUsername(any())
        verify(jwtUtil, never()).generateToken(any())
    }

    @Test
    fun `login should handle empty username`() {
        // Given
        val emptyUsernameRequest = validLoginRequest.copy(username = "")
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Empty username"))

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyUsernameRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login should handle empty password`() {
        // Given
        val emptyPasswordRequest = validLoginRequest.copy(password = "")
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Empty password"))

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyPasswordRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login should handle missing request body`() {
        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login should handle non-existent user`() {
        // Given
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("User not found"))

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `login should handle invalid content type`() {
        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content")
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    fun `register should handle invalid content type`() {
        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content")
        )
            .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    fun `login should call authentication manager with correct parameters`() {
        // Given
        whenever(authenticationManager.authenticate(any())).thenReturn(mock())
        whenever(userDetailsService.loadUserByUsername(validLoginRequest.username)).thenReturn(mockUserDetails)
        whenever(jwtUtil.generateToken(mockUserDetails)).thenReturn(mockJwtToken)

        // When
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest))
        )

        // Then
        verify(authenticationManager).authenticate(argThat<UsernamePasswordAuthenticationToken> { token ->
            token.principal == "testuser" && 
            token.credentials == "password123" &&
            token.authorities.isEmpty()
        })
    }

    @Test
    fun `register should handle malformed JSON`() {
        // When & Then
        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `login should handle malformed JSON`() {
        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")
        )
            .andExpect(status().isBadRequest)
    }
}
