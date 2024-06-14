export interface Identifier
{
    system: string,
    value: string,
}

export interface Patient
{
    resourceType: "Patient",
    id?: string,
    identifier?: Identifier[],
}
