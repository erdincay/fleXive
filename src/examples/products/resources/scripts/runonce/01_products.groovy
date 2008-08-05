import com.flexive.shared.*
import com.flexive.shared.scripting.groovy.GroovyTypeBuilder
import com.flexive.shared.structure.FxDataType
import com.flexive.shared.structure.FxSelectList
import com.flexive.shared.structure.FxType
import com.flexive.shared.structure.FxMultiplicity
import com.flexive.shared.value.FxString
import com.flexive.shared.content.FxPK
import com.flexive.shared.scripting.groovy.GroovyContentBuilder
import com.flexive.shared.value.FxSelectOne
import com.flexive.shared.value.FxHTML
import org.apache.commons.lang.StringUtils
import com.flexive.shared.value.FxReference
import com.flexive.shared.value.ReferencedContent
import com.flexive.shared.structure.UniqueMode
import com.flexive.shared.structure.FxSelectListEdit
import com.flexive.shared.security.ACL
import com.flexive.shared.structure.FxSelectListItemEdit
import com.flexive.shared.value.FxBinary
import com.flexive.shared.value.BinaryDescriptor
import com.flexive.shared.search.*

// create colors select list
final ACL itemAcl = environment.getACL(ACL.Category.SELECTLISTITEM.getDefaultId())
final FxSelectListEdit colorsEdit = FxSelectListEdit.createNew("ARTICLE_COLORS",
        new FxString(true, FxLanguage.ENGLISH, "Article Colors").setTranslation(FxLanguage.GERMAN, "Farben f\u00FCr Artikel"),
        new FxString(true, FxLanguage.ENGLISH, "Article Colors").setTranslation(FxLanguage.GERMAN, "Farben f\u00FCr Artikel"),
        true, itemAcl, itemAcl)
[["black", "schwarz"],
 ["white", "weiss"],
 ["red", "rot"],
 ["green", "gr\u00FCn"],
 ["blue", "blau"]].each { trans ->
    new FxSelectListItemEdit(trans[0], itemAcl, colorsEdit,
            new FxString(FxLanguage.ENGLISH, trans[0]).setTranslation(FxLanguage.GERMAN, trans[1]), trans[0], "")
}
final long colorsId = EJBLookup.selectListEngine.save(colorsEdit)
final FxSelectList colors = CacheAdmin.environment.getSelectList(colorsId)  

// get countries select list
final FxSelectList countries = CacheAdmin.environment.getSelectList(FxSelectList.COUNTRIES)

// create product manufacturer type
new GroovyTypeBuilder().manufacturer(description: new FxString("Manufacturer")) {

    name        (assignment: "ROOT/CAPTION",
                 description: new FxString(FxLanguage.ENGLISH, "Name"))

    description (dataType: FxDataType.HTML,
                 multilang: true,
                 description: new FxString(FxLanguage.ENGLISH, "Description"))

    logo        (dataType: FxDataType.Binary,
                 description: new FxString(FxLanguage.ENGLISH, "Company Logo"))

    basedIn     (dataType: FxDataType.SelectOne,
                 referencedList: countries,
                 description: new FxString(FxLanguage.ENGLISH, "Based in"))

}

EJBLookup.resultPreferencesEngine.saveSystemDefault(
        new ResultPreferences(
                [new ResultColumnInfo("@pk"),
                 new ResultColumnInfo("#manufacturer/basedIn"),
                 new ResultColumnInfo("#manufacturer/name"),
                 new ResultColumnInfo("#manufacturer/description")
                ],
                [new ResultOrderByInfo("#manufacturer/name", SortDirection.ASCENDING)],
                25, 0
        ),
        CacheAdmin.environment.getType("manufacturer").id,
        ResultViewType.LIST,
        AdminResultLocations.values()
)

// create the product type
final FxType manufacturerType = CacheAdmin.environment.getType("manufacturer")
new GroovyTypeBuilder().product(description: new FxString("Product")) {

    name         (assignment: "ROOT/CAPTION",
                  multilang: true,
                  description: new FxString(FxLanguage.ENGLISH, "Name"))

    // create a group "Variant" to store the actual article numbers
    Variant      (description: new FxString(FxLanguage.ENGLISH, "Variant"),
                  multiplicity: FxMultiplicity.MULT_0_N) {

        articleNumber   (description: new FxString(FxLanguage.ENGLISH, "Article number"),
                         uniqueMode: UniqueMode.Type)

        color           (dataType: FxDataType.SelectOne,
                         referencedList: colors,
                         description: new FxString(FxLanguage.ENGLISH, "Color"),
                         uniqueMode: UniqueMode.Instance)

        image       (dataType: FxDataType.Binary,
                      multiplicity: FxMultiplicity.MULT_0_N,
                      description: new FxString(FxLanguage.ENGLISH, "Images"))

    }

    manufacturer (dataType: FxDataType.Reference,       /* Reference contents of another type */
                  referencedType: manufacturerType,     /* Limit references to the manufacturer type */
                  description: new FxString(FxLanguage.ENGLISH, "Manufacturer"))

    description  (dataType: FxDataType.HTML, 
                  multilang: true,
                  description: new FxString(FxLanguage.ENGLISH, "Description"))
                  
    price        (dataType: FxDataType.Double,
                  description: new FxString(FxLanguage.ENGLISH, "Price (EUR)"))

    image       (dataType: FxDataType.Binary,
                  multiplicity: FxMultiplicity.MULT_0_N,
                  description: new FxString(FxLanguage.ENGLISH, "Images"))
}

