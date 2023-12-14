package ser.bn.se.demosystems.documents;

public class Constants {

    public static class Templates{
        public static class CRS{
            public static final String OriginalTemplate = "C:\\SER\\TEMP\\Templates\\CRSTemplate.docx";
            public static final String OriginalTemplateXlsx = "C:\\SER\\TEMP\\Templates\\CRSTemplate.xlsx";
            public static final String HolderPath = "C:\\SER\\TEMP\\output-1.docx";
            public static String FinalPath = "C:\\SER\\TEMP\\output-2.docx";
            public static String FinalPathXlsx = "C:\\SER\\TEMP\\output-2.xlsx";
        }
        public static class Transmittal{
            public static final String OriginalTemplate = "C:\\SER\\TEMP\\Templates\\TransmittalTemplate.docx";
            public static final String HolderPath = "C:\\SER\\TEMP\\output-3.docx";
            public static String FinalPath = "C:\\SER\\TEMP\\output-4.docx";
        }
    }
    public static class ClassIDs{
        public static final String ProjectFolder = "32e74338-d268-484d-99b0-f90187240549";
        public static final String ProjectDocFolder = "897b6ca0-441c-4c43-8cf9-735d8d7453c6";
        public static final String DocumentCycle = "66975476-3c0b-4781-bac1-0a661c40bf97";
        public static final String ProjDocumentArchive = "acb37372-0240-4a44-95e2-424b8f93ffe4";
        public static final String CRSProjDocumentArchive = "3e1fe7b3-3e86-4910-8155-c29b662e71d6";
    }
    public static class Descriptors {
        public static final String ProjectNumber = "ProjectNumber";
        public static final String NumberReference= "ObjectNumberReference";
        public static final String Discipline = "OrgDepartment";
    }

    public static class Literals {
        public static final String ProjectNumberDescriptor = "CCMPRJCARD_CODE";
        public static final String ProjectFolderDB = "PRJ_FOLDER";
    }

    public static class Nodes{
        public static final String Transmittal = "fb57f334-7780-4b81-80ba-21ec74649654";
        public static final String CivilStructure = "b890320e-ab3d-4b74-b98e-49ba39b9933e";
        public static final String NativeFiles = "7bee6401-1f0b-4574-bb1c-87c03c498bfe";
        public static final String Review = "511369a6-4270-4c6d-bf4c-801383a93303";
    }
}
