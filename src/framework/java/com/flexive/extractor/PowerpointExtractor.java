/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.shared.FxSharedUtils;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.util.LittleEndian;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


class PowerpointExtractor implements POIFSReaderListener {
    private FxSummaryInformation fxsi;
    private ByteArrayOutputStream writer;

    /**
     * Extracts the text informations from the powerpoint file.
     *
     * @param in the input stream to read from
     * @return the extraxted informations, or null if no text extraction was possible
     */
    public ExtractedData extract(final InputStream in) {
        try {
            writer = new ByteArrayOutputStream();
            POIFSReader reader = new POIFSReader();
            reader.registerListener(this);
            //FxSummaryInformation.getSummaryInformation(fileName);
            reader.read(in);
            if (fxsi!=null) {
                writer.write(FxSharedUtils.getBytes(fxsi.getFTIndexInformations()));
            }
            writer.flush();
            return new ExtractedData(fxsi,writer.toString());
        } catch (Exception ex) {
            return null;
        } finally{
            try { writer.close(); } catch(Exception exc) {/*ignore*/}
        }
    }


    private void processContent(byte[] buffer,int beginIndex, int endIndex) {
        while (beginIndex < endIndex) {
            int containerFlag = LittleEndian.getUShort(buffer, beginIndex);
            int recordType = LittleEndian.getUShort(buffer, beginIndex + 2);
            long recordLength = LittleEndian.getUInt(buffer, beginIndex + 4);
            beginIndex += 8;
            if ((containerFlag & 0x0f) == 0x0f) {
                processContent(buffer, beginIndex, beginIndex + (int)recordLength);
            } else if (recordType == 4008) {
                writer.write(buffer, beginIndex, (int)recordLength);
                writer.write(' ');
            }
            beginIndex += (int)recordLength;
        }
    }

    public void processPOIFSReaderEvent(POIFSReaderEvent event) {
        try{
            if(event.getName().equalsIgnoreCase("PowerPoint Document")) {
                DocumentInputStream input = event.getStream();
                byte[] buffer = new byte[input.available()];
                //noinspection ResultOfMethodCallIgnored
                input.read(buffer, 0, input.available());
                processContent(buffer,0, buffer.length);
            } else if (event.getName().equals("\005SummaryInformation")) {
                SummaryInformation si = (SummaryInformation) PropertySetFactory.create(event.getStream());
                fxsi = new FxSummaryInformation(si);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
