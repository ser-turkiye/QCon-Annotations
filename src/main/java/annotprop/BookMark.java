package annotprop;

import java.util.HashMap;
import java.util.Map;

public class BookMark {

    public HashMap<String, String> docBkmrks;
    public HashMap<String, String> bookmarks;

    public HashMap<String, String> getDocBkmrks() {
        return docBkmrks;
    }

    public void setDocBkmrks() {

        docBkmrks = new HashMap<>();

        docBkmrks.put("SNo", "");
        docBkmrks.put("page", "ObjectName");
        docBkmrks.put("rev", "ccmPrjDocRevision");
        docBkmrks.put("issue", "ccmPrjDocIssueStatus");
        docBkmrks.put("remarks", "");

        this.docBkmrks = docBkmrks;
    }

    public HashMap<String, String> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks() {

        bookmarks = new HashMap<>();
        bookmarks.put("projectName", "Project Name 123");
        bookmarks.put("contractNo", "contractNo...");
        bookmarks.put("docNo", "docNo....");
        bookmarks.put("TN", "CC CC CC CC");
        bookmarks.put("date", "31/12/2023");
        bookmarks.put("title", "Transmittal 456");

        for (int i = 1; i <= 20; i++) {
            for (Map.Entry<String, String> docBkmrk :
                    docBkmrks.entrySet()) {
                bookmarks.put(docBkmrk.getKey() + (i > 9 ? i : "0" + i), "");
            }
        }

        this.bookmarks = bookmarks;
    }

    public BookMark() {

        setDocBkmrks();
        setBookmarks();

    }
}
