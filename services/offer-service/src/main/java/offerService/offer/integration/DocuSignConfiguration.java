package offerService.offer.integration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
        DocuSignProperties.class
)
public class DocuSignConfiguration {
}
