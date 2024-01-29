package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class OnNewAnnotation extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private IDocument mainDocument;
    private IDocument reviewDoc;
    private ITask mainTask;
    private boolean taskRestarted = false;
    private boolean isRefresh;
    String strFullTimeID = "";
    @Override
    protected Object execute() {
        //(1) Check if the main document is locked
        // if main task is locked then just restart
        mainTask = getEventTask();
        reviewDoc = (IDocument) mainTask.getProcessInstance().getMainInformationObject();

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        strFullTimeID = dateFormat.format(date);

        if(reviewDoc == null) return resultError("Review Doc is NULL");
        try{
            log.info("---- Agent Started -----" + reviewDoc.getID());
            String refreshStatus = (String) getParams().get("OBJECTSTATUS");
            log.info("Extracted Refresh Status: " + refreshStatus);
            if(refreshStatus == null) isRefresh = false;
            else isRefresh = refreshStatus.equals("refresh");

            this.helper = new ProcessHelper(getSes());
            log.info("Is Referesh: " + isRefresh);
            String mainDocID = reviewDoc.getDescriptorValue(Conf.Descriptors.MainDocumentID);
            mainDocument = getDocumentServer().getDocument4ID(mainDocID , getSes());

            log.info("New annotation start for maindoc : " + mainDocID);
            log.info("New annotation start for subdoc : " + reviewDoc.getID());
            if(mainDocument == null){
                return resultError("MainDoc cannot be found or deleted. ID:" + mainDocID);
            }
            if(!this.copyOnlyDiffentLays(reviewDoc, mainDocument)){
                return resultRestart("Restarting Agent for copyOnlyDifferentLays");
            }
            if(!this.copyLayerToOtherSubDocuments(mainDocID)){
                return resultRestart("Restarting Agent for copyLayerToOtherSubDocuments");
            }
            log.info("New annotation finish for maindoc : " + mainDocID);
            log.info("New annotation finish for subdoc : " + reviewDoc.getID());

            reviewDoc.setDescriptorValue("ccmReleased","1");
            reviewDoc.commit();

        }catch (Exception e){
            log.error("Exception Caught");
            log.error(e.getMessage());
            if(taskRestarted) return resultRestart("Restarting Agent");
            log.info("Unlocking the Main Task");
            if(mainTask!=null){mainTask.getProcessInstance().unlock();}
            log.error("----------------- Agent Couldnt execute ----------------");
            return resultError(e.getMessage());
        }
        log.info("----------------- Agent Finished ----------------" + reviewDoc.getID());
        return resultSuccess("Agent Finished sucesfully");
    }
    private boolean copyLayerToOtherSubDocuments(String mainDocID ) throws Exception{
        log.info("Copying Layer from Main document to All other Documents");
        boolean rtrn = true;
        //(1) Check to see if we have an overlayer with the same name
        //ITask[] subProcesses = getSubProcesses(mainDocID);
        ITask[] subTasks = getSubTasks(mainDocID);
        for(ITask task : subTasks){
            log.info("CopyLayerToOtherSubDoc for Task:" + task.getID());
            log.info("CopyLayerToOtherSubDoc for Task Name:" + task.getName());
            IProcessInstance pi = task.getProcessInstance();
            if(pi.findLockInfo().getOwnerID() != null) {
                log.info("Process Instance is locked, skipping subProcess Document");
                continue;
            }
            try{
                log.info("Locking Sub Task to Update its document for:" + reviewDoc.getID());
                pi.lock(task);
                log.info("Getting SubProcess Main Loaded Document for:" + reviewDoc.getID());
                IInformationObject subProcessObject = pi.getMainInformationObject();
                if(subProcessObject == null) continue;
                if(!(subProcessObject instanceof  IDocument)) continue;
                mainDocument = getDocumentServer().getDocument4ID(mainDocID , getSes());
                IDocument subDocument = (IDocument) subProcessObject;
                if (!Objects.equals(subDocument.getID(), reviewDoc.getID())) {
                    log.info("Start Copying mainDocument to subDocument:" + reviewDoc.getID());
                    if(!copyOnlyDiffentLays(mainDocument, subDocument)){
                        throw new Exception("Error while copying layers to review documents");
                    }
                }
            }catch(Exception e){
                log.error("Exception Caught");
                log.error(e.getMessage());
                return false;
            }finally {
                log.info("Unlocking Task");
                pi.unlock();
            }
        }
        return rtrn;
    }
    public void copyLayers(IDocument sourceDoc, IDocument targetDoc){
        String overLayerName = "";
        boolean isSame=false;
        log.info("CopyLayers... sourcedoc to targetdoc:" + targetDoc.getID());
        IDocumentPart sourceDocPart = sourceDoc.getPartDocument( sourceDoc.getDefaultRepresentation() , 0);
        IDocumentPart targetDocPart = targetDoc.getPartDocument( targetDoc.getDefaultRepresentation() , 0);

        int sourceDocPartTotalOverlayers= sourceDocPart.getOverlayLayerCount();
        int targetDocPartTotalOverlayers= targetDocPart.getOverlayLayerCount();
        log.info("CopyLayers...sourcedocPartTotalOverLayers:" + sourceDocPartTotalOverlayers);
        log.info("CopyLayers...targetDocPartTotalOverlayers:" + targetDocPartTotalOverlayers);
        List<IOverlayLayer> listOfLayersToCopy = new ArrayList<>();
        for(int i=0 ; i < sourceDocPartTotalOverlayers ; i++){
            IOverlayLayer sourceLayer = sourceDocPart.getOverlayLayer(i);
            String copiedLayerName = sourceLayer.getOverlayName();
            if(copiedLayerName == null){
                log.info("*** Copied Layer Name is NULLL");
                overLayerName =sourceDocPart.getOverlayLayer(i).getOverlayName();
                copiedLayerName = overLayerName + "-" + ( UUID.randomUUID());
                sourceDocPart.getOverlayLayer(i).setOverlayName(copiedLayerName);
                log.info("New Name is: " + copiedLayerName);
                //reviewDoc.commit();
            }else if(!(copiedLayerName.contains(overLayerName))) continue;

            for(int k=0 ; k < targetDocPartTotalOverlayers ; k++) {

                IOverlayLayer targetLayer = targetDocPart.getOverlayLayer(k);
                if(isSameOverLayLayer(sourceLayer,targetLayer) ){
                    isSame = true;
                    break;
                }
            }

            if (!isSame) listOfLayersToCopy.add(sourceLayer);

        }
        log.info("CopyLayers...listOfLayersToCopy..empty check");
        if(!listOfLayersToCopy.isEmpty()){
            for (IOverlayLayer layer : listOfLayersToCopy) {
                log.info("CopyLayers...addlayer to targetdocpart... layer page info:" + layer.getPageInPart());
                log.info("CopyLayers...addlayer to targetdocpart... layer page info:" + layer.getPageInPart());
                targetDocPart.addOverlayLayer(layer);
                log.info("CopyLayers...targetdocpart addlayer");
            }
            targetDoc.commit();
        }
    }
    public boolean copyOnlyDiffentLays(IDocument sourceDoc, IDocument targetDoc) throws Exception{
        boolean isSame = false;
        try {
            log.info("CopyDiffLays sourcedoc to targetdoc:" + targetDoc.getID());
            int sourceOverlayLayerCount = sourceDoc.getPartDocument(0,0).getOverlayLayerCount();
            int targetOverlayLayerCount = targetDoc.getPartDocument(0,0).getOverlayLayerCount();
            ///if deleted annotation from source, delete all annotation from target
            //if(sourceOverlayLayerCount > targetOverlayLayerCount) {
                deleteAllOverLays(targetDoc);
                for (int i = 0; i < sourceOverlayLayerCount; i++) {
                    IOverlayLayer sourceOverlayLayer = sourceDoc.getPartDocument(0, 0).getOverlayLayer(i);
                    targetDoc.getPartDocument(0,0).addOverlayLayer(sourceOverlayLayer);
                    IOverlayLayer targetOverlayLayer = targetDoc.getPartDocument(0, 0).getOverlayLayer(i);
                    targetOverlayLayer.setPageInPart(sourceOverlayLayer.getPageInPart());
                }
            //}
            targetDoc.setDescriptorValue("ccmOriginated",strFullTimeID);
            targetDoc.commit();
            return true;
        }catch(Exception e){
            log.error("Exception Caught : function is copyOnlyDiffentLays");
            log.error(e.getMessage());
            return false;
        }
    }
    public void deleteAllOverLays(IDocument doc){
        log.info("Start Remove All Layer from MAIN DOC : " + doc.getID());
        IDocumentPart mainDocPart = doc.getPartDocument(0,0);
        mainDocPart.removeAllOverlayLayers();
        doc.commit();
    }
    public List<IOverlay>  getAllOverlays(IDocument doc){
        IDocumentPart docPart = doc.getPartDocument(0,0);
        List<IOverlay> overlays = new ArrayList<>() ;
        int documentOverLayerCount = docPart.getOverlayLayerCount();
        for(int i=0 ; i < documentOverLayerCount ; i++){
            IOverlayLayer overlayLayer = docPart.getOverlayLayer(i);
            int overlayCount = overlayLayer.getOverlayCount();
            for(int k=0; k<overlayCount;k++){
                overlays.add(overlayLayer.getOverlay(k));
            }
        }
        return overlays;
    }
    public static boolean isSameOverLayLayer(IOverlayLayer sourceOverlayLayer,IOverlayLayer targetOverlayLayer){

        int cnt=0;
        int sourceOverLayerCount = sourceOverlayLayer.getOverlayCount();
        int targetOverLayerCount = targetOverlayLayer.getOverlayCount();

        if(sourceOverLayerCount != targetOverLayerCount) return false;

        for(int i=0 ; i < sourceOverLayerCount ;i++){
            for(int k=0 ; k < targetOverLayerCount ;k++){
                if(sourceOverlayLayer.getOverlay(i).getObjectType() == targetOverlayLayer.getOverlay(k).getObjectType() ) {
                    if(isSameOverLay(sourceOverlayLayer.getOverlay(i),targetOverlayLayer.getOverlay(k))){
                        cnt++;
                        break;
                    }
                }
            }
        }
        return sourceOverLayerCount == cnt;
    }
    public static boolean isSameOverLay(IOverlay soruceOverLay, IOverlay targetOverLay){
        Point sourceStartPoint;
        Point sourceEndPoint;
        Point targetStartPoint;
        Point tagetEndPoint;

        String sourceMemo;
        String targetMemo;


        if(soruceOverLay.getObjectType() == targetOverLay.getObjectType() ) {

                int oType = soruceOverLay.getObjectType();
                switch (oType) {
                    case 1: //Marker Overlay

                        sourceStartPoint = ((IMarkerOverlay) soruceOverLay).getStartPosition();
                        sourceEndPoint = ((IMarkerOverlay) soruceOverLay).getEndPosition();

                        targetStartPoint = ((IMarkerOverlay) targetOverLay).getStartPosition();
                        tagetEndPoint = ((IMarkerOverlay) targetOverLay).getEndPosition();

                        sourceMemo = Arrays.toString(((IMarkerOverlay) soruceOverLay).getMemo());
                        targetMemo = Arrays.toString(((IMarkerOverlay) targetOverLay).getMemo());

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        if(sourceEndPoint.getX() != tagetEndPoint.getX() || sourceEndPoint.getY() != tagetEndPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);

                    case 2: //COMMENT OVERLAY
                        sourceStartPoint = ((ICommentOverlay) soruceOverLay).getStartPosition();
                        targetStartPoint = ((ICommentOverlay) targetOverLay).getStartPosition();

                        sourceMemo = Arrays.toString(((ICommentOverlay) soruceOverLay).getText());
                        targetMemo = Arrays.toString(((ICommentOverlay) targetOverLay).getText());

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);
                    case 3: //NOTE OVERLAY
                        sourceStartPoint = ((INoteOverlay) soruceOverLay).getStartPosition();
                        targetStartPoint = ((INoteOverlay) targetOverLay).getStartPosition();

                        sourceMemo = Arrays.toString(((INoteOverlay) soruceOverLay).getText());
                        targetMemo = Arrays.toString(((INoteOverlay) targetOverLay).getText());

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);
                    case 4: //ARROW OVERLAY
                        sourceStartPoint = ((IArrowOverlay) soruceOverLay).getStartPosition();
                        sourceEndPoint = ((IArrowOverlay) soruceOverLay).getEndPosition();

                        targetStartPoint = ((IArrowOverlay) targetOverLay).getStartPosition();
                        tagetEndPoint = ((IArrowOverlay) targetOverLay).getEndPosition();

                        sourceMemo = Arrays.toString(((IArrowOverlay) soruceOverLay).getMemo());
                        targetMemo = Arrays.toString(((IArrowOverlay) targetOverLay).getMemo());

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        if(sourceEndPoint.getX() != tagetEndPoint.getX() || sourceEndPoint.getY() != tagetEndPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);
                    case 5: //FREEHAND OVERLAY
                        sourceMemo = Arrays.toString(((IFreehandOverlay) soruceOverLay).getMemo());
                        targetMemo = Arrays.toString(((IFreehandOverlay) targetOverLay).getMemo());

                        return sourceMemo.equals(targetMemo);
                    case 6: //STAMPOVERLAY
                        sourceMemo = Arrays.toString(((IStampOverlay) soruceOverLay).getMemo());
                        targetMemo = Arrays.toString(((IStampOverlay) targetOverLay).getMemo());

                        return sourceMemo.equals(targetMemo);
                    case 8: //LINK OVERLAY
                        sourceStartPoint = ((ILinkOverlay) soruceOverLay).getStartPosition();
                        targetStartPoint = ((ILinkOverlay) targetOverLay).getStartPosition();

                        sourceMemo = ((ILinkOverlay) soruceOverLay).getAdditionalText();
                        targetMemo = ((ILinkOverlay) targetOverLay).getAdditionalText();

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);
                    case 9: //SHAPE OVERLAY
                        sourceStartPoint = ((IShapeOverlay) soruceOverLay).getStartPosition();
                        sourceEndPoint = ((IShapeOverlay) soruceOverLay).getEndPosition();

                        targetStartPoint = ((IShapeOverlay) targetOverLay).getStartPosition();
                        tagetEndPoint = ((IShapeOverlay) targetOverLay).getEndPosition();

                        sourceMemo = Arrays.toString(((IShapeOverlay) soruceOverLay).getMemo());
                        targetMemo = Arrays.toString(((IShapeOverlay) targetOverLay).getMemo());

                        if(sourceStartPoint.getX() != targetStartPoint.getX() || sourceStartPoint.getY() != targetStartPoint.getY()) return false;
                        if(sourceEndPoint.getX() != tagetEndPoint.getX() || sourceEndPoint.getY() != tagetEndPoint.getY()) return false;
                        return sourceMemo.equals(targetMemo);
                    default: //UNKNOWN OVERLAY
                        return false;
                }

        }

        return false;
    }
    private ITask[] getSubProcesses(String mainDocID) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
        //builder.append(" AND WFL_TASK_STATUS IN (2,4,16)");
        builder.append(" AND ").append(Conf.DescriptorLiterals.MainDocumentID).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 50);
        if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        ITask[] newArr = new ITask[informationObjects.length];
        for(int i=0 ; i < informationObjects.length ; i++){
            newArr[i] = (ITask) informationObjects[i];
        }
        return newArr;
    }
    private ITask[] getSubTasks(String mainDocID) throws Exception {
        List<ITask> rtrn = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
        builder.append(" AND WFL_TASK_CODE = 'review'");
        builder.append(" AND ").append(Conf.DescriptorLiterals.MainDocumentID).append(" = '").append(mainDocID).append("'");
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 100);
        if(informationObjects.length < 1) throw new Exception("No Hits found for query for getSubTasks: " + whereClause);
        for(IInformationObject informationObject : informationObjects) {
            ITask subtask = (ITask) informationObject;
            rtrn.add(subtask);
        }
        return rtrn.toArray(new ITask[0]);
    }
    private ITask getMainTask(IDocument doc) throws Exception {
        StringBuilder builder = new StringBuilder();
        if(doc.getClassID() == Conf.ClassIDs.EngineeringCopy){
            builder.append("TYPE = '").append(Conf.ClassIDs.ReviewSubProcess).append("'");
            builder.append(" AND WFL_TASK_NAME = '").append(Conf.Tasks.SubProcessFirstTask).append("'")
                    .append(" AND ")
                    .append(Conf.DescriptorLiterals.SubDocumentID).append(" = '").append(doc.getID()).append("'");
        }else{
            builder.append("TYPE = '").append(Conf.ClassIDs.MainReviewProcess).append("'");
            builder.append(" AND WFL_TASK_NAME = '").append(Conf.Tasks.MainProcessFirstTask).append("'")
                    .append(" AND ")
                    .append(Conf.DescriptorLiterals.MainDocumentID).append(" = '").append(doc.getID()).append("'");
        }
        String whereClause = builder.toString();
        log.info("Where Clause: " + whereClause);
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
        if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        if(informationObjects.length > 1) throw new Exception("Multiple hits found for query: " + whereClause);
        return (ITask) informationObjects[0];
    }
}
