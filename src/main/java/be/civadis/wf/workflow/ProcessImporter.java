package be.civadis.wf.workflow;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

@Component
public class ProcessImporter {

    private RepositoryService repositoryService;

    public ProcessImporter(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * Importer les processus du répertoire donné pour les tenants donnés
     * @param folderPath
     * @param tenantList
     */
    public void importFiles(String folderPath, List<String> tenantList){

        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();

        for (String tenantId : tenantList){

            for (File file : listFiles(folderPath)){

                try {
                    if (file.isFile()){
                        deploymentBuilder.addInputStream(file.getName(), new FileInputStream(file));
                        deploymentBuilder.name(file.getName());
                        deploymentBuilder.tenantId(tenantId);
                        System.out.println("*************************** Deploying " + file.getName() + "to " + tenantId + " *****************************");
                        deploymentBuilder.deploy();
                    }
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Recherche les files dans un répertoire donné
     * @param folderPath
     * @return
     */
    public File[] listFiles(String folderPath){
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(folderPath);
        if (url != null){
            String path = url.getPath();
            File folder = new File(path);
            if (folder != null){
                return folder.listFiles();
            }
        }
        return new File[]{};
    }

}
