package com.project.back_end.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.JwtException;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    private final DoctorRepository doctorRepository;

    public PatientService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService,
            DoctorRepository doctorRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public int createPatient(Patient patient) {
        if (patient == null || patient.getId() != null) {
            return 0;
        }

        try {
            patientRepository.saveAndFlush(patient);
            return 1;
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("Failed to create patient ({})", e.getClass().getSimpleName());
            return 0;
        }
    }

    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        try {
            Patient patient = findAuthenticatedPatient(token);
            if (patient == null || !patient.getId().equals(id)) {
                return error(HttpStatus.UNAUTHORIZED, "Access to these appointments is not authorized.");
            }

            return appointmentResponse(appointmentRepository.findByPatientId(patient.getId()));
        } catch (RuntimeException e) {
            return requestFailed("load patient appointments", e);
        }
    }

    public ResponseEntity<Map<String, Object>> getPatientAppointmentsForDoctor(Long patientId, String token) {
        if (patientId == null || patientId <= 0) {
            return error(HttpStatus.BAD_REQUEST, "A valid patient ID is required.");
        }

        try {
            if (!tokenService.validateToken(token, "doctor")) {
                return error(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
            }

            Doctor doctor = doctorRepository.findByEmail(tokenService.extractIdentifier(token));
            if (doctor == null) {
                return error(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
            }

            List<Appointment> appointments = appointmentRepository
                    .findByPatient_IdAndDoctor_IdOrderByAppointmentTimeAsc(patientId, doctor.getId());
            return appointmentResponse(appointments);
        } catch (JwtException e) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
        } catch (RuntimeException e) {
            return requestFailed("load patient appointments for doctor", e);
        }
    }

    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Integer status = statusForCondition(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST, "Condition must be past or future.");
        }
        if (id == null || id <= 0) {
            return error(HttpStatus.BAD_REQUEST, "A valid patient ID is required.");
        }

        try {
            List<Appointment> appointments = appointmentRepository
                    .findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);
            return appointmentResponse(appointments);
        } catch (RuntimeException e) {
            return requestFailed("filter patient appointments by condition", e);
        }
    }

    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        if (name == null || name.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "A doctor name is required.");
        }
        if (patientId == null || patientId <= 0) {
            return error(HttpStatus.BAD_REQUEST, "A valid patient ID is required.");
        }

        try {
            List<Appointment> appointments = appointmentRepository.filterByDoctorNameAndPatientId(name.trim(),
                    patientId);
            return appointmentResponse(appointments);
        } catch (RuntimeException e) {
            return requestFailed("filter patient appointments by doctor", e);
        }
    }

    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(
            String condition, String name, long patientId) {
        Integer status = statusForCondition(condition);
        if (status == null) {
            return error(HttpStatus.BAD_REQUEST, "Condition must be past or future.");
        }
        if (name == null || name.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "A doctor name is required.");
        }
        if (patientId <= 0) {
            return error(HttpStatus.BAD_REQUEST, "A valid patient ID is required.");
        }

        try {
            List<Appointment> appointments = appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(
                    name.trim(), patientId, status);
            return appointmentResponse(appointments);
        } catch (RuntimeException e) {
            return requestFailed("filter patient appointments by doctor and condition", e);
        }
    }

    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        try {
            Patient patient = findAuthenticatedPatient(token);
            if (patient == null) {
                return error(HttpStatus.UNAUTHORIZED, "Invalid patient token.");
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("id", patient.getId());
            details.put("name", patient.getName());
            details.put("email", patient.getEmail());
            details.put("phone", patient.getPhone());
            details.put("address", patient.getAddress());

            return ResponseEntity.ok(Map.of("patient", details));
        } catch (RuntimeException e) {
            return requestFailed("load patient details", e);
        }
    }

    private Patient findAuthenticatedPatient(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String email;
        try {
            if (!tokenService.validateToken(token, "patient")) {
                return null;
            }
            email = tokenService.extractIdentifier(token);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }

        if (email == null || email.isBlank()) {
            return null;
        }
        return patientRepository.findByEmail(email);
    }

    private Integer statusForCondition(String condition) {
        if (condition == null) {
            return null;
        }

        return switch (condition.trim().toLowerCase(Locale.ROOT)) {
            case "future" -> 0;
            case "past" -> 1;
            default -> null;
        };
    }

    private ResponseEntity<Map<String, Object>> appointmentResponse(List<Appointment> appointments) {
        List<AppointmentDTO> result = new ArrayList<>();
        for (Appointment appointment : appointments) {
            Patient patient = appointment.getPatient();
            Doctor doctor = appointment.getDoctor();
            result.add(new AppointmentDTO(
                    appointment.getId(),
                    doctor.getId(),
                    doctor.getName(),
                    patient.getId(),
                    patient.getName(),
                    null,
                    null,
                    null,
                    appointment.getAppointmentTime(),
                    appointment.getStatus()));
        }
        return ResponseEntity.ok(Map.of("appointments", result));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, Object>> requestFailed(String operation, RuntimeException e) {
        logger.error("Failed to {} ({})", operation, e.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request.");
    }
}
