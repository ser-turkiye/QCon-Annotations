package events;

import annotprop.Conf;
import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.*;
import com.ser.foldermanager.*;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;

import java.io.IOException;
import java.util.*;

public class OnCreateDCCExcel extends UnifiedAgent {

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

            if(Objects.equals(mainDocument.getClassID(), Conf.ClassIDs.DCCExcelSheet)) {
                this.addToDCCNode("DCC", "Import Metadata From Excel", mainDocument);
            }

            log.info("----OnChangeProjectDoc Agent Started ---for IDocument ID:--" + mainDocument.getID());

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
}
