package com.bemobi.aicontrol.integration.google;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleWorkspacePropertiesTest {

    @Test
    void testDefaultValues() {
        GoogleWorkspaceProperties props = new GoogleWorkspaceProperties();

        assertFalse(props.isEnabled());
        assertNull(props.getCredentials());
        assertNull(props.getDomain());
        assertNull(props.getAdminEmail());
        assertEquals("custom", props.getCustomSchema());
        assertEquals("git_name", props.getGitNameField());
        assertEquals(30000, props.getTimeout());
    }

    @Test
    void testSettersAndGetters() {
        GoogleWorkspaceProperties props = new GoogleWorkspaceProperties();

        props.setEnabled(true);
        props.setCredentials("/path/to/creds.json");
        props.setDomain("example.com");
        props.setAdminEmail("admin@example.com");
        props.setCustomSchema("myschema");
        props.setGitNameField("github_user");
        props.setTimeout(60000);

        assertTrue(props.isEnabled());
        assertEquals("/path/to/creds.json", props.getCredentials());
        assertEquals("example.com", props.getDomain());
        assertEquals("admin@example.com", props.getAdminEmail());
        assertEquals("myschema", props.getCustomSchema());
        assertEquals("github_user", props.getGitNameField());
        assertEquals(60000, props.getTimeout());
    }
}
