package events;

import annotprop.Conf;
import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.*;
import com.ser.blueline.security.DocumentInstanceRight;
import com.ser.foldermanager.*;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;

import java.io.IOException;
import java.util.*;

public class OnChangeProjectDoc extends UnifiedAgent {

    private Logger log = LogManager.getLogger();

    @Override
    protected Object execute() {

        ISession ses = this.getSes();
        IDocumentServer srv = ses.getDocumentServer();
        //(1) Make sure we have a main document
        //IInformationObject mainDocument = getEventDocument();
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();

            log.info("----OnChangeProjectDoc Agent Started ---for IDocument ID:--" + mainDocument.getID());

            if(mainDocument.getDescriptorValue("ccmPRJCard_code") == null){
                return resultSuccess("Agent Finished Project Doc is null");
            }

            if(!Objects.equals(mainDocument.getDescriptorValue("ccmPrjDocCategory"), "Transmittal")) {

                Object chkDoc = checkDublicateEngDocByFileName(mainDocument);
                if (chkDoc != null) {
                    mainDocument.setDescriptorValue("ccmPrjDocFileName", mainDocument.getDescriptorValue("ccmPrjDocFileName") + "(DUBLICATE)");
                }

                if (getEventName() != null && getEventName().equals("createDocument")) {

                    mainDocument.setDescriptorValue("ccmPrjDocApprCode", "");
                    mainDocument.setDescriptorValue("ccmPrjDocWFProcessName", "");
                    mainDocument.setDescriptorValue("ccmPrjDocWFTaskName", "");
                    mainDocument.setDescriptorValue("ccmPrjDocWFTaskRecipients", "");
                    mainDocument.setDescriptorValue("ccmPrjDocTransIncCode", "");
                    mainDocument.setDescriptorValue("ccmPrjDocTransOutCode", "");

                    mainDocument.removeDescriptor(getDocumentServer().getDescriptorForName(getSes(), "ccmPrjDocWFTaskCreation"));
                    // mainDocument.removeDescriptor(getDocumentServer().getDescriptorForName(getSes(),"ccmPrjDocDate"));
                    mainDocument.removeDescriptor(getDocumentServer().getDescriptorForName(getSes(), "ccmPrjDocReqDate"));
                    mainDocument.removeDescriptor(getDocumentServer().getDescriptorForName(getSes(), "ccmPrjDocDueDate"));

                    mainDocument.setDescriptorValueTyped("ccmPrjDocDate", (new java.util.Date()));

                    mainDocument.commit();

                    //this.removeReleaseOldEngDoc(mainDocument);

                    //this.addToDCCNode("DCC", "Eng.Docs.Import", mainDocument);

                }

                this.removeReleaseOldEngDoc(mainDocument);

                /*String taskStatus = mainDocument.getDescriptorValue("ccmPrjDocWFTaskName");
                log.info("----OnChangeProjectDoc Task status : " + taskStatus + " ---for IDocument ID:--" + mainDocument.getID());
                if(Objects.equals(taskStatus, "Completed")){
                    log.info("----OnChangeProjectDoc mutability : " + mainDocument.getMutability() + " ---for IDocument ID:--" + mainDocument.getID());
                    if(mainDocument.getMutability() != IMutability.IMMUTABLE) {
                        mainDocument.commit();
                        srv.updateMutability(getSes(), mainDocument, IMutability.IMMUTABLE);
                        log.info("----OnChangeProjectDoc mutability final : " + mainDocument.getMutability() + " ---for IDocument ID:--" + mainDocument.getID());
                    }
                }*/
            }

            log.info("----OnChangeProjectDoc Agent Finished ---for IDocument ID:--" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    private boolean addToDCCNode(String rootName, String nodeName, IDocument pdoc) throws Exception {
        log.info("Add2RootNode Start");
        boolean add2Node = false;
        IFolder prjFolder = getProjectFolder();
        if(prjFolder == null){
            throw new Exception("Project folder not found.");
        }
        List<INode> nodesByName = prjFolder.getNodesByName(rootName);
        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        INode newNode = root.getItemByName(nodeName);
        if(newNode != null) {
            log.info("Find Node : " + newNode.getID() + " /// " + nodeName);
            boolean isExistElement = false;
            log.info("Start ProjectDoc exit in folder: " + isExistElement);
            IElements nelements = newNode.getElements();
            for(int i=0;i<nelements.getCount2();i++) {
                IElement nelement = nelements.getItem2(i);
                String edocID = nelement.getLink();
                String pdocID = pdoc.getID();
                if(Objects.equals(pdocID, edocID)){
                    isExistElement = true;
                    break;
                }
            }
            log.info("Finish ProjectDoc exit in folder: " + isExistElement);
            if(!isExistElement) {
                add2Node = prjFolder.addInformationObjectToNode(pdoc.getID(), newNode.getID());
                log.info("ProjectDoc add to root folder: " + newNode.getID());
                prjFolder.commit();
            }
        }
        log.info("ProjectDoc add to root node result : " + add2Node);
        return add2Node;
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
    public IQueryDlg findQueryDlgForQueryClass(IQueryClass queryClass) {
        IQueryDlg dlg = null;
        if (queryClass != null) {
            dlg = queryClass.getQueryDlg("default");
        }

        return dlg;
    }

    public IQueryParameter query(ISession session, IQueryDlg queryDlg, Map<String, String> descriptorValues) {
        IDocumentServer documentServer = session.getDocumentServer();
        ISerClassFactory classFactory = documentServer.getClassFactory();
        IQueryParameter queryParameter = null;
        IQueryExpression expression = null;
        IComponent[] components = queryDlg.getComponents();

        for(int i = 0; i < components.length; ++i) {
            if (components[i].getType() == IMaskedEdit.TYPE) {
                IControl control = (IControl)components[i];
                String descriptorId = control.getDescriptorID();
                String value = (String)descriptorValues.get(descriptorId);
                if (value != null && value.trim().length() > 0) {
                    IDescriptor descriptor = documentServer.getDescriptor(descriptorId, session);
                    IQueryValueDescriptor queryValueDescriptor = classFactory.getQueryValueDescriptorInstance(descriptor);
                    queryValueDescriptor.addValue(value);
                    IQueryExpression expr = queryValueDescriptor.getExpression();
                    if (expression != null) {
                        expression = classFactory.getExpressionInstance(expression, expr, 0);
                    } else {
                        expression = expr;
                    }
                }
            }
        }

        if (expression != null) {
            queryParameter = classFactory.getQueryParameterInstance(session, queryDlg, expression);
        }

        return queryParameter;
    }
    public IDocumentHitList executeQuery(ISession session, IQueryParameter queryParameter) {
        IDocumentServer documentServer = session.getDocumentServer();
        return documentServer.query(queryParameter, session);
    }
    public IDocument checkDublicateEngDocByFileName(IDocument doc1){
        IDocument result = null;
        ISession session = this.getSes();
        String searchClassName = "Search Engineering Documents";
        IDocumentServer documentServer = session.getDocumentServer();
        IDescriptor descriptor1 = documentServer.getDescriptorForName(session, "ccmPRJCard_code");
        IDescriptor descriptor2 = documentServer.getDescriptorForName(session, "ccmPrjDocFileName");
        IQueryClass queryClass = documentServer.getQueryClassByName(session, searchClassName);
        IQueryDlg queryDlg = this.findQueryDlgForQueryClass(queryClass);
        Map<String, String> searchDescriptors = new HashMap();
        searchDescriptors.put(descriptor1.getId(), doc1.getDescriptorValue("ccmPRJCard_code"));
        searchDescriptors.put(descriptor2.getId(), doc1.getDescriptorValue("ccmPrjDocFileName"));
        IQueryParameter queryParameter = this.query(session, queryDlg, searchDescriptors);
        if (queryParameter != null) {
            IDocumentHitList hitresult = this.executeQuery(session, queryParameter);
            IDocument[] hits = hitresult.getDocumentObjects();
            queryParameter.close();
            for(IDocument ldoc : hits){
                String docID = doc1.getID();
                String chkID = ldoc.getID();
                if(!Objects.equals(docID, chkID)){
                    result = ldoc;
                    break;
                }
            }
        }
        return result;
    }
    public void removeReleaseOldEngDoc(IDocument doc1){
        IDocument result = null;
        ISession session = this.getSes();
        String searchClassName = "Search Engineering Documents";
        IDocumentServer documentServer = session.getDocumentServer();
        IDescriptor descriptor1 = documentServer.getDescriptorForName(session, "ccmPrjDocNumber");
        IDescriptor descriptor2 = documentServer.getDescriptorForName(session, "ccmPRJCard_code");
        //IDescriptor descriptor2 = documentServer.getDescriptorForName(session, "ccmPrjDocRevision");
        IDescriptor descriptor3 = documentServer.getDescriptorForName(session, "ccmReleased");
        IQueryClass queryClass = documentServer.getQueryClassByName(session, searchClassName);
        IQueryDlg queryDlg = this.findQueryDlgForQueryClass(queryClass);
        Map<String, String> searchDescriptors = new HashMap();
        searchDescriptors.put(descriptor1.getId(), doc1.getDescriptorValue("ccmPrjDocNumber"));
        searchDescriptors.put(descriptor2.getId(), doc1.getDescriptorValue("ccmPRJCard_code"));
        searchDescriptors.put(descriptor3.getId(), "1");
        if(doc1.getDescriptorValue("ccmPrjDocNumber") != null) {
            IQueryParameter queryParameter = this.query(session, queryDlg, searchDescriptors);
            if (queryParameter != null) {
                IDocumentHitList hitresult = this.executeQuery(session, queryParameter);
                IDocument[] hits = hitresult.getDocumentObjects();
                queryParameter.close();
                for (IDocument ldoc : hits) {
                    String docID = doc1.getID();
                    String chkID = ldoc.getID();
                    if(!Objects.equals(docID, chkID)){
//                    if(ldoc.getMutability() == IMutability.FIXED_CONTENT) {
//                        getSes().getDocumentServer().updateMutability(getSes(), ldoc, IMutability.MUTABLE);
//                        ldoc.commit();
//                    }
                        ldoc.setDescriptorValue("ccmReleased","0");
                        ldoc.commit();
                        //getSes().getDocumentServer().updateMutability(getSes(), ldoc, IMutability.FIXED_CONTENT);
                    }
                }
            }
        }
    }
}
