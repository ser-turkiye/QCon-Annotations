package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class OnNewTask extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private IDocument mainDocument;
    private ITask mainTask;


    @Override
    protected Object execute() {
        //(1) Make sure we have a main document
        //(1.a) Set the Main Document to the descriptors
        //(2) Get the number of recievers start a new task with each one
        //(3) Inherit all descriptors and save the name of the recievers to the layerdescriptor
        if (getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of Type ITask");
        try {
            log.info("---- Agent Started -----");
            this.helper = new ProcessHelper(getSes());
            mainDocument = getMainDocument();

            mainDocument.setDescriptorValue("ccmPrjDocApprCode", "");
            mainDocument.commit();

            if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
                log.error("Task is locked.." + getEventTask().getID() + "..restarting agent");
                return resultRestart("Restarting Agent");
            }

            helper.mapDescriptorsFromObjectToObject(mainDocument , getEventTask() , true);
            getEventTask().setDescriptorValue(Conf.Descriptors.MainTaskID , getEventTask().getProcessInstance().getID());
            //Get the Task Reviewers


            List<String> reviewersNew = this.getEventTask().getDescriptorValues("ReceiversNew", String.class);
            List<String> reviewers2 = this.getEventTask().getDescriptorValues("Receivers", String.class);
            List<String> reviewers = new ArrayList<>();
            if (reviewersNew != null ) {
                reviewers = this.helper.mergeLists(reviewersNew, reviewers2);
            }else{
                reviewers = reviewers2;
            }

            this.getEventTask().setDescriptorValues("ReceiversNew", Arrays.asList(""));
            this.getEventTask().setDescriptorValues("Receivers", reviewers);
            this.setDocumentIDOnTask();

            getEventTask().commit();

            try {

                //if (!reviewers.toString().equals(reviewersNew.toString())) {
                    this.startNewTasks(reviewers);
                //}

            }catch (Exception e){
                log.error("Restarting OnNewTask agent");
                return resultRestart("Restarting OnNewTask Agent : " + e);
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }


    private IDocument createNewDocumentCopy(String layerName) throws Exception {
        log.info("Copying Original Document for each WB");
        IDocument copyDoc = null;
        try {
            IArchiveClass ac = getDocumentServer().getArchiveClass(Conf.ClassIDs.EngineeringCopy , getSes());
            IDatabase db = getSes().getDatabase(ac.getDefaultDatabaseID());
            String userName = layerName.substring(layerName.indexOf("(") + 1, layerName.indexOf(")"));
            IUser user = getDocumentServer().getUserByLoginName(getSes(),userName);
    //      getDocumentServer().getClassFactory().getDocumentInstance(db.getDatabaseName() , ac.getID() , "0000" , getSes()).commit();
            copyDoc = getDocumentServer().getClassFactory().getDocumentInstance(db.getDatabaseName() , ac.getID() , "0000" , getSes());
            helper.mapDescriptorsFromObjectToObject(mainDocument , copyDoc , true);
            copyDoc.commit();
            getDocumentServer().copyDocument2(getSes() , mainDocument , copyDoc,
                   CopyScope.COPY_PART_DOCUMENTS , CopyScope.COPY_OVERLAYS );

            copyDoc.setDescriptorValue(Conf.Descriptors.MainDocumentID , mainDocument.getID());
            copyDoc.setDescriptorValue(Conf.Descriptors.SubDocumentID , copyDoc.getID());
            copyDoc.setDescriptorValue(Conf.Descriptors.LayerName, layerName);
            copyDoc.setDescriptorValue("AbacOrgaRead", (user != null ? user.getID() : ""));

            copyDoc.commit();
        } catch (Exception e) {
            log.info("Exeption Caught..createNewDocumentCopy: " + e);
        }
        return copyDoc;
    }
    private void createNewTaskForWB(String wbID, String layerName) throws Exception {
        log.info("Creating New Task for WB");
        try {
            mainDocument = getMainDocument();
            IInformationObject[] reviewProcess = Utils.getSubProcessByWBID(getEventTask().getProcessInstance().getID(),wbID,new ProcessHelper(getSes()));
            if(reviewProcess.length == 0) {
                IProcessInstance pi = helper.buildNewProcessInstanceForID(Conf.ClassIDs.ReviewSubProcess);
                if (pi == null) throw new Exception("Process Instance couldn't be created");
                log.info("Mapping Descritpors to new Task");
                helper.mapDescriptorsFromObjectToObject(getEventTask(), pi, true);
                pi.setDescriptorValue(Conf.Descriptors.LayerName, layerName);
                pi.setDescriptorValue(Conf.Descriptors.ReviewerWBID, wbID);
                pi.setDescriptorValue(Conf.Descriptors.MainTaskID, getEventTask().getProcessInstance().getID());
                pi.setSubject("Review for " + mainDocument.getDescriptorValue("ccmPrjDocNumber") + " " + mainDocument.getDescriptorValue("ccmPrjDocRevision"));
                log.info("Getting Task Document Copy");
                IDocument documentCopy = createNewDocumentCopy(layerName);
                pi.setMainInformationObjectID(documentCopy.getID());
                log.info("Attempting Commit");
                pi.commit();

                log.info("start linked for copydoc : " + pi.getID());

                ILink[] links = getDocumentServer().getReferencedRelationships(getSes(), mainDocument, false, false);
                for (ILink link : links) {
                    IInformationObject xdoc = link.getTargetInformationObject();
                    String docInfo = xdoc.getDisplayName();
                    String docClassID = xdoc.getClassID();
                    InformationObjectType objType = xdoc.getInformationObjectType();
                    log.info("linked usage object type : " + objType);
                    log.info("start linked doc : " + docInfo);
                    log.info("start linked docID : " + xdoc.getID());
                    ILink lnk2 = getDocumentServer().createLink(getSes(), pi.getID(), null, xdoc.getID());
                    lnk2.commit();
                    //Utils.server.removeRelationship(Utils.session, link);
                    log.info("linked doc to copydoc");
                }

            }
        } catch (Exception e) {
            log.info("Exeption Caught..createNewTaskForWB: " + e);
        }
    }
    private void startNewTasks(List<String> reviewers) throws Exception {
        log.info("Starting new task for each reviewer");
        try {
            List<String> wbList = new ArrayList<>();
            HashMap<String, Boolean> createdLayerNames = new HashMap<>();
            for (String reviewer : reviewers) {
                IWorkbasket wb = getBpm().getWorkbasket(reviewer);
                if (wb == null) throw new Exception("Reviwer with WBID: " + reviewer + "'s workbasket not found");
                String layerName = wb.getFullName();
                boolean isExistTaskForReview = this.checkExistSubProcesses(getEventTask().getProcessInstance().getID(),layerName);
                if(isExistTaskForReview){
                    log.info("Exist task reviewer (" + layerName + ") for main task : " + getEventTask().getProcessInstance().getID());
                    continue;
                }
                wbList.add(layerName);
                if (createdLayerNames.containsKey(layerName)) {
                    layerName = reviewer;
                }
                createdLayerNames.put(layerName, true);
                log.info("Found Reviewer: " + wb.getFullName() +" sending task to reviwer");
                this.createNewTaskForWB(reviewer, layerName);
            }
        } catch (Exception e) {
            log.info("Exeption Caught..startNewTasks: " + e);
        }
    }
    private boolean checkExistSubProcesses(String mainTaskID, String reviewer) throws Exception {
        boolean rtrn = false;
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
        //builder.append(" AND WFL_TASK_STATUS IN (2,4,16)");
        builder.append(" AND ").append(Conf.DescriptorLiterals.MainTaskReference).append(" = '").append(mainTaskID).append("'");
        builder.append(" AND ").append(Conf.DescriptorLiterals.Reviewer).append(" = '").append(reviewer).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.BPM} , whereClause , "", 100, false);
        if(informationObjects.length > 0) return true;
        ITask[] newArr = new ITask[informationObjects.length];
        for(int i=0 ; i < informationObjects.length ; i++){
            newArr[i] = (ITask) informationObjects[i];
        }
        return rtrn;
    }
    private void setDocumentIDOnTask() throws Exception {
        log.info("Setting Document ID on Main Task");
        String docID = mainDocument.getID();
        getEventTask().setDescriptorValue(Conf.Descriptors.MainDocumentID, docID);
        getEventTask().commit();
    }

    private IDocument getMainDocument() throws Exception {
        log.info("Getting Main Document from task");
        IInformationObject mainObj = getEventTask().getProcessInstance().getMainInformationObject();
        if (mainObj == null) throw new Exception("Main Object Not found in task");
        if (!(mainObj instanceof IDocument)) throw new Exception("Main Object is not of Type IDocument");
        return (IDocument) mainObj;
    }


}
