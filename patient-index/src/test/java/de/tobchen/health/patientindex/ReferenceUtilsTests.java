package de.tobchen.health.patientindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.r5.model.Reference;
import org.junit.jupiter.api.Test;

import de.tobchen.health.patientindex.util.ReferenceUtils;

public class ReferenceUtilsTests
{
    @Test
    public void getIdFromAbsoluteVersionedReference()
    {
        var reference = new Reference("http://test.org/fhir/Patient/10/_history/55");
        var id = ReferenceUtils.idFromLiteralReference(reference);
        assertNotNull(id);
        assertEquals("http://test.org/fhir", id.getBaseUrl());
        assertEquals("Patient", id.getResourceType());
        assertEquals("10", id.getIdPart());
        assertEquals("55", id.getVersionIdPart());
    }

    @Test
    public void getIdFromAbsoluteReference()
    {
        var reference = new Reference("http://test.org/fhir/Patient/10");
        var id = ReferenceUtils.idFromLiteralReference(reference);
        assertNotNull(id);
        assertEquals("http://test.org/fhir", id.getBaseUrl());
        assertEquals("Patient", id.getResourceType());
        assertEquals("10", id.getIdPart());
        assertNull(id.getVersionIdPart());
    }

    @Test
    public void getIdFromRelativeVersionedReference()
    {
        var reference = new Reference("Patient/10/_history/55");
        var id = ReferenceUtils.idFromLiteralReference(reference);
        assertNotNull(id);
        assertNull(id.getBaseUrl());
        assertEquals("Patient", id.getResourceType());
        assertEquals("10", id.getIdPart());
        assertEquals("55", id.getVersionIdPart());
    }

    @Test
    public void getIdFromRelativeReference()
    {
        var reference = new Reference("Patient/10");
        var id = ReferenceUtils.idFromLiteralReference(reference);
        assertNotNull(id);
        assertNull(id.getBaseUrl());
        assertEquals("Patient", id.getResourceType());
        assertEquals("10", id.getIdPart());
        assertNull(id.getVersionIdPart());
    }
}
