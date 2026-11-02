package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(
            DoctorRepository doctorRepository,
            AppointmentRepository appointmentRepository,
            TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null) {
            return List.of();
        }

        // Include visits that started shortly before midnight and may still be in
        // progress.
        LocalDateTime start = date.atStartOfDay();
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(
                        doctorId, start.minusHours(1), start.plusDays(1));

        List<String> availableSlots = new ArrayList<>();
        for (String value : doctor.getAvailableTimes()) {
            Slot slot = parseSlot(value);
            if (slot == null) {
                continue;
            }

            LocalDateTime slotStart = date.atTime(slot.start());
            LocalDateTime slotEnd = date.atTime(slot.end());
            boolean booked = appointments.stream()
                    .anyMatch(appointment -> appointment.getAppointmentTime().isBefore(slotEnd)
                            && appointment.getEndTime().isAfter(slotStart));

            if (!booked) {
                availableSlots.add(value);
            }
        }
        return availableSlots;
    }

    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor.getId() != null) {
                return 0;
            }
            if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
                return -1;
            }

            doctorRepository.saveAndFlush(doctor);
            return 1;
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 0;
        }
    }

    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null || !doctorRepository.existsById(doctor.getId())) {
                return -1;
            }

            Doctor emailOwner = doctorRepository.findByEmail(doctor.getEmail());
            if (emailOwner != null && !emailOwner.getId().equals(doctor.getId())) {
                return 0;
            }

            doctorRepository.saveAndFlush(doctor);
            return 1;
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 0;
        }
    }

    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        initializeAvailableTimes(doctors);
        return doctors;
    }

    @Transactional
    public int deleteDoctor(long id) {
        try {
            Doctor doctor = doctorRepository.findById(id).orElse(null);
            if (doctor == null) {
                return -1;
            }

            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.delete(doctor);
            doctorRepository.flush();
            return 1;
        } catch (RuntimeException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 0;
        }
    }

    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email and password are required."));
        }

        Doctor doctor = doctorRepository.findByEmail(login.getIdentifier());
        if (doctor == null || !login.getPassword().equals(doctor.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        }

        String token = tokenService.generateToken(doctor.getEmail(), "doctor");
        return ResponseEntity.ok(Map.of("token", token));
    }

    public Map<String, Object> findDoctorByName(String name) {
        return doctorResults(doctorRepository.findByNameLike(name));
    }

    public Map<String, Object> filterDoctorsByNameSpecilityandTime(
            String name, String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return doctorResults(filterDoctorByTime(doctors, amOrPm));
    }

    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        return doctorResults(filterDoctorByTime(doctors, amOrPm));
    }

    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        return doctorResults(doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty));
    }

    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        return doctorResults(filterDoctorByTime(doctors, amOrPm));
    }

    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        return doctorResults(doctorRepository.findBySpecialtyIgnoreCase(specialty));
    }

    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        return doctorResults(filterDoctorByTime(doctorRepository.findAll(), amOrPm));
    }

    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) {
            return doctors;
        }

        String filter = amOrPm.trim();
        boolean morning = "AM".equalsIgnoreCase(filter);
        boolean afternoon = "PM".equalsIgnoreCase(filter);
        Slot exactSlot = parseSlot(filter);

        List<Doctor> result = new ArrayList<>();
        for (Doctor doctor : doctors) {
            for (String value : doctor.getAvailableTimes()) {
                Slot slot = parseSlot(value);
                if (slot == null) {
                    continue;
                }

                boolean startsBeforeNoon = slot.start().isBefore(LocalTime.NOON);
                if ((morning && startsBeforeNoon)
                        || (afternoon && !startsBeforeNoon)
                        || slot.equals(exactSlot)) {
                    result.add(doctor);
                    break;
                }
            }
        }
        return result;
    }

    private Map<String, Object> doctorResults(List<Doctor> doctors) {
        initializeAvailableTimes(doctors);
        return Map.of("doctors", doctors);
    }

    private void initializeAvailableTimes(List<Doctor> doctors) {
        for (Doctor doctor : doctors) {
            // Access the lazy collection while the transaction is open.
            doctor.getAvailableTimes().size();
        }
    }

    private Slot parseSlot(String value) {
        if (value == null) {
            return null;
        }
        String[] times = value.split("-", -1);
        if (times.length != 2) {
            return null;
        }

        try {
            LocalTime start = LocalTime.parse(times[0].trim());
            LocalTime end = LocalTime.parse(times[1].trim());
            return end.isAfter(start) ? new Slot(start, end) : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record Slot(LocalTime start, LocalTime end) {
    }
}
