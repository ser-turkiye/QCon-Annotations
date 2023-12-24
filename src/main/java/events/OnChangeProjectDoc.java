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
                }
                //this.removeReleaseOldEngDoc(mainDocument);
            }

            log.info("----OnChangeProjectDoc Agent Finished ---for IDocument ID:--" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
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

}
