package org.openelisglobal.prisma.dto;

import java.util.List;

public class PrismaOrderRequest {

    // ── Patient info ──────────────────────────────────────────────
    private String patientNationalId;   // رقم الهوية
    private String patientFirstName;
    private String patientLastName;
    private String patientDob;          // YYYY-MM-DD
    private String patientGender;       // M or F
    private String patientPhone;

    // ── Order info ────────────────────────────────────────────────
    private String collectionDate;      // YYYY-MM-DD
    private String sampleType;          // e.g. "Blood", "Urine"
    private String priority;            // ROUTINE or STAT
    private List<String> testIds;       // OpenELIS test IDs

    // ── Prisma visit reference ────────────────────────────────────
    private String prismaVisitId;       // visit ID from Supabase

    // ── Getters & Setters ─────────────────────────────────────────
    public String getPatientNationalId() { return patientNationalId; }
    public void setPatientNationalId(String v) { this.patientNationalId = v; }

    public String getPatientFirstName() { return patientFirstName; }
    public void setPatientFirstName(String v) { this.patientFirstName = v; }

    public String getPatientLastName() { return patientLastName; }
    public void setPatientLastName(String v) { this.patientLastName = v; }

    public String getPatientDob() { return patientDob; }
    public void setPatientDob(String v) { this.patientDob = v; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String v) { this.patientGender = v; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String v) { this.patientPhone = v; }

    public String getCollectionDate() { return collectionDate; }
    public void setCollectionDate(String v) { this.collectionDate = v; }

    public String getSampleType() { return sampleType; }
    public void setSampleType(String v) { this.sampleType = v; }

    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }

    public List<String> getTestIds() { return testIds; }
    public void setTestIds(List<String> v) { this.testIds = v; }

    public String getPrismaVisitId() { return prismaVisitId; }
    public void setPrismaVisitId(String v) { this.prismaVisitId = v; }
}
