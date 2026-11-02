package com.project.back_end.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            SELECT p
            FROM Patient p
            WHERE p.email = :email
            """)
    Patient findByEmail(@Param("email") String email);

    @Query("""
            SELECT p
            FROM Patient p
            WHERE p.email = :email OR p.phone = :phone
            """)
    Patient findByEmailOrPhone(
            @Param("email") String email,
            @Param("phone") String phone);
    // 2. Custom Query Methods:

    // - **findByEmail**:
    // - This method retrieves a Patient by their email address.
    // - Return type: Patient
    // - Parameters: String email

    // - **findByEmailOrPhone**:
    // - This method retrieves a Patient by either their email or phone number,
    // allowing flexibility for the search.
    // - Return type: Patient
    // - Parameters: String email, String phone

}
