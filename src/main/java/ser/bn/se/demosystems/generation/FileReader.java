package ser.bn.se.demosystems.generation;

import com.ser.blueline.IDocument;
import com.ser.blueline.IDocumentPart;
import de.phip1611.Docx4JSRUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.*;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.*;
import java.util.Map;

public class FileReader {

    private InputStream fileInputStream;
    private XWPFDocument wordDocument;
    private XWPFTable table;
    private XWPFTableRow lastRowHolder;
    private boolean isFirstRowEntry;
    private boolean firstRowExisits;
    private static Logger log = LogManager.getLogger();

    public FileReader(boolean firstRowExisits) {
        this.firstRowExisits = firstRowExisits;
    }

    public XWPFDocument readFileFromPath(String filePath) throws Exception {
        log.info("Reading document from path: " + filePath);
        if ((filePath == null) || (filePath.isEmpty())) throw new Exception("Supplied file path is NULL");
        File file = new File(filePath);
        if (!file.exists()) throw new Exception("File in Path: " + filePath + " doesn't exisit");
        this.fileInputStream = new FileInputStream(file);
        log.info("Coverting document to XWPF Document");
        return getXWPFDocument();
    }

    public XWPFDocument readFileFromEventDocument(IDocument eventDocument) throws Exception {
        if (eventDocument == null) throw new Exception("EventDocument is NULL");
        IDocumentPart documentPart = eventDocument.getPartDocument(eventDocument.getDefaultRepresentation(), 0);
        if (documentPart == null) throw new Exception("IDocumentPart couldn't be extracted");
        this.fileInputStream = documentPart.getRawDataAsStream();
        return getXWPFDocument();
    }

    public void replaceTextInTableCell(Map<String, String> placeholderMap , String originalPath, String finalPath) throws Exception {
        this.closeDocument(originalPath);
        WordprocessingMLPackage sourceDocxDoc = WordprocessingMLPackage.load(new File(originalPath));
        Docx4JSRUtil.searchAndReplace(sourceDocxDoc, placeholderMap);
        sourceDocxDoc.save(new File(finalPath));
    }

    public void getLastTable() throws Exception {
        if (wordDocument.getTables().size() < 1) throw new Exception("No tables found in word documents");
        this.table = wordDocument.getTables().get(wordDocument.getTables().size() - 1);
        this.lastRowHolder = table.getRows().get(table.getRows().size() - 1);
        isFirstRowEntry = true;
    }

    public void getTableAtIndex(int index) throws Exception{
        if (wordDocument.getTables().size() < 1) throw new Exception("No tables found in word documents");
        if (wordDocument.getTables().size() < index) throw new Exception("Table at Index " + index + "not found");
        this.table = wordDocument.getTables().get(index);
        this.lastRowHolder = table.getRows().get(table.getRows().size() - 1);
        isFirstRowEntry = true;
    }

    private XWPFDocument getXWPFDocument() throws IOException {
        this.wordDocument = new XWPFDocument(fileInputStream);
        return this.wordDocument;
    }

    public void closeDocument(String path) throws Exception {
        try (FileOutputStream out = new FileOutputStream(path)) {
            wordDocument.write(out);
        } catch (IOException e) {
            throw e;
        }
    }

    int c = 0;
    public void addRowToTable(String[] testText , boolean skipFirst) {
        if (isFirstRowEntry) {
            if (!firstRowExisits) this.createNewRow();
            isFirstRowEntry = false;
        } else {
            this.createNewRow();
        }
        this.appendDataToLastRow(testText , skipFirst);
    }

    private int counter = 1;

    private void appendDataToLastRow(String[] testText , boolean skipFirst) {
        this.lastRowHolder = table.getRows().get(table.getRows().size() - 1);

        for (int i = 0; i < lastRowHolder.getTableCells().size(); i++) {
            XWPFParagraph paragraph = lastRowHolder.getCell(i).getParagraphs().get(0);
            XWPFRun run = paragraph.createRun();

            run.setFontSize(9);
            if (skipFirst && i == 0) {
                run.setText(counter + ".");
            } else {
                int index = skipFirst ? (i-1) : i;
                if(index >= testText.length ) return;
                String toAdd = testText[index];
                if(toAdd == null) toAdd = "";
                run.setText(toAdd);
            }
            if (i <= 1) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
            }
        }
        counter++;
    }

    private void createNewRow() {
        XWPFTableRow newRow = table.createRow();
        boolean isFirst = true;
        if(newRow.getTableCells().size() == lastRowHolder.getTableCells().size()) return;
        for (XWPFTableCell lastCell : lastRowHolder.getTableCells()) {
            if(newRow.getTableCells().size() == lastRowHolder.getTableCells().size()) return;
            XWPFTableCell newCell;
            if (isFirst) {
                newCell = newRow.getCell(0);
                isFirst = false;
            } else {
                newCell = newRow.createCell();
            }
            newCell.getCTTc().setTcPr(lastCell.getCTTc().getTcPr());
        }
    }
}
