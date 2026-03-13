package org.openelisglobal.prisma.controller;

import org.openelisglobal.prisma.dto.PrismaOrderRequest;
import org.openelisglobal.prisma.dto.PrismaOrderResponse;
import org.openelisglobal.prisma.dto.PrismaResultResponse;
import org.openelisglobal.prisma.service.PrismaOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rest/prisma")
public class PrismaRestController {

    @Value("${prisma.api.key:prisma-secret-key}")
    private String validApiKey;

    @Autowired
    private PrismaOrderService prismaOrderService;

    // ── Health check ──────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<?> ping(
            @RequestHeader(value = "X-Prisma-Key", required = false) String apiKey) {

        if (!isValidKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }
        return ResponseEntity.ok(Map.of("status", "ok", "service", "Prisma Integration Layer"));
    }

    // ── Create Order ──────────────────────────────────────────────
    @PostMapping("/createOrder")
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "X-Prisma-Key", required = false) String apiKey,
            @RequestBody PrismaOrderRequest request) {

        if (!isValidKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }

        try {
            PrismaOrderResponse response = prismaOrderService.processOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Get Results ───────────────────────────────────────────────
    @GetMapping("/results/{accessionNumber}")
    public ResponseEntity<?> getResults(
            @RequestHeader(value = "X-Prisma-Key", required = false) String apiKey,
            @PathVariable String accessionNumber) {

        if (!isValidKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }

        try {
            PrismaResultResponse response = prismaOrderService.getResults(accessionNumber);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Accession not found: " + accessionNumber));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Order Status ──────────────────────────────────────────────
    @GetMapping("/orderStatus/{accessionNumber}")
    public ResponseEntity<?> getOrderStatus(
            @RequestHeader(value = "X-Prisma-Key", required = false) String apiKey,
            @PathVariable String accessionNumber) {

        if (!isValidKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid API key"));
        }

        try {
            Map<String, Object> status = prismaOrderService.getOrderStatus(accessionNumber);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────
    private boolean isValidKey(String apiKey) {
        return validApiKey.equals(apiKey);
    }
}
