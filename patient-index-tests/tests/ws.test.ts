import { test, expect } from '@playwright/test';

const nsSoap = "http://www.w3.org/2003/05/soap-envelope";
const nsHl73 = "urn:hl7-org:v3";

function createQueryByParameter()
{
    // https://profiles.ihe.net/ITI/TF/Volume2/ITI-45.html#3.45.4.1.2.2

    return {
        "queryId": {
            "@root": "0.0.0",
            "@extension": "0" // TODO Generate id
        },
        "statusCode": {
            "@code": "new"
        },
        "responsePriorityCode": {
            "@code": "I"
        },
        
    };
}

/*
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:urn="urn:hl7-org:v3">
   <soap:Header />
   <soap:Body>
      <urn:PRPA_IN201309UV02 ITSVersion="XML_1.0">
         <urn:controlActProcess classCode="CACT" moodCode="EVN">
            <urn:queryByParameter>
               <urn:queryId root="1.2.840.114350.1.13.99999.4567.34" extension="33452" />
               <urn:statusCode code="new" />
               <urn:responsePriorityCode code="I" />
               <urn:parameterList>
                  <urn:patientIdentifier>
                     <urn:value root="1.2.840.114350.1.13.99997.2.3412" extension="38273N237" />
                     <urn:semanticsText>Patient.Id</urn:semanticsText>
                  </urn:patientIdentifier>
               </urn:parameterList>
            </urn:queryByParameter>
         </urn:controlActProcess>
      </urn:PRPA_IN201309UV02>
   </soap:Body>
</soap:Envelope>
*/

function createControlActProcess(queryByParameter: any)
{
    // https://profiles.ihe.net/ITI/TF/Volume2/ch-O.html#O.2.3
    return {
        "@classCode": "CACT",
        "@moodCode": "EVN",
        "code": {
            "@code": "PRPA_TE201309UV02",
            "@codeSystem": "2.16.840.1.113883.1.6"
        },
        "authorOrPerformer": {
            "@typeCode": "AUT"
        },
        "queryByParameter": queryByParameter
    };
}

function createTransmissionWrapper(controlActProcess: any)
{
    // https://profiles.ihe.net/ITI/TF/Volume2/ITI-45.html#3.45.4.1.2.3
    // https://profiles.ihe.net/ITI/TF/Volume2/ch-O.html#O.1.1

    return {
        "PRPA_IN201309UV02": {
            "@ITSVersion": "XML_1.0",
            "id": {
                "@root": "" // TODO Unique ID
            },
            "creationTime": {
                "@value": "" // TODO Time
            },
            "interactionId": {
                "@root": "2.16.840.1.113883.1.6",
                "@extension": "PRPA_IN201309UV02"
            },
            "processingCode": {
                "@code": "T"
            },
            "processingModeCode": {
                "@code": "T"
            },
            "acceptAckCode": {
                "@code": "AL"
            },
            "receiver": {
                "@typeCode": "RCV",
                "device": {
                    "@classCode": "DEV",
                    "@determinerCode": "INSTANCE",
                    "id": {
                        "@root": "0.0.0"
                    }
                }
            },
            "sender": {
                "@typeCode": "SND",
                "device": {
                    "@classCode": "DEV",
                    "@determinerCode": "INSTANCE",
                    "id": {
                        "@root": "0.0.0"
                    }
                }
            },
            "controlActProcess": controlActProcess
        }
    };
}

test.describe("PIX Query", () => {
    test("should find", async ({ request }) => {
        
    });
});

/*
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:urn="urn:hl7-org:v3">
   <soap:Header />
   <soap:Body>
      <urn:PRPA_IN201309UV02 ITSVersion="XML_1.0">
         <urn:id root="2220c1c4-87ef-11dc-b865-3603d6866807" />
         <urn:creationTime value="20070810140900" />
         <urn:interactionId root="2.16.840.1.113883.1.6" extension="PRPA_IN201309UV02" />
         <urn:processingCode code="P" />
         <urn:processingModeCode code="T" />
         <urn:acceptAckCode code="AL" />
         <urn:receiver typeCode="RCV">
            <urn:device classCode="DEV" determinerCode="INSTANCE">
               <urn:id root="1.2.840.114350.1.13.99999.4567" />
               <urn:telecom value="https://example.org/PIXQuery"></urn:telecom>
            </urn:device>
         </urn:receiver>
         <urn:sender typeCode="SND">
            <urn:device classCode="DEV" determinerCode="INSTANCE">
               <urn:id root="1.2.840.114350.1.13.99997.2.7788" />
            </urn:device>
         </urn:sender>
         <urn:controlActProcess classCode="CACT" moodCode="EVN">
            <urn:code code="PRPA_TE201309UV02" codeSystem="2.16.840.1.113883.1.6" />
            <urn:authorOrPerformer typeCode="AUT">
               <urn:assignedPerson classCode="ASSIGNED">
                  <urn:id root="1.2.840.114350.1.13.99997.2.7766" extension="USR5568" />
               </urn:assignedPerson>
            </urn:authorOrPerformer>
            <urn:queryByParameter>
               <urn:queryId root="1.2.840.114350.1.13.99999.4567.34" extension="33452" />
               <urn:statusCode code="new" />
               <urn:responsePriorityCode code="I" />
               <urn:parameterList>
                  <urn:patientIdentifier>
                     <urn:value root="1.2.840.114350.1.13.99997.2.3412" extension="38273N237" />
                     <urn:semanticsText>Patient.Id</urn:semanticsText>
                  </urn:patientIdentifier>
               </urn:parameterList>
            </urn:queryByParameter>
         </urn:controlActProcess>
      </urn:PRPA_IN201309UV02>
   </soap:Body>
</soap:Envelope>
*/
