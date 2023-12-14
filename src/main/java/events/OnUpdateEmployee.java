package events;

import annotprop.Conf;
import com.ser.blueline.*;
import com.ser.blueline.bpm.*;
import com.ser.blueline.metaDataComponents.*;
import com.ser.foldermanager.*;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ser.bn.se.demosystems.documents.Constants;

import java.io.IOException;
import java.util.*;

public class OnUpdateEmployee extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    @Override
    protected Object execute() {
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            if(mainDocument == null)return resultError("Main Document is NULL");
            log.info("----OnChangeEmployee Agent Started ---for IDocument ID:--" + mainDocument.getID());

            String userName = mainDocument.getDescriptorValue("orgEmployeeLogin");
            IUser emplUser = getDocumentServer().getUserByLoginName(getSes(),userName);
            if(emplUser == null)return resultError("UserName is NULL");
            String userWBName = getWorkbasketIDfromUserID(emplUser.getID());
            if (userWBName==null)return resultError("Workbasket Name is NULL");
            IWorkbasket userWB = getBpm().getWorkbasketByName(userWBName);
            IReceivers delegatedUsers = userWB.getActionOnAssignReceivers();

            String dUserWBID = mainDocument.getDescriptorValue("orgEmployeeAbsence");
            if(dUserWBID == null){
                //IReceivers receivers = userWB.getActionOnAssignReceivers();
            }else {
                IWorkbasket dUserWB = getBpm().getWorkbasket(dUserWBID);
                IWorkbasket userWBCopy = userWB.getModifiableCopy(getSes());
                userWBCopy.setActionOnAssign(WorkbasketActionOnAssign.DELEGATE);
                IReceivers receivers = getBpm().createReceivers(dUserWB);
                userWBCopy.setActionOnAssignReceivers(receivers);
                userWBCopy.commit();
            }
            log.info("----OnChangeEmployee Agent Finished ---for IDocument ID:--" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }
    public String getWorkbasketIDfromUserID(String userID) throws Exception {
        log.info("Getting workbasket ID from user ID");
        IStringMatrix workbaskets = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
        if (workbaskets == null) throw new Exception("Workbaskets Global Value List not found");
        List<List<String>> rawTable = workbaskets.getRawRows();
        return getDatafromTable(userID,rawTable);
    }
    public String getDatafromTable(String userID, List<List<String>> rawTable) {
        for(List<String> list : rawTable) {
            if(list.contains(userID)) {
                //we found the user
                //return first column as workbasketID
                log.info("workbasket name for user ID: " + userID + " is " + list.get(1));
                return list.get(1);
            }
        }
        return null;
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
