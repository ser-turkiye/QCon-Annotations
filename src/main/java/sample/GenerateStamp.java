//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sample;

import annotprop.Conf;
import annotprop.Utils;
import annotprop.ProcessHelper;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.IOverlay;
import com.ser.blueline.IOverlayLayer;
import com.ser.blueline.ISerClassFactory;
import com.ser.blueline.IStampOverlay;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spire.xls.Workbook;
import com.spire.xls.Worksheet;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

import static java.lang.System.out;

public class GenerateStamp extends UnifiedAgent {
    private Logger log = LogManager.getLogger();

    public GenerateStamp() {
    }

    protected Object execute() {
        try {
            IInformationObject obj = this.getEventTask().getProcessInstance().getMainInformationObject();
            IDocument doc = this.getDocumentServer().getDocument4ID(obj.getID(), this.getSes());
            ISerClassFactory fac = this.getDocumentServer().getClassFactory();
            ProcessHelper helper = new ProcessHelper(getSes());
            IOverlay newAnnotation = fac.getOverlayInstance(6);
            IStampOverlay stampAnnot = (IStampOverlay)newAnnotation;

            if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
                log.error("Task is locked.." + getEventTask().getID() + "..restarting agent");
                log.error("Task is locked on.." + getEventTask().getProcessInstance().findLockInfo().getOwnerID());
                return resultRestart("Restarting Agent");
            }

            IDocument mainDoc = (IDocument) this.getEventTask().getProcessInstance().getMainInformationObject();

            Long prevTaskID = this.getEventTask().getPreviousTaskNumericID();
            ITask prevTask = this.getEventTask().getProcessInstance().findTaskByNumericID(this.getEventTask().getPreviousTaskNumericID());

            Long prev2TaskID = prevTask.getPreviousTaskNumericID();
            ITask prev2Task = this.getEventTask().getProcessInstance().findTaskByNumericID(prev2TaskID);

            //String decisionCode = prev2Task.getDecision().getCode();
            String decisionCode = mainDoc.getDescriptorValue("ccmPrjDocApprCode");
            String completedBy = prev2Task.getFinishedBy().getName();

            String prjCode = this.getEventTask().getDescriptorValue("ccmPRJCard_code");
            String isEnableStamp = this.getEventTask().getDescriptorValue("ccmPrjDocStamp");

            this.log.info("Consalidator Completed By: " + completedBy);

            if(!Objects.equals(isEnableStamp, "false") && isEnableStamp!=null) {
                String ctpn = "REVIEW_STAMP_TEMPLATE";
                IDocument ctpl = Utils.getTemplateDocument(prjCode, ctpn, helper);
                if (ctpl != null) {
                    byte[] stamp = this.generateImage(220, 120, decisionCode, completedBy, prjCode);
                    stampAnnot.setImageData(stamp);
                    Point pt = new Point(100, 10);
                    stampAnnot.setStartPosition(pt);
                    IOverlayLayer ovLayer = fac.getOverlayLayerInstance(stampAnnot);
                    ovLayer.setOverlayName("Approval Stamp");
                    doc.getPartDocument(0, 0).addOverlayLayer(ovLayer);
                    doc.commit();
                }
            }

            /*ApproveCode set etme islemi UpdateWFTask agent a alındı
            IDocument mainDoc = (IDocument) this.getEventTask().getProcessInstance().getMainInformationObject();
            mainDoc.setDescriptorValue("ccmPrjDocApprCode",decisionCode);
            mainDoc.commit();
            */

        } catch (Exception var13) {
            this.log.info(var13.getMessage());
            return this.resultError(var13.getMessage());
        }

