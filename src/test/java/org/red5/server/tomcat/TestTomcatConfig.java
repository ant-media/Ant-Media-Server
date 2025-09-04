import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import org.junit.Test;

public class TestTomcatConfig {

    @Test
    public void testExpectedEntriesExist() throws Exception {
        File xmlFile = new File("src/main/server/conf/jee-container.xml");

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        XPath xpath = XPathFactory.newInstance().newXPath();

        String[] expressions = {
            "//entry[@key='keepAliveTimout' and @value='-1']",
            "//entry[@key='disableUploadTimeout' and @value='false']",
            "//entry[@key='connectionTimeout' and @value='-1']",
            "//entry[@key='connectionUploadTimeout' and @value='-1']"
        };

        for (String expr : expressions) {
            Node result = (Node) xpath.evaluate(expr, doc, XPathConstants.NODE);
            assertNotNull(result);
        }    
  }
}
