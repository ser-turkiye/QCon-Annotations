package events;

import annotprop.Conf;
import com.ser.blueline.*;
import com.ser.blueline.metaDataComponents.IStringMatrix;
import com.ser.blueline.modifiablemetadata.IStringMatrixModifiable;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ser.foldermanager.IFolder;
import com.ser.foldermanager.INode;

import java.util.*;

public class OnChangeProjectCard extends UnifiedAgent {
    private Logger log = LogManager.getLogger();

    @Override
    protected Object execute() {
        ISession ses = this.getSes();
        IDocumentServer srv = ses.getDocumentServer();
        IDocument mainDocument = null;
        try {
            mainDocument = getEventDocument();
            log.info("----OnChangeProjectCard Started ---for IDocument ID:--" + mainDocument.getID());

            updatePrjCardCrspDocTypes2GVList("CCM_CORRESPONDENCE_DOC_TYPE", mainDocument.getDescriptorValue("ccmPRJCard_code"), mainDocument);
            log.info("----OnChangeProjectCard Updated Project Card GVList ---for (ID):" + mainDocument.getID());

            updatePrjCardGVList("CCM_PARAM_PROJECT-CARDS", mainDocument.getDescriptorValue("ccmPRJCard_code"), mainDocument);
            log.info("----OnChangeProjectCard Updated Project Card GVList ---for (ID):" + mainDocument.getID());

            updatePrjCardMembers2GVList("CCM_PARAM_PROJECT-MEMBERS", mainDocument.getDescriptorValue("ccmPRJCard_code"), mainDocument);
            log.info("----OnChangeProjectCard Updated Project Members GVList ---for (ID):" + mainDocument.getID());

        } catch (Exception e) {
            log.error("Exception Caught");
            log.error(e.getMessage());
            return resultError(e.getMessage());
        }
        return resultSuccess("Agent Finished Succesfully");
    }