        return this.resultSuccess("Successfully Finished");
    }

    public byte[] generateImage(int width, int height, String decisionCode, String userName, String prjCode) throws Exception {
        JSONObject scfg = Utils.getSystemConfig(getSes(),null);
        if(scfg.has("LICS.SPIRE_XLS")){
            com.spire.license.LicenseProvider.setLicenseKey(scfg.getString("LICS.SPIRE_XLS"));
        }

        ProcessHelper helper = new ProcessHelper(getSes());
        (new File(Conf.ExcelTransmittalPaths.MainPath)).mkdir();

        String uniqueId = UUID.randomUUID().toString();
        String exportPath = Conf.ExcelTransmittalPaths.MainPath + "/Review_Stamp[" + uniqueId + "]";
        (new File(exportPath)).mkdir();

        String ctpn = "REVIEW_STAMP_TEMPLATE";
        IDocument ctpl = Utils.getTemplateDocument(prjCode, ctpn, helper);
        if(ctpl == null){
            throw new Exception("Template-Document [ " + ctpn + " ] not found.");
        }

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();

        String templatePath = Utils.exportDocument(ctpl, exportPath, ctpn);
        JSONObject bookmarks = new JSONObject();
        bookmarks.put("code", decisionCode);
        bookmarks.put("fullname", userName);
        bookmarks.put("date", dateFormat.format(date));

        String xlsxPath = loadStampExcel(templatePath, exportPath + "/Stamp_Review.xlsx", bookmarks);
        String pngPath = stampImage(xlsxPath, exportPath + "/Stamp_Review.png");
        return transparency(pngPath, exportPath + "/Stamp_Review_Result.png", width, height);
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
    private static String stampImage(String xlsxPath, String pngPath)throws Exception{

        com.spire.xls.Workbook workbook = new Workbook();
        workbook.loadFromFile(xlsxPath);
        Worksheet sheet = workbook.getWorksheets().get(0);

        sheet.saveToImage(pngPath);
        return pngPath;
    }
    private static byte[] transparency(String in, String out, int width, int height)throws Exception{
        BufferedImage bi = ImageIO.read(new File(in));
        int[] pixels = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth());

        for(int i=0;i<pixels.length;i++){
            int color = pixels[i];
            int a = (color>>24)&255;
            int r = (color>>16)&255;
            int g = (color>>8)&255;
            int b = (color)&255;

            if(r == 255 && g == 255 && b == 255){
                a = 0;
            }

            pixels[i] = (a<<24) | (r<<16) | (g<<8) | (b);
        }

        BufferedImage biOut = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        biOut.setRGB(0, 0, bi.getWidth(), bi.getHeight(), pixels, 0, bi.getWidth());
        ImageIO.write(biOut, "png", new File(out));
        //return out;

        BufferedImage bufferedImage = ImageIO.read(new File(out));
        BufferedImage croppedImage = autoCrop(bufferedImage,0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(croppedImage,"png",baos);
        return baos.toByteArray();
    }
    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
    public static BufferedImage autoCrop(BufferedImage source, double tolerance) {
        int COLOR_WHITE = Color.WHITE.getRGB();
        int margin = 10;

        int baseColor = source.getRGB(0, 0);

        int width = source.getWidth();
        int height = source.getHeight();

        int minX = 0;
        int minY = 0;
        int maxX = width;
        int maxY = height;

        // Immediately break the loops when encountering a non-white pixel.
        lable1: for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    minY = y;
                    break lable1;
                }
            }
        }

        lable2: for (int x = 0; x < width; x++) {
            for (int y = minY; y < height; y++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    minX = x;
                    break lable2;
                }
            }
        }

        // Get lower-left pixel color as the "baseline" for cropping
        baseColor = source.getRGB(minX, height - 1);

        lable3: for (int y = height - 1; y >= minY; y--) {
            for (int x = minX; x < width; x++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    maxY = y;
                    break lable3;
                }
            }
        }

        lable4: for (int x = width - 1; x >= minX; x--) {
            for (int y = minY; y < maxY; y++) {
                if (colorWithinTolerance(baseColor, source.getRGB(x, y), tolerance)) {
                    maxX = x;
                    break lable4;
                }
            }
        }

        if ((minX - margin) >= 0) {
            minX -= margin;
        }

        if ((minY - margin) >= 0) {
            minY -= margin;
        }

        if ((maxX + margin) < width) {
            maxX += margin;
        }

        if ((maxY + margin) < height) {
            maxY += margin;
        }

        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;

        // if same size, return the original
        if (newWidth == width && newHeight == height) {
            return source;
        }

        BufferedImage target = new BufferedImage(newWidth, newHeight, source.getType());

        Graphics g = target.getGraphics();
        g.drawImage(source, 0, 0, target.getWidth(), target.getHeight(), minX, minY, maxX + 1, maxY + 1, null);

        g.dispose();

        return target;
    }
    private static boolean colorWithinTolerance(int a, int b, double tolerance) {
        int aAlpha = (int) ((a & 0xFF000000) >>> 24); // Alpha level
        int aRed = (int) ((a & 0x00FF0000) >>> 16); // Red level
        int aGreen = (int) ((a & 0x0000FF00) >>> 8); // Green level
        int aBlue = (int) (a & 0x000000FF); // Blue level

        int bAlpha = (int) ((b & 0xFF000000) >>> 24); // Alpha level
        int bRed = (int) ((b & 0x00FF0000) >>> 16); // Red level
        int bGreen = (int) ((b & 0x0000FF00) >>> 8); // Green level
        int bBlue = (int) (b & 0x000000FF); // Blue level

        double distance = Math.sqrt((aAlpha - bAlpha) * (aAlpha - bAlpha) + (aRed - bRed) * (aRed - bRed)
                + (aGreen - bGreen) * (aGreen - bGreen) + (aBlue - bBlue) * (aBlue - bBlue));

        // 510.0 is the maximum distance between two colors
        // (0,0,0,0 -> 255,255,255,255)
        double percentAway = distance / 510.0d;

        return (percentAway > tolerance);
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
