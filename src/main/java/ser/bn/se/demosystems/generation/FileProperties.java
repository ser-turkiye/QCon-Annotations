package ser.bn.se.demosystems.generation;

import com.ser.blueline.IDocumentServer;
import com.ser.blueline.ISession;
import com.ser.blueline.metaDataComponents.IStringMatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileProperties {
    private ISession session;
    private IDocumentServer documentServer;
    private static final String GVLName = "CRSTemplate_Mapping";
    public Map<String , String> replacements;

    public FileProperties(ISession ses) throws Exception{
        session = ses;
        documentServer = ses.getDocumentServer();
        this.getProperties();
    }
    private void getProperties() throws Exception {
        IStringMatrix gvl = documentServer.getStringMatrix(GVLName , session);
        if(gvl == null) throw new Exception("Global Value List not found");
        replacements = new HashMap<>();
        for(List<String> row : gvl.getRows()){
            replacements.put(row.get(0) , row.get(1));
        }
        if(replacements.size() < 1) throw new Exception("Not entries found in GVL");
    }

}
