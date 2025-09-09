package core.ms;

import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Profile;

@TestConfiguration(proxyBeanMethods = false)
@Profile("test")  // Add this to ensure it onl
class TestcontainersConfiguration {




}
