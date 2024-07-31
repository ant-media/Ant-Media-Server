/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Misc XML utils
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class XMLUtils {
    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(XMLUtils.class);

    /**
     * Converts string representation of XML into Document
     * 
     * @param str
     *            String representation of XML
     * @return DOM object
     * @throws IOException
     *             I/O exception
     */
    public static Document stringToDoc(String str) throws IOException {
        if (StringUtils.isNotEmpty(str)) {
            try (Reader reader = new StringReader(str)) {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.parse(new InputSource(reader));

                return doc;
            } catch (Exception ex) {
                log.debug("String: {}", str);
                throw new IOException(String.format("Error converting from string to doc %s", ex.getMessage()));
            }
        } else {
            throw new IOException("Error - could not convert empty string to doc");
        }
    }

    /**
     * Converts doc to String
     * 
     * @param dom
     *            DOM object to convert
     * @return XML as String
     */
    public static String docToString(Document dom) {
        return XMLUtils.docToString1(dom);
    }

    /**
     * Convert a DOM tree into a String using Dom2Writer
     * 
     * @return XML as String
     * @param dom
     *            DOM object to convert
     */
    public static String docToString1(Document dom) {
        StringWriter sw = new StringWriter();
        DOM2Writer.serializeAsXML(dom, sw);
        return sw.toString();
    }

    /**
     * Convert a DOM tree into a String using transform
     * 
     * @param domDoc
     *            DOM object
     * @throws java.io.IOException
     *             I/O exception
     * @return XML as String
     */
    public static String docToString2(Document domDoc) throws IOException {
        try {
            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter sw = new StringWriter();
            Result result = new StreamResult(sw);
            trans.transform(new DOMSource(domDoc), result);
            return sw.toString();
        } catch (Exception ex) {
            throw new IOException(String.format("Error converting from doc to string %s", ex.getMessage()));
        }
    }

}
