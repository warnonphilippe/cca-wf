package be.civadis.wf.web.rest.workflow;

import be.civadis.wf.security.AuthoritiesConstants;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.DeploymentQueryProperty;
import org.camunda.bpm.engine.query.QueryProperty;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.DeploymentQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Ressource permettant le déploiements de processus (peut être sécurisée par oauth2)
 * Rem :
 *  Les déploiements peuvent aussi être réalisés par l'UI workflow
 *  L'API rest native de workflow peut être exposée par Spring-boot, mais elle est sécurisée par basic oauth
 */
@RestController
@RequestMapping("/workflow")
public class CustomDeploymentResource {
    protected static final String DEPRECATED_API_DEPLOYMENT_SEGMENT = "deployment";
    private static Map<String, QueryProperty> allowedSortProperties = new HashMap();
    @Autowired
    protected RepositoryService repositoryService;

    public CustomDeploymentResource() {
    }

    /**
     * Retourne la liste des déploiements
     * @param allRequestParams
     * @param request
     * @return
     */
    @GetMapping("/deployments")
    @Secured({AuthoritiesConstants.ADMIN})
    public List<Deployment> getDeployments(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        DeploymentQuery deploymentQuery = this.repositoryService.createDeploymentQuery();
        if (allRequestParams.containsKey("name")) {
            deploymentQuery.deploymentName((String)allRequestParams.get("name"));
        }

        if (allRequestParams.containsKey("nameLike")) {
            deploymentQuery.deploymentNameLike((String)allRequestParams.get("nameLike"));
        }

        if (allRequestParams.containsKey("tenantId")) {
            deploymentQuery.tenantIdIn(((String)allRequestParams.get("tenantId")));
        }

        if (allRequestParams.containsKey("withoutTenantId")) {
            Boolean withoutTenantId = Boolean.valueOf((String)allRequestParams.get("withoutTenantId"));
            if (withoutTenantId.booleanValue()) {
                deploymentQuery.withoutTenantId();
            }
        }

        deploymentQuery.orderByDeploymentId();
        return deploymentQuery.list();
    }

    /**
     * Déploie une définition de processus
     * @param tenantId
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/deployments")
    @Secured({AuthoritiesConstants.ADMIN})
    public Deployment uploadDeployment(@RequestParam(value = "tenantId",required = false) String tenantId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new IllegalArgumentException("Multipart request is required");
        } else {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
            if (multipartRequest.getFileMap().size() == 0) {
                throw new IllegalArgumentException("Multipart request with file content is required");
            } else {
                MultipartFile file = (MultipartFile)multipartRequest.getFileMap().values().iterator().next();

                DeploymentBuilder deploymentBuilder = this.repositoryService.createDeployment();
                String fileName = file.getOriginalFilename();
                if (StringUtils.isEmpty(fileName) || !fileName.endsWith(".bpmn20.xml") && !fileName.endsWith(".bpmn") && !fileName.toLowerCase().endsWith(".bar") && !fileName.toLowerCase().endsWith(".zip")) {
                    fileName = file.getName();
                }

                if (!fileName.endsWith(".bpmn20.xml") && !fileName.endsWith(".bpmn")) {
                    if (!fileName.toLowerCase().endsWith(".bar") && !fileName.toLowerCase().endsWith(".zip")) {
                        throw new IllegalArgumentException("File must be of type .bpmn20.xml, .bpmn, .bar or .zip");
                    }

                    deploymentBuilder.addZipInputStream(new ZipInputStream(file.getInputStream()));
                } else {
                    deploymentBuilder.addInputStream(fileName, file.getInputStream());
                }

                deploymentBuilder.name(fileName);
                if (tenantId != null) {
                    deploymentBuilder.tenantId(tenantId);
                }

                Deployment deployment = deploymentBuilder.deploy();
                response.setStatus(HttpStatus.CREATED.value());
                return deployment;
            }
        }
    }

    static {
        allowedSortProperties.put("id", DeploymentQueryProperty.DEPLOYMENT_ID);
        allowedSortProperties.put("name", DeploymentQueryProperty.DEPLOYMENT_NAME);
        allowedSortProperties.put("deployTime", DeploymentQueryProperty.DEPLOY_TIME);
        allowedSortProperties.put("tenantId", DeploymentQueryProperty.TENANT_ID);
    }
}
