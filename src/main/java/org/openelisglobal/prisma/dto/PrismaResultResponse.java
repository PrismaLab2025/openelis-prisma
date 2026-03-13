package org.openelisglobal.prisma.dto;

import java.util.List;

public class PrismaResultResponse {

    private String accessionNumber;
    private String patientNationalId;
    private String status;             // PENDING, PARTIAL, COMPLETE
    private List<TestResult> results;

    public static class TestResult {
        private String testId;
        private String testName;
        private String value;
        private String units;
        private String referenceRange;
        private String flag;           // N, H, L, HH, LL
        private String status;         // RESULTED, VALIDATED, RELEASED

        public String getTestId() { return testId; }
        public void setTestId(String v) { this.testId = v; }

        public String getTestName() { return testName; }
        public void setTestName(String v) { this.testName = v; }

        public String getValue() { return value; }
        public void setValue(String v) { this.value = v; }

        public String getUnits() { return units; }
        public void setUnits(String v) { this.units = v; }

        public String getReferenceRange() { return referenceRange; }
        public void setReferenceRange(String v) { this.referenceRange = v; }

        public String getFlag() { return flag; }
        public void setFlag(String v) { this.flag = v; }

        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
    }

    // Getters & Setters
    public String getAccessionNumber() { return accessionNumber; }
    public void setAccessionNumber(String v) { this.accessionNumber = v; }

    public String getPatientNationalId() { return patientNationalId; }
    public void setPatientNationalId(String v) { this.patientNationalId = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public List<TestResult> getResults() { return results; }
    public void setResults(List<TestResult> v) { this.results = v; }
}
