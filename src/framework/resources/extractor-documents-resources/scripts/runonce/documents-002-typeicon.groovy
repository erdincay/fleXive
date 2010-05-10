import com.flexive.shared.scripting.FxScriptEvent
import com.flexive.shared.scripting.FxScriptInfo
import com.flexive.shared.scripting.groovy.GroovyTypeBuilder
import com.flexive.shared.security.ACLCategory
import com.flexive.shared.value.FxString
import com.flexive.shared.search.*
import com.flexive.shared.structure.*
import com.flexive.shared.*
import com.flexive.shared.value.FxBinary
import com.flexive.shared.value.BinaryDescriptor
import com.flexive.shared.content.FxPK
import com.flexive.shared.scripting.groovy.GroovyContentBuilder
import com.flexive.shared.value.FxReference
import com.flexive.shared.value.ReferencedContent
import com.flexive.shared.structure.FxType

// The next methods were copied from init0080_typeIcons - TODO: create a common helper class
InputStream getImageResource(path) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream("binaries/" + path)
}

FxBinary getImageBinary(name, path) {
    final InputStream stream = getImageResource(path)
    return stream != null ? new FxBinary(false, new BinaryDescriptor(name, stream)) : new FxBinary(false, null).setEmpty();
}

FxPK createTypeIcon(String typeName, String fileName) {
    def builder = new GroovyContentBuilder(FxType.IMAGE)
    builder {
        file(getImageBinary("Type Icon: " + typeName.toUpperCase(), fileName))
    }
    EJBLookup.contentEngine.save(builder.content)
}


def addTypeIcon(String typeName, String fileName) {
    EJBLookup.typeEngine.save(
            CacheAdmin.environment.getType(typeName)
            .asEditable()
            .setIcon(new FxReference(new ReferencedContent(createTypeIcon(typeName, fileName))))
    )
}


if (!FxSharedUtils.isMinimalRunOnceScripts()) {
    addTypeIcon(FxType.DOCUMENT, "type-document.png")
}