EJBLookup.resultPreferencesEngine.saveSystemDefault(
        new ResultPreferences(
                [new ResultColumnInfo("@pk"),
                 new ResultColumnInfo("#product/name"),
                 new ResultColumnInfo("#product/price"),
                 new ResultColumnInfo("#product/manufacturer"),
                 new ResultColumnInfo("#product/description")
                ],
                [new ResultOrderByInfo("#product/name", SortDirection.ASCENDING)],
                25, 0
        ),
        CacheAdmin.environment.getType("product").id,
        ResultViewType.LIST,
        AdminResultLocations.values()
)


// create test data

InputStream getImageResource(path) {
    return Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("productsResources/scripts/runonce/" + path)
}

FxBinary getImageBinary(name, path) {
    final InputStream stream = getImageResource(path)
    return stream != null ? new FxBinary(false, new BinaryDescriptor(name, stream)) : new FxBinary(false, null).setEmpty();
}

// create some manufacturers...
final List<FxPK> manufacturers = [];
GroovyContentBuilder builder = new GroovyContentBuilder("manufacturer")
builder {
    name("Police Inc.")
    basedIn(new FxSelectOne(false, countries.getItemByData("ca")))
    description(new FxHTML(FxLanguage.ENGLISH, "A global distributor of police-related accessoires.")
                .setTranslation(FxLanguage.GERMAN, "Ein weltweiter Distributor von Polizei-Accessoires."))
    logo(getImageBinary("Police Inc. Logo", "logos/agent.png"))
}
manufacturers.add(EJBLookup.contentEngine.save(builder.content))

builder = new GroovyContentBuilder("manufacturer")
builder {
    name("Amor")
    basedIn(new FxSelectOne(false, countries.getItemByData("it")))
    description(new FxHTML(FxLanguage.ENGLISH, "Love is Amor's business.")
                .setTranslation(FxLanguage.GERMAN, "Liebe ist Amor's Gesch\u00E4ft."))
    logo(getImageBinary("Amor Logo", "logos/amor.png"))
}
manufacturers.add(EJBLookup.contentEngine.save(builder.content))

builder = new GroovyContentBuilder("manufacturer")
builder {
    name("Atlantic Enterprises")
    basedIn(new FxSelectOne(false, countries.getItemByData("us")))
    description(new FxHTML(FxLanguage.ENGLISH, "Atlantic deals with poker hardware.")
                .setTranslation(FxLanguage.GERMAN, "Atlantic ist ein Hersteller von Gl\u00FCcksspiel-Hardware."))
    logo(getImageBinary("Atlantic Inc. Logo", "logos/atlantik.png"))
}
manufacturers.add(EJBLookup.contentEngine.save(builder.content))

builder = new GroovyContentBuilder("manufacturer")
builder {
    name("Book Trading Unlimited")
    basedIn(new FxSelectOne(false, countries.getItemByData("gb")))
    description(new FxHTML(FxLanguage.ENGLISH, "Books and media distribution.")
                .setTranslation(FxLanguage.GERMAN, "Book Trading ist ein Buch- und Medienh\u00E4ndler."))
    logo(getImageBinary("Book Trading Unlimited Logo", "logos/bookcase.png"))
}
manufacturers.add(EJBLookup.contentEngine.save(builder.content))


// ...and create some products
int currentArticleNr = new Random().nextInt(50000)

[["camera", "Kamera"],
 ["pda", "pda"],
 ["printer", "Drucker"],
 ["tv", "Fernseher"]].each { product, label_de ->
    builder = new GroovyContentBuilder("product")
    final int manuIdx = new Random().nextInt(manufacturers.size())
    final FxPK manuPk = manufacturers.get(manuIdx)
    final FxString manuLabel = EJBLookup.contentEngine.load(manuPk).getValue("/name")
    builder {
        name(new FxString(FxLanguage.ENGLISH, StringUtils.capitalize(product)).setTranslation(FxLanguage.GERMAN, label_de))
        manufacturer(new FxReference(false, new ReferencedContent(manuPk)))
        description(new FxHTML(FxLanguage.ENGLISH, "The newest " + product + " by " + manuLabel + ".")
                    .setTranslation(FxLanguage.GERMAN, "Das neueste Modell '" + label_de + "' von " + manuLabel + "."))
        price((double) ((new Random().nextInt(500) + 10) / 10 * 10) - 0.01)
        image(getImageBinary(product, "products/${product}/1.png"))
        image(getImageBinary(product, "products/${product}/2.png"))
        image(getImageBinary(product, "products/${product}/3.png"))
        variant {
            articleNumber("A" + (currentArticleNr += new Random().nextInt(1000)))
            color(colors.getItemByData("white").id)
            image(getImageBinary(product, "products/${product}/1_white.png"))
        }
        variant {
            articleNumber("A" + (currentArticleNr += new Random().nextInt(1000)))
            color(colors.getItemByData("red").id)
            image(getImageBinary(product, "products/${product}/1_red.png"))
        }
        variant {
            articleNumber("A" + (currentArticleNr += new Random().nextInt(1000)))
            color(colors.getItemByData("blue").id)
            image(getImageBinary(product, "products/${product}/1_blue.png"))
        }
    }
    EJBLookup.contentEngine.save(builder.content)
}