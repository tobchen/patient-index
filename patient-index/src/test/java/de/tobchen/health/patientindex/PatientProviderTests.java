package de.tobchen.health.patientindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Patient.LinkType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import de.tobchen.health.patientindex.model.repositories.PatientRepository;
import de.tobchen.health.patientindex.services.PatientProvider;
import io.opentelemetry.api.OpenTelemetry;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestInstance(Lifecycle.PER_CLASS)
public class PatientProviderTests
{
    @Autowired
    private PatientRepository repository;

    private PatientProvider provider;

    @BeforeAll
    public void initTests()
    {
        provider = new PatientProvider(OpenTelemetry.noop(), repository);
    }

    @Test
    public void createWithIdAndGetById()
    {
        var result = provider.update(new IdType("test-id"), null,
            createPatient("test-id"));
        assertAll(result, "test-id", true);

        var outputPatient = provider.read(new IdType("test-id"));
        assertAll(outputPatient, "test-id");
    }

    @Test
    public void createWithoutIdAndGetById()
    {
        var createOutcome = provider.create(createPatient());
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var outputPatient = provider.read(id);
        assertNotNull(outputPatient);
        assertAll(outputPatient, id.getIdPart());
    }

    @Test
    public void createWithOneIdentifierAndGetById()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "one-id-get-by-id");

        var createOutcome = provider.create(createPatient(identifierOne));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var outputPatient = provider.read(id);
        assertNotNull(outputPatient);
        assertAll(outputPatient, id.getIdPart(), identifierOne);
    }

    @Test
    public void createWithOneIdentifierAndGetByIdentifier()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "one-id-get-by-identifier");

        var createOutcome = provider.create(createPatient(identifierOne));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var patients = provider.searchByIdentifier(
            new TokenParam(identifierOne.system(), identifierOne.value()));
        assertNotNull(patients);
        assertEquals(1, patients.size());
        var patient = patients.get(0);
        assertAll(patient, id.getIdPart(), identifierOne);
    }

    @Test
    public void createWithOneIdentifierThenAddAnother()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "one-id-add-another-1");
        var identifierTwo = new Identifier("urn:oid:11.12.13", "one-id-add-another-2");

        var createOutcome = provider.create(createPatient(identifierOne));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var beforePatient = provider.read(id);
        assertNotNull(beforePatient);
        assertAll(beforePatient, id.getIdPart(), identifierOne);

        var updateOutcome = provider.update(new IdType(id.getIdPart()), null,
            createPatient(id.getIdPart(), identifierOne, identifierTwo));
        assertAll(updateOutcome, false);
        assertEquals(id.getIdPart(), updateOutcome.getId().getIdPart());

        var afterPatient = provider.read(id);
        assertNotNull(afterPatient);
        assertAll(afterPatient, id.getIdPart(), identifierOne, identifierTwo);
    }

    @Test
    public void createWithTwoIdentifiersThenRemoveOne()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "two-ids-remove-one-1");
        var identifierTwo = new Identifier("urn:oid:11.12.13", "two-ids-remove-one-2");

        var createOutcome = provider.create(createPatient(identifierOne, identifierTwo));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var beforePatient = provider.read(id);
        assertNotNull(beforePatient);
        assertAll(beforePatient, id.getIdPart(), identifierOne, identifierTwo);

        var updateOutcome = provider.update(new IdType(id.getIdPart()), null,
            createPatient(id.getIdPart(), identifierOne));
        assertAll(updateOutcome, false);
        assertEquals(id.getIdPart(), updateOutcome.getId().getIdPart());

        var afterPatient = provider.read(id);
        assertNotNull(afterPatient);
        assertAll(afterPatient, id.getIdPart(), identifierOne);
    }

    @Test
    public void createWithTwoIdentifiersReplaceOne()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "two-ids-replace-one-1");
        var identifierTwo = new Identifier("urn:oid:11.12.13", "two-ids-replace-one-2");
        var identifierThree = new Identifier("urn:oid:11.12.13", "two-ids-replace-one-3");

        var createOutcome = provider.create(createPatient(identifierOne, identifierTwo));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var beforePatient = provider.read(id);
        assertNotNull(beforePatient);
        assertAll(beforePatient, id.getIdPart(), identifierOne, identifierTwo);

        var updateOutcome = provider.update(new IdType(id.getIdPart()), null,
            createPatient(id.getIdPart(), identifierTwo, identifierThree));
        assertAll(updateOutcome, false);
        assertEquals(id.getIdPart(), updateOutcome.getId().getIdPart());

        var afterPatient = provider.read(id);
        assertNotNull(afterPatient);
        assertAll(afterPatient, id.getIdPart(), identifierTwo, identifierThree);
    }

    @Test
    public void createWithFourIdentifierAndGetByIdentifier()
    {
        var identifierOne = new Identifier("urn:oid:11.12.13", "four-id-get-by-identifier-1");
        var identifierTwo = new Identifier("urn:oid:11.12.13", "four-id-get-by-identifier-2");
        var identifierThree = new Identifier("urn:oid:11.12.13", "four-id-get-by-identifier-3");
        var identifierFour = new Identifier("urn:oid:11.12.13", "four-id-get-by-identifier-4");

        var createOutcome = provider.create(
            createPatient(identifierOne, identifierTwo, identifierThree, identifierFour));
        assertAll(createOutcome, true);
        
        var id = createOutcome.getId();
        assertNotNull(id);

        var patients = provider.searchByIdentifier(
            new TokenParam(identifierThree.system(), identifierThree.value()));
        assertNotNull(patients);
        assertEquals(1, patients.size());
        var patient = patients.get(0);
        assertAll(patient, id.getIdPart(),
            identifierOne, identifierTwo, identifierThree, identifierFour);
    }

    @Test
    public void merge()
    {
        var sourceCreateOutcome = provider.create(createPatient());
        assertAll(sourceCreateOutcome, true);
        
        var sourceId = sourceCreateOutcome.getId();
        assertNotNull(sourceId);

        var targetCreateOutcome = provider.create(createPatient());
        assertAll(targetCreateOutcome, true);
        
        var targetId = targetCreateOutcome.getId();
        assertNotNull(targetId);

        // TODO Check parameters
        provider.merge(new Reference(sourceId), new Reference(targetId));

        var outputPatient = provider.read(sourceId);
        assertNotNull(outputPatient);
        assertAll(outputPatient, sourceId.getIdPart());

        var linkList = outputPatient.getLink();
        assertNotNull(linkList);
        assertEquals(1, linkList.size());
        var link = linkList.get(0);
        assertNotNull(link);
        assertEquals(LinkType.REPLACEDBY, link.getType());
        var other = link.getOther();
        assertNotNull(other);
        var otherIdType = other.getReferenceElement();
        assertNotNull(otherIdType);
        assertEquals(targetId.getIdPart(), otherIdType.getIdPart());
    }

    private Patient createPatient(Identifier... identifiers)
    {
        return createPatient(null, identifiers);
    }

    private Patient createPatient(String id, Identifier... identifiers)
    {
        var patient = new Patient();
        if (id != null)
        {
            patient.setId(id);
        }
        for (var identifier : identifiers)
        {
            patient.addIdentifier().setSystem(identifier.system()).setValue(identifier.value());
        }
        return patient;
    }

    private void assertAll(Patient patient, String id, Identifier... identifiers)
    {
        assertNotNull(patient);
        assertEquals(id, patient.getIdElement().getIdPart());
        var patientIdentifiers = patient.getIdentifier();
        if (identifiers.length == 0)
        {
            assertTrue(patientIdentifiers == null || patientIdentifiers.size() == 0);
        }
        else
        {
            assertNotNull(patientIdentifiers);
            assertEquals(identifiers.length, patientIdentifiers.size());
            for (var identifier : identifiers)
            {
                boolean identifierFound = false;
                for (var patientIdentifier : patientIdentifiers)
                {
                    identifierFound = identifierFound
                        || (identifier.system().equals(patientIdentifier.getSystem())
                            && identifier.value().equals(patientIdentifier.getValue()));
                }
                assertTrue(identifierFound);
            }
        }
    }

    private void assertAll(MethodOutcome outcome, boolean created)
    {
        assertAll(outcome, null, created);
    }

    private void assertAll(MethodOutcome outcome, String id, boolean created)
    {
        assertNotNull(outcome);
        if (id != null)
        {
            assertEquals(id, outcome.getId().getIdPart());
        }
        if (created)
        {
            assertTrue(outcome.getCreated());
        }
        else
        {
            assertFalse(outcome.getCreated());
        }
    }

    private record Identifier(String system, String value) { }
}
