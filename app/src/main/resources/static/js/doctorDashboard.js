import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");
let selectedDate = getTodayDate();
let patientName = null;
let latestRequest = 0;

// Costruisce la data locale, evitando lo spostamento di giorno dovuto a UTC.
function getTodayDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

document.addEventListener("DOMContentLoaded", () => {
    renderContent();

    const searchBar = document.getElementById("searchBar");
    const datePicker = document.getElementById("datePicker");
    const todayButton = document.getElementById("todayButton");

    datePicker.value = selectedDate;

    searchBar.addEventListener("input", () => {
        patientName = searchBar.value.trim() || "null";
        loadAppointments();
    });

    todayButton.addEventListener("click", () => {
        selectedDate = getTodayDate();
        datePicker.value = selectedDate;
        loadAppointments();
    });

    datePicker.addEventListener("change", () => {
        selectedDate = datePicker.value || getTodayDate();
        datePicker.value = selectedDate;
        loadAppointments();
    });

    loadAppointments();
});

function showTableMessage(message) {
    tableBody.innerHTML = "";

    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 5;
    cell.classList.add("noPatientRecord");
    cell.textContent = message;

    row.appendChild(cell);
    tableBody.appendChild(row);
}

async function loadAppointments() {
    const requestId = ++latestRequest;

    if (!token) {
        showTableMessage("Please log in again to view appointments.");
        return;
    }

    try {
        const data = await getAllAppointments(selectedDate, patientName, token);

        if (requestId !== latestRequest) {
            return;
        }

        // Accetta una lista diretta oppure il contenitore { appointments: [...] }.
        const appointments = Array.isArray(data) ? data : data.appointments;

        if (!Array.isArray(appointments)) {
            throw new Error("Unexpected appointment response.");
        }

        tableBody.innerHTML = "";

        if (appointments.length === 0) {
            showTableMessage(selectedDate === getTodayDate()
                ? "No Appointments found for today."
                : "No Appointments found for the selected date.");
            return;
        }

        appointments.forEach(appointment => {
            // I nomi dei campi corrispondono ad AppointmentDTO.
            const patient = {
                id: appointment.patientId,
                name: appointment.patientName,
                phone: appointment.patientPhone,
                email: appointment.patientEmail
            };

            const row = createPatientRow(patient, appointment.id, appointment.doctorId);
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error("Failed to load appointments:", error.message);

        if (requestId === latestRequest) {
            showTableMessage("Error loading appointments. Try again later.");
        }
    }
}
