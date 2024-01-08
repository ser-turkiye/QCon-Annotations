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

            if(!Objects.equals(mainDocument.getDescriptorValue("ccmPrjDocCategory"), "Correspondence")) {

                Object chkDoc = checkDublicateEngDocByFileName(mainDocument);
                if (chkDoc != null) {
                    mainDocument.setDescriptorValue("ccmPrjDocFileName", "_DUBLICATED_" + mainDocument.getDescriptorValue("ccmPrjDocFileName"));
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
                    try {
                        mainDocument.commit();
                    } catch (Exception e){
                        log.info("Exception Caught.. commit error:" + e);
                        resultRestart("OnChangeProjectDoc error. Restarting agent...");
                    }

                    IFolder prjFolder = this.getProjectFolder();
                    if (prjFolder != null) {
                        this.addToRootNode(prjFolder, "Workspace", "Import Document", mainDocument);
                    }
                    //this.removeReleaseOldEngDoc(mainDocument);
                }
            }

            log.info("----OnChangeProjectDoc Agent Finished ---for IDocument ID:--" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
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
    public INode getNodesByList(List<String> fNames) throws Exception {
        IFolder prjFolder = getProjectFolder();
        if(prjFolder == null){
            throw new Exception("Project folder not found.");
        }
        INode prjDocNode = prjFolder.getNodeByID(Constants.ClassIDs.ProjectDocFolder);
        if(prjDocNode == null){
            throw new Exception("Project Docs. folder not found.");
        }
        boolean isFirstNode = true;
        INode newNode = null;
        INodes childs = null;
        for(String fname : fNames) {
            if(isFirstNode) {
                childs = (INodes) prjDocNode.getChildNodes();
                isFirstNode = false;
            }else{
                if(newNode != null) {
                    childs = (INodes) newNode.getChildNodes();
                }
            }
            if(childs != null){
                newNode = childs.getItemByName(fname);
            }
        }
        log.info("Add NewNode Final ?? : " + newNode);
        return newNode;
    }
    private boolean addToRootNode(IFolder folder, String rootName, String nodeName, IDocument pdoc) throws Exception {
        log.info("Add2RootNode Start");
        boolean add2Node = false;
        List<INode> nodesByName = folder.getNodesByName(rootName);
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
                add2Node = folder.addInformationObjectToNode(pdoc.getID(), newNode.getID());
                log.info("ProjectDoc add to root folder: " + newNode.getID());
                folder.commit();
            }
        }
        log.info("ProjectDoc add to root node result : " + add2Node);
        return add2Node;
    }
    private boolean addToNode(INode newNode, IDocument pdoc) throws Exception {
        log.info("Add2Node Start");
        boolean add2Node = false;
        IFolder prjFolder = getProjectFolder();
        if(prjFolder == null){
            throw new Exception("Project folder not found.");
        }
        prjFolder.refresh(true);

        if(newNode != null) {
            newNode.refresh(true);
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
                INodes root = (INodes)newNode.getChildNodes();
                String oldNodeID = pdoc.getDescriptorValue("ccmLinkedFolderID");
                if (oldNodeID != newNode.getID()) {
                    INode oldNode = root.getItemByID(oldNodeID);
                    if (oldNode != null) {
                        IElements elements = oldNode.getElements();
                        while(elements.getCount2() > 0) {
                            elements.remove(0);
                        }
                        prjFolder.commit();
                    }
                }

                add2Node = prjFolder.addInformationObjectToNode(pdoc.getID(), newNode.getID());
                log.info("ProjectDoc add to folder: " + newNode.getID());
                if (add2Node) {
                    pdoc.setDescriptorValue("ccmLinkedFolderID", newNode.getID());
                    pdoc.commit();
                    log.info("ProjectDoc setting new node ID: " + newNode.getID());
                }
                prjFolder.commit();
            }
        }
        return add2Node;
    }
    private void removeFromNode(IFolder folder, String rootName, String nodeName, IDocument pdoc) throws Exception {
        log.info("Remove from Node Start");
        boolean add2Node = false;
        String oldNodeID = pdoc.getDescriptorValue("ccmLinkedFolderID");
        List<INode> nodesByName = folder.getNodesByName(rootName);
        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        //INode newNode = root.getItemByName(nodeName);
        INode newNode = root.getItemByID(oldNodeID);
        if(newNode != null) {
            IElements elements = newNode.getElements();
            while(elements.getCount2() > 0) {
                IElement element = elements.getItem2(0);
                elements.remove(0);
            }
            folder.commit();
        }
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
}
