package com.project.back_end.services;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;

@org.springframework.stereotype.Service
public class PrescriptionService {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public ResponseEntity<Map<String, String>> savePrescription(Prescription prescription) {
        if (prescription == null
                || prescription.getAppointmentId() == null
                || prescription.getAppointmentId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A valid appointment ID is required."));
        }

        Long appointmentId = prescription.getAppointmentId();
        try {
            if (hasPrescription(appointmentId)) {
                return duplicatePrescription();
            }

            Prescription newPrescription = new Prescription(
                    prescription.getPatientName(),
                    prescription.getMedication(),
                    prescription.getDosage(),
                    prescription.getDoctorNotes(),
                    appointmentId);

            // Concurrent requests for the same appointment share one unique MongoDB ID.
            newPrescription.setId("appointment-" + appointmentId);
            prescriptionRepository.insert(newPrescription);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Prescription saved."));
        } catch (DuplicateKeyException e) {
            return duplicatePrescription();
        } catch (RuntimeException e) {
            logger.error("Failed to save prescription ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to save the prescription."));
        }
    }

    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A valid appointment ID is required."));
        }

        try {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);

            if (prescriptions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "prescription", prescriptions,
                        "message", "No prescription exists for this appointment."));
            }

            return ResponseEntity.ok(Map.of("prescription", prescriptions));
        } catch (RuntimeException e) {
            logger.error("Failed to load prescription ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unable to load the prescription."));
        }
    }

    boolean hasPrescription(Long appointmentId) {
        return !prescriptionRepository.findByAppointmentId(appointmentId).isEmpty();
    }

    private ResponseEntity<Map<String, String>> duplicatePrescription() {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "A prescription already exists for this appointment."));
    }
}
