package core.ms;

import org.springframework.boot.SpringApplication;

public class TestMsApplication {

    public static void main(String[] args) {
        SpringApplication.from(MsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
