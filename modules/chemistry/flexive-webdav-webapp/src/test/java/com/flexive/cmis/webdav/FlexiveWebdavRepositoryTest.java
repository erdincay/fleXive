package com.flexive.cmis.webdav;

import com.flexive.chemistry.webdav.tests.WebdavTestCase;
import com.flexive.chemistry.webdav.ChemistryResourceFactory;
import org.apache.chemistry.Repository;
import com.flexive.cmis.TestFixture;
import com.flexive.cmis.spi.FlexiveRepository;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class FlexiveWebdavRepositoryTest extends WebdavTestCase {
    @Override
    protected Repository getRepository() {
        new TestFixture();
        return new FlexiveRepository("");
    }

    @Override
    protected ChemistryResourceFactory getResourceFactory(Repository repository) {
        return new FlexiveResourceFactory();
    }
}
