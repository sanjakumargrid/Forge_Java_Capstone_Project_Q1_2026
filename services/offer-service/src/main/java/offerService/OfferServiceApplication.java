package offerService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "offerService",
        "com.talentgrid.audit.client",
        "com.talentgrid.kafka.producer"
})
public class OfferServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferServiceApplication.class, args);
    }
}