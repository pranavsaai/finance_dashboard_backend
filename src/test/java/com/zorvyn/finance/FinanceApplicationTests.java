package com.zorvyn.finance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/finance_test"
})
class FinanceApplicationTests {

    @Test
    void contextLoads() {
        // it will verify the Spring context starts up correctly with all beans wired
    }
}
