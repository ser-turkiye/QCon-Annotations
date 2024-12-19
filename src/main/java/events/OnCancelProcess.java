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
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.StringBuilder;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import ser.bn.se.demosystems.documents.CounterHelper;

import javax.swing.text.html.HTMLDocument;

import static annotprop.Utils.loadTableRows;

public class OnCancelProcess extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    ISession ses;
    IDocumentServer srv;
    IBpmService bpm;
    private ProcessHelper helper;
    public Utils utils;
    IDocument mailTemplate = null;
    JSONObject projects = new JSONObject();
    List<String> docs = new ArrayList<>();

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

            ITask[] subProcesses = getSubProcesses(processInstance.getID());
            for (ITask task : subProcesses) {
                if (task.getStatus() != TaskStatus.CANCELED) {
                    task.setDescriptorValue("Notes", "Process Cancelled by " + currentUser);
                    task.cancel();
                    task.commit();
                }
            }

            processInstance.setDescriptorValue("ccmCrrsStatus","Cancelled");
            processInstance.commit();


            Date tbgn = null, tend = new Date();
            if(mainTask.getReadyDate() != null){
                tbgn = mainTask.getReadyDate();
            }
            long durd  = 0L;
            double durh  = 0.0;
            if(tend != null && tbgn != null) {
                long diff = (tend.getTime() > tbgn.getTime() ? tend.getTime() - tbgn.getTime() : tbgn.getTime() - tend.getTime());
                durd = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                durh = ((TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS) - (durd * 24 * 60)) * 100 / 60) / 100d;
            }
            String rcvf = "", rcvo = "";
            if(mainTask.getPreviousWorkbasket() != null){
                rcvf = mainTask.getPreviousWorkbasket().getFullName();
            }
            if(tbgn != null){
                rcvo = (new SimpleDateFormat("dd-MM-yyyy HH:mm")).format(tbgn);
            }
            String mdno = "", mdrn = "", mdnm = "";
            if(mainDocument != null &&  Utils.hasDescriptor((IInformationObject) mainDocument, Conf.Descriptors.ProjectNo)){
                prjn = mainDocument.getDescriptorValue(Conf.Descriptors.ProjectNo, String.class);
            }
            if(mainDocument != null &&  Utils.hasDescriptor((IInformationObject) mainDocument, Conf.Descriptors.DocNumber)){
                mdno = mainDocument.getDescriptorValue(Conf.Descriptors.DocNumber, String.class);
            }
            if(mainDocument != null &&  Utils.hasDescriptor((IInformationObject) mainDocument, Conf.Descriptors.Revision)){
                mdrn = mainDocument.getDescriptorValue(Conf.Descriptors.Revision, String.class);
            }
            if(mainDocument != null &&  Utils.hasDescriptor((IInformationObject) mainDocument, Conf.Descriptors.Name)){
                mdnm = mainDocument.getDescriptorValue(Conf.Descriptors.Name, String.class);
            }

            Long prevTaskID = this.getEventTask().getPreviousTaskNumericID();
            ITask prevTask = this.getEventTask().getProcessInstance().findTaskByNumericID(this.getEventTask().getPreviousTaskNumericID());
            log.info("Previev task name :" + (prevTask != null ? prevTask.getName() : "---"));

            int cnt = 0;
            cnt++;
            String mtpn = "PROCESS_CANCEL_MAIL";
            JSONObject dbks = new JSONObject();
            dbks.put("DoxisLink", Conf.CancelProcess.WebBase + helper.getTaskURL(processInstance.getID()));
            dbks.put("Title", mainDocument.getDisplayName());
            //dbks.put("Task", mainTask.getName());
            dbks.put("Task" + cnt, (prevTask != null ? prevTask.getName() : mainTask.getName()));
            dbks.put("ProcessTitle" + cnt, (processInstance != null ? processInstance.getDisplayName() : ""));
            dbks.put("ProjectNo" + cnt, (prjn != null  ? prjn : ""));
            dbks.put("DocNo" + cnt, (mdno != null  ? mdno : ""));
            dbks.put("RevNo" + cnt, (mdrn != null  ? mdrn : ""));
            dbks.put("DocName" + cnt, (mdnm != null  ? mdnm : ""));
            dbks.put("ReceivedOn" + cnt, (rcvo != null ? rcvo : ""));
            dbks.put("CancelledOn" + cnt, (rcvo != null ? rcvo : ""));


            docs.add(mainDocument.getDescriptorValue(Conf.Descriptors.DocNumber));

            projects = Utils.getProjectWorkspaces(this.helper);
            mailTemplate = null;

            log.info("Getting mail template start");
            for(String prjnmbr : projects.keySet()){
                IInformationObject prjt = (IInformationObject) projects.get(prjnmbr);
                log.info("Getting mail template for project : " + prjnmbr);
                IDocument dtpl = Utils.getTemplateDocument(prjt, Conf.MailTemplates.CancelProcess);
                log.info("mail template : " + dtpl);
                if(dtpl == null){continue;}
                mailTemplate = dtpl;
                break;
            }
            log.info("Mail template :" + (mailTemplate != null ? mailTemplate.getDisplayName() : "---"));

            if(mailTemplate == null){
                log.info("Template-Document [ " + mtpn + " ] not found.");
                //throw new Exception("Template-Document [ " + mtpn + " ] not found.");
            }else {
                String tplMailPath = Utils.exportDocument(mailTemplate, Conf.CancelProcess.MainPath, mtpn + "[" + uniqueId + "]");
                log.info("Mail template export path:" + tplMailPath);
                //String mailExcelPath = Utils.saveDocReviewExcel(tplMailPath, Conf.CancelProcessSheetIndex.Mail,Conf.CancelProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].xlsx", dbks);

                loadTableRows(tplMailPath, 0, "Task", 0, docs.size());
                String mailExcelPath = Utils.saveToExcel(tplMailPath, 0,
                        Conf.CancelProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].xlsx", dbks
                );

                log.info("Mail mailExcelPath :" + mailExcelPath);
                String mailHtmlPath = Utils.convertExcelToHtml(mailExcelPath, Conf.CancelProcess.MainPath + "/" + mtpn + "[" + uniqueId + "].html");
                log.info("Mail mailHtmlPath :" + mailHtmlPath);
                String umail = processOwner.getEMailAddress();
                log.info("Mail umail :" + umail);
                List<String> mails = new ArrayList<>();
                if (umail != null) {
                    mails.add(umail);
                    JSONObject mail = new JSONObject();
                    mail.put("To", String.join(";", mails));
                    mail.put("Subject", "Process Cancelled -" + (prevTask != null ? prevTask.getName() : "") + " - " + (mdno != null  ? mdno : "") + "/" + (mdrn != null  ? mdrn : ""));
                    mail.put("BodyHTMLFile", mailHtmlPath);
                    Utils.sendHTMLMail(ses, srv, "CCM_MAIL_CONFIG", mail);
                } else {
                    log.info("Mail adress is null :" + processOwner.getFullName());
                }
                log.info("Mail process finish");
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            log.error(e.getLocalizedMessage());
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
        //IInformationObject[] objects = createQuery(Constants.Literals.ProjectFolderDB , whereClause.toString() , 2);
        IInformationObject[] objects = helper.createQuery(new String[]{Conf.Databases.ProjectWorkspace} , whereClause.toString() , "", 1, false);;

        if(objects == null) throw new Exception("Not Folder with: " + projectNumber + " was found");
        if(objects.length < 1)throw new Exception("Not Folder with: " + projectNumber + " was found");
        return (IFolder) objects[0];
    }
    private ITask[] getSubProcesses(String mainDocID) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
        builder.append(" AND WFL_TASK_STATUS IN (2,4,16)");
        builder.append(" AND ").append(Conf.DescriptorLiterals.MainTaskReference).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        //IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.BPM} , whereClause , "", 100, false);
        //if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        ITask[] newArr = new ITask[informationObjects.length];
        for(int i=0 ; i < informationObjects.length ; i++){
            newArr[i] = (ITask) informationObjects[i];
        }
        return newArr;
    }
    public static INode getNode(IFolder fold, String fldn){
        List<INode> nodesByName = fold.getNodesByName(fldn);
        return fold.getNodeByID(nodesByName.get(0).getID());
    }
}