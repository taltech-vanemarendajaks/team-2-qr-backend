package ee.valiit.mystuffback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MystuffbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(MystuffbackApplication.class, args);
	}

}
