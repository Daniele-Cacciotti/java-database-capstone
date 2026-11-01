import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

export function createDoctorCard(doctor) {
    const card = document.createElement("div");
    card.classList.add("doctor-card");

    const role = localStorage.getItem("userRole");

    const infoDiv = document.createElement("div");
    infoDiv.classList.add("doctor-info");

    const name = document.createElement("h3");
    name.textContent = doctor.name;

    const specialization = document.createElement("p");
    specialization.textContent = `Specialty: ${doctor.specialty}`;

    const email = document.createElement("p");
    email.textContent = `Email: ${doctor.email}`;

    const availability = document.createElement("p");
    availability.textContent =
        `Available times: ${(doctor.availableTimes || []).join(", ")}`;

    infoDiv.appendChild(name);
    infoDiv.appendChild(specialization);
    infoDiv.appendChild(email);
    infoDiv.appendChild(availability);

    const actionsDiv = document.createElement("div");
    actionsDiv.classList.add("card-actions");

    if (role === "admin") {
        const removeBtn = document.createElement("button");
        removeBtn.type = "button";
        removeBtn.textContent = "Delete";

        removeBtn.addEventListener("click", async () => {
            if (!confirm(`Delete ${doctor.name}?`)) {
                return;
            }

            const token = localStorage.getItem("token");

            if (!token) {
                alert("Please log in again.");
                window.location.href = "/";
                return;
            }

            removeBtn.disabled = true;

            try {
                const result = await deleteDoctor(doctor.id, token);

                if (result.success) {
                    card.remove();
                    alert(result.message || "Doctor deleted successfully.");
                } else {
                    alert(result.message || "Unable to delete the doctor.");
                }
            } catch (error) {
                console.error("Failed to delete doctor:", error);
                alert("Unable to delete the doctor. Please try again.");
            } finally {
                removeBtn.disabled = false;
            }
        });

        actionsDiv.appendChild(removeBtn);
    } else if (role === "patient") {
        const bookNow = document.createElement("button");
        bookNow.type = "button";
        bookNow.textContent = "Book Now";

        bookNow.addEventListener("click", () => {
            alert("Patient needs to login first.");
        });

        actionsDiv.appendChild(bookNow);
    } else if (role === "loggedPatient") {
        const bookNow = document.createElement("button");
        bookNow.type = "button";
        bookNow.textContent = "Book Now";

        bookNow.addEventListener("click", async (event) => {
            const token = localStorage.getItem("token");

            if (!token) {
                localStorage.setItem("userRole", "patient");
                alert("Please log in before booking an appointment.");
                window.location.href = "/pages/patientDashboard.html";
                return;
            }

            bookNow.disabled = true;

            try {
                const patientData = await getPatientData(token);

                if (!patientData) {
                    alert("Unable to load your patient profile. Please log in again.");
                    return;
                }

                // This file also initializes the logged-patient page.
                // Load it only when a logged-in patient opens booking.
                const { showBookingOverlay } = await import("../loggedPatient.js");
                showBookingOverlay(event, doctor, patientData);
            } catch (error) {
                console.error("Failed to open booking:", error);
                alert("Unable to open booking. Please try again.");
            } finally {
                bookNow.disabled = false;
            }
        });

        actionsDiv.appendChild(bookNow);
    }

    card.appendChild(infoDiv);
    card.appendChild(actionsDiv);

    return card;
}
