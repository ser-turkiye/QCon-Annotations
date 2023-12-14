//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sample;

import com.ser.blueline.*;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class GenerateStampYedek extends UnifiedAgent {
    private Logger log = LogManager.getLogger();

    public GenerateStampYedek() {
    }

    protected Object execute() {
        try {
            IInformationObject obj = this.getEventTask().getProcessInstance().getMainInformationObject();
            IDocument doc = this.getDocumentServer().getDocument4ID(obj.getID(), this.getSes());
            ISerClassFactory fac = this.getDocumentServer().getClassFactory();
            IOverlay newAnnotation = fac.getOverlayInstance(6);
            IStampOverlay stampAnnot = (IStampOverlay)newAnnotation;

            if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
                log.error("Task is locked.." + getEventTask().getID() + "..restarting agent");
                log.error("Task is locked on.." + getEventTask().getProcessInstance().findLockInfo().getOwnerID());
                return resultRestart("Restarting Agent");
            }

            //String decisionCode = this.getEventTask().getProcessInstance().findTaskByNumericID(this.getEventTask().getPreviousTaskNumericID()).getDecision().getCode();
            //String decisionCode = this.getEventTask().getDescriptorValue("ccmPrjDocStatus");

            String userName = this.getEventTask().getProcessInstance().findTaskByNumericID(this.getEventTask().getPreviousTaskNumericID()).getFinishedBy().getLogin();
            String prcsName = this.getEventTask().getProcessInstance().getDisplayName();

            Long prevTaskID = this.getEventTask().getPreviousTaskNumericID();
            ITask prevTask = this.getEventTask().getProcessInstance().findTaskByNumericID(this.getEventTask().getPreviousTaskNumericID());


            Long prev2TaskID = prevTask.getPreviousTaskNumericID();
            ITask prev2Task = this.getEventTask().getProcessInstance().findTaskByNumericID(prev2TaskID);

            //String decisionCode = prev2Task.getDescriptorValue("TaskDecisioncode");
            String decisionCode = prev2Task.getDecision().getCode();
            String completedBy = prev2Task.getFinishedBy().getName();

            this.log.info("Consalidator Completed By: " + completedBy);

            //byte[] stamp = this.produceImage(220, 120, decisionCode, userName);
            byte[] stamp = this.produceImage(220, 120, decisionCode, completedBy);
            stampAnnot.setImageData(stamp);
            Point pt = new Point(100, 10);
            stampAnnot.setStartPosition(pt);
            IOverlayLayer ovLayer = fac.getOverlayLayerInstance(stampAnnot);
            ovLayer.setOverlayName("Approval Stamp");

            doc.getPartDocument(0, 0).addOverlayLayer(ovLayer);
            doc.commit();

            IDocument mainDoc = (IDocument) this.getEventTask().getProcessInstance().getMainInformationObject();
            mainDoc.setDescriptorValue("ccmPrjDocApprCode",decisionCode);

            mainDoc.commit();

        } catch (Exception var13) {
            this.log.info(var13.getMessage());
            return this.resultError(var13.getMessage());
        }

        return this.resultSuccess("Done");
    }

    public byte[] produceImage(int width, int height, String decisionCode, String userName) throws Exception {
        int x = 0;
        int y = 0;
        BufferedImage image = new BufferedImage(width, height, 1);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", 1, 10));
        g2d.drawString("RLP Status Stamp", x + 100, y + 10);
        g2d.setFont(new Font("Segoe UI", 0, 8));
        g2d.drawRect(x, y + 15, 12, 10);
        g2d.drawString("ACC - Accepted", x + 25, y + 25);
        g2d.drawRect(x, y + 30, 12, 10);
        g2d.drawString("AAN - Accepted as Noted, Revised and Re-Issue", x + 25, y + 40);
        g2d.drawRect(x, y + 45, 12, 10);
        g2d.drawString("AANR - Accepted as Noted - Resubmit", x + 25, y + 55);
        g2d.drawRect(x, y + 60, 12, 10);
        g2d.drawString("NR - Not Reviewed", x + 25, y + 70);
        g2d.drawRect(x, y + 75, 12, 10);
        g2d.drawString("RRR - Rejected, Revise, Resubmit", x + 25, y + 85);
        g2d.drawString(userName, x + 10, y + 100);
        g2d.drawLine(x, y + 105, x + 80, y + 105);
        g2d.setFont(new Font("Segoe UI", 0, 8));
        g2d.drawString("Status By", x + 10, y + 120);
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        g2d.drawString(dateFormat.format(date), x + 140, y + 100);
        g2d.drawLine(x + 110, y + 105, x + 220, y + 105);
        g2d.drawString("Date", x + 160, y + 120);
        if (Objects.equals(decisionCode, "ACC")) {
            g2d.drawLine(x, y + 15, x + 12, y + 25);
            g2d.drawLine(x, y + 25, x + 12, y + 15);
        }

        if (Objects.equals(decisionCode, "AAN")) {
            g2d.drawLine(x, y + 30, x + 12, y + 40);
            g2d.drawLine(x, y + 40, x + 12, y + 30);
        }

        if (Objects.equals(decisionCode, "AANR")) {
            g2d.drawLine(x, y + 45, x + 12, y + 55);
            g2d.drawLine(x, y + 55, x + 12, y + 45);
        }

        if (Objects.equals(decisionCode, "NR")) {
            g2d.drawLine(x, y + 60, x + 12, y + 70);
            g2d.drawLine(x, y + 70, x + 12, y + 60);
        }

        if (Objects.equals(decisionCode, "RRR")) {
            g2d.drawLine(x, y + 75, x + 12, y + 85);
            g2d.drawLine(x, y + 85, x + 12, y + 75);
        }

        g2d.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = (ImageWriter)ImageIO.getImageWritersByFormatName("jpeg").next();
        JPEGImageWriteParam param = (JPEGImageWriteParam)writer.getDefaultWriteParam();
        param.setCompressionMode(2);
        param.setCompressionQuality(1.0F);
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.prepareWriteSequence((IIOMetadata)null);
        writer.writeToSequence(new IIOImage(image, (List)null, (IIOMetadata)null), param);
        writer.endWriteSequence();
        ios.close();
        return baos.toByteArray();
    }

    private void getOverlayAnnotations(IDocument doc) {
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
        int totalOverLayersCount = doc.getPartDocument(0, 0).getOverlayLayerCount();

        for(int i = 0; i < totalOverLayersCount; ++i) {
            IOverlayLayer overlayer = doc.getPartDocument(0, 0).getOverlayLayer(i);
            this.log.info("(I) Layer Name " + overlayer.getOverlayName());
            this.log.info("(I) Layer Page " + overlayer.getPageInPart());
            int totalOverLayerElements = overlayer.getOverlayCount();

            for(int j = 0; j < totalOverLayerElements; ++j) {
                IOverlay overlay = overlayer.getOverlay(j);
                this.log.info("(I) \tAnno Creator " + overlay.getCreatingUser());
                this.log.info("(I) \tAnno Date " + overlay.getCreationDateAsString());
                this.log.info("(I) \tAnno Type " + (String)types.get(overlay.getObjectType()));
            }
        }

    }

}
