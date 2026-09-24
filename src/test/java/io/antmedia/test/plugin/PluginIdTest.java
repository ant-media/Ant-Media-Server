package io.antmedia.test.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.antmedia.plugin.api.PluginId;

public class PluginIdTest {

    @Test
    public void testIsValid_accepted() {
        assertTrue(PluginId.isValid("clip-creator"));
        assertTrue(PluginId.isValid("my_plugin"));
        assertTrue(PluginId.isValid("plugin123"));
        assertTrue(PluginId.isValid("3"));
        assertTrue(PluginId.isValid("clip-creator-3.0.0"));
    }

    @Test
    public void testIsValid_rejected() {
        assertFalse(PluginId.isValid(null));
        assertFalse(PluginId.isValid(""));
        assertFalse(PluginId.isValid("Clip-Creator"));
        assertFalse(PluginId.isValid("plugin name"));
        assertFalse(PluginId.isValid("plugin/name"));
        assertFalse(PluginId.isValid("-leading-hyphen"));
        assertFalse(PluginId.isValid(".leading-dot"));
    }

    /** The id becomes a filename and a URL segment, so no traversal may be constructible. */
    @Test
    public void testIsValid_rejectsTraversal() {
        assertFalse(PluginId.isValid(".."));
        assertFalse(PluginId.isValid("../etc/passwd"));
        assertFalse(PluginId.isValid("a..b"));
        assertFalse(PluginId.isValid("..a"));
    }

    @Test
    public void testIsValid_lengthBoundary() {
        assertTrue(PluginId.isValid("a".repeat(64)));
        assertFalse(PluginId.isValid("a".repeat(65)));
    }

    /** Drops the "-plugin" suffix so a manifest name maps onto the catalog id. */
    @Test
    public void testFromName_dropsPluginSuffix() {
        assertEquals("clip-creator", PluginId.fromName("Clip Creator Plugin"));
        assertEquals("my", PluginId.fromName("My Plugin"));
        assertEquals("filter", PluginId.fromName("Filter Plugin"));
    }

    @Test
    public void testFromName_withoutSuffix() {
        assertEquals("media-push", PluginId.fromName("Media Push"));
        assertEquals("abc", PluginId.fromName("  --abc--  "));
    }

    @Test
    public void testFromName_nullAndEmpty() {
        assertEquals("", PluginId.fromName(null));
        assertEquals("", PluginId.fromName("!!!"));
        assertEquals("", PluginId.fromName(""));
    }

    /** Whatever fromName derives must itself be a usable id. */
    @Test
    public void testFromName_producesValidId() {
        assertTrue(PluginId.isValid(PluginId.fromName("Clip Creator Plugin")));
        assertTrue(PluginId.isValid(PluginId.fromName("Some !!! Weird @@ Name")));
    }
}
