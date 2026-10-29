# Smart Clinic Portal — User Stories

## Admin User Stories

### 1. Admin: Log in

**Title:**
As an admin, I want to log into the portal with my username
and password, so that I can manage the platform securely.

**Acceptance Criteria:**
1. Valid admin credentials grant access to the admin area.
2. Invalid credentials display an error without granting access.
3. Unauthenticated users cannot access admin functions.

**Priority:** High
**Story Points:** 3
**Notes:**
- Passwords must not be displayed or logged in plain text.

### 2. Admin: Log out

**Title:**
As an admin, I want to log out of the portal,
so that I can protect the platform from unauthorized access.

**Acceptance Criteria:**
1. An authenticated admin can select the logout option.
2. Logging out invalidates the current session.
3. Accessing admin functions after logout requires logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Previously accessible admin pages must remain protected after logout.

### 3. Admin: Add a doctor

**Title:**
As an admin, I want to add doctors to the portal,
so that patients can find them and book appointments.

**Acceptance Criteria:**
1. Only an authenticated admin can add a doctor.
2. Missing or invalid required information produces an error.
3. A successfully added doctor appears in the doctors list.

**Priority:** High
**Story Points:** 3
**Notes:**
- Required doctor information must be validated before saving.

### 4. Admin: Delete a doctor

**Title:**
As an admin, I want to delete a doctor's profile,
so that doctors who no longer use the platform are removed.

**Acceptance Criteria:**
1. Only an authenticated admin can delete a doctor's profile.
2. The admin must confirm the deletion before it is performed.
3. After successful deletion, the doctor no longer appears
   in the list available for new bookings.

**Priority:** High
**Story Points:** 3
**Notes:**
- Existing appointments must be handled consistently
  to avoid broken references or accidental data loss.

### 5. Admin: Count monthly appointments

**Title:**
As an admin, I want to run a stored procedure in the MySQL CLI
to count appointments per month, so that I can track usage statistics.

**Acceptance Criteria:**
1. The stored procedure can be executed from the MySQL CLI
   using an authorized database account.
2. Results show appointment counts grouped by year and month.
3. Counts match the appointment records stored in the database.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Months from different years must be counted separately.
- Running the procedure must not modify appointment data.

## Patient User Stories

### 1. Patient: View doctors without logging in

**Title:**
As a visitor, I want to view the doctors list without logging in,
so that I can explore available doctors before registering.

**Acceptance Criteria:**
1. The doctors list is accessible without authentication.
2. Each entry displays the doctor's name and specialization.
3. If no doctors are available, an appropriate message is shown.

**Priority:** High
**Story Points:** 2
**Notes:**
- Booking an appointment requires authentication.

### 2. Patient: Sign up

**Title:**
As a patient, I want to register using my email and password,
so that I can book appointments.

**Acceptance Criteria:**
1. Valid registration information creates a patient account.
2. Invalid email addresses or missing required fields show an error.
3. An email already associated with an account cannot register again.

**Priority:** High
**Story Points:** 3
**Notes:**
- Passwords must be securely hashed before storage.

### 3. Patient: Log in

**Title:**
As a patient, I want to log into the portal,
so that I can manage my bookings.

**Acceptance Criteria:**
1. Valid patient credentials grant access to patient functions.
2. Invalid credentials display an error without granting access.
3. Unauthenticated users cannot access personal bookings.

**Priority:** High
**Story Points:** 3
**Notes:**
- Patients can access only their own personal information.

### 4. Patient: Log out

**Title:**
As a patient, I want to log out of the portal,
so that I can secure my account.

**Acceptance Criteria:**
1. An authenticated patient can select the logout option.
2. Logging out invalidates the current session.
3. Accessing personal bookings afterward requires logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Personal pages must remain protected after logout.

### 5. Patient: Book an appointment

**Title:**
As a patient, I want to book an hour-long appointment,
so that I can consult with a doctor.

**Acceptance Criteria:**
1. Only an authenticated patient can book an appointment.
2. The patient can select an available doctor and one-hour slot.
3. A successful booking is saved and confirmed to the patient.

**Priority:** High
**Story Points:** 5
**Notes:**
- Past slots and unavailable slots cannot be booked.
- Conflicting bookings for the same doctor must be prevented.

### 6. Patient: View upcoming appointments

**Title:**
As a patient, I want to view my upcoming appointments,
so that I can prepare for my consultations.

**Acceptance Criteria:**
1. An authenticated patient sees only their own upcoming appointments.
2. Each appointment displays the doctor, date, and time.
3. If there are no upcoming appointments, a clear message is shown.

**Priority:** High
**Story Points:** 2
**Notes:**
- Appointments are displayed in chronological order.

## Doctor User Stories

### 1. Doctor: Log in

**Title:**
As a doctor, I want to log into the portal,
so that I can manage my appointments.

**Acceptance Criteria:**
1. Valid doctor credentials grant access to the doctor area.
2. Invalid credentials display an error without granting access.
3. Unauthenticated users cannot access doctor functions.

**Priority:** High
**Story Points:** 3
**Notes:**
- Doctors can access only their own appointment information.

### 2. Doctor: Log out

**Title:**
As a doctor, I want to log out of the portal,
so that I can protect my data.

**Acceptance Criteria:**
1. An authenticated doctor can select the logout option.
2. Logging out invalidates the current session.
3. Accessing protected doctor pages afterward requires logging in again.

**Priority:** High
**Story Points:** 1
**Notes:**
- Previously accessible pages must remain protected after logout.

### 3. Doctor: View appointment calendar

**Title:**
As a doctor, I want to view my appointment calendar,
so that I can organize my consultations.

**Acceptance Criteria:**
1. An authenticated doctor can view their own appointment calendar.
2. Each appointment displays its date, time, and patient name.
3. Dates without appointments display an appropriate empty state.

**Priority:** High
**Story Points:** 3
**Notes:**
- Appointments must appear at their scheduled dates and times.

### 4. Doctor: Mark unavailability

**Title:**
As a doctor, I want to mark periods when I am unavailable,
so that patients can book only available slots.

**Acceptance Criteria:**
1. An authenticated doctor can mark their own unavailable periods.
2. Unavailable periods cannot be selected for new bookings.
3. Invalid time ranges display an error and are not saved.

**Priority:** High
**Story Points:** 5
**Notes:**
- Conflicts with existing appointments must be flagged
  rather than silently cancelling those appointments.

### 5. Doctor: Update profile

**Title:**
As a doctor, I want to update my specialization and contact information,
so that patients can see accurate information about me.

**Acceptance Criteria:**
1. An authenticated doctor can edit only their own profile.
2. Missing or invalid required information displays an error.
3. Successfully saved changes appear in the doctor's profile.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Profile updates must not change the doctor's role or permissions.

### 6. Doctor: View patient details for upcoming appointments

**Title:**
As a doctor, I want to view patient details for my upcoming appointments,
so that I can prepare for each consultation.

**Acceptance Criteria:**
1. An authenticated doctor can select an upcoming appointment.
2. The selected appointment displays the associated patient's
   relevant details.
3. Doctors cannot access patient details through appointments
   assigned to other doctors.

**Priority:** High
**Story Points:** 3
**Notes:**
- Display only information necessary for the consultation.