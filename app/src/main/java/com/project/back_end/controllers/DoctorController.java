package com.project.back_end.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;

import jakarta.validation.Valid;

@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable("user") String user,
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("date") String date,
            @PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return message(HttpStatus.BAD_REQUEST, "Use the date format YYYY-MM-DD.");
        }

        return ResponseEntity.ok(Map.of(
                "availableTimes", doctorService.getDoctorAvailability(doctorId, requestedDate)));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        return ResponseEntity.ok(Map.of("doctors", doctorService.getDoctors()));
    }

    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(
            @Valid @RequestBody Doctor doctor,
            @PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        return switch (doctorService.saveDoctor(doctor)) {
            case 1 -> message(HttpStatus.CREATED, "Doctor added to db.");
            case -1 -> message(HttpStatus.CONFLICT, "Doctor already exists.");
            default -> message(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add doctor.");
        };
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@Valid @RequestBody Login login) {
        return doctorService.validateDoctor(login);
    }

    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @Valid @RequestBody Doctor doctor,
            @PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        return switch (doctorService.updateDoctor(doctor)) {
            case 1 -> message(HttpStatus.OK, "Doctor updated.");
            case -1 -> message(HttpStatus.NOT_FOUND, "Doctor not found.");
            default -> message(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update doctor.");
        };
    }

    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable("id") long id,
            @PathVariable("token") String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return validation;
        }

        return switch (doctorService.deleteDoctor(id)) {
            case 1 -> message(HttpStatus.OK, "Doctor deleted successfully.");
            case -1 -> message(HttpStatus.NOT_FOUND, "Doctor not found.");
            default -> message(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete doctor.");
        };
    }

    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(
            @PathVariable("name") String name,
            @PathVariable("time") String time,
            @PathVariable("speciality") String specialty) {
        return ResponseEntity.ok(service.filterDoctor(name, specialty, time));
    }

    private ResponseEntity<Map<String, String>> message(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }
}
