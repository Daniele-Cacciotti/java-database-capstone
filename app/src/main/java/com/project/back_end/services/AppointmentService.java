package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.server.ResponseStatusException;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.models.Prescription;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.JwtException;

@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final Service service;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PrescriptionService prescriptionService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            Service service,
            TokenService tokenService,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            PrescriptionService prescriptionService) {

        this.appointmentRepository = appointmentRepository;
        this.service = service;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.prescriptionService = prescriptionService;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.saveAndFlush(appointment);
            return 1;
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus()
                    .setRollbackOnly();
            return 0;
        }
    }

    public boolean matchesAuthenticatedPatient(Long patientId, String token) {
        if (patientId == null || !tokenService.validateToken(token, "patient")) {
            return false;
        }

        try {
            Patient patient = patientRepository.findByEmail(tokenService.extractIdentifier(token));
            return patient != null && patientId.equals(patient.getId());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Map<String, String>> reserveAppointment(Appointment request) {
        if (request.getId() != null || request.getDoctor().getId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Provide a doctor ID and omit the appointment ID."));
        }

        try {
            // Serialize availability checks and writes for this doctor.
            Doctor doctor = doctorRepository.findByIdForUpdate(request.getDoctor().getId())
                    .orElse(null);
            if (doctor == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Doctor not found."));
            }

            int availability = service.validateAppointment(request);
            if (availability != 1) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "The requested appointment time is unavailable."));
            }

            Appointment appointment = new Appointment();
            appointment.setDoctor(doctor);
            appointment.setPatient(patientRepository.getReferenceById(request.getPatient().getId()));
            appointment.setAppointmentTime(request.getAppointmentTime());
            appointment.setStatus(0);

            if (bookAppointment(appointment) == 1) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Appointment booked successfully."));
            }
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to book appointment."));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<Map<String, String>> updateAppointment(
            Appointment appointment) {

        try {
            if (appointment == null
                    || appointment.getId() == null
                    || appointment.getPatient() == null
                    || appointment.getPatient().getId() == null
                    || appointment.getDoctor() == null
                    || appointment.getDoctor().getId() == null
                    || appointment.getAppointmentTime() == null) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Missing appointment information."));
            }

            Appointment existing = appointmentRepository
                    .findById(appointment.getId())
                    .orElse(null);

            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Appointment not found."));
            }

            if (!Objects.equals(
                    existing.getPatient().getId(),
                    appointment.getPatient().getId())) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Patient does not match."));
            }

            if (!appointment.getAppointmentTime()
                    .isAfter(LocalDateTime.now())) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "message",
                                "Appointment must be in the future."));
            }

            boolean doctorChanged = !Objects.equals(
                    existing.getDoctor().getId(),
                    appointment.getDoctor().getId());

            boolean timeChanged = !Objects.equals(
                    existing.getAppointmentTime(),
                    appointment.getAppointmentTime());

            if (doctorChanged || timeChanged) {
                Doctor doctor = doctorRepository.findByIdForUpdate(appointment.getDoctor().getId())
                        .orElse(null);
                if (doctor == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Doctor not found."));
                }

                if (service.validateAppointment(appointment) != 1) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(
                                    "message",
                                    "The requested appointment time is unavailable."));
                }

                existing.setDoctor(doctor);

                existing.setAppointmentTime(
                        appointment.getAppointmentTime());
            }

            appointmentRepository.saveAndFlush(existing);

            return ResponseEntity.ok(
                    Map.of("message", "Appointment updated successfully."));

        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus()
                    .setRollbackOnly();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update appointment."));
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        try {
            if (!tokenService.validateToken(token, "patient")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid patient token."));
            }

            String email = tokenService.extractIdentifier(token);
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Patient not found."));
            }

            Appointment appointment = appointmentRepository.findById(id).orElse(null);
            if (appointment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Appointment not found."));
            }

            if (!Objects.equals(patient.getId(), appointment.getPatient().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "This appointment belongs to another patient."));
            }

            appointmentRepository.delete(appointment);
            appointmentRepository.flush();

            return ResponseEntity.ok(Map.of("message", "Appointment cancelled successfully."));
        } catch (DataAccessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to cancel appointment."));
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        if (!tokenService.validateToken(token, "doctor")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
        }

        Doctor doctor = doctorRepository.findByEmail(tokenService.extractIdentifier(token));
        if (doctor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Doctor not found.");
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime nextDay = date.plusDays(1).atStartOfDay();

        String name = pname == null ? "" : pname.trim();
        List<Appointment> appointments;

        if (name.isEmpty() || "null".equalsIgnoreCase(name)) {
            appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    doctor.getId(), start, nextDay);
        } else {
            appointments = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctor.getId(), name, start, nextDay);
        }

        List<AppointmentDTO> result = new ArrayList<>();

        for (Appointment appointment : appointments) {
            // BETWEEN includes both endpoints, so exclude midnight of the next day.
            if (!appointment.getAppointmentTime().isBefore(nextDay)) {
                continue;
            }

            Patient patient = appointment.getPatient();
            result.add(new AppointmentDTO(
                    appointment.getId(),
                    doctor.getId(),
                    doctor.getName(),
                    patient.getId(),
                    patient.getName(),
                    patient.getEmail(),
                    patient.getPhone(),
                    patient.getAddress(),
                    appointment.getAppointmentTime(),
                    appointment.getStatus()));
        }

        return Map.of("appointments", result);
    }

    @Transactional
    public ResponseEntity<Map<String, String>> issuePrescription(Prescription prescription, String token) {
        Long appointmentId = prescription.getAppointmentId();
        String doctorEmail = verifiedDoctorEmail(appointmentId, token);
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));
        checkAssignedDoctor(appointment, doctorEmail);

        ResponseEntity<Map<String, String>> result = prescriptionService.savePrescription(prescription);
        if (result.getStatusCode().equals(HttpStatus.CREATED)) {
            changeStatus(appointmentId, 1);
            return result;
        }

        // A prior request may have saved to MongoDB before MySQL completed.
        if (result.getStatusCode().equals(HttpStatus.BAD_REQUEST)
                && appointment.getStatus() != 1
                && prescriptionService.hasPrescription(appointmentId)) {
            changeStatus(appointmentId, 1);
            return ResponseEntity.ok(Map.of("message", "Prescription already saved. Appointment completed."));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public void requireDoctorAccess(Long appointmentId, String token) {
        String doctorEmail = verifiedDoctorEmail(appointmentId, token);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));
        checkAssignedDoctor(appointment, doctorEmail);
    }

    private String verifiedDoctorEmail(Long appointmentId, String token) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid appointment ID is required.");
        }
        try {
            if (tokenService.validateToken(token, "doctor")) {
                return tokenService.extractIdentifier(token);
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid doctor token.", e);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid doctor token.");
    }

    private void checkAssignedDoctor(Appointment appointment, String doctorEmail) {
        if (!doctorEmail.equals(appointment.getDoctor().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This appointment belongs to another doctor.");
        }
    }

    @Transactional
    public void changeStatus(long id, int status) {
        appointmentRepository.updateStatus(status, id);
    }

}
