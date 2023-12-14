package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DoxisSearch {


    public static IQueryDlg findQueryDlgForQueryClass(IQueryClass queryClass) {
        IQueryDlg dlg = null;

        if (queryClass != null) {
            // Retrieves the dialog of type "DemoArchiveDlg" from the archive class.
            dlg = queryClass.getQueryDlg("default");
        }
        return dlg;
    }

    public static IQueryParameter query(ISession session, IQueryDlg queryDlg, Map<String, String> descriptorValues) {
        IDocumentServer documentServer = session.getDocumentServer();
        ISerClassFactory classFactory = documentServer.getClassFactory();


        IQueryParameter queryParameter = null;
        IQueryExpression expression = null;
        // Retrieve all components from the query dialog
        IComponent[] components = queryDlg.getComponents();
        int i;

        // Create the query expression by traversing over all components of the dialog.
        for (i = 0; i < components.length; i++) {
            // If the component is a masked edit field, check for the assigned descriptor.
            if (components[i].getType() == IMaskedEdit.TYPE) {
                // If the component is of type "masked edit", the component might be casted to
                // IControl or IMaskedEdit.
                IControl control = (IControl) components[i];

                // Get the descriptor ID from the control.
                String descriptorId = control.getDescriptorID();

                // Get the value for this descriptor.
                String value = descriptorValues.get(descriptorId);

                // If the value is not null and not an empty string, add this descriptor to the
                // query expression. Descriptors on documents must not be null or empty strings.
                if (value != null && value.trim().length() > 0) {
                    IQueryValueDescriptor queryValueDescriptor;

                    // Get the descriptor instance from the document server.
                    IDescriptor descriptor = documentServer.getDescriptor(descriptorId, session);

                    // Create a value descriptor for the descriptor instance and add the value.
                    queryValueDescriptor = classFactory.getQueryValueDescriptorInstance(descriptor);
                    queryValueDescriptor.addValue(value);

                    // Create an expression instance for this query value descriptor.
                    IQueryExpression expr = queryValueDescriptor.getExpression();

                    // If expression has been built during the previous loops, combine the existing
                    // expression with the new one using the AND operator.
                    if (expression != null) {
                        expression = classFactory.getExpressionInstance(expression, expr, IQueryOperator.AND);
                    }
                    // Otherwise just initialize expression with expr.
                    else {
                        expression = expr;
                    }
                }
            }
        }

        if (expression != null) {
            // Create a query parameter instance from the session, the query dialog to use and
            // the constructed expression.
            queryParameter = classFactory.getQueryParameterInstance(session, queryDlg, expression);
        }
        return queryParameter;
    }

    public static IDocumentHitList executeQuery(ISession session, IQueryParameter queryParameter) {
        IDocumentServer documentServer = session.getDocumentServer();
        return documentServer.query(queryParameter, session);
    }

    public static IInformationObject searchInformationObject(ISession session,String searchClassName,HashMap<String,Object> queryList) throws IOException {

        // String searchClassName = "EngineeringDocumentSearch";

        IDocumentServer documentServer = session.getDocumentServer();
        IQueryClass queryClass = documentServer.getQueryClassByName(session, searchClassName);
        IDocumentHitList result;
        // search for the query dialog.
        IQueryDlg queryDlg = findQueryDlgForQueryClass(queryClass);

        Map<String, String> searchDescriptors = new HashMap<>();

        for (Map.Entry<String, Object> entry : queryList.entrySet()) {
            String descriptorName = entry.getKey();
            Object value = entry.getValue();

            IDescriptor descriptor = documentServer.getDescriptorForName(session, descriptorName);
            searchDescriptors.put(descriptor.getId(), value.toString());
        }

        // Search for documents
        IQueryParameter queryParameter = query(session, queryDlg, searchDescriptors);

        // If the result list is not empty, create a csv-file with the results.
        if (queryParameter != null) {
            result = executeQuery(session, queryParameter);
            IInformationObject[] hits = result.getDocumentObjects();
            queryParameter.close();
            if (hits != null && hits.length > 0) {
                return hits[0];
            } else
                return null;


        } else {
            return null;
        }
    }
}
