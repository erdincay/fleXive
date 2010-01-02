/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.shared.FxSharedUtils;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


class ExcelExtractor  implements POIFSReaderListener {

    private FxSummaryInformation fxsi = null;
    ByteArrayOutputStream writer=null;

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
     * Extracts the text informations from the excel file.
     *
     * @param in the input stream to read from
     * @return the extraxted informations, or null if no text extraction was possible
     */
    public ExtractedData extract(final InputStream in) {


        BufferedInputStream bis=null;
        try {

            writer = new ByteArrayOutputStream();

            // We need to read the stream 2 times, so we use a buffered input stream and mark the
            // beginning
            bis = new BufferedInputStream(in);
            bis.mark(Integer.MAX_VALUE);

            // Retrieve summary information
            POIFSReader r = new POIFSReader();
            r.registerListener(this,"\005SummaryInformation");
            r.read(bis);
            bis.reset();

            // Retrieve text by processing all sheets
            HSSFWorkbook wb = new HSSFWorkbook(bis);
            for (int i=0;i<wb.getNumberOfSheets();i++) {
                HSSFSheet sheet = wb.getSheetAt(i);
                processSheet(sheet);
            }

            // Append summary info to text
            if (fxsi!=null) {
                writer.write(FxSharedUtils.getBytes(fxsi.getFTIndexInformations()));
            }
            writer.flush();

            return new ExtractedData(fxsi,writer.toString());
        } catch (Exception exc) {
            exc.printStackTrace();
            return null;
        } finally {
            try { if (writer!=null) writer.close(); } catch(Exception exc) {/*ignore*/}
            try { if (bis!=null) bis.close(); } catch(Exception exc) {/*ignore*/}
        }
    }


    private void processSheet(HSSFSheet sheet)  {
        try {
            // Use the HFFS functions for the number of rows & columns
            int rowCount = sheet.getPhysicalNumberOfRows();
            int colCount = sheet.getRow(0).getPhysicalNumberOfCells();
            HSSFRow row;
            HSSFCell cell;
            String cellValue;
            for(int i = 0; i < rowCount; i++) {
                row = sheet.getRow(i);
                for(short j = 0; j <colCount; j++) {
                    cell = row.getCell(j);
                    if (cell!=null) {
                        try {
                            switch(cell.getCellType()) {
                                case HSSFCell.CELL_TYPE_BOOLEAN:
                                    cellValue = String.valueOf(cell.getBooleanCellValue());
                                    break;
                                case HSSFCell.CELL_TYPE_NUMERIC:
                                    cellValue = String.valueOf(cell.getNumericCellValue());
                                    break;
                                case HSSFCell.CELL_TYPE_FORMULA:
                                    // Doesnt make much sense to index a cell formula
                                    cellValue = "";
                                    break;
                                case HSSFCell.CELL_TYPE_ERROR:
                                    cellValue = String.valueOf(cell.getErrorCellValue());
                                    break;
                                case HSSFCell.CELL_TYPE_BLANK:
                                    cellValue = "";
                                    break;
                                default:
                                    cellValue=cell.getStringCellValue();
                            }
                        } catch(Exception exc) {
                            cellValue = "";
                        }
                        writer.write(FxSharedUtils.getBytes(cellValue));
                    }
                }
            }
        }
        catch (Exception eN) {
            System.out.println("Error reading sheet:"+eN.toString());
        }
    }


}
