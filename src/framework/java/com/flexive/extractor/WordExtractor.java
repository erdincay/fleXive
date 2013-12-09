/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.extractor;

import org.apache.poi.hdf.extractor.WordDocument;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;


class WordExtractor implements POIFSReaderListener {

    private FxSummaryInformation fxsi = null;

    /**
     * Proccesses the Summary section.
     *
     * @param event the summary section event.
     */
    public void processPOIFSReaderEvent(POIFSReaderEvent event) {
        try {
            SummaryInformation si = (SummaryInformation) PropertySetFactory.create(event.getStream());
            fxsi = new FxSummaryInformation(si);
        } catch (Exception ex) {
            //
        }
    }

    /**
     * Extracts the text informations from the word file.
     *
     * @param in the input stream to read from
     * @return the extraxted informations, or null if no text extraction was possible
     */
    public ExtractedData extract(final InputStream in) {
        ByteArrayOutputStream baos=null;
        PrintWriter writer=null;
        BufferedInputStream bis=null;
        try {

            baos = new ByteArrayOutputStream();
            writer = new PrintWriter(baos);

            // We need to read the stream 2 times, so we use a buffered input stream and mark the
            // beginning
            bis = new BufferedInputStream(in);
            bis.mark(Integer.MAX_VALUE);

            // Retrieve summary information
            POIFSReader r = new POIFSReader();
            r.registerListener(this,"\005SummaryInformation");
            r.read(bis);
            bis.reset();

            // Retrieve text
            WordDocument wd = new WordDocument(bis);
            wd.writeAllText(writer);
            if (fxsi!=null) {
                writer.write(fxsi.getFTIndexInformations());
            }
            writer.flush();

            return new ExtractedData(fxsi,baos.toString());
        } catch (Exception exc) {
            return null;
        } finally {
            try { if (writer!=null) writer.close(); } catch(Exception exc) {/*ignore*/}
            try { if (baos!=null) baos.close(); } catch(Exception exc) {/*ignore*/}
            try { if (bis!=null) bis.close(); } catch(Exception exc) {/*ignore*/}
        }
    }

}