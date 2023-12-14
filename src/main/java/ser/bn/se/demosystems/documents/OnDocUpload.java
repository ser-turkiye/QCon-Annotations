package ser.bn.se.demosystems.documents;

import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.FMLinkType;
import com.ser.foldermanager.IElement;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class OnDocUpload extends UnifiedAgent {
    private Logger log = LogManager.getLogger();
    private static boolean isPdf;
    private static boolean isCrs;
    private boolean isRefresh;
    @Override
    protected Object execute() {

        try{
            if(getEventDocument() == null) return resultError("OBJECT CLIENT ID is null or not of type IDocument");

            IDocument curDoc = getEventDocument();

            curDoc.setDescriptorValue("ccmPrjDocApprCode" ,"");
            curDoc.setDescriptorValue("ccmPrjDocWFProcessName" ,"");
            curDoc.setDescriptorValue("ccmPrjDocWFTaskName" ,"");
            curDoc.setDescriptorValue("ccmPrjDocWFTaskCreation" ,"");
            curDoc.setDescriptorValue("ccmPrjDocWFTaskRecipients" ,"");
            curDoc.setDescriptorValue("ccmPrjDocTransInCode" ,"");
            curDoc.setDescriptorValue("ccmPrjDocTransOutCode" ,"");
            curDoc.commit();


            //(1) Link Document To Project Folder if extension is dmg or pdf
            String fileName = getEventDocument().getDescriptorValue("ObjectSubject");
            if(fileName == null) return resultError("File Name is NULL");
            fileName = fileName.toLowerCase();
            isPdf = fileName.toLowerCase().contains("pdf");
            isCrs = fileName.toLowerCase().contains("crs");
            IFolder projectFolder = getProjectFolder();
            linkDocToFolder(projectFolder , fileName);
            linkDocToDoc();
            //(2) Start Workflow and attach document in it
            if(isPdf){
             //   startProcess();
            }
        }catch (Exception e){
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished");
    }



    private void startProcess() throws Exception {
        log.info("Starting new process instance");
        IProcessInstance pi = buildNewProcessInstanceForID(Constants.ClassIDs.DocumentCycle);
        if(pi == null) throw new Exception("Coudln't start PI with ID: " + Constants.ClassIDs.DocumentCycle);
        mapDescriptorsFromObjectToObject(getEventDocument() , pi , false);
        pi.setDescriptorValue("TaskVersion" , "0.1");
        pi.setMainInformationObjectID(getEventInfObjID());
        log.info("Attempting to commit");
        pi.commit();
    }


    private void linkDocToFolder(IFolder folder , String fileName) throws Exception{
        log.info("Linking document to folder");
        String nodeId = "";
        if(fileName.contains("pdf")) nodeId = Constants.Nodes.CivilStructure;
        else if(fileName.contains("transm")) nodeId = Constants.Nodes.Transmittal;
        else if(fileName.contains("crs")) nodeId = Constants.Nodes.Review;
        else nodeId = Constants.Nodes.NativeFiles;
        INode node = folder.getNodeByID(nodeId);
        if(node == null) throw new Exception("Node with ID: " + nodeId + " not found");
        log.info("Linking document to node: " + node.getDisplayString());
        IElement el = node.getElements().addNew(FMLinkType.DOCUMENT);
        el.setLink(getEventInfObjID());
        log.info("Attempting commit");
        folder.commit();
    }

    private void linkDocToDoc()throws Exception{
        log.info("Linking Doc to Doc");
        String linkedDocID = getEventDocument().getDescriptorValue(Constants.Descriptors.NumberReference );
        if(linkedDocID == null) return;
        if(linkedDocID.isEmpty()) return;
        log.info("Found doc to Link to ");
        IDocument doc = getDocumentServer().getDocument4ID(linkedDocID , getSes());
        if(doc == null) throw new Exception("Document with ID: " + linkedDocID + " not found");
        getDocumentServer().linkInformationObjects(getSes() , getEventInfObjID() , doc.getID());
        getDocumentServer().linkInformationObjects(getSes() , doc.getID() ,  getEventInfObjID());
        log.info("Linked");
    }

    private IFolder getProjectFolder() throws Exception {
        log.info("Getting Project Folder");
        String projectNumber = getEventDocument().getDescriptorValue("ProjectNumber");
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

    /**
     * Map Descriptors from Source IInformationObject to Target IInformationObject
     * @param srcObject Source Object
     * @param targetObject Target Object
     * @param overwriteValues Overwrite pre-exisitng values in target object
     * @return true for success false for failure
     */
    public boolean mapDescriptorsFromObjectToObject(IInformationObject srcObject , IInformationObject targetObject , boolean overwriteValues){
        log.info("Mapping Descriptors from PInformation to Information Object");
        String[] targeObjectAssignedDesc;
        if(targetObject instanceof IFolder){
            log.info("Information Object is of type IFolder");
            String classID = targetObject.getClassID();
            IArchiveFolderClass folderClass = getDocumentServer().getArchiveFolderClass(classID , getSes());
            targeObjectAssignedDesc = folderClass.getAssignedDescriptorIDs();
        }else if(targetObject instanceof IDocument){
            log.info("Information Object is of type IDocument");
            IArchiveClass documentClass = ((IDocument)targetObject).getArchiveClass();
            targeObjectAssignedDesc = documentClass.getAssignedDescriptorIDs();
        }else if(targetObject instanceof ITask){
            log.info("Information Object is of type ITask");
            IProcessType processType = ((ITask)targetObject).getProcessType();
            targeObjectAssignedDesc = processType.getAssignedDescriptorIDs();
        }else if(targetObject instanceof IProcessInstance){
            log.info("Information Object is of type IProcessInstace");
            IProcessType processType = ((IProcessInstance)targetObject).getProcessType();
            targeObjectAssignedDesc = processType.getAssignedDescriptorIDs();
        }else{
            log.error("Information Object is not of Supported type");
            return false;
        }
        List<String> targetDesc = Arrays.asList(targeObjectAssignedDesc);
        IValueDescriptor[] srcDesc = srcObject.getDescriptorList();
        for(int i=0; i <  srcDesc.length; i++){
            IValueDescriptor vd = srcDesc[i];
            String descID = vd.getId();
            String descName = vd.getName();
            try{
                String descValue = "";
                for (String val:vd.getStringValues()) {
                    descValue += val;
                }
                if(descValue ==null || descValue =="") continue;
                if(targetDesc.contains(descID)){
                    if(targetObject.getDescriptorValue(descID) != null && targetObject.getDescriptorValue(descID) != "")
                        if(!overwriteValues)continue;
                    log.info("Adding descriptor: "+descName +" with value: "+descValue);
                    targetObject.setDescriptorValue(descID , descValue);
                }
            } catch (Exception e) {
                log.error("Exception caught while adding descriptor: "+descName);
                log.error(e.getMessage());
                return false;
            }
        }
        return true;
    }

    public IProcessInstance buildNewProcessInstanceForID(String id){
        try{
            log.info("Building new Process Instance with ID: " +id );
            if(id ==null) return null;
            IBpmService bpm = getBpm();
            IProcessType processType = bpm.getProcessType(id);
            if(processType ==null) {
                log.error("Process Type with ID couldn't be found");
                return null;
            }
            IBpmService.IProcessInstanceBuilder processBuilder = bpm.buildProcessInstanceObject().setProcessType(processType);
            IBpmDatabase processDB = getSes().getBpmDatabaseByName(processType.getDefaultDatabase().getName());
            if(processDB ==null){
                log.error("Process Type: " + processType.getName() + " has no assigned databases");
                return null;
            }
            processBuilder.setBpmDatabase(processDB);
            processBuilder.setValidationProcessDefinition(processType.getActiveProcessDefinition());
            return processBuilder.build();
        }catch(Exception e){
            log.error(e.getMessage());
            return null;
        }
    }
}
