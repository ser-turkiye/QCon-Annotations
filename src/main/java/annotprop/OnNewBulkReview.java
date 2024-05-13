package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.bpm.IWorkbasket;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OnNewBulkReview extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private IDocument mainDocument;
    private ITask mainTask;
    String prjCode = "";
    @Override
    protected Object execute() {
        if (getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of Type ITask");
        try {
            log.info("----OnNewBulkReview Agent Started -----");
            this.helper = new ProcessHelper(getSes());
            if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
                log.error("Task is locked.." + getEventTask().getID() + "..restarting agent");
                return resultRestart("Restarting Agent");
            }
            IDocumentServer srv = getEventTask().getSession().getDocumentServer();
            IInformationObjectLinks links = getEventTask().getProcessInstance().getLoadedInformationObjectLinks();
            for (ILink link : links.getLinks()) {
                IInformationObject xdoc = link.getTargetInformationObject();
                if (!xdoc.getClassID().equals(Conf.ClassIDs.EngineeringDocument)){continue;}
                String disp = xdoc.getDisplayName();
                String dpjn = xdoc.getDescriptorValue(Conf.Descriptors.ProjectNo, String.class);
                String category = xdoc.getDescriptorValue("ccmPrjDocCategory");
                if (category != null && category.trim().equalsIgnoreCase("Correspondence")){continue;}
                this.createNewMainProcess((IDocument) xdoc);
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    private void createNewMainProcess(IDocument mainDocument) throws Exception {
        log.info("Creating New Main Review Process for :" + mainDocument.getID());
        try {
            IProcessInstance pi = helper.buildNewProcessInstanceForID(Conf.ClassIDs.MainReviewProcess);
            if (pi != null) {
                log.info("Mapping Descritpors to new Task");
                helper.mapDescriptorsFromObjectToObject(getEventTask(), pi, true);
                //pi.setSubject("Review for " + mainDocument.getDescriptorValue("ccmPrjDocNumber") + " " + mainDocument.getDescriptorValue("ccmPrjDocRevision"));
                log.info("Getting Task Document Copy");
                //IDocument documentCopy = createNewDocumentCopy(mainDocument);
                pi.setMainInformationObjectID(mainDocument.getID());
                log.info("Attempting Commit");
                pi.commit();

                log.info("start linked for main process : " + pi.getID());

                ILink[] links = getDocumentServer().getReferencedRelationships(getSes(), mainDocument, false, false);
                for (ILink link : links) {
                    IInformationObject xdoc = link.getTargetInformationObject();
                    String docInfo = xdoc.getDisplayName();
                    String docClassID = xdoc.getClassID();
                    InformationObjectType objType = xdoc.getInformationObjectType();
                    log.info("start linked doc : " + docInfo);
                    log.info("start linked docID : " + xdoc.getID());
                    ILink lnk2 = getDocumentServer().createLink(getSes(), pi.getID(), null, xdoc.getID());
                    lnk2.commit();
                    //Utils.server.removeRelationship(Utils.session, link);
                    log.info("linked doc to main process");
                }
                log.info("finish linked for main process : " + pi.getID());
            }
        } catch (Exception e) {
            throw new Exception("Exeption Caught..createNewMainProcess: " + e);
        }
    }
    private IDocument createNewDocumentCopy(IDocument mainDocument) throws Exception {
        log.info("Copying Original Document from:" + mainDocument.getID());
        try {
            IArchiveClass ac = getDocumentServer().getArchiveClass(Conf.ClassIDs.EngineeringCopy, getSes());
            IDatabase db = getSes().getDatabase(ac.getDefaultDatabaseID());
            IDocument copyDoc = getDocumentServer().getClassFactory().getDocumentInstance(db.getDatabaseName(), ac.getID(), "0000", getSes());
            helper.mapDescriptorsFromObjectToObject(mainDocument, copyDoc, true);
            copyDoc.commit();
            getDocumentServer().copyDocument2(getSes(), mainDocument, copyDoc, CopyScope.COPY_PART_DOCUMENTS, CopyScope.COPY_OVERLAYS);
            copyDoc.commit();
            return copyDoc;
        } catch (Exception e) {
            throw new Exception("Exeption Caught..createNewDocumentCopy: " + e);
        }
    }
}
