package ser.bn.se.demosystems.generation;

import com.ser.blueline.*;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;
import ser.bn.se.demosystems.documents.CounterHelper;

import java.text.SimpleDateFormat;
import java.util.*;

public class TransmittalDoc extends UnifiedAgent {

    private static Logger log = LogManager.getLogger();

    private FileReader fileReader;

    @Override
    protected Object execute() {
        try {
            if(getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of type ITask");
            log.info("---- agent Started ----");;
            fileReader = new FileReader(true);
            this.createNewDocument();
            getLinkedDocuments();
            fileReader.closeDocument(Constants.Templates.Transmittal.HolderPath);
            populateDocuments();
            archiveNewTemplate();
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished");
    }

    private void archiveNewTemplate() throws Exception {
        FilingHelper filingHelper = new FilingHelper(getSes());
        IDocument doc = filingHelper.archiveFileToDocumentClass(Constants.Templates.Transmittal.FinalPath , Constants.ClassIDs.ProjDocumentArchive);
        filingHelper.mapDescriptorsFromObjectToObject(getEventTask() , doc , false);
        doc.setDescriptorValue("ObjectSubject" , "Transmittal Sheet.docx");
        doc.setDescriptorValue("ObjectType" , "Transmittal");
        doc.setDescriptorValue("ObjectName2" , "Transmittal");
        doc.setDescriptorValue("Rendition" , "CONVERT");
        //doc.setDescriptorValue("ObjectNumberReference" , getEventTask().getProcessInstance().getMainInformationObjectID());
        IInformationObjectLinks links = getEventTask().getProcessInstance().getLoadedInformationObjectLinks();

        for(ILink link : links.getLinks()){
            getDocumentServer().linkInformationObjects(getSes() , link.getTargetDocumentId() , doc.getID());
            getDocumentServer().linkInformationObjects(getSes() , doc.getID() ,  link.getTargetDocumentId());
        }
        CounterHelper counterHelper = new CounterHelper(getSes() , doc.getClassID());
        String counterStr = counterHelper.getCounterStr();
        String paddedNo = String.format("%6s", counterStr).replace(' ', '0');
        doc.setDescriptorValue("ObjectNumber" , "30_TQCONRLP_" + paddedNo);
        doc.commit();
        getEventTask().getProcessInstance().getLoadedInformationObjectLinks().addInformationObject(doc.getID());
        getEventTask().commit();
    }

    private void getLinkedDocuments(){
        log.info("Adding Linked Documents Row by Row");

        IInformationObjectLinks links = getEventTask().getProcessInstance().getLoadedInformationObjectLinks();

        for(ILink link : links.getLinks()){
            String docID = link.getTargetDocumentId();
            IDocument doc = getDocumentServer().getDocument4ID(docID , getSes());
            if(doc == null) continue;
            String[] row = new String[5];
            row[0] = doc.getDescriptorValue("ObjectNumber");
            row[1] = doc.getDescriptorValue("ObjectName2");
            row[2] = doc.getDescriptorValue("Remarks");
            row[3] = doc.getDescriptorValue("ObjectDescription");
            row[4] = "";
            fileReader.addRowToTable(row , false);
        }

        log.info("Document Links Added");
    }

    private void createNewDocument() throws Exception {
        fileReader.readFileFromPath(Constants.Templates.Transmittal.OriginalTemplate);
        fileReader.getTableAtIndex(1);
    }

    private String formulateDateValue(String dateStr){
        if(dateStr == null) return "";
        List<SimpleDateFormat> knownPatterns = new ArrayList<>();
        knownPatterns.add(new SimpleDateFormat("yyyyMMdd"));
        for (SimpleDateFormat pattern : knownPatterns) {
            try {
                // Take a try
                pattern.setLenient(false);
                Date d = pattern.parse(dateStr);
                return new SimpleDateFormat("dd/MM/YYYY").format(d);
            } catch (java.text.ParseException pe) {
                // Loop on
            }
        }
        return dateStr;
    }

    private String personName(String personId){
        if(personId == null) return "";
        IUser user = getDocumentServer().getUser(getSes() , personId);
        if(personId == null) return personId;
        return user.getFirstName() + " " + user.getLastName();
    }
    private String cleanVal(String val){
        if(val == null) return "";
        else return val;
    }
    private void populateDocuments() throws Exception {
        log.info("Populating Document");
        Map<String , String> placeHolder = new HashMap<>();
        placeHolder.put("##ProjectName##" , cleanVal(getEventTask().getDescriptorValue("ObjectName")));
        placeHolder.put("##JobNo##" , cleanVal(getEventTask().getDescriptorValue("JobNo")));
        placeHolder.put("##DateStart##" , formulateDateValue(getEventTask().getDescriptorValue("DateStart")));
        placeHolder.put("##JobNo##" , cleanVal(getEventTask().getDescriptorValue("JobNo")));
        //-------------
        placeHolder.put("##TransmittalNo##" , cleanVal(getEventTask().getDescriptorValue("TN")));
        placeHolder.put("##IssueDate##" , getEventTask().getProcessInstance().getCreationDate().toString());
        placeHolder.put("##Discipiline##" , cleanVal(getEventTask().getDescriptorValue("Department")));

        placeHolder.put("##OrignatorName##" , personName(getEventTask().getProcessInstance().getOwnerID()));
        placeHolder.put("##OrignatorGroup##" , "Document Controller (DC)");


        placeHolder.put("##To##" , personName(getEventTask().getDescriptorValue("To-Receiver")));
        placeHolder.put("##cc##" , personName(getEventTask().getDescriptorValue("CC-Receiver")));
        placeHolder.put("##Attention##" , personName(getEventTask().getDescriptorValue("ObjectAuthors")));

        long id = getEventTask().getPreviousTaskNumericID();
        ITask prevTask = getEventTask().getProcessInstance().findTaskByNumericID(id);
        if(prevTask == null) prevTask = getEventTask();
        placeHolder.put("##ApprovedName##" , personName(prevTask.getFinishedBy().getID()));
        placeHolder.put("##AproovedGroup##" , "Document Controller (DC)");

        placeHolder.put("##OriginDate##" , getEventTask().getProcessInstance().getCreationDate().toString());
        placeHolder.put("##ApproveDate##" , prevTask.getFinishedDate().toString());


        log.info("Passing on to replace fields");
        fileReader.replaceTextInTableCell(placeHolder , Constants.Templates.Transmittal.HolderPath, Constants.Templates.Transmittal.FinalPath);


    }
}
