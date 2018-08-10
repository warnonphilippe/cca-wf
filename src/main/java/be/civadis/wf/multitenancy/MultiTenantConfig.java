package be.civadis.wf.multitenancy;

import be.civadis.wf.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * Beans spring nécessaire au multitenant
 */
@Configuration
@EnableTransactionManagement
public class MultiTenantConfig {

    private final Logger log = LoggerFactory.getLogger(MultiTenantConfig.class);

    private final Environment env;
    private JpaProperties jpaProperties;
    private ApplicationProperties applicationProperties;

    public MultiTenantConfig(Environment env, JpaProperties jpaProperties, ApplicationProperties applicationProperties) {
        this.env = env;
        this.jpaProperties = jpaProperties;
        this.applicationProperties = applicationProperties;
    }

    //secu oauth2

    @Bean(name = "multiResourceServerProperties")
    @Primary
    public ResourceServerProperties multiResourceServerProperties(){
        return new MultiResourceServerProperties();
    }

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    @Primary
    public ClientCredentialsResourceDetails oauth2RemoteResource() {
        ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        return details;
    }

    @Bean
    public OAuth2ClientContext oauth2ClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }

    @Bean(name = "multiAuthorizationCodeResourceDetails")
    public AuthorizationCodeResourceDetails multiAuthorizationCodeResourceDetails(){
        return new MultiAuthorizationCodeResourceDetails();
    }

    @Bean
    @Primary
    public OAuth2RestTemplate oauth2RestTemplate(@Qualifier("multiAuthorizationCodeResourceDetails") OAuth2ProtectedResourceDetails details,
                                                 OAuth2ClientContext oauth2ClientContext) {
        OAuth2RestTemplate template = new OAuth2RestTemplate(details, oauth2ClientContext);
        return template;
    }

    @Bean
    @Primary
    public UserInfoRestTemplateFactory userInfoRestTemplateFactory(){
        return new UserInfoRestTemplateFactory() {
            @Override
            public OAuth2RestTemplate getUserInfoRestTemplate() {
                return oauth2RestTemplate(multiAuthorizationCodeResourceDetails(), oauth2ClientContext());
            }
        };
    }

    //liquibase : Pas de config Multitenant car on n'a que la db camunda gérée par le starter spring boot

    //hibernate : Pas de config Multitenant car on n'a que la db camunda gérée par le starter spring boot


}
