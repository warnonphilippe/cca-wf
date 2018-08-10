package be.civadis.wf.multitenancy;

import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;

import java.io.Serializable;
import java.util.Optional;

/**
 * Extension de ResourceServerProperties
 * Permet d'injecter le tenant courant dans les urls
 */
public class MultiResourceServerProperties extends ResourceServerProperties implements Serializable{

    //private TenantUtils tenantUtils;

    //public MultiResourceServerProperties(TenantUtils tenantUtils) {
    //    this.tenantUtils = tenantUtils;
    //}

    //public MultiResourceServerProperties(String clientId, String clientSecret, TenantUtils tenantUtils) {
    //    super(clientId, clientSecret);
    //    this.tenantUtils = tenantUtils;
    //}

    @Override
    public String getUserInfoUri() {
        String uri = super.getUserInfoUri();
        if (uri != null && getTenant() != null){
            uri = uri.replace(TenantUtils.TENANT_PATH_VAR, getTenant());
            System.out.println("UserInfoUri " + uri);
        }
        return uri;
    }

    @Override
    public String getTokenInfoUri() {
        String uri = super.getTokenInfoUri();
        if (uri != null && getTenant() != null){
            uri = uri.replace(TenantUtils.TENANT_PATH_VAR, getTenant());
            System.out.println("TokenInfoUri " + uri);
        }
        return uri;
    }

    @Override
    public Jwt getJwt() {
        Jwt jwt = super.getJwt();
        if (jwt != null && jwt.getKeyUri() != null){
            jwt.setKeyUri(jwt.getKeyUri().replace(TenantUtils.TENANT_PATH_VAR, getTenant()));
        }
        return jwt;
    }

    private String getTenant(){
        //get selected tenant
        return Optional.ofNullable(TenantUtils.getTenant()).orElse(TenantUtils.TENANT_PATH_VAR);
    }
}
