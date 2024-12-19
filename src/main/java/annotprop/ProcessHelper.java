//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package annotprop;

import com.ser.blueline.IDocument;
import com.ser.blueline.IDocumentHitList;
import com.ser.blueline.IDocumentServer;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.IOrderByExpression;
import com.ser.blueline.IQueryParameter;
import com.ser.blueline.ISerClassFactory;
import com.ser.blueline.ISession;
import com.ser.blueline.IValueDescriptor;
import com.ser.blueline.bpm.IBpmDatabase;
import com.ser.blueline.bpm.IBpmService;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IProcessType;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IArchiveFolderClass;
import com.ser.foldermanager.IFolder;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.InetAddress;

public class ProcessHelper {
    private IDocumentServer documentServer;
    private ISession session;
    private Logger log = LogManager.getLogger();

    public ProcessHelper(ISession session) {
        this.session = session;
        this.documentServer = session.getDocumentServer();
    }
    public boolean isServerName(String serverName){

        String SystemName = null;
        try {
            SystemName = InetAddress.getLocalHost().getHostName();
            if(SystemName.equals(serverName)) return true;
        } catch (UnknownHostException e) {
            return false;
        }

            return false;
    }
    public List<String> mergeLists(List<String> list1, List<String> list2) {
        if(list1 == null && list2 == null){return null;}
        if(list1 == null){return list2;}
        if(list2 == null){return list1;}

        List<String> result = new ArrayList<>(list1);
        Iterator<String> var4 = list2.iterator();

        while(var4.hasNext()) {
            String e = var4.next();
            if (!result.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    public IProcessInstance buildNewProcessInstanceForID(String id) {
        try {
            this.log.info("Building new Process Instance with ID: " + id);
            if (id == null) {
                return null;
            } else {
                IBpmService bpm = this.session.getBpmService();
                IProcessType processType = bpm.getProcessType(id);
                if (processType == null) {
                    this.log.error("Process Type with ID couldn't be found");
                    return null;
                } else {
                    IBpmService.IProcessInstanceBuilder processBuilder = bpm.buildProcessInstanceObject().setProcessType(processType);
                    IBpmDatabase processDB = this.session.getBpmDatabaseByName(processType.getDefaultDatabase().getName());
                    if (processDB == null) {
                        this.log.error("Process Type: " + processType.getName() + " has no assigned databases");
                        return null;
                    } else {
                        processBuilder.setBpmDatabase(processDB);
                        processBuilder.setValidationProcessDefinition(processType.getActiveProcessDefinition());
                        return processBuilder.build();
                    }
                }
            }
        } catch (Exception var6) {
            this.log.error(var6.getMessage());
            return null;
        }
    }

    public boolean mapDescriptorsFromObjectToObject(IInformationObject srcObject, IInformationObject targetObject, boolean overwriteValues) {
        this.log.info("Mapping Descriptors from PInformation to Information Object");
        String[] targeObjectAssignedDesc;
        if (targetObject instanceof IFolder) {
            this.log.info("Information Object is of type IFolder");
            String classID = targetObject.getClassID();
            IArchiveFolderClass folderClass = this.documentServer.getArchiveFolderClass(classID, this.session);
            targeObjectAssignedDesc = folderClass.getAssignedDescriptorIDs();
        } else if (targetObject instanceof IDocument) {
            this.log.info("Information Object is of type IDocument");
            IArchiveClass documentClass = ((IDocument)targetObject).getArchiveClass();
            targeObjectAssignedDesc = documentClass.getAssignedDescriptorIDs();
        } else {
            IProcessType processType;
            if (targetObject instanceof ITask) {
                this.log.info("Information Object is of type ITask");
                processType = ((ITask)targetObject).getProcessType();
                targeObjectAssignedDesc = processType.getAssignedDescriptorIDs();
            } else {
                if (!(targetObject instanceof IProcessInstance)) {
                    this.log.error("Information Object is not of Supported type");
                    return false;
                }

                this.log.info("Information Object is of type IProcessInstace");
                processType = ((IProcessInstance)targetObject).getProcessType();
                targeObjectAssignedDesc = processType.getAssignedDescriptorIDs();
            }
        }

        List<String> targetDesc = Arrays.asList(targeObjectAssignedDesc);
        IValueDescriptor[] srcDesc = srcObject.getDescriptorList();

        for(int i = 0; i < srcDesc.length; ++i) {
            IValueDescriptor vd = srcDesc[i];
            String descID = vd.getId();
            String descName = vd.getName();
            int descType = vd.getMultiValueType();

            try {
                if (targetDesc.contains(descID) && (targetObject.getDescriptorValue(descID) == null || targetObject.getDescriptorValue(descID) == "" || overwriteValues)) {
                    if (vd.getMultiValueType() != 0) {
                        targetObject.setDescriptorValues(descID, srcObject.getDescriptorValues(descID, String.class));
                    } else {
                        targetObject.setDescriptorValue(descID, srcObject.getDescriptorValue(descID));
                    }
                }
            } catch (Exception var13) {
                this.log.error("Exception caught while adding descriptor: " + descName);
                this.log.error(var13.getMessage());
                return false;
            }
        }

        return true;
    }

    public IInformationObject[] createQuery1(String[] dbNames, String whereClause, int maxHits) {
        ISerClassFactory fac = this.documentServer.getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(this.session, dbNames, fac.getExpressionInstance(whereClause), (Date)null, (Date)null);
        que.setMaxHits(maxHits);
        que.setHitLimit(maxHits + 1);
        que.setHitLimitThreshold(maxHits + 1);
        IDocumentHitList hits = que.getSession() != null ? que.getSession().getDocumentServer().query(que, que.getSession()) : null;
        return hits == null ? null : hits.getInformationObjects();
    }

    public IInformationObject[] createQuery(String[] dbNames, String whereClause, String order, int maxHits, boolean lver) {
        ISerClassFactory fac = this.documentServer.getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(this.session, dbNames, fac.getExpressionInstance(whereClause), (Date)null, (Date)null);
        if (lver) {
            que.setCurrentVersionOnly(true);
        }

        if (maxHits > 0) {
            que.setMaxHits(maxHits);
            que.setHitLimit(maxHits + 1);
            que.setHitLimitThreshold(maxHits + 1);
        }

        if (!order.isEmpty()) {
            IOrderByExpression oexr = fac.getOrderByExpressionInstance(this.session.getDocumentServer().getInternalDescriptor(this.session, order), true);
            que.setOrderByExpression(oexr);
        }

        IDocumentHitList hits = que.getSession() != null ? que.getSession().getDocumentServer().query(que, que.getSession()) : null;
        return hits == null ? null : hits.getInformationObjects();
    }

    public String getDocumentURL(String documentID) {
        StringBuilder webcubeUrl = new StringBuilder();
        webcubeUrl.append("?system=").append(this.session.getSystem().getName());
        webcubeUrl.append("&action=showdocument&home=1&reusesession=1&id=").append(documentID);
        return webcubeUrl.toString();
    }

    public String getTaskURL(String taskID) {
        StringBuilder webcubeUrl = new StringBuilder();
        webcubeUrl.append("?system=").append(this.session.getSystem().getName());
        webcubeUrl.append("&action=showtask&home=1&reusesession=1&id=").append(taskID);
        return webcubeUrl.toString();
    }
}
