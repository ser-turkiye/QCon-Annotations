package ser.bn.se.demosystems.generation;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IProcessType;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.IFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FilingHelper {
    private ISession session;
    private IDocumentServer documentServer;
    private Logger log = LogManager.getLogger();
    public FilingHelper(ISession session){
        this.session = session;
        this.documentServer = session.getDocumentServer();
    }


    public IDocument archiveFileToDocumentClass(String filePath , String archiveClassID) throws Exception {
        IArchiveClass cls = documentServer.getArchiveClass(archiveClassID, session);
        if (cls == null) cls = documentServer.getArchiveClassByName(session, archiveClassID);
        if (cls == null) throw new Exception("Document Class: " + archiveClassID + " not found");

        String dbName = session.getDatabase(cls.getDefaultDatabaseID()).getDatabaseName();

        IDocument doc = documentServer.getClassFactory().getDocumentInstance(dbName, cls.getID(), "0000", session);

        File file = new File(filePath);
        IRepresentation representation = doc.addRepresentation(".docx" , "Signed document");
        IDocumentPart newDocumentPart = representation.addPartDocument(filePath);
        doc.commit();
        file.delete();
        return doc;
    }


    public boolean mapDescriptorsFromObjectToObject(IInformationObject srcObject , IInformationObject targetObject , boolean overwriteValues){
        log.info("Mapping Descriptors from PInformation to Information Object");
        String[] targeObjectAssignedDesc;
        if(targetObject instanceof IFolder){
            log.info("Information Object is of type IFolder");
            String classID = targetObject.getClassID();
            IArchiveFolderClass folderClass = documentServer.getArchiveFolderClass(classID , session);
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
}
