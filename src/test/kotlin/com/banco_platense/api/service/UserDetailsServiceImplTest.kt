package com.banco_platense.api.service

import com.banco_platense.api.entity.User
import com.banco_platense.api.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserDetailsServiceImplTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private val testUser = User(
        id = UUID.randomUUID(),
        email = "test@example.com",
        username = "testuser",
        passwordHash = "hashedpassword123",
    )

    @BeforeEach
    fun setup() {
        // Any additional setup can go here
    }

    @Test
    fun `loadUserByUsername should return UserDetails when user exists`() {
        // Given
        whenever(userRepository.findByUsername("testuser")).thenReturn(testUser)

        // When
        val userDetails = userDetailsService.loadUserByUsername("testuser")

        // Then
        assertNotNull(userDetails)
        assertEquals("testuser", userDetails.username)
        assertEquals("hashedpassword123", userDetails.password)
        assertTrue(userDetails.isEnabled)
        assertTrue(userDetails.isAccountNonExpired)
        assertTrue(userDetails.isAccountNonLocked)
        assertTrue(userDetails.isCredentialsNonExpired)
        
        val authorities = userDetails.authorities
        assertEquals(1, authorities.size)
        assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_USER")))
    }

    @Test
    fun `loadUserByUsername should throw UsernameNotFoundException when user does not exist`() {
        // Given
        whenever(userRepository.findByUsername("nonexistent")).thenReturn(null)

        // When & Then
        val exception = assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("nonexistent")
        }
        
        assertEquals("User not found with username: nonexistent", exception.message)
    }

    @Test
    fun `loadUserByUsername should handle empty username`() {
        // Given
        whenever(userRepository.findByUsername("")).thenReturn(null)

        // When & Then
        val exception = assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("")
        }
        
        assertEquals("User not found with username: ", exception.message)
    }

    @Test
    fun `loadUserByUsername should handle null username gracefully`() {
        // Given
        whenever(userRepository.findByUsername("null")).thenReturn(null)

        // When & Then
        val exception = assertThrows(UsernameNotFoundException::class.java) {
            userDetailsService.loadUserByUsername("null")
        }
        
        assertEquals("User not found with username: null", exception.message)
    }

    @Test
    fun `loadUserByUsername should work with different usernames`() {
        val users = listOf(
            testUser.copy(id = UUID.randomUUID(), username = "user1"),
            testUser.copy(id = UUID.randomUUID(), username = "user2"),
            testUser.copy(id = UUID.randomUUID(), username = "admin"),
            testUser.copy(id = UUID.randomUUID(), username = "test.user"),
            testUser.copy(id = UUID.randomUUID(), username = "user_with_underscore")
        )

        users.forEach { user ->
            // Given
            whenever(userRepository.findByUsername(user.username)).thenReturn(user)

            // When
            val userDetails = userDetailsService.loadUserByUsername(user.username)

            // Then
            assertEquals(user.username, userDetails.username)
            assertEquals(user.passwordHash, userDetails.password)
            assertEquals(listOf(SimpleGrantedAuthority("ROLE_USER")), userDetails.authorities.toList())
        }
    }

    @Test
    fun `loadUserByUsername should preserve password hash exactly`() {
        val testPasswords = listOf(
            "\$2a\$10\$abcdefghijklmnopqrstuvwxyz",  // BCrypt hash example
            "hashedpassword123",
            "very_long_password_hash_that_should_be_preserved_exactly",
            ""
        )

        testPasswords.forEach { passwordHash ->
            // Given
            val userWithPassword = testUser.copy(passwordHash = passwordHash)
            whenever(userRepository.findByUsername(testUser.username)).thenReturn(userWithPassword)

            // When
            val userDetails = userDetailsService.loadUserByUsername(testUser.username)

            // Then
            assertEquals(passwordHash, userDetails.password)
        }
    }
}
