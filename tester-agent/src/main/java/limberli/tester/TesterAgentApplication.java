package limberli.tester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TesterAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TesterAgentApplication.class, args);
    }
}
