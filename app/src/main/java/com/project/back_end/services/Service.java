package com.project.back_end.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.JwtException;

@org.springframework.stereotype.Service
public class Service {

    private static final Logger logger = LoggerFactory.getLogger(Service.class);

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(
            TokenService tokenService,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            DoctorService doctorService,
            PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        try {
            if (token != null && !token.isBlank() && user != null
                    && tokenService.validateToken(token, user)) {
                return ResponseEntity.ok(Map.of());
            }
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token."));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid or expired token."));
    }

    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        try {
            if (receivedAdmin != null && receivedAdmin.getUsername() != null
                    && receivedAdmin.getPassword() != null) {
                Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
                if (admin != null && receivedAdmin.getPassword().equals(admin.getPassword())) {
                    String token = tokenService.generateToken(admin.getUsername(), "admin");
                    return ResponseEntity.ok(Map.of("token", token));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password."));
        } catch (RuntimeException e) {
            logger.error("Admin login failed ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to complete login."));
        }
    }

    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        name = normalizeFilter(name);
        specialty = normalizeFilter(specialty);
        time = normalizeFilter(time);

        if (name != null && specialty != null && time != null) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        }
        if (name != null && specialty != null) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        }
        if (name != null && time != null) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        }
        if (specialty != null && time != null) {
            return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
        }
        if (name != null) {
            return doctorService.findDoctorByName(name);
        }
        if (specialty != null) {
            return doctorService.filterDoctorBySpecility(specialty);
        }
        if (time != null) {
            return doctorService.filterDoctorsByTime(time);
        }
        return Map.of("doctors", doctorService.getDoctors());
    }

    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null
                || appointment.getDoctor().getId() == null) {
            return -1;
        }

        Long doctorId = appointment.getDoctor().getId();
        if (doctorRepository.findById(doctorId).isEmpty()) {
            return -1;
        }

        LocalDateTime requestedTime = appointment.getAppointmentTime();
        if (requestedTime == null || !requestedTime.isAfter(LocalDateTime.now())) {
            return 0;
        }

        for (String slot : doctorService.getDoctorAvailability(
                doctorId, requestedTime.toLocalDate())) {
            String start = slot.split("-", 2)[0].trim();
            try {
                if (LocalTime.parse(start).equals(requestedTime.toLocalTime())) {
                    return 1;
                }
            } catch (DateTimeParseException e) {
                logger.warn("Skipped an invalid availability slot for doctor {}", doctorId);
            }
        }
        return 0;
    }

    public boolean validatePatient(Patient patient) {
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()
                || patient.getPhone() == null || patient.getPhone().isBlank()) {
            return false;
        }

        try {
            return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
        } catch (IncorrectResultSizeDataAccessException e) {
            // The email and phone can match two different patient records.
            return false;
        }
    }

    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        try {
            if (login != null && login.getIdentifier() != null && login.getPassword() != null) {
                Patient patient = patientRepository.findByEmail(login.getIdentifier());
                if (patient != null && login.getPassword().equals(patient.getPassword())) {
                    String token = tokenService.generateToken(patient.getEmail(), "patient");
                    return ResponseEntity.ok(Map.of("token", token));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        } catch (RuntimeException e) {
            logger.error("Patient login failed ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to complete login."));
        }
    }

    public ResponseEntity<Map<String, Object>> filterPatient(
            String condition, String name, String token) {
        try {
            if (!validateToken(token, "patient").getStatusCode().is2xxSuccessful()) {
                return invalidPatientToken();
            }

            String email = tokenService.extractIdentifier(token);
            if (email == null || email.isBlank()) {
                return invalidPatientToken();
            }

            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return invalidPatientToken();
            }

            Long patientId = patient.getId();
            condition = normalizeFilter(condition);
            name = normalizeFilter(name);

            if (condition != null && name != null) {
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            }
            if (condition != null) {
                return patientService.filterByCondition(condition, patientId);
            }
            if (name != null) {
                return patientService.filterByDoctor(name, patientId);
            }
            return patientService.getPatientAppointment(patientId, token);
        } catch (JwtException e) {
            return invalidPatientToken();
        } catch (RuntimeException e) {
            logger.error("Patient appointment filtering failed ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to filter appointments."));
        }
    }

    private ResponseEntity<Map<String, Object>> invalidPatientToken() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid or expired patient token."));
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }
}
