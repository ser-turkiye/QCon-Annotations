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

        //def agent = new OnNewTask();
        //def agent = new OnSubTaskComplete();
        //def agent = new OnNewAnnotation();
        //def agent = new OnNewBulkReview();
        def agent = new AnnotationTable();
        //def agent = new GenerateStamp();
        //def agent = new OnChangeAnnotation();
        //def agent = new OnChangeDiscipline();
        //def agent = new OnChangeProjectDoc();
        //def agent = new OnChangeProjectCard();
        //def agent = new OnUpdateEmployee();
        //def agent = new OnCreateDCCExcel();
        //def agent = new OnCancelProcess();
        //def agent = new TestTaskRemove();

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24166dbdc8-1c1e-4326-877b-8371ad181a64182023-10-11T11:27:58.673Z010"
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY247bb1907c-ff20-42f7-8e55-cb9438e70846182023-10-20T15:04:32.147Z011"
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24d74bc836-604a-4a96-92eb-3fda32bb5139182023-11-02T12:55:03.598Z017"

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY24612e3615-bea9-4b33-91ce-a864dcfeda67182023-12-06T09:40:19.795Z011" //newannotation-bulent
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD09D_ENGCOPY24cbf86bb4-33ea-4da1-a72c-0fdad9cedaab182023-10-23T08:06:07.171Z011" //newannotation-bulent
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24cd37e29b-1624-4674-b22b-eb6985e733fd182023-12-08T07:11:18.933Z011" //newannotation-tolunay
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2462cff716-c335-423d-8224-7af9ae0843d0182023-12-07T14:41:26.257Z012" //newannotation-tolunay

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24487ed15b-079a-480e-8187-a269c72bf044182024-01-02T09:58:00.700Z010" //on new task
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24f77ede01-2e6d-4b4d-878c-96cb30672569182024-01-25T17:10:18.011Z011" //on new annotation
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24d84780c7-98a4-4244-967e-5744d4593bca182023-12-04T06:43:15.399Z012" //on sub task complete

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM247fdcd280-f11f-428e-9463-65bac3d4e814182023-12-29T16:11:18.769Z010" //on bulk review

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM243dfee922-4457-49ce-a1f4-2e072551cbff182024-03-07T09:05:37.131Z013" //anotation table
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24c131ba10-5919-4918-8c82-f83a7678c2a2182023-11-15T13:28:46.751Z013" //anotation table
        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24d2734ecd-90a2-4ebb-b90e-ebd927d1f7d4182024-03-14T08:41:59.972Z013" //anotation table

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2452a6d5d6-d298-4a60-b48b-82632fae16f5182023-12-13T09:11:18.580Z019" //generate stamp
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD06D_QCON24d967d8b9-1f04-412b-9d8e-30fd55c02c8b182023-12-25T07:20:47.799Z011" //DCCExcel

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD07PRJ_DOC24a1797dfd-3087-499b-9385-6cbe60a6bfc9182023-11-01T13:29:34.664Z011" //changeannotation

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24c4515c15-8008-48a0-9d1c-f466079d2b03182023-10-31T11:35:58.376Z011" //change discipline
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24543de771-386f-413b-bf0c-142f53986b97182023-11-01T06:49:27.928Z011" //change discipline

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD07PRJ_DOC2417b4f3eb-7689-4ffe-9c5b-f31903abb32b182023-12-28T10:23:35.044Z011" //change prj doc

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aPRJ_FOLDER24359e212c-459e-4714-a787-0fc2ca9b6add182023-11-29T08:15:03.302Z011" //change prj card

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR0aF_EMPLOYEE24335684f3-cf16-4571-8dba-0a78aa1ff8e7182023-12-01T12:45:05.040Z011"  //change employee file

        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM24f4806a58-db22-4201-91f9-2c78eeb0e31d182023-11-08T10:33:33.870Z013" //convert2pdf
        //binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "ST03BPM2477ecad6f-2f1a-4e58-a0f5-795f02b40a75182024-03-07T15:42:03.839Z016" //cancelProcess

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
