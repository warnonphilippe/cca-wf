package be.civadis.wf.config;

import be.civadis.wf.multitenancy.CustomServiceUserInfoTokenServices;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import be.civadis.wf.security.oauth2.SimpleAuthoritiesExtractor;
import be.civadis.wf.security.oauth2.SimplePrincipalExtractor;
import org.springframework.context.annotation.Primary;

@Configuration
public class OAuth2TokenServicesConfiguration {

    private static final String OAUTH2_PRINCIPAL_ATTRIBUTE = "preferred_username";
    private static final String OAUTH2_AUTHORITIES_ATTRIBUTE = "roles";

    private final ResourceServerProperties resourceServerProperties;

	public OAuth2TokenServicesConfiguration(ResourceServerProperties resourceServerProperties) {
		this.resourceServerProperties = resourceServerProperties;
	}

    @Bean
    public PrincipalExtractor principalExtractor() {
        return new SimplePrincipalExtractor(OAUTH2_PRINCIPAL_ATTRIBUTE);
    }

    @Bean
    public AuthoritiesExtractor authoritiesExtractor() {
        return new SimpleAuthoritiesExtractor(OAUTH2_AUTHORITIES_ATTRIBUTE);
    }

    @Bean
    @Primary
    public CustomServiceUserInfoTokenServices userInfoTokenServices(PrincipalExtractor principalExtractor, AuthoritiesExtractor authoritiesExtractor) {
        CustomServiceUserInfoTokenServices userInfoTokenServices =
            new CustomServiceUserInfoTokenServices(resourceServerProperties.getUserInfoUri(), resourceServerProperties.getClientId());

        userInfoTokenServices.setPrincipalExtractor(principalExtractor);
        userInfoTokenServices.setAuthoritiesExtractor(authoritiesExtractor);
        return userInfoTokenServices;
    }

}
