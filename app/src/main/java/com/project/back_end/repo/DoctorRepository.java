package com.project.back_end.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Doctor;

import jakarta.persistence.LockModeType;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT d
            FROM Doctor d
            WHERE d.email = :email
            """)
    Doctor findByEmail(@Param("email") String email);

    @Query("""
            SELECT d
            FROM Doctor d
            WHERE d.name LIKE CONCAT('%', :name, '%')
            """)
    List<Doctor> findByNameLike(@Param("name") String name);

    @Query("""
            SELECT d
            FROM Doctor d
            WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :string, '%')) AND d.specialty = :specialty
            """)
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
            @Param("string") String string,
            @Param("specialty") String specialty);

    @Query("""
            SELECT d
            FROM Doctor d
            WHERE LOWER(d.specialty) = LOWER(:specialty)
            """)
    List<Doctor> findBySpecialtyIgnoreCase(@Param("specialty") String specialty);
}
