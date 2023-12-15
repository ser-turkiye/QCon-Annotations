package events;

import annotprop.Conf;
import annotprop.ProcessHelper;
import annotprop.Utils;
import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.foldermanager.*;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.lang.StringBuilder;

import org.json.JSONObject;
import ser.bn.se.demosystems.documents.CounterHelper;

import javax.swing.text.html.HTMLDocument;

public class OnCancelProcess extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    ISession ses;
    IDocumentServer srv;
    IBpmService bpm;
    private ProcessHelper helper;
    public Utils utils;

    @Override
    protected Object execute() {
        //(1) Make sure we have a main document
        //IInformationObject mainDocument = getEventDocument();
        if (getEventTask() == null) {
            return resultError("Task is Null");
        }

        try {
            com.spire.license.LicenseProvider.setLicenseKey(Conf.Licences.SPIRE_XLS);

            ses = getSes();
            srv = ses.getDocumentServer();
            bpm = getBpm();
            this.helper = new ProcessHelper(getSes());

            ITask mainTask = getEventTask();
            IProcessInstance processInstance = mainTask.getProcessInstance();
            String currentUser = getSes().getUser().getFullName();
            IUser processOwner = processInstance.getOwner();
            String uniqueId = UUID.randomUUID().toString();
            String prjn = processInstance.getDescriptorValue(Conf.Descriptors.ProjectNo, String.class);

            IDocument mainDocument = (IDocument) processInstance.getMainInformationObject();

            ITask[] subProcesses = getSubProcesses(mainDocument.getID());
            for (ITask task : subProcesses) {
                if (task.getStatus() != TaskStatus.CANCELED) {
                    task.setDescriptorValue("Notes", "Process Cancelled by " + currentUser);
                    task.cancel();
                    task.commit();
                }
            }

            processInstance.setDescriptorValue("ccmCrrsStatus","Cancelled");
            processInstance.commit();

            String mtpn = "PROCESS_CANCEL_MAIL";
            JSONObject dbks = new JSONObject();
            dbks.put("DoxisLink", Conf.CancelProcess.WebBase + helper.getTaskURL(processInstance.getID()));
            dbks.put("Title", mainDocument.getDisplayName());
            dbks.put("Task", mainTask.getName());

            IDocument mtpl = Utils.getTemplateDocument(prjn, mtpn, helper);
            if(mtpl == null){
                log.info("Template-Document [ " + mtpn + " ] not found.");
                //throw new Exception("Template-Document [ " + mtpn + " ] not found.");
            }else {
                String tplMailPath = Utils.exportDocument(mtpl, Conf.CancelProcess.MainPath, mtpn + "[" + uniqueId + "]");
                String mailExcelPath = Utils.saveDocReviewExcel(tplMailPath, Conf.CancelProcessSheetIndex.Mail,
                        Conf.CancelProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].xlsx", dbks
                );
                String mailHtmlPath = Utils.convertExcelToHtml(mailExcelPath, Conf.CancelProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].html");

                String umail = processOwner.getEMailAddress();
                List<String> mails = new ArrayList<>();

                if (umail != null) {
                    mails.add(umail);
                    JSONObject mail = new JSONObject();
                    mail.put("To", String.join(";", mails));
                    mail.put("Subject", "Cancelled Process");
                    mail.put("BodyHTMLFile", mailHtmlPath);
                    Utils.sendHTMLMail(ses, srv, "CCM_MAIL_CONFIG", mail);
                } else {
                    log.info("Mail adress is null :" + processOwner.getFullName());
                }
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }

    private IFolder getProjectFolder() throws Exception {
        log.info("Getting Project Folder");
        String projectNumber = getEventDocument().getDescriptorValue("ccmPRJCard_code");
        if(projectNumber == null) throw new Exception("Project Number is NULL");
        if(projectNumber.isEmpty()) throw new Exception("Project Number is NULL");
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("TYPE = '")
                .append(Constants.ClassIDs.ProjectFolder)
                .append("' AND ")
                .append(Constants.Literals.ProjectNumberDescriptor)
                .append(" = '")
                .append(projectNumber).append("'");
        log.info("Attemptign Query");
        IInformationObject[] objects = createQuery(Constants.Literals.ProjectFolderDB , whereClause.toString() , 2);
        if(objects == null) throw new Exception("Not Folder with: " + projectNumber + " was found");
        if(objects.length < 1)throw new Exception("Not Folder with: " + projectNumber + " was found");
        return (IFolder) objects[0];
    }
    private ITask[] getSubProcesses(String mainDocID) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
        builder.append(" AND WFL_TASK_STATUS IN (2,4,16)");
        builder.append(" AND ").append(Conf.DescriptorLiterals.MainDocumentID).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
        //if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        ITask[] newArr = new ITask[informationObjects.length];
        for(int i=0 ; i < informationObjects.length ; i++){
            newArr[i] = (ITask) informationObjects[i];
        }
        return newArr;
    }
    private IInformationObject[] createQuery(String dbName , String whereClause , int maxHits){
        String[] databaseNames = {dbName};

        ISerClassFactory fac = getDocumentServer().getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(
                getSes() ,
                databaseNames ,
                fac.getExpressionInstance(whereClause) ,
                null,null);
        que.setMaxHits(maxHits);
        que.setHitLimit(maxHits + 1);
        que.setHitLimitThreshold(maxHits + 1);
        IDocumentHitList hits = que.getSession() != null? que.getSession().getDocumentServer().query(que, que.getSession()):null;
        if(hits == null) return null;
        else return hits.getInformationObjects();
    }

    public static INode getNode(IFolder fold, String fldn){
        List<INode> nodesByName = fold.getNodesByName(fldn);
        return fold.getNodeByID(nodesByName.get(0).getID());
    }
}
