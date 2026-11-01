import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// Ignora eventuali risposte a ricerche precedenti, arrivate in ritardo.
let latestRequest = 0;

document.addEventListener("DOMContentLoaded", () => {
    const addDoctorButton = document.getElementById("addDocBtn");

    if (addDoctorButton) {
        addDoctorButton.addEventListener("click", () => openModal("addDoctor"));
    }

    document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
    document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
    document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

    loadDoctorCards();
});

async function loadDoctorCards() {
    const requestId = ++latestRequest;

    try {
        const doctors = await getDoctors();

        if (requestId === latestRequest) {
            renderDoctorCards(doctors);
        }
    } catch (error) {
        console.error("Failed to load doctors:", error.message);

        if (requestId === latestRequest) {
            document.getElementById("content").textContent =
                "Error loading doctors. Try again later.";
        }
    }
}

async function filterDoctorsOnChange() {
    const requestId = ++latestRequest;
    const name = document.getElementById("searchBar").value.trim() || null;
    const time = document.getElementById("filterTime").value || null;
    const specialty = document.getElementById("filterSpecialty").value || null;

    try {
        const response = await filterDoctors(name, time, specialty);

        if (requestId === latestRequest) {
            renderDoctorCards(response.doctors);
        }
    } catch (error) {
        console.error("Failed to filter doctors:", error.message);

        if (requestId === latestRequest) {
            alert("An error occurred while filtering doctors.");
        }
    }
}

export function renderDoctorCards(doctors) {
    const contentDiv = document.getElementById("content");
    contentDiv.innerHTML = "";

    if (doctors.length === 0) {
        contentDiv.textContent = "No doctors found with the given filters.";
        return;
    }

    doctors.forEach(doctor => {
        contentDiv.appendChild(createDoctorCard(doctor));
    });
}

// Accessibile a modals.js quando viene premuto Save.
window.adminAddDoctor = async function () {
    const token = localStorage.getItem("token");

    if (!token) {
        alert("Please log in again before adding a doctor.");
        return;
    }

    const saveButton = document.getElementById("saveDoctorBtn");
    saveButton.disabled = true;

    try {
        const availableTimes = Array.from(
            document.querySelectorAll('#modal-body input[name="availability"]:checked')
        ).map(checkbox => checkbox.value);

        const doctor = {
            name: document.getElementById("doctorName").value.trim(),
            specialty: document.getElementById("specialization").value,
            email: document.getElementById("doctorEmail").value.trim(),
            password: document.getElementById("doctorPassword").value,
            phone: document.getElementById("doctorPhone").value.trim(),
            availableTimes
        };

        const { success, message } = await saveDoctor(doctor, token);
        alert(message);

        if (success) {
            document.getElementById("modal").style.display = "none";
            window.location.reload();
        }
    } catch (error) {
        console.error("Failed to add doctor:", error.message);
        alert("Unable to add doctor. Please try again.");
    } finally {
        saveButton.disabled = false;
    }
};
