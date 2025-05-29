package com.banco_platense.api

import com.banco_platense.api.config.TestSecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
	properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@ActiveProfiles("test")
class ApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
