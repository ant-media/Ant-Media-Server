package io.antmedia.test;

import io.antmedia.datastore.db.types.VoDIdStreamIdPair;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class VoDIdStreamIdPairUnitTest extends AbstractJUnit4SpringContextTests {

    @Test
    public void getterAndSetterIndeedWork() {
        String voDId = "vodId";
        String streamId = "streamId";

        VoDIdStreamIdPair voDIdStreamIdPair0 = new VoDIdStreamIdPair();

        voDIdStreamIdPair0.setVoDId(voDId);
        assertEquals(voDId, voDIdStreamIdPair0.getVoDId());

        voDIdStreamIdPair0.setStreamId(streamId);
        assertEquals(streamId, voDIdStreamIdPair0.getStreamId());

        VoDIdStreamIdPair voDIdStreamIdPair1 = new VoDIdStreamIdPair(streamId, voDId);
        assertEquals(voDId, voDIdStreamIdPair1.getVoDId());
        assertEquals(streamId, voDIdStreamIdPair1.getStreamId());
    }
}
