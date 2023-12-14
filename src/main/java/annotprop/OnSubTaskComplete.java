package annotprop;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IDecision;
import com.ser.blueline.bpm.IPossibleDecision;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class OnSubTaskComplete extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private ITask mainTask;
    private IDocument mainDocument;

    @Override
    protected Object execute() {

        if(getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of type IDocument");
        try {
            this.helper = new ProcessHelper(getSes());
            String mainDocID = getEventTask().getDescriptorValue(Conf.Descriptors.MainTaskID);

            if(mainDocID == null) return resultError("Main Document ID is NULL");

            mainTask = getMainTask(mainDocID);
            if(mainTask == null) return resultError("Main TASK is NULL");

            if(mainTask.getProcessInstance().findLockInfo().getOwnerID() != null){
                    log.error("Main Task is locked");
                    return resultRestart("Restarting Agent");
            }

            String totalReviewerStr = mainTask.getDescriptorValue("TotalReviewed");
            if(totalReviewerStr == null) totalReviewerStr = "0";
            int totalReviewers = Integer.parseInt(totalReviewerStr);
            List<String> recieversStr = mainTask.getDescriptorValues("Receivers", String.class);
            int receivers = recieversStr.size();
            totalReviewers++;
            log.info("Already reviewed: " + totalReviewerStr);
            log.info("Total Recievers: " + receivers);
            if(totalReviewers == receivers){
                log.info("All Reviewers have completed");
                //(Get Task to the next step)
                if(!(mainTask.getCode().equals("waitforapproval"))) return resultError("Main Task is not at correct task");
                moveCurrentTaskToNext(mainTask);
            }else{
                log.info("Pending for more reveiwers");
                mainTask.setDescriptorValue("TotalReviewed" , ""+totalReviewers);
                mainTask.commit();
            }

        } catch (Exception e) {
           if(mainTask != null){mainTask.unlock();}
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }

        return resultSuccess("Agent Finished");
    }


    private void moveCurrentTaskToNext(ITask eventTask) throws Exception{
        List<IPossibleDecision> decisions = eventTask.findPossibleDecisions();
        //if(decisions.size() > 1 )throw new Exception("Found more than one decision going out from Status Check Task");
        for(IPossibleDecision pdecision : decisions){
           IDecision decision = pdecision.getDecision();
           if(Objects.equals(decision.getCode(), "ok")){
               eventTask.complete(decision);
           }
        }
        eventTask.commit();
    }

    private ITask getMainTask(String mainDocID) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.MainReviewProcess).append("'");
        builder.append(" AND WFL_TASK_NAME = '").append(Conf.Tasks.MainProcessFirstTask).append("'")
                .append(" AND ")
                .append(Conf.Descriptors.MainTaskID).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
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
