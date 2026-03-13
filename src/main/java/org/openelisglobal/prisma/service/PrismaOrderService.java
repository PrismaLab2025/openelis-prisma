package org.openelisglobal.prisma.service;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.prisma.dto.PrismaOrderRequest;
import org.openelisglobal.prisma.dto.PrismaOrderResponse;
import org.openelisglobal.prisma.dto.PrismaResultResponse;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PrismaOrderService {

    @Autowired private PatientService patientService;
    @Autowired private PersonService personService;
    @Autowired private SampleService sampleService;
    @Autowired private SampleHumanService sampleHumanService;
    @Autowired private SampleItemService sampleItemService;
    @Autowired private AnalysisService analysisService;
    @Autowired private TestService testService;
    @Autowired private TypeOfSampleService typeOfSampleService;

    // ── Create Order ──────────────────────────────────────────────
    public PrismaOrderResponse processOrder(PrismaOrderRequest req) throws Exception {

        // 1. Find or create patient
        Patient patient = findOrCreatePatient(req);

        // 2. Create Sample (the OpenELIS "order")
        Sample sample = new Sample();
        sample.setAccessionNumber(generateAccessionNumber());
        sample.setCollectionDate(DateUtil.convertStringDateToSqlDate(req.getCollectionDate()));
        sample.setEnteredDate(DateUtil.getNowAsSqlDate());
        sample.setStatus("O"); // Order received
        sample.setSysUserId("1");
        sample = sampleService.save(sample);

        // 3. Link patient to sample
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patient.getId());
        sampleHuman.setSysUserId("1");
        sampleHumanService.save(sampleHuman);

        // 4. Create SampleItem (tube/container)
        TypeOfSample typeOfSample = findSampleType(req.getSampleType());
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(typeOfSample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatus("O");
        sampleItem.setSysUserId("1");
        sampleItem = sampleItemService.save(sampleItem);

        // 5. Create Analysis per test
        if (req.getTestIds() != null) {
            for (String testId : req.getTestIds()) {
                Test test = testService.getTestById(testId);
                if (test != null) {
                    Analysis analysis = new Analysis();
                    analysis.setSampleItem(sampleItem);
                    analysis.setTest(test);
                    analysis.setStatus("O"); // Ordered
                    analysis.setSysUserId("1");
                    analysisService.save(analysis);
                }
            }
        }

        // 6. Return response
        PrismaOrderResponse response = new PrismaOrderResponse(
                sample.getAccessionNumber(),
                sample.getId(),
                patient.getId(),
                "CREATED",
                req.getPrismaVisitId()
        );

        return response;
    }

    // ── Get Results ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PrismaResultResponse getResults(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) return null;

        SampleHuman sampleHuman = sampleHumanService.getDataBySample(sample);
        Patient patient = sampleHuman != null
                ? patientService.getPatientById(sampleHuman.getPatientId())
                : null;

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());

        List<PrismaResultResponse.TestResult> results = new ArrayList<>();
        String overallStatus = "PENDING";
        int resultedCount = 0;

        for (SampleItem item : sampleItems) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(item);
            for (Analysis analysis : analyses) {
                PrismaResultResponse.TestResult tr = new PrismaResultResponse.TestResult();
                tr.setTestId(analysis.getTest().getId());
                tr.setTestName(analysis.getTest().getLocalizedTestName().getLocalizedValue());
                tr.setStatus(mapAnalysisStatus(analysis.getStatus()));

                // Value if available
                if (analysis.getResult() != null) {
                    tr.setValue(analysis.getResult().getValue());
                    tr.setUnits(analysis.getTest().getUnitOfMeasure() != null
                            ? analysis.getTest().getUnitOfMeasure().getUnitOfMeasureName() : "");
                    resultedCount++;
                }

                results.add(tr);
            }
        }

        if (resultedCount == results.size() && !results.isEmpty()) overallStatus = "COMPLETE";
        else if (resultedCount > 0) overallStatus = "PARTIAL";

        PrismaResultResponse response = new PrismaResultResponse();
        response.setAccessionNumber(accessionNumber);
        response.setPatientNationalId(patient != null ? patient.getNationalId() : null);
        response.setStatus(overallStatus);
        response.setResults(results);

        return response;
    }

    // ── Order Status ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatus(String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        Map<String, Object> status = new HashMap<>();

        if (sample == null) {
            status.put("found", false);
            return status;
        }

        status.put("found", true);
        status.put("accessionNumber", accessionNumber);
        status.put("status", sample.getStatus());
        status.put("collectionDate", sample.getCollectionDate());
        return status;
    }

    // ── Private helpers ───────────────────────────────────────────

    private Patient findOrCreatePatient(PrismaOrderRequest req) {
        // Try find by national ID first
        List<Patient> existing = patientService.getPatientsByNationalId(req.getPatientNationalId());
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        // Create new person + patient
        Person person = new Person();
        person.setFirstName(req.getPatientFirstName());
        person.setLastName(req.getPatientLastName());
        person.setSysUserId("1");
        person = personService.save(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setNationalId(req.getPatientNationalId());
        patient.setBirthDateForDisplay(req.getPatientDob());
        patient.setGender(req.getPatientGender());
        patient.setSysUserId("1");

        return patientService.save(patient);
    }

    private TypeOfSample findSampleType(String sampleTypeName) {
        if (sampleTypeName == null) sampleTypeName = "Blood";
        List<TypeOfSample> types = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample t : types) {
            if (t.getLocalizedName() != null &&
                t.getLocalizedName().equalsIgnoreCase(sampleTypeName)) {
                return t;
            }
        }
        return types.isEmpty() ? null : types.get(0); // fallback
    }

    private String generateAccessionNumber() {
        // Format: PRM-YYYYMMDD-XXXX
        String date = new java.text.SimpleDateFormat("yyyyMMdd")
                .format(new java.util.Date());
        String rand = String.format("%04d", (int)(Math.random() * 9999));
        return "PRM-" + date + "-" + rand;
    }

    private String mapAnalysisStatus(String status) {
        if (status == null) return "PENDING";
        switch (status) {
            case "O": return "ORDERED";
            case "R": return "RESULTED";
            case "V": return "VALIDATED";
            case "A": return "RELEASED";
            default:  return status;
        }
    }
}
