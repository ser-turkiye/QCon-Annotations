package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import events.OnCancelProcess
import events.OnChangeProjectCard
import events.OnChangeProjectDoc
import events.OnCreateDCCExcel
import events.OnUpdateEmployee
import org.junit.*
import ser.bn.se.demosystems.*;
import sample.*;
import annotprop.*
import ser.bn.se.demosystems.generation.AnnotationTable

class ExampleTests {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {

        def agent = new OnNewTask();
        //def agent = new OnSubTaskComplete();
        //def agent = new OnNewAnnotation();
        //def agent = new OnNewBulkReview();
        //def agent = new AnnotationTable();
        //def agent = new GenerateStamp();
        //def agent = new OnChangeAnnotation();
        //def agent = new OnChangeDiscipline();
        //def agent = new OnChangeProjectDoc();
        //def agent = new OnChangeProjectCard();
        //def agent = new OnUpdateEmployee();
        //def agent = new OnCreateDCCExcel();
        //def agent = new OnCancelProcess();
        //def agent = new TestTaskRemove();

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24eb6cd282-fc12-4e41-bd68-73162b860e52182024-05-03T09:05:58.389Z010"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        System.out.println(result)
//        assert result.resultCode == 0
//        assert result.executionMessage.contains("Linux")
//        assert agent.eventInfObj instanceof IDocument
    }

    @Test
    void testForGroovyAgentMethod() {
//        def agent = new GroovyAgent()
//        agent.initializeGroovyBlueline(binding.variables)
//        assert agent.getServerVersion().contains("Linux")
    }

    @Test
    void testForJavaAgentMethod() {
//        def agent = new JavaAgent()
//        agent.initializeGroovyBlueline(binding.variables)
//        assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        println("RLEASE BINDING RUNNING.....")
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
