import com.flexive.shared.scripting.groovy.*
import com.flexive.shared.value.*
import com.flexive.shared.structure.*
import com.flexive.shared.EJBLookup
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.security.AccountEdit
import com.flexive.shared.interfaces.ACLEngine
import com.flexive.shared.interfaces.UserGroupEngine;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxEnvironment;

// Create needed types, users, usergroups and acls if not already present
if (!CacheAdmin.getEnvironment().typeExists("announcementEntry")) {
    // Get engines
    UserGroupEngine ugEng = EJBLookup.getUserGroupEngine();
    AccountEngine accEng= EJBLookup.getAccountEngine();
    ACLEngine aclEng = EJBLookup.getAclEngine()

    // Create a type ACL used to protect the FxType
    // announcemntEntry on type level
    long announcementTypeACLId = aclEng.create(
            "Announcement Type ACL",
            new FxString("Announcement Type ACL"),
            FxContext.get().getTicket().getMandatorId(),
            "#CC9900",
            "Announcement Type ACL",
            ACLCategory.STRUCTURE
    )

    // Create [fleXive] data structure announcementEntry
    new GroovyTypeBuilder().announcementEntry(
            description: new FxString(true, "Announcement"),
            useTypePermissions: true,
            useInstancePermissions: true,
            acl: CacheAdmin.getEnvironment().getACL(announcementTypeACLId))
            {
                caption(assignment: "ROOT/CAPTION", multiplicity: FxMultiplicity.MULT_1_1)
                announcementText(FxDataType.Text, multiplicity: FxMultiplicity.MULT_1_1, description: new FxString(true, "Announcement Text"), multiline: true)
                publishDate(dataType: FxDataType.Date, multiplicity: FxMultiplicity.MULT_0_1, description: new FxString(true, "Publish Date"))
                publishURL(multiplicity: FxMultiplicity.MULT_0_1, description: new FxString(true, "Publish URL"))
            }

    // Create user groups
    long uGroupEditors = ugEng.create("Editors", "#CC9900", FxContext.get().getTicket().getMandatorId())
    long uGroupVisitors = ugEng.create("Visitors", "#CC9900", FxContext.get().getTicket().getMandatorId())
    
    // Create users accounts...
    AccountEdit editorAccount = new AccountEdit()
    AccountEdit visitorAccount = new AccountEdit()

    editorAccount.setName("announcement.editor")
    editorAccount.setEmail("as@as.net")
    visitorAccount.setName("announcement.visitor")
    visitorAccount.setEmail("vs@vs.net")

    long accountEditorId = accEng.create(editorAccount, "editor")
    long accountVisitorId = accEng.create(visitorAccount, "visitor")

    // Add users to user groups
    accEng.addGroup(accountEditorId, uGroupEditors)
    accEng.addGroup(accountVisitorId, uGroupVisitors)

    // Assign type ACL to user groups
    aclEng.assign(announcementTypeACLId, uGroupEditors, ACLPermission.READ, ACLPermission.EDIT, ACLPermission.CREATE, ACLPermission.DELETE)
    aclEng.assign(announcementTypeACLId, uGroupVisitors, ACLPermission.READ)

    // Create an instance ACL which will be used
   // for all announcement instances
    long instanceAclReadAllId = aclEng.create(
            "Announcement Instance Read All",
            new FxString("Announcement Instance Read All"),
            FxContext.get().getTicket().getMandatorId(),
            "#CC9900",
            "Announcement Instance Read All",
            ACLCategory.INSTANCE
    )

    // Assign "read all"-instance ACL to user groups so that  
    // both user groups have instance read permission
    // and editors also have create, edit and delete permission
    aclEng.assign(instanceAclReadAllId, uGroupEditors, ACLPermission.READ, ACLPermission.EDIT, ACLPermission.CREATE, ACLPermission.DELETE)
    aclEng.assign(instanceAclReadAllId, uGroupVisitors, ACLPermission.READ)

    // Create instance ACL where only editors have instance read permission
    long instanceAclEditorsOnlyId = aclEng.create(
            "Announcement Instance Editors Only",
            new FxString("Announcement Instance Editors Only"),
            FxContext.get().getTicket().getMandatorId(),
            "#CC9900",
            "Announcement Instance Editors Only",
            ACLCategory.INSTANCE
    )
    // assign "editors only"-instance ACL to user group
    aclEng.assign(instanceAclEditorsOnlyId, uGroupEditors, ACLPermission.READ, ACLPermission.EDIT, ACLPermission.CREATE, ACLPermission.DELETE)
}
