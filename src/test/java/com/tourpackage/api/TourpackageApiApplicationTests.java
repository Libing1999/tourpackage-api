package com.tourpackage.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Pinned to the test profile like every other test: without it this ran
// Flyway against the development database, which a test must never touch.
@ActiveProfiles("test")
@SpringBootTest
class TourpackageApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
