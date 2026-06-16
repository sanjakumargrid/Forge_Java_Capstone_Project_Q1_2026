package offerService.offer.integration;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "docusign")
public class DocuSignProperties {

    private String integrationKey;
    private String userId;
    private String accountId;
    private String basePath;
    private String privateKeyPath;
}