package be.civadis.wf.workflow;

import be.civadis.wf.config.ApplicationProperties;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;

//@Configuration
public class WorkflowConfiguration  { //AbstractProcessEngineAutoConfiguration

    //ATTENTION, PATCH de la config Jhipster pour utiliser Camunda

    @Autowired
    private ApplicationProperties applicationProperties;

    //TODO : A adapter pour Camunda
    // faire une seule DB ou une séparée pour camunda et le service ? A priori le service ne devrait contenir que camunda
    // si pas de création auto, on injectera manuellement par l'UI ?


    /**
     * Crée un bean d'initialisation qui va importer les définitions de processus du répertoire resources/processes dans workflow
     * Par exemple, si les tenants sont tenant1 et tenant2 (définis dans la config de l'application, propriété schemas)
     *  -> le répertoire resources/processes doit alors être structuté comme ceci:
     *      /all : les fichiers seront importés et associés à tenant1 et tenant2
     *      /tenant1 : fichiers importés uniquement pour tenant1
     *      /tenant2 : fichiers importés uniquement pour tenant2
     *  Par défaut,
     *      spring boot importe tous les fichiers à la racine de processes, mais sans les associés aux tenants,
     *      celà ne convient donc que dans un environnement single tenant
     * @param importer
     * @param applicationProperties
     * @return
     */

    //@Bean
    InitializingBean deploy(final ProcessImporter importer, final ApplicationProperties applicationProperties) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                if (applicationProperties.getSchemas() != null){

                    //deploy DMN
/*
                    // import des processus généraux (sous le répertoire /all)
                    importer.importFiles("decision/all", applicationProperties.getSchemas());

                    //import des processus spécifiques (sous les répertoires associés à des tenants)
                    for (String tenant : applicationProperties.getSchemas()){
                        importer.importFiles("decision/" + tenant, Arrays.asList(tenant));
                    }
*/
                    //deploy BPMN
/*
                    // import des processus généraux (sous le répertoire /all)
                    importer.importFiles("processes/all", applicationProperties.getSchemas());

                    //import des processus spécifiques (sous les répertoires associés à des tenants)
                    for (String tenant : applicationProperties.getSchemas()){
                        importer.importFiles("processes/" + tenant, Arrays.asList(tenant));
                    }
                    */
                }
            }
        };
    }


    // pour demo
    //@Bean
    InitializingBean deployHiring(final RepositoryService repositoryService) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                try {
                    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();


                    String fileName = "processes/all/Developer_Hiring.bpmn";

                    ClassLoader classLoader = getClass().getClassLoader();
                    File file = new File(classLoader.getResource(fileName).getFile());

                    deploymentBuilder.addInputStream(fileName, new FileInputStream(file));

                    deploymentBuilder.name(fileName);
                    deploymentBuilder.tenantId("jhipster");

                    Deployment deployment = deploymentBuilder.deploy();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
                }

            }

        };


    }

    /**
     * Création de groupe et user pour la basic auth de l'api rest native activiti
     * @param identityService
     * @return
     */
    // pour demo,
    //@Bean
    InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {
        // TODO : si besoin de l'api en prod, créer les groupes et users sécurisés (ldap, db,... ?)
        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                Group group = identityService.newGroup("user");
                group.setName("users");
                group.setType("security-role");
                identityService.saveGroup(group);

                User admin = identityService.newUser("admin");
                admin.setPassword("admin");
                identityService.saveUser(admin);

            }
        };
    }


}
