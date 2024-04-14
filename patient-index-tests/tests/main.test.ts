import { test, expect } from '@playwright/test';
import { randomUUID } from 'crypto';

function randomIdentifier()
{
    return {
        system: `urn:uuid:${randomUUID()}`,
        value: randomUUID(),
    };
}

test.use({
    baseURL: "http://localhost:8080/fhir/r5/"
})

test.describe("create, update, and search", () => {
    test('should create patient without supplying id', async ({ request }) => {
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
    });
    
    test('should create patient with supplying id', async ({ request }) => {
        const id = randomUUID();
    
        const newPatient = await request.put(`Patient/${id}`, {
            data: {
                resourceType: "Patient",
                id: id,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.id).toBe(id)
    
        const foundPatient = await request.get(`Patient/${id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(id);
    });
    
    test('should create patient with one identifier', async ({ request }) => {
        const identifiers = [
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier).toEqual(identifiers)
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier).toEqual(identifiers);
    });
    
    test('should create patient with one identifier and find with identifier', async ({ request }) => {
        const identifier = randomIdentifier();
        const identifiers = [
            identifier,
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier).toEqual(identifiers)
    
        const foundBundle = await request.get(
            `Patient?identifier=${encodeURIComponent(`${identifier.system}|${identifier.value}`)}`);
        expect(foundBundle.ok()).toBeTruthy();
    
        const foundBundleData = await foundBundle.json();
        expect(foundBundleData.entry).toBeDefined();
        expect(foundBundleData.entry.length).toBe(1)
    
        const foundPatientData = foundBundleData.entry[0].resource;
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier).toEqual(identifiers);
    });
    
    test('should create patient with six identifiers', async ({ request }) => {
        const identifiers = [
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier.sort()).toEqual(identifiers.sort())
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier.sort()).toEqual(identifiers.sort());
    });
    
    test('should create patient with six identifiers and find with identifier', async ({ request }) => {
        const identifier = randomIdentifier();
        const identifiers = [
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
            identifier,
            randomIdentifier(),
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier).toEqual(identifiers)
    
        const foundBundle = await request.get(
            `Patient?identifier=${encodeURIComponent(`${identifier.system}|${identifier.value}`)}`);
        expect(foundBundle.ok()).toBeTruthy();
    
        const foundBundleData = await foundBundle.json();
        expect(foundBundleData.entry).toBeDefined();
        expect(foundBundleData.entry.length).toBe(1)
    
        const foundPatientData = foundBundleData.entry[0].resource;
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier).toEqual(identifiers);
    });
    
    test('should create patient with one identifier and add one', async ({ request }) => {
        const identifiers = [
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier).toEqual(identifiers)
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier).toEqual(identifiers);
    
        identifiers.push(randomIdentifier());
    
        const changedPatient = await request.put(`Patient/${newPatientData.id}`, {
            data: {
                resourceType: "Patient",
                id: newPatientData.id,
                identifier: identifiers,
            }
        });
        expect(changedPatient.ok()).toBeTruthy();
    
        const changedPatientData = await changedPatient.json();
        expect(changedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(changedPatientData.identifier.sort()).toEqual(identifiers.sort());
    
        const foundChangedPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundChangedPatient.ok()).toBeTruthy();
    
        const foundChangedPatientData = await foundChangedPatient.json();
        expect(foundChangedPatientData.id).toBe(newPatientData.id);
        expect(foundChangedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(foundChangedPatientData.identifier.sort()).toEqual(identifiers.sort());
    });
    
    test('should create patient with two identifiers and remove one', async ({ request }) => {
        const identifiers = [
            randomIdentifier(),
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier.sort()).toEqual(identifiers.sort())
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier.sort()).toEqual(identifiers.sort());
    
        identifiers.pop();
    
        const changedPatient = await request.put(`Patient/${newPatientData.id}`, {
            data: {
                resourceType: "Patient",
                id: newPatientData.id,
                identifier: identifiers,
            }
        });
        expect(changedPatient.ok()).toBeTruthy();
    
        const changedPatientData = await changedPatient.json();
        expect(changedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(changedPatientData.identifier.sort()).toEqual(identifiers.sort());
    
        const foundChangedPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundChangedPatient.ok()).toBeTruthy();
    
        const foundChangedPatientData = await foundChangedPatient.json();
        expect(foundChangedPatientData.id).toBe(newPatientData.id);
        expect(foundChangedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(foundChangedPatientData.identifier.sort()).toEqual(identifiers.sort());
    });
    
    test('should create patient with four identifiers and replace one', async ({ request }) => {
        const identifiers = [
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
            randomIdentifier(),
        ]
    
        const newPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
                identifier: identifiers,
            }
        });
        expect(newPatient.ok()).toBeTruthy();
    
        const newPatientData = await newPatient.json();
        expect(newPatientData.identifier.sort()).toEqual(identifiers.sort())
    
        const foundPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundPatient.ok()).toBeTruthy();
    
        const foundPatientData = await foundPatient.json();
        expect(foundPatientData.id).toBe(newPatientData.id);
        expect(foundPatientData.identifier.sort()).toEqual(identifiers.sort());
    
        identifiers.splice(2, 1, randomIdentifier());
    
        const changedPatient = await request.put(`Patient/${newPatientData.id}`, {
            data: {
                resourceType: "Patient",
                id: newPatientData.id,
                identifier: identifiers,
            }
        });
        expect(changedPatient.ok()).toBeTruthy();
    
        const changedPatientData = await changedPatient.json();
        expect(changedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(changedPatientData.identifier.sort()).toEqual(identifiers.sort());
    
        const foundChangedPatient = await request.get(`Patient/${newPatientData.id}`);
        expect(foundChangedPatient.ok()).toBeTruthy();
    
        const foundChangedPatientData = await foundChangedPatient.json();
        expect(foundChangedPatientData.id).toBe(newPatientData.id);
        expect(foundChangedPatientData.meta.versionId).not.toBe(newPatientData.meta.versionId);
        expect(foundChangedPatientData.identifier.sort()).toEqual(identifiers.sort());
    });
});

test.describe("merge", () => {
    test('should create two patients and merge', async ({ request }) => {
        const newSourcePatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
            }
        });
        expect(newSourcePatient.ok()).toBeTruthy();
    
        const newSourcePatientData = await newSourcePatient.json();
        expect(newSourcePatientData.active).toBeTruthy();
    
        const newTargetPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
            }
        });
        expect(newTargetPatient.ok()).toBeTruthy();
    
        const newTargetPatientData = await newTargetPatient.json();
        expect(newTargetPatientData.active).toBeTruthy();

        const mergePatient = await request.post("Patient/$merge", {
            data: {
                resourceType: "Parameters",
                parameter: [
                    {
                        name: "source-patient",
                        valueReference: {
                            reference: `Patient/${newSourcePatientData.id}`
                        }
                    },
                    {
                        name: "target-patient",
                        valueReference: {
                            reference: `Patient/${newTargetPatientData.id}`
                        }
                    },
                ],
            },
        });
        expect(mergePatient.ok()).toBeTruthy();

        const foundSourcePatient = await request.get(`Patient/${newSourcePatientData.id}`);
        expect(foundSourcePatient.ok()).toBeTruthy();
    
        const foundSourcePatientData = await foundSourcePatient.json();
        expect(foundSourcePatientData.active).toBeFalsy();
        expect(foundSourcePatientData.link.length).toBe(1);
        // TODO Check link type
        // TODO Check linked reference
    });

    test('should create target patient and fail due to invalid source patient', async ({ request }) => {
        const newTargetPatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
            }
        });
        expect(newTargetPatient.ok()).toBeTruthy();
    
        const newTargetPatientData = await newTargetPatient.json();
        expect(newTargetPatientData.active).toBeTruthy();

        const mergePatient = await request.post("Patient/$merge", {
            data: {
                resourceType: "Parameters",
                parameter: [
                    {
                        name: "source-patient",
                        valueReference: {
                            reference: `Patient/${randomUUID()}`
                        }
                    },
                    {
                        name: "target-patient",
                        valueReference: {
                            reference: `Patient/${newTargetPatientData.id}`
                        }
                    },
                ],
            },
        });
        expect(mergePatient.ok()).toBeFalsy();
    });

    test('should create source patient and fail due to invalid target patient', async ({ request }) => {
        const newSourcePatient = await request.post("Patient", {
            data: {
                resourceType: "Patient",
            }
        });
        expect(newSourcePatient.ok()).toBeTruthy();
    
        const newSourcePatientData = await newSourcePatient.json();
        expect(newSourcePatientData.active).toBeTruthy();

        const mergePatient = await request.post("Patient/$merge", {
            data: {
                resourceType: "Parameters",
                parameter: [
                    {
                        name: "source-patient",
                        valueReference: {
                            reference: `Patient/${newSourcePatientData.id}`
                        }
                    },
                    {
                        name: "target-patient",
                        valueReference: {
                            reference: `Patient/${randomUUID()}`
                        }
                    },
                ],
            },
        });
        expect(mergePatient.ok()).toBeFalsy();
    });
});
