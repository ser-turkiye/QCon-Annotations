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

        def agent = new OnCancelProcess();

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2485b01fe3-209a-4c4f-9cdb-e7ab6c2725a4182024-08-01T11:43:29.914Z016"


        def result = (AgentExecutionResult) agent.execute(binding.variables)
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