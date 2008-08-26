import com.flexive.shared.scripting.groovy.*
import com.flexive.shared.value.*
import com.flexive.shared.structure.*
import com.flexive.shared.EJBLookup
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.security.AccountEdit
import com.flexive.shared.interfaces.ACLEngine
import com.flexive.shared.interfaces.UserGroupEngine;

// Create [fleXive] data structure announcementEntry
new GroovyTypeBuilder().announcementEntry(
        description: new FxString(true, "Announcement"),
        useInstancePermissions: true,
        useStepPermissions: false,
        useTypePermissions: true)
        {
            caption(assignment: "ROOT/CAPTION")
            entryTitle(multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Title"))
            submissionDate(dataType: FxDataType.Date, multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Submission Date"))
            publishDate(dataType: FxDataType.Date, multiplicity: FxMultiplicity.MULT_0_1, description: new FxString(true, "Publish Date"))
            submissionURL(multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Submission URL"))
            publishURL(multiplicity: FxMultiplicity.MULT_0_1, description: new FxString(true, "Publish URL"))
            entryAnnouncementText(FxDataType.HTML, multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Announcement text"), multiline: true)
        }

// Get engines
UserGroupEngine ue = EJBLookup.getUserGroupEngine();
AccountEngine ae = EJBLookup.getAccountEngine();
ACLEngine acle = EJBLookup.getAclEngine()

// Create user groups
long uGroupId1 = ue.create("Editors", "#AAAAAA", FxContext.get().getTicket().getMandatorId())
long uGroupId2 = ue.create("Visitors", "#FF0055", FxContext.get().getTicket().getMandatorId())

// Create accounts ...
AccountEdit accountEdit1 = new AccountEdit()
AccountEdit accountEdit2 = new AccountEdit()

accountEdit1.setEmail("announcement.editor@ucs.at")
accountEdit1.setName("announcement.editor")
accountEdit2.setEmail("announcement.visitor@ucs.at")
accountEdit2.setName("announcement.visitor")

long accountId1 = ae.create(accountEdit1, "password")
long accountId2 = ae.create(accountEdit2, "password")

// ... and add them to user groups
ae.addGroup(accountId1, uGroupId1)
ae.addGroup(accountId2, uGroupId2)

// Type ACL settings
acle.assign(ACL.Category.STRUCTURE.getDefaultId(), UserGroup.GROUP_EVERYONE, ACL.Permission.READ)
acle.assign(ACL.Category.STRUCTURE.getDefaultId(), uGroupId1, ACL.Permission.READ, ACL.Permission.EDIT, ACL.Permission.CREATE, ACL.Permission.DELETE)

// Create instance ACL and assign it to user groups
long instanceAclId = acle.create(
        "Announcement_Entry_ACL",
        new FxString("Announcement Entry ACL"),
        FxContext.get().getTicket().getMandatorId(),
        "#0033CC",
        "Announcement Entry Instance ACL",
        ACL.Category.INSTANCE
)
acle.assign(instanceAclId, uGroupId1, ACL.Permission.READ, ACL.Permission.EDIT, ACL.Permission.CREATE, ACL.Permission.DELETE)
acle.assign(instanceAclId, uGroupId2, ACL.Permission.READ)