    public void updatePrjCardGVList(String paramName, String paramKey, IDocument doc) throws Exception {
        try {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
            String rowValue = "";
            IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                rowValue = settingsMatrix.getValue(i, 0);
                if (rowValue.equalsIgnoreCase(paramKey)) {
                    srtMatrixModify.removeRow(i);
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                }
            }
            srtMatrixModify.appendRow();
            settingsMatrix.refresh();

            int rowCount = settingsMatrix.getRowCount();
            srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
            srtMatrixModify.setValue(rowCount, 1, doc.getDescriptorValue("ccmPRJCard_name"), false);
            srtMatrixModify.setValue(rowCount, 2, doc.getDescriptorValue("ccmContractNumber"), false);
            srtMatrixModify.setValue(rowCount, 3, doc.getDescriptorValue("ccmPRJCard_status"), false);
            srtMatrixModify.setValue(rowCount, 4, doc.getDescriptorValue("ccmPRJCard_country"), false);
            srtMatrixModify.setValue(rowCount, 5, doc.getDescriptorValue("ccmPrjDocClientPrjNumber"), false);
            srtMatrixModify.setValue(rowCount, 6, doc.getDescriptorValue("ccmPrjDocClient"), false);
            srtMatrixModify.setValue(rowCount, 7, doc.getDescriptorValue("ccmPRJCard_prefix"), false);
            srtMatrixModify.setValue(rowCount, 8, doc.getDescriptorValue("ccmPRJCard_ResponseDay"), false);
            srtMatrixModify.setValue(rowCount, 9, doc.getDescriptorValue("ccmPRJCard_ResponseDaySecond"), false);
            srtMatrixModify.setValue(rowCount, 10, doc.getDescriptorValue("ccmPRJCard_ConsalidatorDrtn"), false);
            srtMatrixModify.setValue(rowCount, 11, doc.getDescriptorValue("ccmPRJCard_DCCDrtn"), false);
            srtMatrixModify.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardGVList: " + e);
        }
    }
    public void updatePrjCardCrspDocTypes2GVList(String paramName, String paramKey, IDocument doc) throws Exception {
        try {
            String cDocTypeTemps = doc.getDescriptorValue("ccmCrspDocTypes");

            String prjCode = (doc.getDescriptorValue("ccmPRJCard_code") != null ? doc.getDescriptorValue("ccmPRJCard_code") : "");
            removeByPrjCodeFromGVList(paramName, prjCode);
            if(cDocTypeTemps == null) {return;}

            String[] cDocTypes = cDocTypeTemps.replace("[", "").replace("]", "").split(",");


            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
            IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
            settingsMatrix.refresh();
            for (String cDocType : cDocTypes) {
                if(!cDocType.contains("-")){continue;}

                String cDocShort = cDocType.substring(0, cDocType.indexOf("-"));
                String cDocName = cDocType.substring(cDocType.indexOf("-") + 1);

                srtMatrixModify.appendRow();
                srtMatrixModify.commit();
                settingsMatrix.refresh();
                int rowCount = settingsMatrix.getRowCount()-1;
                srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                srtMatrixModify.setValue(rowCount, 1, cDocShort, false);
                srtMatrixModify.setValue(rowCount, 2, cDocName, false);
                srtMatrixModify.commit();
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardMembers2GVList: " + e);
        }
    }

    public void updatePrjCardMembers2GVList(String paramName, String paramKey, IDocument doc) throws Exception {
        try {
            String managerID = doc.getDescriptorValue("ccmPRJCard_EngMng");
            String managerName = getUserByWB(managerID);
            String pmanagerID = doc.getDescriptorValue("ccmPRJCard_prjmngr");
            String pmanagerName = getUserByWB(pmanagerID);
            String dccMembers = doc.getDescriptorValue("ccmPrjCard_DccList");
            String otherMembers = doc.getDescriptorValue("ccmPrjCardUsers");
            String membersD = "";
            String membersOth = "";
            String[] membersIDs = new String[0];
            String[] membersOthIDs = new String[0];

            if(dccMembers != null) {
                membersD = doc.getDescriptorValue("ccmPrjCard_DccList").replace("[", "").replace("]", "");
                membersIDs = membersD.split(",");
            }
            if(otherMembers != null) {
                membersOth = doc.getDescriptorValue("ccmPrjCardUsers").replace("[", "").replace("]", "");
                membersOthIDs = membersOth.split(",");
            }

            List<String> memberList = new ArrayList<>();
            for (String memberID : membersIDs) {
                String memberName = getUserByWB(memberID);
                //assert false;
                memberList.add(memberName);
            }
            List<String> memberOthList = new ArrayList<>();
            for (String memberOthID : membersOthIDs) {
                String memberName = getUserByWB(memberOthID);
                //assert false;
                memberOthList.add(memberName);
            }
            String prjCode = (doc.getDescriptorValue("ccmPRJCard_code") != null ? doc.getDescriptorValue("ccmPRJCard_code") : "");
            boolean isRemove = removeByPrjCodeFromGVList(paramName, prjCode);

            int rowCount = 0;
            if(!Objects.equals(managerName, "")) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                srtMatrixModify.appendRow();
                srtMatrixModify.commit();
                settingsMatrix.refresh();

                rowCount = settingsMatrix.getRowCount()-1;
                //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
                srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                srtMatrixModify.setValue(rowCount, 1, "EM", false);
                srtMatrixModify.setValue(rowCount, 2, managerID, false);
                srtMatrixModify.setValue(rowCount, 3, managerName, false);
                srtMatrixModify.commit();
                settingsMatrix.refresh();
            }

            if(!Objects.equals(pmanagerName, "")) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                srtMatrixModify.appendRow();
                srtMatrixModify.commit();
                settingsMatrix.refresh();

                rowCount = settingsMatrix.getRowCount()-1;
                //srtMatrixModify.setValue(rowCount, 0, String.valueOf(rowCount + 2), false);
                srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                srtMatrixModify.setValue(rowCount, 1, "PM", false);
                srtMatrixModify.setValue(rowCount, 2, pmanagerID, false);
                srtMatrixModify.setValue(rowCount, 3, pmanagerName, false);
                srtMatrixModify.commit();
                settingsMatrix.refresh();
            }

            if(!memberList.isEmpty()) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                int c = 0;
                for (String memberName : memberList) {
                    String mmbr = membersIDs[c];
                    srtMatrixModify.appendRow();
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    rowCount = settingsMatrix.getRowCount()-1;
                    srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                    srtMatrixModify.setValue(rowCount, 1, "DCC", false);
                    srtMatrixModify.setValue(rowCount, 2, mmbr, false);
                    srtMatrixModify.setValue(rowCount, 3, memberName, false);
                    srtMatrixModify.commit();
                    c++;
                }
            }

            if(!memberOthList.isEmpty()) {
                IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
                IStringMatrixModifiable srtMatrixModify = getDocumentServer().getStringMatrix(paramName, getSes()).getModifiableCopy(getSes());
                settingsMatrix.refresh();
                int c = 0;
                for (String memberOthName : memberOthList) {
                    String mmbrOth = membersOthIDs[c];
                    srtMatrixModify.appendRow();
                    srtMatrixModify.commit();
                    settingsMatrix.refresh();
                    rowCount = settingsMatrix.getRowCount()-1;
                    srtMatrixModify.setValue(rowCount, 0, doc.getDescriptorValue("ccmPRJCard_code"), false);
                    srtMatrixModify.setValue(rowCount, 1, "OTHER", false);
                    srtMatrixModify.setValue(rowCount, 2, mmbrOth, false);
                    srtMatrixModify.setValue(rowCount, 3, memberOthName, false);
                    srtMatrixModify.commit();
                    c++;
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updatePrjCardMembers2GVList: " + e);
        }
    }
    public boolean removeByPrjCodeFromGVList(String paramName, String paramKey){
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValuePrjCode = "";
        IStringMatrixModifiable srtMatrixModify = settingsMatrix.getModifiableCopy(getSes());
        for(int i = 0; i < srtMatrixModify.getRowCount(); i++) {
            rowValuePrjCode = srtMatrixModify.getValue(i, 0);
            if (rowValuePrjCode.equalsIgnoreCase(paramKey)) {
                srtMatrixModify.removeRow(i);
                srtMatrixModify.commit();
                if(removeByPrjCodeFromGVList(paramName, paramKey)){break;}
            }
        }
        return true;
    }
    public boolean existPRJGVList(String paramName, String key1) {
        boolean rtrn = false;
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValueParamKey = "";
        for(int i = 0; i < settingsMatrix.getRowCount(); i++) {
            rowValueParamKey = settingsMatrix.getValue(i, 2);
            if (rowValueParamKey.equalsIgnoreCase(key1)) {
               return true;
            }
        }
        return rtrn;
    }
    public void updateRolesFromGVList(IDocument doc) throws Exception {
        try {
            IRole dccRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.DCCUsersRole);
            IRole prjRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.PRJUsersRole);

            if(dccRole==null || prjRole == null){
                throw new Exception("Exeption Caught..updateRolesFromGVList..dccRole or prjRole is NULL");
            }

            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix("CCM_PARAM_PROJECT-MEMBERS", getSes());
            if(settingsMatrix!=null) {
                for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                    String roleType = settingsMatrix.getValue(i, 1);
                    String wbID = settingsMatrix.getValue(i, 2);
                    String userId = getUserIDByWB(wbID);
                    IUser user = getDocumentServer().getUser(getSes() , userId);
                    if (roleType.equalsIgnoreCase("DCC")) {
                        addToRole(user, dccRole.getID());
                        addToRole(user, prjRole.getID());
                    }else{
                        addToRole(user, prjRole.getID());
                    }
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updateRolesFromGVList: " + e);
        }
    }
    public boolean existDCCGVList(String paramName, String key1, String key2) {
        boolean rtrn = false;
        IStringMatrix settingsMatrix = getDocumentServer().getStringMatrix(paramName, getSes());
        String rowValuePrjCode = "";
        String rowValueParamKey1 = "";
        String rowValueParamKey2 = "";
        for(int i = 0; i < settingsMatrix.getRowCount(); i++) {
            rowValuePrjCode = settingsMatrix.getValue(i, 0);
            rowValueParamKey1 = settingsMatrix.getValue(i, 1);
            rowValueParamKey2 = settingsMatrix.getValue(i, 2);
            if (rowValueParamKey1.equalsIgnoreCase(key1) && rowValueParamKey2.equalsIgnoreCase(key2)) {
               return true;
            }
        }
        return rtrn;
    }
    public void updateRolesByPrjCard(IDocument doc) throws Exception {
        try {
            IRole dccRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.DCCUsersRole);
            IRole prjRole = getSes().getDocumentServer().getRoleByName(getSes(),Conf.RoleNames.PRJUsersRole);
            String[] dccRoleIDs = new String[]{dccRole.getID()};
            String[] prjRoleIDs = new String[]{prjRole.getID()};

            List<String> dccUsers = new ArrayList<>();
            List<String> prjUsers = new ArrayList<>();

            String emanagerId = getUserIDByWB(doc.getDescriptorValue("ccmPRJCard_EngMng"));
            IUser emanagerUser = getDocumentServer().getUser(getSes() , emanagerId);
            if(emanagerUser != null){
                addToRole(emanagerUser, prjRoleIDs[0]);
                prjUsers.add(emanagerId);
            }

            String pmanagerId = getUserIDByWB(doc.getDescriptorValue("ccmPRJCard_prjmngr"));
            IUser pmanagerUser = getDocumentServer().getUser(getSes() , pmanagerId);
            if(pmanagerUser != null){
                addToRole(pmanagerUser, prjRoleIDs[0]);
                prjUsers.add(pmanagerId);
            }

            String prjMembers = doc.getDescriptorValue("ccmPrjCardUsers");
            String membersP = "";
            String[] membersPIDs = new String[0];
            if(prjMembers != null) {
                membersP = doc.getDescriptorValue("ccmPrjCardUsers").replace("[", "").replace("]", "");
                membersPIDs = membersP.split(",");
            }
            List<String> memberPList = new ArrayList<>();
            for (String memberID : membersPIDs) {
                String memberUserId = getUserIDByWB(memberID);
                IUser memberUser = getDocumentServer().getUser(getSes() , memberUserId);
                if(memberUser != null){
                    addToRole(memberUser, prjRoleIDs[0]);
                    prjUsers.add(memberUserId);
                }
            }

            String dccMembers = doc.getDescriptorValue("ccmPrjCard_DccList");
            String membersD = "";
            String[] membersIDs = new String[0];
            if(dccMembers != null) {
                membersD = doc.getDescriptorValue("ccmPrjCard_DccList").replace("[", "").replace("]", "");
                membersIDs = membersD.split(",");
            }
            List<String> memberList = new ArrayList<>();
            for (String memberID : membersIDs) {
                String memberUserId = getUserIDByWB(memberID);
                IUser memberUser = getDocumentServer().getUser(getSes() , memberUserId);
                if(memberUser != null){
                    addToRole(memberUser, dccRoleIDs[0]);
                    addToRole(memberUser, prjRoleIDs[0]);
                    if(memberUser.getLicenseType() != LicenseType.NORMAL_USER) {
                        IUser cuser = memberUser.getModifiableCopy(getSes());
                        cuser.setLicenseType(LicenseType.NORMAL_USER);
                        cuser.commit();
                    }
                    prjUsers.add(memberUserId);
                    dccUsers.add(memberUserId);
                }
            }

            IUser[] dccRoleMembers = dccRole.getUserMembers();
            if (dccRoleMembers != null) {
                for (IUser dccMember : dccRoleMembers) {
                    String wbID = getBpm().getWorkbasketByName(dccMember.getName()).getID();
                    boolean isDCCMember = existDCCGVList("CCM_PARAM_PROJECT-MEMBERS","DCC",wbID);
                    if (!dccUsers.contains(dccMember.getID()) && !isDCCMember) {
                        removeFromRole(dccMember,dccRole.getID());
                        log.info("removed role [" + dccRole.getName() + "] from user:" + dccMember.getFullName());
                    }
                }
            }

            IUser[] prjRoleMembers = prjRole.getUserMembers();
            if (prjRoleMembers != null) {
                for (IUser prjMember : prjRoleMembers) {
                    String wbID = getBpm().getWorkbasketByName(prjMember.getName()).getID();
                    boolean isPRJMember = existPRJGVList("CCM_PARAM_PROJECT-MEMBERS",wbID);
                    if (!prjUsers.contains(prjMember.getID()) && !isPRJMember) {
                        removeFromRole(prjMember,prjRole.getID());
                        log.info("removed role [" + prjRole.getName() + "] from user:" + prjMember.getFullName());
                    }
                }
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..updateRolesByPrjCard: " + e);
        }
    }
    public void updateUnitsByPrjCard(String unitName, List<String> members) throws Exception {
        List<String> prjUnitUserIDs = new ArrayList<>();
        ISerClassFactory classFactory = getDocumentServer().getClassFactory();
        IUnit punit = getDocumentServer().getUnitByName(getSes(), "Projects");
        IUnit unit = getDocumentServer().getUnitByName(getSes(), unitName);
        if(punit != null){
            if(unit == null){
                unit = classFactory.createUnitInstance(getSes(),unitName);
                unit.commit();
                IUnit cunit = unit.getModifiableCopy(getSes());
                cunit.setParent(punit);
                cunit.commit();
            }
            if(unit != null){
                if(unit.getParent() == null || (unit.getParent() != null && !Objects.equals(unit.getParent().getID(), punit.getID()))) {
                    IUnit cunit = unit.getModifiableCopy(getSes());
                    cunit.setParent(punit);
                    cunit.commit();
                }

                log.info("Unit update start:" + unit.getName());
                for (String memberID : members) {
                    IUser memberUser = getDocumentServer().getUser(getSes(), memberID);
                    if (memberUser != null) {
                        addToUnit(memberUser,unit.getID());
                        log.info("add user:" + memberUser.getFullName() + " to unit " + unitName);
                    }
                }

                IUser[] prjUnitMembers = unit.getUserMembers();
                if (prjUnitMembers != null) {
                    for (IUser pMember : prjUnitMembers) {
                        prjUnitUserIDs.add(pMember.getID());
                    }
                }
                for (String prjUserID : prjUnitUserIDs) {
                    IUser prjUnitUser = getDocumentServer().getUser(getSes(), prjUserID);
                    if (!members.contains(prjUserID)) {
                        removeFromUnit(prjUnitUser,unit.getID());
                        log.info("removed user:" + prjUnitUser.getFullName() + " from unit:" + unitName);
                    }
                }

            }
        }
    }
    public void addToRole(IUser user, String roleID) throws Exception {
        try {
            String[] roleIDs = (user != null ? user.getRoleIDs() : null);
            boolean isExist = Arrays.asList(roleIDs).contains(roleID);
            if(!isExist){
                List<String> rtrn = new ArrayList<String>(Arrays.asList(roleIDs));
                rtrn.add(roleID);
                IUser cuser = user.getModifiableCopy(getSes());
                String[] newRoleIDs = rtrn.toArray(new String[0]);
                cuser.setRoleIDs(newRoleIDs);
                cuser.commit();
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..addToRole : " + e);
        }
    }
    public void removeFromRole(IUser user, String roleID) throws Exception {
        try {
            String[] roleIDs = (user != null ? user.getRoleIDs() : null);
            List<String> rtrn = new ArrayList<String>(Arrays.asList(roleIDs));
            for (int i = 0; i < roleIDs.length; i++) {
                String rID = roleIDs[i];
                if (Objects.equals(rID, roleID)) {
                    rtrn.remove(roleID);
                }
            }
            IUser cuser = user.getModifiableCopy(getSes());
            String[] newRoleIDs = rtrn.toArray(new String[0]);
            cuser.setRoleIDs(newRoleIDs);
            cuser.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..removeFromRole : " + e);
        }
    }
    public void addToUnit(IUser user, String unitID) throws Exception {
        try {
            String[] unitIDs = (user != null ? user.getUnitIDs() : null);
            boolean isExist = Arrays.asList(unitIDs).contains(unitID);
            if(!isExist){
                List<String> rtrn = new ArrayList<String>(Arrays.asList(unitIDs));
                rtrn.add(unitID);
                IUser cuser = user.getModifiableCopy(getSes());
                String[] newUnitIDs = rtrn.toArray(new String[0]);
                cuser.setUnitIDs(newUnitIDs);
                cuser.commit();
            }
        }catch (Exception e){
            throw new Exception("Exeption Caught..addToRole : " + e);
        }
    }
    public void removeFromUnit(IUser user, String unitID) throws Exception {
        try {
            String[] unitIDs = (user != null ? user.getUnitIDs() : null);
            List<String> rtrn = new ArrayList<String>(Arrays.asList(unitIDs));
            for (int i = 0; i < unitIDs.length; i++) {
                String rID = unitIDs[i];
                if (Objects.equals(rID, unitID)) {
                    rtrn.remove(unitID);
                }
            }
            IUser cuser = user.getModifiableCopy(getSes());
            String[] newUnitIDs = rtrn.toArray(new String[0]);
            cuser.setUnitIDs(newUnitIDs);
            cuser.commit();
        }catch (Exception e){
            throw new Exception("Exeption Caught..removeFromUnit : " + e);
        }
    }
    public String[] removeUnitFromList(IUser user, String unitID){
        String[] unitIDs = (user != null ? user.getUnitIDs() : null);
        int cnt = 0;
        //String[] rtrn = null;
        List<String> rtrn = new ArrayList<String>(Arrays.asList(unitIDs));
        for(int i=0;i<unitIDs.length;i++){
            String rID = unitIDs[i];
            if(Objects.equals(rID, unitID)){
                rtrn.remove(unitIDs[i]);
            }
        }
        return rtrn.toArray(new String[0]);
    }
    public List<String> getPrjMembersFromPrjCard(IDocument doc){
        List<String> prjUsers = new ArrayList<>();

        String emanagerId = getUserIDByWB(doc.getDescriptorValue("ccmPRJCard_EngMng"));
        IUser emanagerUser = getDocumentServer().getUser(getSes() , emanagerId);
        if(emanagerUser != null){
            prjUsers.add(emanagerId);
        }

        String pmanagerId = getUserIDByWB(doc.getDescriptorValue("ccmPRJCard_prjmngr"));
        IUser pmanagerUser = getDocumentServer().getUser(getSes() , pmanagerId);
        if(pmanagerUser != null){
            prjUsers.add(pmanagerId);
        }

        String prjMembers = doc.getDescriptorValue("ccmPrjCardUsers");
        String membersP = "";
        String[] membersPIDs = new String[0];
        if(prjMembers != null) {
            membersP = doc.getDescriptorValue("ccmPrjCardUsers").replace("[", "").replace("]", "");
            membersPIDs = membersP.split(",");
        }
        for (String memberID : membersPIDs) {
            String memberUserId = getUserIDByWB(memberID);
            IUser memberUser = getDocumentServer().getUser(getSes() , memberUserId);
            if(memberUser != null){
                prjUsers.add(memberUserId);
            }
        }

        String dccMembers = doc.getDescriptorValue("ccmPrjCard_DccList");
        String membersD = "";
        String[] membersIDs = new String[0];
        if(dccMembers != null) {
            membersD = doc.getDescriptorValue("ccmPrjCard_DccList").replace("[", "").replace("]", "");
            membersIDs = membersD.split(",");
        }
        for (String memberID : membersIDs) {
            String memberUserId = getUserIDByWB(memberID);
            IUser memberUser = getDocumentServer().getUser(getSes() , memberUserId);
            if(memberUser != null){
                prjUsers.add(memberUserId);
            }
        }
        return prjUsers;
    }
    public String getUserIDByWB(String wbID){
        String rtrn = "";
        if(wbID != null) {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowID = settingsMatrix.getValue(i, 0);
                if (rowID.equalsIgnoreCase(wbID)) {
                    rtrn = settingsMatrix.getValue(i, 6);
                    break;
                }
            }
        }
        return rtrn;
    }
    public String getUserByWB(String wbID){
        String rtrn = "";
        if(wbID != null) {
            IStringMatrix settingsMatrix = getDocumentServer().getStringMatrixByID("Workbaskets", getSes());
            for (int i = 0; i < settingsMatrix.getRowCount(); i++) {
                String rowID = settingsMatrix.getValue(i, 0);
                if (rowID.equalsIgnoreCase(wbID)) {
                    rtrn = settingsMatrix.getValue(i, 2);
                    break;
                }
            }
        }
        return rtrn;
    }
    private IInformationObject[] createQuery(String dbName , String whereClause , int maxHits){
        String[] databaseNames = {dbName};

        ISerClassFactory fac = getDocumentServer().getClassFactory();
        IQueryParameter que = fac.getQueryParameterInstance(
                getSes() ,
                databaseNames ,
                fac.getExpressionInstance(whereClause) ,
                null,null);
        que.setMaxHits(maxHits);
        que.setHitLimit(maxHits + 1);
        que.setHitLimitThreshold(maxHits + 1);
        IDocumentHitList hits = que.getSession() != null? que.getSession().getDocumentServer().query(que, que.getSession()):null;
        if(hits == null) return null;
        else return hits.getInformationObjects();
    }
    public static INode getNode(IFolder fold, String fldn){
        List<INode> nodesByName = fold.getNodesByName(fldn);
        return fold.getNodeByID(nodesByName.get(0).getID());
    }
}
