import { test, expect } from '@playwright/test';
import { randomUUID } from 'crypto';
import { create } from 'xmlbuilder2';
import { Patient } from './patient';

const nsSoap = "http://www.w3.org/2003/05/soap-envelope";
const nsHl73 = "urn:hl7-org:v3";
const nsWsa = "http://www.w3.org/2005/08/addressing";

const receiverOid = "1.2.3";
const senderOid = "4.5.6";

function hl7FormatDate(date: Date)
{
    // YYYY-MM-DDTHH:mm:ss.sssZ
    const str = date.toISOString();
    return `${str.substring(0, 4)}${str.substring(5, 7)}${str.substring(8, 10)}`
        + `${str.substring(11, 13)}${str.substring(14, 16)}${str.substring(17, 19)}`;
}

function createSoapMsg(idSystem: string, idValue: string, whiteList: string[])
{
    let doc = create({ version: "1.0" })
        .ele(nsSoap, "Envelope")
            .ele(nsSoap, "Header")
                .ele(nsWsa, "Action").att(nsSoap, "mustUnderstand", "1").txt("urn:hl7-org:v3:PRPA_IN201309UV02").up()
                .ele(nsWsa, "MessageID").att(nsSoap, "mustUnderstand", "1").txt(`urn:uuid:${randomUUID()}`).up()
                .ele(nsWsa, "ReplyTo").att(nsSoap, "mustUnderstand", "1")
                    .ele(nsWsa, "Address").txt("http://www.w3.org/2005/08/addressing/anonymous").up()
                .up()
                .ele(nsWsa, "To").att(nsSoap, "mustUnderstand", "1").txt("").up()
            .up()
            .ele(nsSoap, "Body")
                .ele(nsHl73, "PRPA_IN201309UV02", { ITSVersion: "XML_1.0" })
                    .ele(nsHl73, "id", { root: randomUUID() }).up()
                    .ele(nsHl73, "creationTime", { value: hl7FormatDate(new Date()) }).up()
                    .ele(nsHl73, "interactionId", { root: "2.16.840.1.113883.1.6", extension: "PRPA_IN201309UV02" }).up()
                    .ele(nsHl73, "processingCode", { code: "T" }).up()
                    .ele(nsHl73, "processingModeCode", { code: "T" }).up()
                    .ele(nsHl73, "acceptAckCode", { code: "AL" }).up()
                    .ele(nsHl73, "receiver", { typeCode: "RCV" })
                        .ele(nsHl73, "device", { classCode: "DEV", determinerCode: "INSTANCE" })
                            .ele(nsHl73, "id", { root: receiverOid }).up()
                        .up()
                    .up()
                    .ele(nsHl73, "sender", { typeCode: "SND" })
                        .ele(nsHl73, "device", { classCode: "DEV", determinerCode: "INSTANCE" })
                            .ele(nsHl73, "id", { root: senderOid }).up()
                        .up()
                    .up()
                    .ele(nsHl73, "controlActProcess", { classCode: "CACT", moodCode: "EVN" })
                        .ele(nsHl73, "code", { codeSystem: "2.16.840.1.113883.1.6", code: "PRPA_TE201309UV02" }).up()
                        .ele(nsHl73, "queryByParameter")
                            .ele(nsHl73, "queryId", { extension: randomUUID() }).up()
                            .ele(nsHl73, "statusCode", { code: "new" }).up()
                            .ele(nsHl73, "responsePriorityCode", { code: "I" }).up()
                            .ele(nsHl73, "parameterList");
                                for (const system of whiteList)
                                {
                                    doc = doc.ele(nsHl73, "dataSource")
                                        .ele(nsHl73, "value", { root: system }).up()
                                        .ele(nsHl73, "semanticsText").txt("DataSource.id").up()
                                    .up();
                                }
                                doc = doc.ele(nsHl73, "patientIdentifier")
                                    .ele(nsHl73, "value", { root: idSystem, extension: idValue }).up()
                                    .ele(nsHl73, "semanticsText").txt("Patient.id").up()
                                .up()
                            .up()
                        .up()
                    .up()
                .up()
            .up()
        .up()
        .doc();
    
    return doc;
}

function randomOid()
{
    const path = new Array<string>();
    
    for (let i = 0; i < 10; ++i)
    {
        path.push(String(Math.floor(Math.random() * 1000)));    
    }

    return path.join(".");
}

function findNodes(node: Node, path: Array<string>): Array<Node>
{
    const result = new Array<Node>();

    if (path.length > 0 && node.nodeType == node.ELEMENT_NODE)
    {
        const complexNodeName = node.nodeName.split(":", 2);
        const nodeName = complexNodeName.length == 1 ? complexNodeName[0] : complexNodeName[1];

        if (path[0] == nodeName)
        {
            if (path.length == 1)
            {
                result.push(node);
            }
            else
            {
                const pathNext = path.slice(1);

                for (const child of node.childNodes)
                {
                    result.push(...findNodes(child, pathNext));
                }
            }
        }
    }

    return result;
}

