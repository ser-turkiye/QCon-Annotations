package annotprop;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class OnNewAnnotation extends UnifiedAgent {

    private Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    private IDocument mainDocument;
    private ISerClassFactory factory;
    private IDocument reviewDoc;
    private ITask mainTask;
    private boolean taskRestarted = false;

    String strFullTimeID = "";
    @Override
    protected Object execute() {
        //(1) Check if the main document is locked
        // if main task is locked then just restart

        factory = getDocumentServer().getClassFactory();

        mainTask = getEventTask();
        reviewDoc = (IDocument) mainTask.getProcessInstance().getMainInformationObject();

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        strFullTimeID = dateFormat.format(date);

        if(reviewDoc == null) return resultError("Review Doc is NULL");
        try{
            log.info("---- Agent Started -----" + reviewDoc.getID());

            this.helper = new ProcessHelper(getSes());
            String mainDocID = reviewDoc.getDescriptorValue(Conf.Descriptors.MainDocumentID);
            mainDocument = getDocumentServer().getDocument4ID(mainDocID , getSes());

            if(!this.copyOnlyDiffentLayers(reviewDoc, mainDocument)){
                return resultRestart("Restarting Agent for copyOnlyDifferentLays");
            }
            if(!this.copyLayerToOtherSubDocuments(mainDocID)){
                return resultRestart("Restarting Agent for copyLayerToOtherSubDocuments");
            }
            log.info("New annotation finish for maindoc : " + mainDocID);
            log.info("New annotation finish for subdoc : " + reviewDoc.getID());

            reviewDoc.setDescriptorValue("ccmApproved","1");
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
              //  continue;
            }
            try{
                log.info("Locking Sub Task to Update its document for:" + reviewDoc.getID());
                //pi.lock(task);
                log.info("Getting SubProcess Main Loaded Document for:" + reviewDoc.getID());
                IInformationObject subProcessObject = pi.getMainInformationObject();
                if(subProcessObject == null) continue;
                if(!(subProcessObject instanceof  IDocument)) continue;
                mainDocument = getDocumentServer().getDocument4ID(mainDocID , getSes());
                IDocument subDocument = (IDocument) subProcessObject;
                if (!Objects.equals(subDocument.getID(), reviewDoc.getID())) {
                    log.info("Start Copying mainDocument to subDocument:" + reviewDoc.getID());
                    if(!copyOnlyDiffentLayers(mainDocument, subDocument)){
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


    public boolean copyOnlyDiffentLayers(IDocument sourceDoc, IDocument targetDoc) throws Exception {
        boolean isExist = false;
        boolean isUpdated = false;

        try {

            List<IOverlayLayer> sourceOverLayers =getAllOverlayers(sourceDoc);
            List<IOverlayLayer> targetOverLayers =getAllOverlayers(targetDoc);

            for(IOverlayLayer sourceLayer: sourceOverLayers){
                isExist = false;

                for(IOverlayLayer targetLayer : targetOverLayers){

                    if( isSameOverLayLayer(sourceLayer,targetLayer,true)){
                      isExist = true;
                      break;
                    }

                }

                if(!isExist){
                    targetDoc.getPartDocument(0,0).addOverlayLayer(sourceLayer);
                    isUpdated = true;
                }

            }

            if(isUpdated) targetDoc.commit();

            return true;

        }catch(Exception e){
            log.error("Exception Caught : function is copyOnlyDiffentLays");
            log.error(e.getMessage());
            return false;
        }
    }

    public List<IOverlayLayer> getAllOverlayers(IDocument sourceDoc){
        List<IOverlayLayer> sourceOverLayers = new ArrayList<>();


        for(int cnt=0 ; cnt<sourceDoc.getPartDocument(0,0).getOverlayLayerCount();cnt++){
            sourceOverLayers.add(sourceDoc.getPartDocument(0,0).getOverlayLayer(cnt));
        }

        return sourceOverLayers;
    }

    public static boolean isSameOverLayLayer(IOverlayLayer sourceOverlayLayer,IOverlayLayer targetOverlayLayer , boolean checkPageNo){

        int cnt=0;
        int sourceOverLayerCount = sourceOverlayLayer.getOverlayCount();
        int targetOverLayerCount = targetOverlayLayer.getOverlayCount();

        if(checkPageNo && sourceOverlayLayer.getPageInPart() != targetOverlayLayer.getPageInPart()) return false;
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

}
