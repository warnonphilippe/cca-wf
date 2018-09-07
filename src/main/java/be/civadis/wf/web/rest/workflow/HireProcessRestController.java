package be.civadis.wf.web.rest.workflow;

import be.civadis.wf.domain.Applicant;
import be.civadis.wf.workflow.EngineFacade;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/workflow")
public class HireProcessRestController {

    @Autowired
    private EngineFacade engineFacade;

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/start-hire-process", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    //@Secured({AuthoritiesConstants.ADMIN})
    public void startHireProcess(@RequestParam(value="businessKey", required = false) String businessKey, @RequestBody Map<String, String> data) {

        Applicant applicant = new Applicant(data.get("name"), data.get("email"), data.get("phoneNumber"));

        Map<String, Object> vars = Collections.<String, Object>singletonMap("applicant", applicant);
        engineFacade.startProcess("hireProcessWithJpa", businessKey, vars);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/test-hire-process", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public void testHireProcess(){
        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@workflow.org", "12344");
        //applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        variables.put("saison", "ete");
        //variables.put("saison", "printemps");
        variables.put("service", "csd");
        variables.put("salaire", 4000.0);
        variables.put("salaireMax", 5000.0);
        ProcessInstance processInstance = engineFacade.startProcess("hireProcessWithJpa", "JohnDoe", variables);

        // First, the 'phone interview' should be active
        Task task = engineFacade.findClaimableTasks(
            null,
            Arrays.asList("dev-managers"),
            null, processInstance.getId(), null).singleResult();

        if (task != null){

            if (!"Telephone interview".equalsIgnoreCase(task.getName())){
                throw new RuntimeException("Tel interview should be active");
            }

            // Claim the task
            engineFacade.claim(task.getId(), "phw");

            // Check assigned task
            List<Task> taskAssignedList = engineFacade.findAssignedTasks("phw", null, null, null).list();
            if (!"Telephone interview".equalsIgnoreCase(taskAssignedList.get(0).getName())){
                throw new RuntimeException("Tel interview should be assigned");
            }

            // Completing the phone interview with success should trigger two new tasks
            Map<String, Object> taskVariables = new HashMap<String, Object>();
            taskVariables.put("telephoneInterviewOutcome", true);
            engineFacade.completeTask(task.getId(), taskVariables);

            // Liste des tâches d'un process donné
            List<Task> tasks = engineFacade.findClaimableTasks(null, null, null, processInstance.getId(), null)
                .orderByTaskName().asc()
                .list();

            if (!"Financial negotiation".equalsIgnoreCase(tasks.get(0).getName())){
                throw new RuntimeException("Financial negotiation should be active");
            }

            if (!"Tech interview".equalsIgnoreCase(tasks.get(1).getName())){
                throw new RuntimeException("Tech interview active");
            }

            // Completing both should wrap up the subprocess, send out the 'welcome mail' and end the process instance
            taskVariables = new HashMap<String, Object>();
            taskVariables.put("financialOk", true);
            engineFacade.completeTask(tasks.get(0).getId(), taskVariables);

            System.out.println("************************ Hire Process should be waiting at tech validation *********************************");

        } else {
            System.out.println("************************ Hire Process ended *********************************");

        }

    }

    // exemples CURL

    //appel des ressoures placées sous /workflow (avec secu oauth2)

    // start process
    // curl --header "Authorization: Bearer <your-token> -H "Content-Type: application/json" -d '{"name":"John Doe", "email": "john.doe@alfresco.com", "phoneNumber":"123456789"}' http://localhost:8081/activiti/start-hire-process

    // l'API rest native d'workflow est exposée (cfr doc workflow pour urls), secu basic
    // à voir si on garde cette exposition ou si on passe par nos ressources /workflow

    // read tasks
    // curl -u admin:admin -H "Content-Type: application/json" http://localhost:8081/runtime/tasks
    // | python -m json.tool

    // complete tasks
    // curl -u admin:admin -H "Content-Type: application/json" -d '{"action" : "complete", "variables": [ {"name":"telephoneInterviewOutcome", "value":true} ]}' http://localhost:8081/runtime/tasks/14

}
