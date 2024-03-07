package annotprop;

import com.ser.blueline.IDocument;
import com.ser.blueline.IDocumentServer;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.ISession;
import com.ser.blueline.bpm.*;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;

public class TestTaskRemove extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private ITask mainTask;
    private IDocument mainDocument;

    @Override
    protected Object execute() {


        ISession ses = this.getSes();
        IDocumentServer srv = ses.getDocumentServer();

        if(getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of type IDocument");
        try {
            this.helper = new ProcessHelper(getSes());

            mainTask = getEventTask();
            if(mainTask == null) return resultError("Main TASK is NULL");

            mainTask.isFCFS();
            IProcessInstance pi = mainTask.getProcessInstance();
            Collection<ITask> tsks = pi.findTasks();
            for(ITask ttsk : tsks) {
                if (ttsk.getStatus() != TaskStatus.COMPLETED) {
                    continue;
                }
                pi.deleteFCFSTask(ttsk);
            }


        } catch (Exception e) {
           if(mainTask != null){mainTask.unlock();}
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }

        return resultSuccess("Agent Finished");
    }

    private ITask getMainTask(String mainDocID) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.MainReviewProcess).append("'");
        builder.append(" AND WFL_TASK_NAME = '").append(Conf.Tasks.MainProcessFirstTask).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.MainDocumentID).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        //IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.BPM} , whereClause , "", 2, false);
        if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        if(informationObjects.length > 1) throw new Exception("Multiple hits found for query: " + whereClause);
        return (ITask) informationObjects[0];
    }

    private IDocument getMainDocument() throws Exception {
        log.info("Getting Main Document from task");
        IInformationObject mainObj = getEventTask().getProcessInstance().getMainInformationObject();
        if (mainObj == null) throw new Exception("Main Object Not found in task");
        if (!(mainObj instanceof IDocument)) throw new Exception("Main Object is not of Type IDocument");
        return (IDocument) mainObj;
    }
}
