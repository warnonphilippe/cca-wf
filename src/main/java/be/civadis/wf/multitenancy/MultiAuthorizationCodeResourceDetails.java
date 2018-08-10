package be.civadis.wf.multitenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

import java.io.Serializable;
import java.util.Optional;

/**
 * Extension de AuthorizationCodeResourceDetails
 * Permet d'injecter le tenant courant dans les urls
 */
@ConfigurationProperties(prefix = "security.oauth2.client")
public class MultiAuthorizationCodeResourceDetails extends AuthorizationCodeResourceDetails implements Serializable {

    //private TenantUtils tenantUtils;

    //public MultiAuthorizationCodeResourceDetails(TenantUtils tenantUtils) {
    //    this.tenantUtils = tenantUtils;
    //}

    @Override
    public String getAccessTokenUri() {
        String uri = super.getAccessTokenUri();
        if (uri != null && getTenant() != null){
            uri = uri.replace(TenantUtils.TENANT_PATH_VAR, getTenant());
            System.out.println("getAccessTokenUri " + uri);
        }
        return uri;
    }

    @Override
    public String getUserAuthorizationUri() {
        String uri = super.getUserAuthorizationUri();
        if (uri != null && getTenant() != null){
            uri = uri.replace(TenantUtils.TENANT_PATH_VAR, getTenant());
            System.out.println("getUserAuthorizationUri " + uri);
        }
        return uri;
    }

    private String getTenant(){
        //get selected tenant
        return Optional.ofNullable(TenantUtils.getTenant()).orElse(TenantUtils.TENANT_PATH_VAR);
    }
}
