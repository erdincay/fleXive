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
package com.flexive.war.beans.admin;

import com.flexive.shared.FxSharedUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic RSS provider bean (used on the start page). Feeds are cached in the session.
 *
 * <h3>Usage:</h3>
 * {@code #{rssProviderBean.feed['http://blog.flexive.org/feed/']}}
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.0.2
 */
public class RssProviderBean {
    private static final Log LOG = LogFactory.getLog(RssProviderBean.class);

    /**
     * Maximum number of items of a blog feed
     */
    private static final int MAX_ITEMS = 5;

    public static class RssEntry implements Serializable {
        private static final long serialVersionUID = 1122438657219678458L;
        private final String title;
        private final String link;

        public RssEntry(String title, String link) {
            this.title = title;
            this.link = link;
        }

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }
    }

    // cached feeds
    private final Map<String, List<RssEntry>> feeds = new HashMap<String, List<RssEntry>>();
    // Map function that returns RssEntries for String URLs
    private final Map<String, List<RssEntry>> feedMapper =
            FxSharedUtils.getMappedFunction(
                    new FxSharedUtils.ParameterMapper<String, List<RssEntry>>() {
                        private static final long serialVersionUID = -3824115213705113244L;

                        public synchronized List<RssEntry> get(Object key) {
                            if (key == null) {
                                return null;
                            }
                            final String url = key.toString();
                            if (!feeds.containsKey(url)) {
                                feeds.put(url, fetchFeed(url, MAX_ITEMS));
                            }
                            return feeds.get(url);
                        }
                    });

    /**
     * Return a map that returns the items for a given feed URL, e.g.:
     * <p>
     * {@code #{rssProviderBean.feed['http://blog.flexive.org/feed/']}}
     * </p>
     *
     * @return  a map that returns the items for a given feed URL.
     */
    public Map<String, List<RssEntry>> getFeed() {
        return feedMapper;
    }

    /**
     * Fetch and parse a news feed (currently only tested with blog.flexive.org).
     *
     * @param url the feed URL
     * @param maxItems the maximum number of items returned
     * @return the news items
     */
    private List<RssEntry> fetchFeed(String url, int maxItems) {
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            // open url
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setConnectTimeout(2000);
            in = urlConnection.getInputStream();

            // parse RSS feed
            final DocumentBuilder domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = domBuilder.parse(new InputSource(in));

            // iterate over items
            final NodeList items = doc.getElementsByTagName("item");
            final int numItems = Math.min(items.getLength(), maxItems);
            final List<RssEntry> result = new ArrayList<RssEntry>(numItems);
            for (int i = 0; i < numItems; i++) {
                final NodeList childNodes = items.item(i).getChildNodes();
                String title = null;
                String link = null;
                // find title and link children
                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node child = childNodes.item(j);
                    if ("title".equals(child.getNodeName())) {
                        title = child.getTextContent();
                        if (link != null) {
                            break;
                        }
                    } else if ("link".equals(child.getNodeName())) {
                        link = child.getTextContent();
                        if (title != null) {
                            break;
                        }
                    }
                }
                if (title != null && link != null) {
                    result.add(new RssEntry(title, link));
                }
            }
            return result;

        } catch (IOException e) {
            LOG.error("Failed to fetch stream from " + url + ": " + e.getMessage(), e);
            return new ArrayList<RssEntry>(0);
        } catch (SAXException e) {
            LOG.error("Failed to parse XML stream: " + url + ": " + e.getMessage(), e);
            return new ArrayList<RssEntry>(0);
        } catch (ParserConfigurationException e) {
            LOG.error("Failed to create parser: " + url + ": " + e.getMessage(), e);
            return new ArrayList<RssEntry>(0);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close input stream: " + e.getMessage(), e);
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
