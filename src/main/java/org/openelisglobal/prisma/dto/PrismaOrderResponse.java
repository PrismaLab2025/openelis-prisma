package org.openelisglobal.prisma.dto;

public class PrismaOrderResponse {

    private String accessionNumber;
    private String sampleId;
    private String patientId;
    private String status;
    private String prismaVisitId;
    private String message;

    public PrismaOrderResponse() {}

    public PrismaOrderResponse(String accessionNumber, String sampleId,
                                String patientId, String status,
                                String prismaVisitId) {
        this.accessionNumber = accessionNumber;
        this.sampleId = sampleId;
        this.patientId = patientId;
        this.status = status;
        this.prismaVisitId = prismaVisitId;
    }

    // Getters & Setters
    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String v) { this.accessionNumber = v; }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String v) { this.sampleId = v; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String v) { this.patientId = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getPrismaVisitId() { return prismaVisitId; }
    public void setPrismaVisitId(String v) { this.prismaVisitId = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
}
