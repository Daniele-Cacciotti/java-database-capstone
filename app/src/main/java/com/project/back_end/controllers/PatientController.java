package com.project.back_end.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPatient(@PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        return patientService.getPatientDetails(token);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@Valid @RequestBody Patient patient) {
        if (patient.getId() != null || patient.getEmail().isBlank()) {
            return message(HttpStatus.BAD_REQUEST, "Provide an email and omit the patient ID.");
        }
        if (!service.validatePatient(patient)) {
            return message(HttpStatus.CONFLICT, "A patient with that email or phone number already exists.");
        }
        if (patientService.createPatient(patient) == 1) {
            return message(HttpStatus.CREATED, "Signup successful.");
        }
        return message(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to register patient.");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    @GetMapping({ "/{id}/{token}", "/{id}/{user}/{token}" })
    public ResponseEntity<?> getPatientAppointment(
            @PathVariable("id") Long id,
            @PathVariable(name = "user", required = false) String user,
            @PathVariable("token") String token) {
        String role = user == null ? "patient" : user;
        if (!"patient".equals(role) && !"doctor".equals(role)) {
            return message(HttpStatus.FORBIDDEN, "This role cannot read patient appointments.");
        }

        ResponseEntity<Map<String, String>> validation = service.validateToken(token, role);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        if (id <= 0) {
            return message(HttpStatus.BAD_REQUEST, "A valid patient ID is required.");
        }

        if ("doctor".equals(role)) {
            return patientService.getPatientAppointmentsForDoctor(id, token);
        }
        return patientService.getPatientAppointment(id, token);
    }

    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(
            @PathVariable("condition") String condition,
            @PathVariable("name") String name,
            @PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }
        return service.filterPatient(condition, name, token);
    }

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }
}
