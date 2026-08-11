package com.careerpilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest
@ActiveProfiles("test")
class CareerpilotApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    private org.springframework.data.redis.connection.ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

	@Test
	void contextLoads() {
	}

}
