package ser.bn.se.demosystems.generation;

import annotprop.Conf;
import annotprop.ProcessHelper;
import annotprop.Utils;
import annotprop.BookMark;
import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import ser.bn.se.demosystems.documents.Constants;
import ser.bn.se.demosystems.documents.CounterHelper;

import java.awt.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotationTable extends UnifiedAgent {
    private static Logger log = LogManager.getLogger();
    private ProcessHelper helper;
    ISession ses;
    IDocumentServer server;
    private  IDocument mainDocument;
    private ITask mainTask;
    @Override
    protected Object execute() {
        try {
            com.spire.license.LicenseProvider.setLicenseKey(Conf.Licences.SPIRE_XLS);
            ses = getSes();
            server = ses.getDocumentServer();
            this.helper = new ProcessHelper(getSes());
            //IInformationObject ttt = getEventInfObj();
            if(getEventTask() == null) return resultError("OBJECT CLIENT ID is NULL or not of type ITask");
            log.info("---- agent Started ----");
            IInformationObject informationObject = getEventTask().getProcessInstance().getMainInformationObject();
            if(!(informationObject instanceof IDocument)) return resultError("Main Information Object is not IDocument");

            if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
                log.error("Task is locked.." + getEventTask().getID() + "..restarting agent");
                log.error("Task is locked on.." + getEventTask().getProcessInstance().findLockInfo().getOwnerID());
                return resultRestart("Restarting Agent");
            }

           /* mainDocument = (IDocument) informationObject;
            String mainDocID = mainDocument.getID();
            ITask[] subProcesses = getSubProcesses(mainDocID);
            for(ITask task : subProcesses){
                //(1) If task is locked skip else lock it
                IProcessInstance pi = task.getProcessInstance();
                if(pi.findLockInfo().getOwnerID() != null) {
                    log.info("Process Instance is locked, skipping subProcess Document");
                    continue;
                }
                try{
                    log.info("Getting SubProcess Main Loaded Document");
                    IInformationObject subProcessObject = pi.getMainInformationObject();
                    if(subProcessObject == null) continue;
                    if(!(subProcessObject instanceof  IDocument)) continue;
                    IDocument subDocument = (IDocument) subProcessObject;
                    log.info("annotation update start on CRS generation maindocID: " + mainDocID);
                    log.info("annotation update start on CRS generation subdocID: " + subDocument.getID());
                    this.copyOnlyDiffentLays(subDocument, mainDocument);
                    this.copyLayerToOtherSubDocuments(mainDocID,subDocument);
                    log.info("annotation update finish on CRS generation maindocID: " + mainDocID);
                    log.info("annotation update finish on CRS generation subdocID: " + subDocument.getID());
                }catch(Exception e){
                    log.error("Exception Caught");
                    log.error(e.getMessage());
                }finally {
                    log.info("Unlocking Task");
                    pi.unlock();
                }
            }*/

            HashMap<String, String> prjBkmrks = initPrjBookmarks((IDocument) informationObject);
            //HashMap<String, String> bookmarks = new HashMap<>();
            JSONObject bookmarks = new JSONObject();
            for (Map.Entry<String, String> prjBkmrk : prjBkmrks.entrySet()) {
                String dscpName = prjBkmrk.getValue();
                System.out.println("*** PRJ-KEY :" + prjBkmrk.getKey() + "*** PRJ-DSCP :" + dscpName);
                bookmarks.put(prjBkmrk.getKey(), dscpName);
            }

            HashMap<String, String> docBkmrks = initLineBookmarks((IDocument) informationObject);
            for (Map.Entry<String, String> docBkmrk : docBkmrks.entrySet()) {
                String dscpName = docBkmrk.getValue();
                System.out.println("*** LINE-KEY :" + docBkmrk.getKey() + "*** LINE-DSCP :" + dscpName);
                bookmarks.put(docBkmrk.getKey(), dscpName);
            }

            String uniqueId = UUID.randomUUID().toString();
            String exportPath = Conf.ExcelTransmittalPaths.MainPath + "/Generate_CRS[" + uniqueId + "]";
            (new File(exportPath)).mkdir();

            String prjCode = this.getEventTask().getDescriptorValue("ccmPRJCard_code");
            String ctpn = "GENERATE_CRS_FROM_EXCEL";
            IDocument ctpl = Utils.getTemplateDocument(prjCode, ctpn, helper);
            if(ctpl != null){
                String templatePath = Utils.exportDocument(ctpl, exportPath, ctpn);
                String xlsxPath = loadStampExcel(templatePath, exportPath + "/Generate_CRS.xlsx", bookmarks);
                //String mailHtmlPath = Utils.convertExcelToHtml(tpltSavePath, mainPath + "/result[" + uniqueId + "].html");
                String mailPdfPath = Utils.convertExcelToPdf(xlsxPath, exportPath + "/Generate_CRS.pdf");
                this.archiveNewTemplate(xlsxPath, mailPdfPath);
            }

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished");
    }
    private static String loadStampExcel(String templateXlsxPath, String xslxPath, JSONObject bookmarks)throws Exception{
        FileInputStream tist = new FileInputStream(templateXlsxPath);
        XSSFWorkbook twrb = new XSSFWorkbook(tist);
        Sheet tsht = twrb.getSheetAt(0);
        for (Row trow : tsht){
            for(Cell tcll : trow){
                if(tcll.getCellType() != CellType.STRING){continue;}
                String clvl = tcll.getRichStringCellValue().getString();
                String clvv = Utils.updateCell(clvl, bookmarks);
                if(!clvv.equals(clvl)){
                    tcll.setCellValue(clvv);
                }
            }
        }
        FileOutputStream tost = new FileOutputStream(xslxPath);
        twrb.write(tost);
        tost.close();
        return xslxPath;
    }
    public static String cellUpdate(String str, HashMap<String, String> bookmarks){
        StringBuffer rtrn = new StringBuffer();

        Pattern ptrn = Pattern.compile( "\\{([\\w\\.]+)\\}" );
        Matcher mtcr = ptrn.matcher(str);

        while(mtcr.find()) {
            String mk = mtcr.group(1);
            String mv = "";
            if(bookmarks.containsKey(mk)){
                mv = bookmarks.get(mk);
            }
            mtcr.appendReplacement(rtrn,  mv);
        }
        mtcr.appendTail(rtrn);
        return rtrn.toString();
    }
    private void archiveNewTemplate(String tpltSavePath, String pdfPath) throws Exception {
        FilingHelper filingHelper = new FilingHelper(getSes());
        IDocument doc = newFileToDocumentClass(tpltSavePath, pdfPath, Constants.ClassIDs.CRSProjDocumentArchive);
        filingHelper.mapDescriptorsFromObjectToObject(getEventTask() , doc , true);
        doc.setDescriptorValue("ccmFileName" , "Comment Resolution Sheet.xlsx");
        doc.setDescriptorValue("ccmPrjDocDocType" , "CRS");
        //doc.setDescriptorValue("ObjectName2" , "Comment Resolution Sheet");
        doc.setDescriptorValue("ccmReferenceNumber" , getEventTask().getProcessInstance().getMainInformationObjectID());
        CounterHelper counterHelper = new CounterHelper(getSes() , doc.getClassID());
        String counterStr = counterHelper.getCounterStr();
        String paddedNo = String.format("%5s", counterStr).replace(' ', '0');
        doc.setDescriptorValue("ObjectNumber" , "CRS_" + paddedNo);
        //doc.setDescriptorValue("ccmPrjDocNumber" , "CRS_" + paddedNo);
        doc.commit();
        getEventTask().getProcessInstance().getLoadedInformationObjectLinks().addInformationObject(doc.getID());
        getEventTask().commit();
    }
    public IDocument newFileToDocumentClass(String filePath, String filePathPDF, String archiveClassID) throws Exception {
        IArchiveClass cls = server.getArchiveClass(archiveClassID, ses);
        if (cls == null) cls = server.getArchiveClassByName(ses, archiveClassID);
        if (cls == null) throw new Exception("Document Class: " + archiveClassID + " not found");

        String dbName = ses.getDatabase(cls.getDefaultDatabaseID()).getDatabaseName();

        IDocument doc = server.getClassFactory().getDocumentInstance(dbName, cls.getID(), "0000", ses);

        File file = new File(filePath);
        IRepresentation representation = doc.addRepresentation(".xlsx" , "Signed document");
        IDocumentPart newDocumentPart = representation.addPartDocument(filePath);
        doc.commit();
        file.delete();

        /*
        file = new File(filePathPDF);
        representation = doc.addRepresentation(".pdf" , "Signed document (PDF)");
        newDocumentPart = representation.addPartDocument(filePathPDF);
        doc.setDefaultRepresentation(doc.getRepresentationCount()-1);
        doc.commit();
        file.delete();
        */
        return doc;
    }
    private String getTextOrType(IOverlay overlay){
        String text;
        if(overlay instanceof ICommentOverlay){
            log.info("(I)\t\tText " + Arrays.asList(((ICommentOverlay)overlay).getText()));
            text = "" + Arrays.asList(((ICommentOverlay)overlay).getText());
            return text.replaceAll("\\[", "").replaceAll("\\]", "");
        }else if (overlay instanceof INoteOverlay) {
            log.info("(I)\t\tText " + Arrays.asList(((INoteOverlay)overlay).getText()));
            text = "" + Arrays.asList(((INoteOverlay)overlay).getText());
            return (text.replaceAll("\\[", "").replaceAll("\\]", ""));
        } else if (overlay instanceof IStampOverlay) {
            log.info("(I)\t\tText " + Arrays.asList(((IStampOverlay)overlay).getText()));
            log.info("(I)\t\tFont " + ((IStampOverlay)overlay).getFontName() + "(" + ((IStampOverlay)overlay).getFontSize() + ") " + ((IStampOverlay)overlay).getColor());
            text = "" + Arrays.asList(((IStampOverlay)overlay).getText());
            return (text.replaceAll("\\[", "").replaceAll("\\]", ""));
        } else if (overlay instanceof ILinkOverlay) {
            log.info("(I)\t\tURL " + ((ILinkOverlay)overlay).getURL());
            log.info("(I)\t\tText " + ((ILinkOverlay)overlay).getAdditionalText());
           return(((ILinkOverlay)overlay).getURL() + " : " + ((ILinkOverlay)overlay).getAdditionalText());
        } else if (overlay instanceof IArrowOverlay) {
            log.info("(I)\t\tStart " + ((IArrowOverlay)overlay).getStartPosition());
            log.info("(I)\t\tEnd " + ((IArrowOverlay)overlay).getEndPosition());
            return "No Text";
        } else if (overlay instanceof IMarkerOverlay) {
            log.info("(I)\t\tStart " + ((IMarkerOverlay)overlay).getStartPosition());
            log.info("(I)\t\tEnd " + ((IMarkerOverlay)overlay).getEndPosition());
            text = "" + Arrays.asList(((IMarkerOverlay)overlay).getMemo());
            return (text.replaceAll("\\[", "").replaceAll("\\]", ""));
        } else {
            return "No Text";
        }
    }

    public HashMap<String, String> initLineBookmarks(IDocument doc) {
        HashMap<String, String> prjbookmarks = new HashMap<>();
        log.info("Adding Annotation Row by Row");

        Map<Integer, String> types = new HashMap();
        types.put(4, "Arrow");
        types.put(2, "Comment");
        types.put(5, "Freehand");
        types.put(8, "Link");
        types.put(1, "Marker");
        types.put(3, "Note");
        types.put(9, "Shape");
        types.put(6, "Stamp");
        types.put(-1, "Unknown");

        int cnt = 0;
        int cnt1 = 0;
        int totalOverLayersCount = doc.getPartDocument(0, 0).getOverlayLayerCount();
        for(int i=0 ; i < totalOverLayersCount ; i++){
            IOverlayLayer overlayer = doc.getPartDocument(0, 0).getOverlayLayer(i);
            log.info("(I) Layer Name " + overlayer.getOverlayName());
            log.info("(I) Layer Page " + overlayer.getPageInPart());
            int totalOverLayerElements = overlayer.getOverlayCount();
            for(int j = 0 ; j < totalOverLayerElements ; j++){
                IOverlay overlay = overlayer.getOverlay(j);
                log.info("(I) \tAnno Creator " + overlay.getCreatingUser());
                log.info("(I) \tAnno Date " + overlay.getCreationDateAsString());
                log.info("(I) \tAnno Type " + (String)types.get(overlay.getObjectType()));
                cnt++;
                cnt1 = cnt -1;
                String[] row = new String[5];
                prjbookmarks.put("SNo" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , cnt + "" );

                //Section/Page
                row[0] = overlayer.getPageInPart() + "";
                prjbookmarks.put("page" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , row[0]);
                //comments
                row[1] = getTextOrType(overlay);
                prjbookmarks.put("desc" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , row[1]);
                //Company
                row[2] = "QCon";
                prjbookmarks.put("comp" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , row[2]);
                //Discipline
                row[3] = overlay.getCreatingUser();
                prjbookmarks.put("discipline" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , row[3]);
                //Contractor Response
                //row[4] = overlay.getCreatingUserID() + "-" + overlay.getCreationDateAsString() + "-" + overlay.getObjectType();
                row[4] = "";
                prjbookmarks.put("response" + (cnt1 > 9 ? cnt1 : "0" + cnt1) , row[4]);

            }
        }

        log.info("Annotations Added");
        return prjbookmarks;
    }
    public HashMap<String, String> initPrjBookmarks(IDocument doc) {
        HashMap<String, String> prjBkmrks = new HashMap<>();
        IInformationObject iobj = getEventTask().getProcessInstance().getMainInformationObject();
        log.info("Adding Project Information");

        String valPrjName = doc.getDescriptorValue("ccmPRJCard_name");
        if(valPrjName == null){
            valPrjName = iobj.getDescriptorValue("ccmPRJCard_name");
        }
        prjBkmrks.put("projectName", (valPrjName == null ? "" : valPrjName));

        String valCntNo = doc.getDescriptorValue("ccmContractNumber");
        if(valCntNo == null){
            valCntNo = iobj.getDescriptorValue("ccmContractNumber");
        }
        prjBkmrks.put("contractNo", (valCntNo == null ? "" : valCntNo));

        String valDocNo = doc.getDescriptorValue("ccmPrjDocNumber");
        if(valDocNo == null){
            valDocNo = iobj.getDescriptorValue("ccmPrjDocNumber");
        }
        prjBkmrks.put("docNo", (valDocNo == null ? "" : valDocNo));


        String valTN = doc.getDescriptorValue("ccmPrjDocTransOutCode");
        if(valTN == null){
            valTN = iobj.getDescriptorValue("ccmPrjDocTransOutCode");
        }
        prjBkmrks.put("TN", (valTN == null ? "" : valTN));

        String valTitle = doc.getDescriptorValue("ObjectName");
        if(valTitle == null){
            valTitle = iobj.getDescriptorValue("ObjectName");
        }
        prjBkmrks.put("title", (valTitle == null ? "" : valTitle));

        String valDate = doc.getDescriptorValue("ccmPrjDocDate");
        if(valDate == null){
            valDate = iobj.getDescriptorValue("ccmPrjDocDate");
        }
        prjBkmrks.put("date", (valDate == null ? "" : valDate));

        return prjBkmrks;
    }
    private void copyLayerToOtherSubDocuments(String mainDocID,IDocument eventDoc) throws Exception{
        log.info("Copying Layer from Main document to All other Documents");
        //(1) Check to see if we have an overlayer with the same name
        ITask[] subProcesses = getSubProcesses(mainDocID);
        for(ITask task : subProcesses){
            //(1) If task is locked skip else lock it
            IProcessInstance pi = task.getProcessInstance();
            if(pi.findLockInfo().getOwnerID() != null) {
                log.info("Process Instance is locked, skipping subProcess Document");
                continue;
            }
            try{
                log.info("Locking Sub Task to Update its document");
                pi.lock(task);
                log.info("Getting SubProcess Main Loaded Document");
                IInformationObject subProcessObject = pi.getMainInformationObject();
                if(subProcessObject == null) continue;
                if(!(subProcessObject instanceof  IDocument)) continue;
                mainDocument = getDocumentServer().getDocument4ID(mainDocID , getSes());
                IDocument subDocument = (IDocument) subProcessObject;
                if (!Objects.equals(subDocument.getID(), eventDoc.getID())) {
                    copyOnlyDiffentLays(mainDocument, subDocument);
                }
            }catch(Exception e){
                log.error("Exception Caught");
                log.error(e.getMessage());
            }finally {
                log.info("Unlocking Task");
                pi.unlock();
            }
        }
    }
    public void copyLayers(IDocument sourceDoc, IDocument targetDoc){
        String overLayerName = "";
        boolean isSame=false;

        IDocumentPart sourceDocPart = sourceDoc.getPartDocument( sourceDoc.getDefaultRepresentation() , 0);
        IDocumentPart targetDocPart = targetDoc.getPartDocument( targetDoc.getDefaultRepresentation() , 0);

        int sourceDocPartTotalOverlayers= sourceDocPart.getOverlayLayerCount();
        int targetDocPartTotalOverlayers= targetDocPart.getOverlayLayerCount();

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
                if(getEventDocument()!=null){getEventDocument().commit();}
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

        if(!listOfLayersToCopy.isEmpty()){
            for (IOverlayLayer layer :listOfLayersToCopy  ) {
                targetDocPart.addOverlayLayer(layer);
            }
            targetDoc.commit();
        }

    }
    public void copyOnlyDiffentLays(IDocument sourceDoc, IDocument targetDoc){
        boolean isSame = false;
        List<IOverlay> sourceOverLays = getAllOverlays(sourceDoc);
        List<IOverlay> targetOverLays = getAllOverlays(targetDoc);

        ///if deleted annotation from source, delete all annotation from target
        if(targetOverLays.size() > sourceOverLays.size()){
            deleteAllOverLays(targetDoc);
        }

        sourceOverLays = getAllOverlays(sourceDoc);
        targetOverLays = getAllOverlays(targetDoc);

        if((!sourceOverLays.isEmpty()) && (targetOverLays.isEmpty())){
            copyLayers(sourceDoc,targetDoc) ;
        }else {
            IOverlayLayer targetOverLayLayer = targetDoc.getPartDocument(0,0).getOverlayLayer(0);
            for (int i = 0; i < sourceOverLays.size(); i++) {
                isSame = false;
                for (int k = 0; k < targetOverLays.size(); k++) {
                    if (isSameOverLay(sourceOverLays.get(i), targetOverLays.get(k))) isSame = true;
                }

                if (!isSame) {
                    targetOverLayLayer.addOverlay(sourceOverLays.get(i));
                }
            }
        }
        targetDoc.commit();
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
        IInformationObject[] informationObjects = helper.createQuery(new String[]{"BPM"} , whereClause , 2);
        if(informationObjects.length < 1) throw new Exception("No Hits found for query: " + whereClause);
        ITask[] newArr = new ITask[informationObjects.length];
        for(int i=0 ; i < informationObjects.length ; i++){
            newArr[i] = (ITask) informationObjects[i];
        }
        return newArr;
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
