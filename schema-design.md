## MySQL Database Design

MySQL stores the clinic's structured data: patients, doctors,
appointments, administrator accounts, and doctor availability.

### Table: patients

- id: BIGINT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- email: VARCHAR(255), Not Null, Unique
- password: VARCHAR(255), Not Null
- phone: VARCHAR(20), Not Null
- address: VARCHAR(255), Not Null

**Design notes:**
- The password column stores a password hash.
- Email format, phone format, and required text fields are validated in the application.
- A patient can have multiple appointments.

### Table: doctors

- id: BIGINT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- specialty: VARCHAR(50), Not Null
- email: VARCHAR(255), Not Null, Unique
- password: VARCHAR(255), Not Null
- phone: VARCHAR(20), Not Null

**Design notes:**
- The password column stores a password hash.
- Email and phone formats are validated in the application.
- Each doctor can have multiple appointments and available time slots.

### Table: doctor_available_times

- doctor_id: BIGINT, Not Null, Foreign Key → doctors(id)
- available_times: VARCHAR(50), Not Null
- Primary Key: (doctor_id, available_times)

**Design notes:**
- Each row represents an available time slot for one doctor.
- The composite primary key prevents duplicate slots for the same doctor.
- Slot values must follow a consistent format validated by the application.
- Deleting a doctor also deletes their availability rows
  using ON DELETE CASCADE.
- Existing appointments must still be considered when calculating
  which slots can be booked.

### Table: appointments

- id: BIGINT, Primary Key, Auto Increment
- doctor_id: BIGINT, Not Null, Foreign Key → doctors(id)
- patient_id: BIGINT, Not Null, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: TINYINT, Not Null, Default 0
  (0 = Scheduled, 1 = Completed, 2 = Cancelled)

**Design notes:**
- Each appointment belongs to exactly one doctor and one patient.
- Each appointment lasts one hour; its end time is calculated
  from appointment_time.
- A CHECK constraint restricts status to 0, 1, or 2.
- New bookings must use future times within the doctor's availability.
- The booking service prevents overlapping active appointments
  for both the doctor and the patient.
- Availability checks and booking creation use a transaction
  with appropriate locking to prevent concurrent double bookings.
- Foreign keys use ON DELETE RESTRICT to prevent deleting a doctor
  or patient while their appointment records still exist.
- An index on (doctor_id, appointment_time) supports calendar queries.
- An index on (patient_id, appointment_time) supports patient bookings.

### Table: admin

- id: BIGINT, Primary Key, Auto Increment
- username: VARCHAR(50), Not Null, Unique
- password: VARCHAR(255), Not Null

**Design notes:**
- The password column stores a password hash.
- Required fields are validated in the application.
- Admin accounts are kept separate from doctor and patient accounts.

### Relationships and Data Management

- One patient can have many appointments.
- One doctor can have many appointments.
- One doctor can have many available time slots.
- Appointment history is preserved when referenced accounts cannot
  be deleted; long-term retention is managed through a separate policy.
- Cancelled appointments remain in the history but do not block
  future bookings for the same slot.
- Prescriptions are stored in MongoDB and linked to an appointment
  through appointmentId.
- The application validates this cross-database reference because
  MySQL foreign keys cannot enforce relationships with MongoDB documents.



  ## MongoDB Collection Design

MongoDB stores prescriptions, which combine core information
with optional notes and flexible metadata.

### Collection: prescriptions

```json
{
  "_id": "rx-demo-001",
  "appointmentId": 51,
  "patientName": "Alex Example",
  "medication": "Example medication",
  "doctorNotes": "Follow-up consultation requested.",
  "tags": [
    "outpatient",
    "follow-up"
  ],
  "pharmacy": {
    "name": "Example Pharmacy",
    "location": "Example City"
  },
  "metadata": {
    "schemaVersion": 1,
    "source": "doctor_portal",
    "language": "en"
  }
}
```

### Design Decisions

- Each document represents a prescription associated with an appointment.
- `_id` uniquely identifies the document. This example uses a string ID.
- `appointmentId` references the corresponding MySQL appointment.
- The full patient object is not duplicated. The application can retrieve
  patient and doctor details through the appointment.
- `patientName` is retained as a snapshot of the name when the prescription
  was issued.
- `doctorNotes` stores free-form text.
- `tags` is an array that supports categorization.
- `pharmacy` embeds related optional information in the same document.
- `metadata` groups additional information without changing MySQL tables.

### Validation and Schema Evolution

- The application verifies that appointmentId references an existing
  appointment and that the doctor is authorized to issue the prescription.
- Required fields are validated before saving.
- MongoDB does not enforce foreign keys to MySQL.
- Optional fields can be added as requirements evolve.
- Existing documents may omit newly introduced optional fields.
- schemaVersion helps the application distinguish document formats
  when structural changes require migration.