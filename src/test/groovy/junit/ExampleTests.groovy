package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import events.OnCancelProcess
import events.OnChangeProjectCard
import events.OnChangeProjectDoc
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

        //def agent = new OnNewTask();
        //def agent = new OnSubTaskComplete();
        //def agent = new OnNewAnnotation();
        //def agent = new AnnotationTable();
        //def agent = new GenerateStamp();
        //def agent = new OnChangeAnnotation();
        //def agent = new OnChangeDiscipline();
        //def agent = new OnChangeProjectDoc();
        def agent = new OnChangeProjectCard();
        //def agent = new OnUpdateEmployee();
        //def agent = new Convert2PDF();
        //def agent = new OnCancelProcess();
        //def agent = new TestTaskRemove();

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24166dbdc8-1c1e-4326-877b-8371ad181a64182023-10-11T11:27:58.673Z010"
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY247bb1907c-ff20-42f7-8e55-cb9438e70846182023-10-20T15:04:32.147Z011"
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24d74bc836-604a-4a96-92eb-3fda32bb5139182023-11-02T12:55:03.598Z017"

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY24612e3615-bea9-4b33-91ce-a864dcfeda67182023-12-06T09:40:19.795Z011" //newannotation-bulent
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY24cbf86bb4-33ea-4da1-a72c-0fdad9cedaab182023-10-23T08:06:07.171Z011" //newannotation-bulent
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24cd37e29b-1624-4674-b22b-eb6985e733fd182023-12-08T07:11:18.933Z011" //newannotation-tolunay
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2462cff716-c335-423d-8224-7af9ae0843d0182023-12-07T14:41:26.257Z012" //newannotation-tolunay

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24160d1fe2-3f07-4310-adfa-e233f1c33565182023-12-11T10:56:19.079Z011" //on new task
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24d84780c7-98a4-4244-967e-5744d4593bca182023-12-04T06:43:15.399Z012" //on sub task complete

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2421f67c40-f6a0-445a-b24e-8c20a316581e182023-11-02T06:32:42.619Z013" //anotation table
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24c131ba10-5919-4918-8c82-f83a7678c2a2182023-11-15T13:28:46.751Z013" //anotation table
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24505ce08a-d51a-4199-b9b3-edbb0afb049c182023-12-06T06:21:33.921Z013" //anotation table

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER249a40f87a-b6a4-427d-9c06-f93bc8a72b1b182024-01-04T14:27:22.180Z011" //generate stamp

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD07PRJ_DOC24a1797dfd-3087-499b-9385-6cbe60a6bfc9182023-11-01T13:29:34.664Z011" //changeannotation

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24c4515c15-8008-48a0-9d1c-f466079d2b03182023-10-31T11:35:58.376Z011" //change discipline
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24543de771-386f-413b-bf0c-142f53986b97182023-11-01T06:49:27.928Z011" //change discipline

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD07PRJ_DOC24aacfca94-4ae6-4da5-9536-aea94d7a7dcc182023-12-03T14:05:25.681Z011" //change prj doc

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER245b3e7202-c582-41df-a21a-f943ddde7c5f182023-11-15T07:10:36.815Z011" //change prj card

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aF_EMPLOYEE24335684f3-cf16-4571-8dba-0a78aa1ff8e7182023-12-01T12:45:05.040Z011"  //change employee file

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24f4806a58-db22-4201-91f9-2c78eeb0e31d182023-11-08T10:33:33.870Z013" //convert2pdf
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM248d532701-30ec-4404-a98e-3e49a892797a182023-12-14T08:46:06.928Z013" //cancelProcess

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2413d2c0cc-84a3-4d9e-8d1b-4b6806074790182023-11-19T16:33:32.237Z012" //removeTask

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
