/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.tests.embedded;

import com.flexive.core.IMParser;
import com.flexive.shared.FxSharedUtils;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * Test for ImageMagick/ImageParser
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"image", "imageParser"})
public class ImageParserTest {

    private static String testData =
            "Image: IMG_0048.JPG\n" +
                    "  Format: JPEG (Joint Photographic Experts Group JFIF format)\n" +
                    "  Class: DirectClass\n" +
                    "  Geometry: 2048x1536+0+0\n" +
                    "  Type: TrueColor\n" +
                    "  Endianess: Undefined\n" +
                    "  Colorspace: RGB\n" +
                    "  Channel depth:\n" +
                    "    Red: 8-bit\n" +
                    "    Green: 8-bit\n" +
                    "    Blue: 8-bit\n" +
                    "  Channel statistics:\n" +
                    "    Red:\n" +
                    "      Min: 0 (0)\n" +
                    "      Max: 255 (1)\n" +
                    "      Mean: 115.399 (0.452545)\n" +
                    "      Standard deviation: 88.9127 (0.348677)\n" +
                    "    Green:\n" +
                    "      Min: 0 (0)\n" +
                    "      Max: 255 (1)\n" +
                    "      Mean: 82.4621 (0.323381)\n" +
                    "      Standard deviation: 60.6466 (0.23783)\n" +
                    "    Blue:\n" +
                    "      Min: 0 (0)\n" +
                    "      Max: 255 (1)\n" +
                    "      Mean: 66.7881 (0.261914)\n" +
                    "      Standard deviation: 53.3319 (0.209145)\n" +
                    "  Colors: 272549\n" +
                    "  Rendering intent: Undefined\n" +
                    "  Resolution: 180x180\n" +
                    "  Units: PixelsPerInch\n" +
                    "  Filesize: 840.578kb\n" +
                    "  Interlace: None\n" +
                    "  Background color: white\n" +
                    "  Border color: rgb(223,223,223)\n" +
                    "  Matte color: grey74\n" +
                    "  Transparent color: black\n" +
                    "  Page geometry: 2048x1536+0+0\n" +
                    "  Dispose: Undefined\n" +
                    "  Iterations: 0\n" +
                    "  Compression: JPEG\n" +
                    "  Quality: 90\n" +
                    "  Orientation: TopLeft\n" +
                    "  Exif:DateTime: 2006:08:07 22:27:24\n" +
                    "  Exif:Make: Canon\n" +
                    "  Exif:Model: Canon PowerShot A400\n" +
                    "  Exif:Orientation: 1\n" +
                    "  Exif:ResolutionUnit: 2\n" +
                    "  Exif:XResolution: 180/1\n" +
                    "  Exif:YCbCrPositioning: 1\n" +
                    "  Exif:YResolution: 180/1\n" +
                    "  Jpeg:colorspace: 2\n" +
                    "  Jpeg:sampling-factor: 2x1,1x1,1x1\n" +
                    "  Signature: 8af18c5d3e9992092fdf56d60bde44ba3380ff29aed343d8fa4eea33bb0af8ea\n" +
                    "  Unknown: 196\n" +
                    "  Profile-exif: 8700 bytes\n" +
                    "  Tainted: False\n" +
                    "  User time: 0.141u\n" +
                    "  Elapsed time: 0:01\n" +
                    "  Pixels per second: 14.7784mb\n" +
                    "  Version: ImageMagick 6.3.4 05/11/07 Q16 http://www.imagemagick.org";

