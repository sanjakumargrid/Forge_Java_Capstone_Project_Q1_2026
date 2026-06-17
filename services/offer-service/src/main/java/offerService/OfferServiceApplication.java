package offerService;

import offerService.offer.integration.DocuSignProperties;
import offerService.offer.integration.GeminiProperties;
import offerService.offer.integration.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        scanBasePackages = {
                "offerService",
                "com.talentgrid.kafka",
                "com.talentgrid.clients.notification",
                "com.talentgrid.audit"
        }
)
@EnableConfigurationProperties({
        GeminiProperties.class,
        DocuSignProperties.class,
        OpenAiProperties.class
})
public class OfferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
    }
}