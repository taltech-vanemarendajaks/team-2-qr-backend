package ee.valiit.mystuffback.infrastructure.config;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailtrapClientConfig {

    @Value("${mailtrap.api-token}")
    private String apiToken;

    @Bean
    public MailtrapClient mailtrapClient() {
        MailtrapConfig config = new MailtrapConfig.Builder()
                .token(apiToken)
                .build();
        return MailtrapClientFactory.createMailtrapClient(config);
    }
}