    private static final String TEST_RESULT = "<?xml version=\"1.0\" ?><Image source=\"IMG_0048.JPG\"><Format>JPEG (Joint Photographic Experts Group JFIF format)</Format><Class>DirectClass</Class><Geometry>2048x1536+0+0</Geometry><Type>TrueColor</Type><Endianess>Undefined</Endianess><Colorspace>RGB</Colorspace><Channel-depth><Red>8-bit</Red><Green>8-bit</Green><Blue>8-bit</Blue></Channel-depth><Channel-statistics><Red><Min>0 (0)</Min><Max>255 (1)</Max><Mean>115.399 (0.452545)</Mean><Standard-deviation>88.9127 (0.348677)</Standard-deviation></Red><Green><Min>0 (0)</Min><Max>255 (1)</Max><Mean>82.4621 (0.323381)</Mean><Standard-deviation>60.6466 (0.23783)</Standard-deviation></Green><Blue><Min>0 (0)</Min><Max>255 (1)</Max><Mean>66.7881 (0.261914)</Mean><Standard-deviation>53.3319 (0.209145)</Standard-deviation></Blue></Channel-statistics><Colors>272549</Colors><Rendering-intent>Undefined</Rendering-intent><Resolution>180x180</Resolution><Units>PixelsPerInch</Units><Filesize>840.578kb</Filesize><Interlace>None</Interlace><Background-color>white</Background-color><Border-color>rgb(223,223,223)</Border-color><Matte-color>grey74</Matte-color><Transparent-color>black</Transparent-color><Page-geometry>2048x1536+0+0</Page-geometry><Dispose>Undefined</Dispose><Iterations>0</Iterations><Compression>JPEG</Compression><Quality>90</Quality><Orientation>TopLeft</Orientation><Exif-DateTime>2006:08:07 22:27:24</Exif-DateTime><Exif-Make>Canon</Exif-Make><Exif-Model>Canon PowerShot A400</Exif-Model><Exif-Orientation>1</Exif-Orientation><Exif-ResolutionUnit>2</Exif-ResolutionUnit><Exif-XResolution>180/1</Exif-XResolution><Exif-YCbCrPositioning>1</Exif-YCbCrPositioning><Exif-YResolution>180/1</Exif-YResolution><Jpeg-colorspace>2</Jpeg-colorspace><Jpeg-sampling-factor>2x1,1x1,1x1</Jpeg-sampling-factor><Signature>8af18c5d3e9992092fdf56d60bde44ba3380ff29aed343d8fa4eea33bb0af8ea</Signature><Unknown>196</Unknown><Profile-exif>8700 bytes</Profile-exif><Tainted>False</Tainted><User-time>0.141u</User-time><Elapsed-time>0:01</Elapsed-time><Pixels-per-second>14.7784mb</Pixels-per-second><Version>ImageMagick 6.3.4 05/11/07 Q16 http://www.imagemagick.org</Version></Image>";
    private static final String TEST_FILE_EXIF = "src" + File.separatorChar + "framework" + File.separatorChar + "testresources" + File.separatorChar + "image" + File.separatorChar + "Exif.JPG";
    private static final String TEST_FILE_GIF = "src" + File.separatorChar + "framework" + File.separatorChar + "testresources" + File.separatorChar + "image" + File.separatorChar + "GIF_Image.GIF";

    @Test(dependsOnMethods = {"identifyTest"})
    public void parserTest() throws Exception {
        assert IMParser.parse(new ByteArrayInputStream(FxSharedUtils.getBytes(testData))).startsWith(TEST_RESULT);
        FxSharedUtils.ProcessResult res = FxSharedUtils.executeCommand("identify", "-verbose", TEST_FILE_EXIF);
        String result = IMParser.parse(new ByteArrayInputStream(FxSharedUtils.getBytes(res.getStdOut())));
        assert result.indexOf("<Profile-exif>4604 bytes</Profile-exif>") > 0;
        assert result.indexOf("<Geometry>2048x1536+0+0</Geometry>") > 0;
        assert result.indexOf("<Channel-depth><Red>8-bit</Red><Green>8-bit</Green>") > 0;
        res = FxSharedUtils.executeCommand("identify", "-verbose", TEST_FILE_GIF);
        result = IMParser.parse(new ByteArrayInputStream(FxSharedUtils.getBytes(res.getStdOut())));
        assert result.indexOf("<Signature>2f0ab5863f31c675bab16309bfa161fd331e55e6c6781ec1f6cd0305f725cfa9</Signature>") > 0;
        assert result.indexOf("<Compression>LZW</Compression>") > 0;
    }

    @Test
    public void identifyTest() throws Exception {
        FxSharedUtils.ProcessResult res = FxSharedUtils.executeCommand("identify", "-version");
        System.out.println("Commandline: [" + res.getCommandLine() + "]");
        System.out.println("ExitCode: " + res.getExitCode());
        System.out.println("Error: [" + res.getStdErr() + "]");
        System.out.println("Out: [" + res.getStdOut() + "]");
        if (res.getExitCode() != 0 || res.getStdOut().indexOf("ImageMagick") <= 0) {
            assert false : "Please install ImageMagick (and add it to your path) for imaging tests to work!";
        }
    }
}
