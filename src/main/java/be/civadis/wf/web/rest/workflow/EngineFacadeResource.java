package be.civadis.wf.web.rest.workflow;

import be.civadis.wf.security.AuthoritiesConstants;
import be.civadis.wf.security.SecurityUtils;
import be.civadis.wf.web.rest.util.PaginationUtil;
import be.civadis.wf.workflow.EngineFacade;
import be.civadis.wf.workflow.dto.ProcessInstanceDto;
import be.civadis.wf.workflow.dto.TaskDto;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
//import org.camunda.bpm.engine.rest.dto.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Ressource permettant d'effectuer des opérations sur EngineFacade (ou directement sur l'API workflow si nécessaire)
 * Les opérations peuvent être sécurisée par oauth2
 *
 * Rem :
 *  L'EngineFacade peut être utilisée dans d'autres ressources rest du service afin de combiner action sur les données et sur workflow
 *  L'API rest native de workflow peut être exposée par Spring-boot, mais elle est sécurisée par basic auth
 *  TODO : si nécessaire, affiner secu (par exemple ouvrir au user ordinaire mais restreindre l'accès uniquement à leurs tâches)
 */
@RestController
@RequestMapping("/workflow")
public class EngineFacadeResource {

    private static List<String> properties = new ArrayList<>();

    private EngineFacade engineFacade;
    //protected RestResponseFactory restResponseFactory;

    public EngineFacadeResource(EngineFacade engineFacade) {
        this.engineFacade = engineFacade;
    }

    @GetMapping(value = "/hello")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<Boolean> hello(){
        return ResponseEntity.ok(true);
    }

    /**
     * Start un process workflow
     * @param processName nom du process
     * @param businessKey clé métier pour identifier l'instance du processus
     * @param variables variables d'initialisation
     * @return
     */
    @PostMapping(value = "/processes/{processName}/start")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<ProcessInstanceDto> startProcess(@PathVariable String processName, @RequestParam(value="businessKey", required = false) String businessKey, @RequestBody Map<String, Object> variables){
        ProcessInstance processInstance = engineFacade.startProcess(processName, businessKey, variables);
        return response(ProcessInstanceDto.fromProcessInstance(processInstance));
    }

    /**
     * Recherche la liste des tasks pouvant être traitées à un user selon ses groupes,
     * @param groups groupes autorisés à traiter les tâches
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/tasks-claimable")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<List<TaskDto>> findClaimableTasks(Pageable pageable,
                                                            @RequestParam(value="user", required=false) String user,
                                                            @RequestParam(value="groups", required=false) List<String> groups,
                                                            @RequestParam(value="processKey", required=false) String processKey,
                                                            @RequestParam(value="processInstanceId", required=false) String processInstanceId,
                                                            @RequestParam(value="processInstanceBusinessKey", required=false)String processInstanceBusinessKey) {

        TaskQuery query = engineFacade.findClaimableTasks(user, groups, processKey, processInstanceId, processInstanceBusinessKey);
        return executeTaskQuery(query, pageable, "/workflow/tasks-claimable");
    }

    /**
     * Recherche la liste des tasks pouvant être traitées à le user courant
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/my-tasks-claimable")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<List<TaskDto>> findMyClaimableTasks(Pageable pageable,
                                                              @RequestParam(value="processKey", required=false) String processKey,
                                                              @RequestParam(value="processInstanceId", required=false) String processInstanceId,
                                                              @RequestParam(value="processInstanceBusinessKey", required=false)String processInstanceBusinessKey) {

        TaskQuery query = engineFacade.findClaimableTasks(getCurrentUser(), getCurrentGroups(), processKey, processInstanceId, processInstanceBusinessKey);
        return executeTaskQuery(query, pageable, "/workflow/my-tasks-claimable");
    }

    /**
     * Recherche la liste des tasks déjà assignées à un user
     * @param user
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/tasks-assigned")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<List<TaskDto>> findAssignedTasks(Pageable pageable,
                                                           @RequestParam("user") String user,
                                                           @RequestParam(value="processKey", required=false) String processKey,
                                                           @RequestParam(value="processInstanceId", required=false) String processInstanceId,
                                                           @RequestParam(value="processInstanceBusinessKey", required=false)String processInstanceBusinessKey) {

        TaskQuery query = engineFacade.findAssignedTasks(user, processKey, processInstanceId, processInstanceBusinessKey);
        return executeTaskQuery(query, pageable, "/workflow/tasks-assigned");
    }

    /**
     * Recherche la liste des tasks déjà assignées user courant
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/my-tasks-assigned")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<List<TaskDto>> findMyAssignedTasks(Pageable pageable,
                                                             @RequestParam(value="processKey", required=false) String processKey,
                                                             @RequestParam(value="processInstanceId", required=false) String processInstanceId,
                                                             @RequestParam(value="processInstanceBusinessKey", required=false)String processInstanceBusinessKey) {

        TaskQuery query = engineFacade.findAssignedTasks(getCurrentUser(), processKey, processInstanceId, processInstanceBusinessKey);
        return executeTaskQuery(query, pageable, "/workflow/my-tasks-assigned");
    }

    /**
     * Demande l'assignation d'une task à un user
     * @param taskId
     * @param userId
     */
    @PostMapping(value = "/tasks/{taskId}/claim")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> claim(@PathVariable("taskId") String taskId, @RequestParam("userId") String userId){
        Task task = engineFacade.findClaimableTask(taskId, null, null);
        if (task != null){
            engineFacade.claim(taskId, userId);
            return response(true);
        }
        return response(false);
    }

    /**
     * Demande l'assignation d'une task au user courant
     * @param taskId
     */
    @PostMapping(value = "/my-tasks/{taskId}/claim")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<Boolean> myClaim(@PathVariable("taskId") String taskId){
        Task task = engineFacade.findClaimableTask(taskId, getCurrentUser(), getCurrentGroups());
        if (task != null){
            engineFacade.claim(taskId, getCurrentUser());
            return response(true);
        }
        return response(false);
    }

    /**
     * Annulation de l'assignation de la task
     * @param taskId
     */
    @PostMapping(value = "/tasks/{taskId}/unclaim")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> unclaim(@PathVariable("taskId") String taskId){
        engineFacade.unclaim(taskId);
        return response(true);
    }

    /**
     * Annulation de l'assignation de la task, check si associée au user courant
     * @param taskId
     */
    @PostMapping(value = "/my-tasks/{taskId}/unclaim")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<Boolean> myUnclaim(@PathVariable("taskId") String taskId){
        Task task = engineFacade.findAssignedTask(taskId, getCurrentUser());
        if (task != null){
            engineFacade.unclaim(taskId);
            return response(true);
        }
        return response(false);
    }

    /**
     * Complete une task
     * @param taskId
     * @param params
     */
    @PostMapping(value = "/tasks/{taskId}/complete")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> completeTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params){
        engineFacade.completeTask(taskId, params);
        return response(true);
    }

    /**
     * Complete une task, check si associée au user courant
     * @param taskId
     * @param params
     */
    @PostMapping(value = "/my-tasks/{taskId}/complete")
    @Secured({AuthoritiesConstants.SERVICE_WF_CLIENT})
    public ResponseEntity<Boolean> myCompleteTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params){
        //check si le user peut traité la tache ou si elle lui est assignée
        Task task = engineFacade.findClaimableTask(taskId, getCurrentUser(), getCurrentGroups());
        if (task == null){
            task = engineFacade.findAssignedTask(taskId, getCurrentUser());
        }
        //si ok, complete task
        if (task != null){
            engineFacade.completeTask(taskId, params);
            return response(true);
        }
        return response(false);
    }

    private ResponseEntity<List<TaskDto>> executeTaskQuery(TaskQuery query, Pageable pageable, String url){

        //sort
        if (pageable != null && pageable.getSort() != null){

            Iterator<Sort.Order> iterator = pageable.getSort().iterator();
            while(iterator.hasNext()){
                Sort.Order order = iterator.next();
                String prop = order.getProperty();

                boolean asc = true;
                if (order.getDirection() != null && order.getDirection().isDescending()){
                    asc = false;
                }

                boolean ordered = true;

                if ("id".equalsIgnoreCase(prop)){
                    query.orderByTaskId();
                } else if ("name".equalsIgnoreCase(prop)){
                    query.orderByTaskName();
                } else if ("description".equalsIgnoreCase(prop)){
                    query.orderByTaskDescription();
                } else if ("dueDate".equalsIgnoreCase(prop)){
                    query.orderByDueDate();
                } else if ("createTime".equalsIgnoreCase(prop)){
                    query.orderByTaskCreateTime();
                } else if ("priority".equalsIgnoreCase(prop)){
                    query.orderByTaskPriority();
                } else if ("executionId".equalsIgnoreCase(prop)){
                    query.orderByExecutionId();
                } else if ("tenantId".equalsIgnoreCase(prop)){
                    query.orderByTenantId();
                } else if ("assignee".equalsIgnoreCase(prop)){
                    query.orderByTaskAssignee();
                } else if ("processInstanceId".equalsIgnoreCase(prop)){
                    query.orderByProcessInstanceId();
                } else {
                    ordered = false;
                }

                if (ordered){
                    if (asc){
                        query.asc();
                    } else {
                        query.desc();
                    }
                }

            }

        }

        //query
        List<TaskDto> taskList = new ArrayList<>();
        query.listPage((int) pageable.getOffset(), pageable.getPageSize()).stream().forEach(
            item -> taskList.add(TaskDto.fromEntity(item))
        );

        //response
        return response(
            PaginationUtil.generatePaginationHttpHeaders(new PageImpl(taskList, pageable, query.count()), url), taskList);
    }

    private <T> ResponseEntity<T> response(T entity){
        return ResponseEntity.ok()
            .body(entity);
    }

    private <T> ResponseEntity<T> response(HttpHeaders headers, T entity){
        return ResponseEntity.ok()
            .headers(headers)
            .body(entity);
    }

    private String getCurrentUser(){
        return SecurityUtils.getCurrentUserLogin().orElse("");
    }

    private List<String> getCurrentGroups(){
        //REM : Nous ne définissons pas de user / group dans la secu d'activiti/camunda, nous utilisons les users et roles oauth2
       return SecurityUtils.getRoles();
    }


}