const resourceOid = "0.0.0";

const patient: Patient = {
    resourceType: "Patient",
    id: randomUUID(),
    identifier: [
        {
            system: `urn:oid:${randomOid()}`,
            value: randomUUID(),
        },
        {
            system: `urn:oid:${randomOid()}`,
            value: randomUUID(),
        }
    ]
};

test.beforeAll(async ({ request }) => {
    await request.put(`http://localhost:8080/fhir/r5/Patient/${patient.id}`, {
        data: patient,
    });
});

test.describe("IHE Cases ( https://profiles.ihe.net/ITI/TF/Volume2/ITI-45.html#3.45.4.2.3 )", () => {
    test.describe("Case 1", () => {
        test("with resource id", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(resourceOid, patient.id!, [ patient.identifier![0].system.substring(8) ]).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;

            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("OK");

            const foundIds = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/subject/registrationEvent/subject1/patient/id"
                    .split("/"));
            expect(foundIds.length).toBe(1);
    
            //console.log(doc.end({ prettyPrint: true }));
        });
    
        test("with patient identifier", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(patient.identifier![0].system.substring(8), patient.identifier![0].value,
                    [ patient.identifier![1].system.substring(8) ]).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;

            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("OK");

            const foundIds = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/subject/registrationEvent/subject1/patient/id"
                    .split("/"));
            expect(foundIds.length).toBe(1);
    
            //console.log(doc.end({ prettyPrint: true }));
        });
    });

    test.describe("Case 2", () => {
        test("with resource id", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(resourceOid, patient.id!, []).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;
    
            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("OK");

            const foundIds = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/subject/registrationEvent/subject1/patient/id"
                    .split("/"));
            expect(foundIds.length).toBe(2);
    
            // console.log(doc.end({ prettyPrint: true }));
        });

        test("with patient identifier", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(patient.identifier![0].system.substring(8), patient.identifier![0].value, []).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;

            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("OK");

            const foundIds = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/subject/registrationEvent/subject1/patient/id"
                    .split("/"));
            expect(foundIds.length).toBe(2);
    
            //console.log(doc.end({ prettyPrint: true }));
        });
    });

    test.describe("Case 3", () => {
        test("with resource id", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(resourceOid, patient.id!, [ randomOid() ]).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;

            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("NF");
    
            //console.log(doc.end({ prettyPrint: true }));
        });
    
        test("with patient identifier", async ({ request }) => {
            const response = await request.post("http://localhost:9080/ws/", {
                headers: {
                    "Content-Type": 'application/soap+xml;charset=UTF-8'
                },
                data: createSoapMsg(patient.identifier![0].system.substring(8), patient.identifier![0].value,
                    [ randomOid() ]).end()
            })
    
            expect(response.ok()).toBeTruthy();
    
            const doc = create((await response.body()).toString("utf-8"));
            const root = doc.root().node as unknown as Node;

            const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
            expect(ackTypeCode.length).toBe(1);
            expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AA");
    
            const responseCode = findNodes(root,
                "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
            expect(responseCode.length).toBe(1);
            expect((responseCode[0] as Element).getAttribute("code")).toBe("NF");
    
            //console.log(doc.end({ prettyPrint: true }));
        });
    });

    test("Case 4", async ({ request }) => {
        const response = await request.post("http://localhost:9080/ws/", {
            headers: {
                "Content-Type": 'application/soap+xml;charset=UTF-8'
            },
            data: createSoapMsg(resourceOid, randomUUID(), []).end()
        })

        expect(response.ok()).toBeTruthy();

        const doc = create((await response.body()).toString("utf-8"));
        const root = doc.root().node as unknown as Node;

        const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
        expect(ackTypeCode.length).toBe(1);
        expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AE");

        const responseCode = findNodes(root,
            "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
        expect(responseCode.length).toBe(1);
        expect((responseCode[0] as Element).getAttribute("code")).toBe("AE");

        //console.log(doc.end({ prettyPrint: true }));
    });

    test("Case 5", async ({ request }) => {
        const response = await request.post("http://localhost:9080/ws/", {
            headers: {
                "Content-Type": 'application/soap+xml;charset=UTF-8'
            },
            data: createSoapMsg(randomOid(), randomUUID(), []).end()
        })

        expect(response.ok()).toBeTruthy();

        const doc = create((await response.body()).toString("utf-8"));
        const root = doc.root().node as unknown as Node;

        const ackTypeCode = findNodes(root, "Envelope/Body/PRPA_IN201310UV02/acknowledgement/typeCode".split("/"));
        expect(ackTypeCode.length).toBe(1);
        expect((ackTypeCode[0] as Element).getAttribute("code")).toBe("AE");

        const responseCode = findNodes(root,
            "Envelope/Body/PRPA_IN201310UV02/controlActProcess/queryAck/queryResponseCode".split("/"));
        expect(responseCode.length).toBe(1);
        expect((responseCode[0] as Element).getAttribute("code")).toBe("AE");

        //console.log(doc.end({ prettyPrint: true }));
    });
});
