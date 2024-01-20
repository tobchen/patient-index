package de.tobchen.health.patientindex.util;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.UriType;
import org.springframework.lang.Nullable;

import ca.uhn.fhir.util.UrlUtil;

public class ReferenceUtils {
    public static @Nullable IIdType idFromLiteralReference(Reference reference)
    {
        IIdType id;

        var literalReference = reference.getReference();
        if (literalReference != null)
        {
            if (UrlUtil.isAbsolute(literalReference))
            {
                id = new IdType(new UriType(literalReference));
            }
            else
            {
                var parts = UrlUtil.parseUrl(literalReference);
                id = new IdType(parts.getResourceType(), parts.getResourceId(), parts.getVersionId());
            }
        }
        else
        {
            id = null;
        }

        return id;
    }
}
