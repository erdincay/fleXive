import com.flexive.shared.CacheAdmin
import com.flexive.shared.EJBLookup
import com.flexive.shared.configuration.SystemParameters
import com.flexive.shared.security.ACL
import com.flexive.shared.structure.*
import com.flexive.shared.value.FxString
/**
* Core System initialization script, has to be run before anything else!
*
* @author Markus Plesser, Unique Computing Solutions (UCS)
*/

println "Setting up [fleXive] ..."


//create the /CAPTION property and register it in the global configuration
FxPropertyEdit caption = FxPropertyEdit.createNew("CAPTION", new FxString("Caption"),
        new FxString("Caption"), FxMultiplicity.MULT_0_1,
        CacheAdmin.environment.getACL(ACL.Category.STRUCTURE.defaultId), FxDataType.String1024).setMultiLang(true)
long captionId = EJBLookup.assignmentEngine.createProperty(caption, "/")
long captionAssignment = CacheAdmin.environment.getAssignment("ROOT/CAPTION").id
println "Created /CAPTION with id $captionId, assignment $captionAssignment"
EJBLookup.configurationEngine.put(SystemParameters.TREE_CAPTION_PROPERTY, captionId);
EJBLookup.configurationEngine.put(SystemParameters.TREE_CAPTION_ROOTASSIGNMENT, captionAssignment);

//create the /FQN property and register it in the global configuration
FxPropertyEdit fqn = FxPropertyEdit.createNew("FQN", new FxString("FQN"),
        new FxString("Fully qualified name"), FxMultiplicity.MULT_0_1,
        CacheAdmin.environment.getACL(ACL.Category.STRUCTURE.defaultId), FxDataType.String1024).setMultiLang(false)
long fqnId = EJBLookup.assignmentEngine.createProperty(fqn, "/")
long fqnAssignment = CacheAdmin.environment.getAssignment("ROOT/FQN").id
println "Created /FQN with id $fqnId, assignment $fqnAssignment"
EJBLookup.configurationEngine.put(SystemParameters.TREE_FQN_PROPERTY, fqnId);
EJBLookup.configurationEngine.put(SystemParameters.TREE_FQN_ROOTASSIGNMENT, fqnAssignment);

//create mandator groups
EJBLookup.userGroupEngine.rebuildMandatorGroups()