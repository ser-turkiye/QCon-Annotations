package annotprop;

import com.ser.blueline.IDocument;
import com.ser.blueline.IDocumentPart;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.IOverlayLayer;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.types.resources.selectors.InstanceOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class OnChangeAnnotation extends UnifiedAgent {

    private Logger log = LogManager.getLogger();

    @Override
    protected Object execute() {
        //(1) Make sure we have a main document
        //IInformationObject mainDocument = getEventDocument();
        String overLayerName = "";
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            log.info("---- Agent Started ---for IDocument ID:--" + mainDocument.getID());
            IDocumentPart mainDocPart = mainDocument.getPartDocument(0,0);

            int mainDocPartTotalOverlayers = mainDocPart.getOverlayLayerCount();

            List<IOverlayLayer> listOfLayers = new ArrayList<>();
            for(int i=0 ; i < mainDocPartTotalOverlayers ; i++){
                IOverlayLayer mainLayer = mainDocPart.getOverlayLayer(i);
                overLayerName = mainLayer.getOverlayName();

                if(overLayerName.equals("Approval Stamp")){
                    ///remove stamp layer
                    log.info("Remove Stamp Layer is: " + overLayerName);

                    mainDocPart.removeOverlayLayer(i);
                    //mainLayer.removeOverlay(mainLayer.getOverlay(i));
                    mainDocument.commit();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }

    private IDocument getMainDocument() throws Exception {
        log.info("Getting Main Document from task");
        IInformationObject mainObj = getEventTask().getProcessInstance().getMainInformationObject();
        if (mainObj == null) throw new Exception("Main Object Not found in task");
        if (!(mainObj instanceof IDocument)) throw new Exception("Main Object is not of Type IDocument");
        return (IDocument) mainObj;
    }


}
