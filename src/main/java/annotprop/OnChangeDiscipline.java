package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;
import com.ser.foldermanager.FMNodeType;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;
import com.ser.foldermanager.INodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OnChangeDiscipline extends UnifiedAgent {

    private Logger log = LogManager.getLogger();

    @Override
    protected Object execute() {
        //(1) Make sure we have a main document
        //IInformationObject mainDocument = getEventDocument();
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            log.info("---- Agent Started ---for IDocument ID:--" + mainDocument.getID());

            IFolder prjFolder = getProjectFolder();
            if(prjFolder == null){
                throw new Exception("Project folder not found.");
            }

            INode newNode = addNewNode(prjFolder, "Engineering Docs", mainDocument.getDescriptorValue("ccmDisciplineName"));
            //removeNode(prjFolder, "Engineering Docs", mainDocument.getDescriptorValue("ccmDisciplineName"));
            //removeAllNodes(prjFolder, "Engineering Docs");

            updateDisciplineMembers2GVList("CCM_PARAM_DISCIPLINE-BYMEMBERS", mainDocument.getID(), mainDocument);
            updateParamGVList("CCM_PARAM_DISCIPLINE-CODES", mainDocument.getID(), mainDocument, newNode.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }

    public String getUserByWB(String wbID){
        String rtrn = "";
        if(wbID != null) {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowID = settingsMatrix.getValue(i, 0);
                if (rowID.equalsIgnoreCase(wbID)) {
                    rtrn = settingsMatrix.getValue(i, 2);
                    break;
                }
            }
        }
        return rtrn;
    }

    public void updateDisciplineMembers2GVList(String paramName, String paramKey, IDocument doc){
        String rowValue = "";
        String managerID = doc.getDescriptorValue("ccmDisciplineManager");
        String managerName = getUserByWB(managerID);
        String discMembers = doc.getDescriptorValue("ccmDisciplineMembers");
        String membersD = "";
        String[] membersIDs = new String[0];

        if(discMembers != null) {
            membersD = doc.getDescriptorValue("ccmDisciplineMembers").replace("[", "").replace("]", "");
            membersIDs = membersD.split(",");
        }
        List<String> memberList = new ArrayList<>();;

        for (String memberID : membersIDs) {
            String memberName = getUserByWB(memberID);
            //assert false;
            memberList.add(memberName);
        }

        int rowCount = 0;
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        List<List<String>> rows = settingsMatrix.getRows();

        IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
        for(int i = 0; i < settingsMatrix.getRowCount(); i++) {
            rowValue = settingsMatrix.getValue(i, 1);

            if (rowValue.equalsIgnoreCase(paramKey)) {
                try {
                    srtMatrixModify.removeRow(i);
                }catch (Exception e){
                    srtMatrixModify.removeRow(i-1);
                }
                srtMatrixModify.commit();
            }
        }
        settingsMatrix.refresh();


        if(!Objects.equals(managerName, "")) {
            srtMatrixModify.appendRow();
            settingsMatrix.refresh();
            rowCount = settingsMatrix.getRowCount();
            //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
            srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
            srtMatrixModify.setValue(rowCount, 1, doc.getID(), false);
            srtMatrixModify.setValue(rowCount, 2, doc.getDescriptorValue("ccmDisciplineName"), false);
            srtMatrixModify.setValue(rowCount, 3, doc.getDescriptorValue("ccmDisciplineManager"), false);
            srtMatrixModify.setValue(rowCount, 4, managerName, false);
            srtMatrixModify.setValue(rowCount, 5, "Manager", false);
            srtMatrixModify.commit();
        }

        if(!memberList.isEmpty()) {
            int c = 0;
            for (String memberName : memberList) {
                String mmbr = membersIDs[c];
                srtMatrixModify.appendRow();
                settingsMatrix.refresh();

                rowCount = settingsMatrix.getRowCount();

                //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
                srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                srtMatrixModify.setValue(rowCount, 1, doc.getID(), false);
                srtMatrixModify.setValue(rowCount, 2, doc.getDescriptorValue("ccmDisciplineName"), false);
                srtMatrixModify.setValue(rowCount, 3, mmbr, false);
                srtMatrixModify.setValue(rowCount, 4, memberName, false);
                srtMatrixModify.setValue(rowCount, 5, "Member", false);

                srtMatrixModify.commit();
                c++;
            }
        }
    }
    public void updateParamGVList(String paramName, String paramKey, IDocument doc, String nodeID){

        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValue = "";
        IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
        for(int i = 0; i < settingsMatrix.getRowCount(); i++) {
            rowValue = settingsMatrix.getValue(i, 1);
            if (rowValue.equalsIgnoreCase(paramKey)) {
                srtMatrixModify.removeRow(i);
                srtMatrixModify.commit();
            }
        }

        srtMatrixModify.appendRow();
        settingsMatrix.refresh();

        int rowCount = settingsMatrix.getRowCount();
        srtMatrixModify.setValue(rowCount,0, doc.getDescriptorValue("ccmPRJCard_code"), false);
        srtMatrixModify.setValue(rowCount,1, doc.getID(), false);
        srtMatrixModify.setValue(rowCount,2, doc.getDescriptorValue("ccmDisciplineName"), false);
        if(!Objects.equals(nodeID, "")) {
            srtMatrixModify.setValue(rowCount, 3, nodeID, false);
            srtMatrixModify.commit();
        }
    }
    private INode addNewNode(IFolder folder, String rootName, String nodeName) throws Exception {
        log.info("Add NewNode Start: " + rootName + " under new Node: " + nodeName);
        List<INode> nodesByName = folder.getNodesByName(rootName);
        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        INode newNode = root.getItemByName(nodeName);
        log.info("Find NewNode ?? : " + newNode);
        if(newNode == null) {
            newNode = root.addNew(FMNodeType.STATIC);
            newNode.setName(nodeName);
            folder.commit();
        }
        log.info("Add NewNode Final ?? : " + newNode);
        return newNode;
    }
    private void removeNode(IFolder folder, String rootName, String nodeName) throws Exception {
        log.info("Add NewNode Start");
        List<INode> nodesByName = folder.getNodesByName(rootName);
        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        INode newNode = root.getItemByName(nodeName);
        if(newNode != null) {
            root.removeByID(newNode.getID());
            folder.commit();
        }
    }
    private void removeAllNodes(IFolder folder, String rootName) throws Exception {
        log.info("Remove AllNode Start");
        List<INode> nodesByName = folder.getNodesByName(rootName);
        INode iNode = nodesByName.get(0);
        INodes root = (INodes) iNode.getChildNodes();
        root.removeAll();
    }

    public String getParamGVList(String paramName, String paramValue){
        String rtrn = "";

        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        for(int i = 0; i < settingsMatrix.getRowCount(); ++i) {
            if (settingsMatrix.getValue(i, 0).equalsIgnoreCase(paramValue)) {
                rtrn = settingsMatrix.getValue(i, 0);
            }
        }
        return rtrn;
    }
    public void appendParamGVList(String paramName, String newValue){

        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());

        IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
        srtMatrixModify.appendRow();
        int rowCount = settingsMatrix.getRowCount();
        srtMatrixModify.setValue(rowCount,0, newValue, false);
        srtMatrixModify.commit();
    }
    public void deleteParamGVList(String paramName,String value) {

        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());

        IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
        for (int i = 0; i < settingsMatrix.getRowCount(); ++i) {
            if (settingsMatrix.getValue(i, 0).equalsIgnoreCase(value)) {
                srtMatrixModify.removeRow(i);
                srtMatrixModify.commit();
            }
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

    public static INode getNode(IFolder fold, String fldn){
        List<INode> nodesByName = fold.getNodesByName(fldn);
        return fold.getNodeByID(nodesByName.get(0).getID());
    }
}
