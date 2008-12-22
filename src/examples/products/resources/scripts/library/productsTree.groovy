import com.flexive.shared.EJBLookup
import com.flexive.shared.tree.FxTreeMode
import com.flexive.shared.content.FxContent
import com.flexive.shared.content.FxPK
import com.flexive.shared.value.FxReference
import com.flexive.shared.exceptions.FxNotFoundException
import com.flexive.shared.tree.FxTreeNodeEdit
import com.flexive.shared.exceptions.FxApplicationException

/** Script Type: AfterContentCreate
    Description: Adds new products to the manufacturer folder */

final long folderId = EJBLookup.treeEngine.getIdByPath(FxTreeMode.Edit, "/Products/Manufacturers")
final FxContent product = EJBLookup.contentEngine.load(pk)
final FxPK manufacturerPk = ((FxReference) product.getValue("/manufacturer")).defaultTranslation

// check if manufacturer folder exists
long manufacturerFolderId
try {
    manufacturerFolderId = EJBLookup.treeEngine.findChild(FxTreeMode.Edit, folderId, manufacturerPk).id
} catch (FxApplicationException e) {
    final FxContent manufacturer = EJBLookup.contentEngine.load(manufacturerPk)
    // create folder
    manufacturerFolderId = EJBLookup.treeEngine.save(
            FxTreeNodeEdit.createNew(
                    manufacturer.getValue("/name").toString()
            ).setParentNodeId(folderId)
            .setReference(manufacturerPk)
    )
}

// attach new product
EJBLookup.treeEngine.save(
        FxTreeNodeEdit.createNew(product.getValue("/name").toString())
        .setReference(pk)
        .setParentNodeId(manufacturerFolderId)
)

// product now exists in edit tree, for it to automatically show up on the front end web page
// you'd have to activate the folder with TreeEngine#activate