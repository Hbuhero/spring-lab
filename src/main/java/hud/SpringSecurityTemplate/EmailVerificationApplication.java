package hud.SpringSecurityTemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EmailVerificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailVerificationApplication.class, args);
	}

}
