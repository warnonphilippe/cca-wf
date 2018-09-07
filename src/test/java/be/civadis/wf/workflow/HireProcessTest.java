package be.civadis.wf.workflow;


import be.civadis.wf.config.ApplicationProperties;
import be.civadis.wf.domain.Applicant;
import be.civadis.wf.multitenancy.TenantContext;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.subethamail.wiser.Wiser;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = WfApp.class)
public class HireProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EngineFacade engineFacade;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ApplicationProperties applicationProperties;

    //@Autowired
    //private ApplicantRepository applicantRepository;

    private Wiser wiser;

    @Before
    public void setup() {
        wiser = new Wiser();
        wiser.setPort(1025);
        wiser.start();
    }

    @After
    public void cleanup() {
        wiser.stop();
    }


    //@Test
    public void testFacade() {

        List<String> tenants = applicationProperties.getSchemas();
        TenantContext.setCurrentTenant(tenants.get(0));

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@workflow.org", "12344");
        //applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        variables.put("saison", "ete");
        variables.put("service", "csd");
        ProcessInstance processInstance = engineFacade.startProcess("hireProcessWithJpa", "JohnDoe", variables);

        // First, the 'phone interview' should be active
        Task task = engineFacade.findClaimableTasks(
                null,
                Arrays.asList("dev-managers"),
                null, processInstance.getId(), null).singleResult();
        Assert.assertEquals("Telephone interview", task.getName());

        // Claim the task
        engineFacade.claim(task.getId(), "phw");

        // Check assigned task
        List<Task> taskAssignedList = engineFacade.findAssignedTasks("phw", null, null, null).list();
        Assert.assertEquals("Telephone interview", taskAssignedList.get(0).getName());

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        engineFacade.completeTask(task.getId(), taskVariables);

        // Liste des tâches d'un process donné
        List<Task> tasks = engineFacade.findClaimableTasks(null, null, null, processInstance.getId(), null)
                .orderByTaskName().asc()
                .list();
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals("Financial negotiation", tasks.get(0).getName());
        Assert.assertEquals("Tech interview", tasks.get(1).getName());

        // Completing both should wrap up the subprocess, send out the 'welcome mail' and end the process instance
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        engineFacade.completeTask(tasks.get(0).getId(), taskVariables);

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financialOk", true);
        engineFacade.completeTask(tasks.get(1).getId(), taskVariables);

        // Verify email
        //Assert.assertEquals(1, wiser.getMessages().size());

        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());

    }

    //@Test
    public void testApi() {

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@workflow.org", "12344");
        //applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("hireProcessWithJpa", variables);

        // First, the 'phone interview' should be active
        Task task = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .taskCandidateGroup("dev-managers")
            .singleResult();
        Assert.assertEquals("Telephone interview", task.getName());

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        taskService.complete(task.getId(), taskVariables);

        // Liste des tâches d'un process donné
        List<Task> tasks = taskService.createTaskQuery()
            .processInstanceId(processInstance.getId())
            .orderByTaskName().asc()
            .list();
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals("Financial negotiation", tasks.get(0).getName());
        Assert.assertEquals("Tech interview", tasks.get(1).getName());

        // Completing both should wrap up the subprocess, send out the 'welcome mail' and end the process instance
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        taskService.complete(tasks.get(0).getId(), taskVariables);

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financialOk", true);
        taskService.complete(tasks.get(1).getId(), taskVariables);

        // Verify email
        //Assert.assertEquals(1, wiser.getMessages().size());

        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());

    }


}
