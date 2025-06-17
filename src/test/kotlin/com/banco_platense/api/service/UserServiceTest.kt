package com.banco_platense.api.service

import com.banco_platense.api.dto.RegistrationRequest
import com.banco_platense.api.dto.RegistrationResult
import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UserDetailsService
import com.banco_platense.api.config.JwtUtil
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID
import com.banco_platense.api.dto.LoginRequest
import com.banco_platense.api.dto.LoginResponse
import com.banco_platense.api.dto.UserData
import org.springframework.security.core.userdetails.UserDetails
import org.junit.jupiter.api.Assertions.assertThrows

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var walletService: WalletService

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    @Mock
    private lateinit var userDetailsService: UserDetailsService

    @Mock
    private lateinit var jwtUtil: JwtUtil

    @InjectMocks
    private lateinit var userService: UserService

    private val validRegistrationRequest = RegistrationRequest(
        email = "test@example.com",
        username = "testuser",
        password = "password123"
    )

    private val existingUser = User(
        id = UUID.randomUUID(),
        email = "existing@example.com",
        username = "existinguser",
        passwordHash = "hashedpassword"
    )

    @BeforeEach
    fun setup() {
        reset(userRepository, walletService, passwordEncoder, authenticationManager, userDetailsService, jwtUtil)
    }

    @Test
    fun `registerUser should successfully register a new user with valid data`() {
        // Given
        val encodedPassword = "encoded_password"
        val savedUserId = UUID.randomUUID()
        whenever(passwordEncoder.encode(validRegistrationRequest.password)).thenReturn(encodedPassword)
        whenever(userRepository.findByEmail(validRegistrationRequest.email)).thenReturn(null)
        whenever(userRepository.findByUsername(validRegistrationRequest.username)).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenReturn(existingUser.copy(id = savedUserId))
        whenever(walletService.createWallet(any())).thenReturn(mock())

        // When
        val result = userService.registerUser(validRegistrationRequest)

        // Then
        assertTrue(result is RegistrationResult.Success)
        assertEquals(savedUserId, (result as RegistrationResult.Success).userId)

        verify(userRepository).findByEmail(validRegistrationRequest.email)
        verify(userRepository).findByUsername(validRegistrationRequest.username)
        verify(userRepository).save(any<User>())
        verify(walletService).createWallet(savedUserId)
    }

    @Test
    fun `registerUser should fail with invalid email format`() {
        // Given
        val invalidEmailRequest = validRegistrationRequest.copy(email = "invalid-email")

        // When
        val result = userService.registerUser(invalidEmailRequest)

        // Then
        assertTrue(result is RegistrationResult.Failure)
        assertEquals("Invalid email format", (result as RegistrationResult.Failure).message)

        verify(userRepository, never()).findByEmail(any())
        verify(userRepository, never()).findByUsername(any())
        verify(userRepository, never()).save(any<User>())
        verify(walletService, never()).createWallet(any())
    }

    @Test
    fun `registerUser should fail when email already exists`() {
        // Given
        whenever(userRepository.findByEmail(validRegistrationRequest.email)).thenReturn(existingUser)

        // When
        val result = userService.registerUser(validRegistrationRequest)

        // Then
        assertTrue(result is RegistrationResult.Failure)
        assertEquals("Email already exists", (result as RegistrationResult.Failure).message)

        verify(userRepository).findByEmail(validRegistrationRequest.email)
        verify(userRepository, never()).findByUsername(any())
        verify(userRepository, never()).save(any<User>())
        verify(walletService, never()).createWallet(any())
    }

    @Test
    fun `registerUser should fail when username already exists`() {
        // Given
        whenever(userRepository.findByEmail(validRegistrationRequest.email)).thenReturn(null)
        whenever(userRepository.findByUsername(validRegistrationRequest.username)).thenReturn(existingUser)

        // When
        val result = userService.registerUser(validRegistrationRequest)

        // Then
        assertTrue(result is RegistrationResult.Failure)
        assertEquals("Username already exists", (result as RegistrationResult.Failure).message)

        verify(userRepository).findByEmail(validRegistrationRequest.email)
        verify(userRepository).findByUsername(validRegistrationRequest.username)
        verify(userRepository, never()).save(any<User>())
        verify(walletService, never()).createWallet(any())
    }

    @Test
    fun `registerUser should encode password before saving`() {
        // Given
        val encodedPassword = "encoded_password_123"
        val savedUserId = UUID.randomUUID()
        whenever(passwordEncoder.encode(validRegistrationRequest.password)).thenReturn(encodedPassword)
        whenever(userRepository.findByEmail(validRegistrationRequest.email)).thenReturn(null)
        whenever(userRepository.findByUsername(validRegistrationRequest.username)).thenReturn(null)
        whenever(userRepository.save(any<User>())).thenAnswer { invocation ->
            val user = invocation.getArgument<User>(0)
            user.copy(id = savedUserId)
        }
        whenever(walletService.createWallet(any())).thenReturn(mock())

        // When
        val result = userService.registerUser(validRegistrationRequest)

        // Then
        assertTrue(result is RegistrationResult.Success)

        verify(passwordEncoder).encode(validRegistrationRequest.password)
        verify(userRepository).save(argThat<User> { user ->
            user.passwordHash == encodedPassword
        })
    }

    @Test
    fun `registerUser should validate email format correctly`() {
        // Test valid email formats
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "123@number-domain.com"
        )

        validEmails.forEach { email ->
            val request = validRegistrationRequest.copy(email = email)
            val savedUserId = UUID.randomUUID()
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded_password")
            whenever(userRepository.findByEmail(email)).thenReturn(null)
            whenever(userRepository.findByUsername(request.username)).thenReturn(null)
            whenever(userRepository.save(any<User>())).thenReturn(existingUser.copy(id = savedUserId))
            whenever(walletService.createWallet(any())).thenReturn(mock())

            val result = userService.registerUser(request)
            assertTrue(result is RegistrationResult.Success, "Email $email should be valid")

            reset(userRepository, walletService, passwordEncoder)
        }

        // Test invalid email formats
        val invalidEmails = listOf(
            "invalid-email",
            "@domain.com",
            "user@",
            "user@domain",
            "user.domain.com",
            ""
        )

        invalidEmails.forEach { email ->
            val request = validRegistrationRequest.copy(email = email)
            val result = userService.registerUser(request)
            assertTrue(result is RegistrationResult.Failure, "Email $email should be invalid")
            assertEquals("Invalid email format", (result as RegistrationResult.Failure).message)
        }
    }

    @Test
    fun `login should return LoginResponse when credentials are valid`() {
        // Given
        val loginRequest = LoginRequest(username = "testuser", password = "password123")
        val mockUserEntity = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            username = "testuser",
            passwordHash = "hashed"
        )
        val mockUserDetails: UserDetails = mock()
        val mockJwtToken = "jwt-token"
        whenever(authenticationManager.authenticate(any())).thenReturn(mock())
        whenever(userDetailsService.loadUserByUsername(loginRequest.username)).thenReturn(mockUserDetails)
        whenever(jwtUtil.generateToken(mockUserDetails)).thenReturn(mockJwtToken)
        whenever(userRepository.findByUsername(loginRequest.username)).thenReturn(mockUserEntity)

        // When
        val response = userService.login(loginRequest)

        // Then
        assertEquals(mockJwtToken, response.token)
        assertEquals(loginRequest.username, response.user.username)
        verify(authenticationManager).authenticate(argThat<UsernamePasswordAuthenticationToken> {
            principal == loginRequest.username && credentials == loginRequest.password
        })
        verify(userDetailsService).loadUserByUsername(loginRequest.username)
        verify(jwtUtil).generateToken(mockUserDetails)
    }

    @Test
    fun `login should throw AuthenticationException when credentials invalid`() {
        // Given
        val loginRequest = LoginRequest(username = "testuser", password = "wrong")
        whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("Invalid credentials"))

        // When/Then
        assertThrows(BadCredentialsException::class.java) {
            userService.login(loginRequest)
        }
    }
}
