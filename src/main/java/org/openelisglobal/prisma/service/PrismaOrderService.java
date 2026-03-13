package org.openelisglobal.prisma.service;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    public PrismaOrderResponse processOrder(PrismaOrderRequest req) throws Exception {
        // 1. Find or create patient
        Patient patient = findOrCreatePatient(req);

        // 2. Create Sample
        Sample sample = new Sample();
        sample.setAccessionNumber(generateAccessionNumber());
        sample.setStatus("O");
        sample.setSysUserId("1");
        sample = sampleService.save(sample);

        // 3. Link patient to sample
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patient.getId());
        sampleHuman.setSysUserId("1");
        sampleHumanService.save(sampleHuman);

        // 4. Create SampleItem
        TypeOfSample typeOfSample = findSampleType(req.getSampleType());
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(typeOfSample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId("O");
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
                    analysis.setStatus("O");
                    analysis.setSysUserId("1");
                    analysisService.save(analysis);
                }
            }
        }

        return new PrismaOrderResponse(
            sample.getAccessionNumber(),
            sample.getId(),
            patient.getId(),
            "CREATED",
            req.getPrismaVisitId()
        );
    }

    @Transactional(readOnly = true)
    public PrismaResultResponse getResults(String accessionNumber) {
        // Find sample by accession number
        Sample sampleQuery = new Sample();
        sampleQuery.setAccessionNumber(accessionNumber);
        sampleService.getSampleByAccessionNumber(sampleQuery);

        if (sampleQuery.getId() == null) return null;

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sampleQuery.getId());
        List<PrismaResultResponse.TestResult> results = new ArrayList<>();

        for (SampleItem item : sampleItems) {
            List<Analysis> analyses = analysisService.getAnalysesBySampleItem(item);
            for (Analysis analysis : analyses) {
                PrismaResultResponse.TestResult tr = new PrismaResultResponse.TestResult();
                tr.setTestId(analysis.getTest().getId());
                tr.setTestName(analysis.getTest().getName());
                tr.setStatus(mapStatus(analysis.getStatus()));
                results.add(tr);
            }
        }

        PrismaResultResponse response = new PrismaResultResponse();
        response.setAccessionNumber(accessionNumber);
        response.setStatus(results.isEmpty() ? "PENDING" : "PARTIAL");
        response.setResults(results);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatus(String accessionNumber) {
        Sample sampleQuery = new Sample();
        sampleQuery.setAccessionNumber(accessionNumber);
        sampleService.getSampleByAccessionNumber(sampleQuery);

        Map<String, Object> status = new HashMap<>();
        if (sampleQuery.getId() == null) {
            status.put("found", false);
            return status;
        }
        status.put("found", true);
        status.put("accessionNumber", accessionNumber);
        status.put("status", sampleQuery.getStatus());
        return status;
    }

    private Patient findOrCreatePatient(PrismaOrderRequest req) {
        List<Patient> existing = patientService.getPatientsByNationalId(req.getPatientNationalId());
        if (existing != null && !existing.isEmpty()) return existing.get(0);

        Person person = new Person();
        person.setFirstName(req.getPatientFirstName());
        person.setLastName(req.getPatientLastName());
        person.setSysUserId("1");
        person = personService.save(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setNationalId(req.getPatientNationalId());
        patient.setGender(req.getPatientGender());
        patient.setSysUserId("1");
        return patientService.save(patient);
    }

    private TypeOfSample findSampleType(String name) {
        if (name == null) name = "Blood";
        List<TypeOfSample> types = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample t : types) {
            if (name.equalsIgnoreCase(t.getLocalizedName())) return t;
        }
        return types.isEmpty() ? null : types.get(0);
    }

    private String generateAccessionNumber() {
        return "PRM-" + new SimpleDateFormat("yyyyMMdd").format(new Date())
             + "-" + String.format("%04d", (int)(Math.random() * 9999));
    }

    private String mapStatus(String s) {
        if (s == null) return "PENDING";
        switch (s) {
            case "O": return "ORDERED";
            case "R": return "RESULTED";
            case "V": return "VALIDATED";
            case "A": return "RELEASED";
            default: return s;
        }
    }
}
