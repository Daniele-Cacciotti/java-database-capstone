import { getDoctors, filterDoctors } from "./services/doctorServices.js";
import { openModal } from "./components/modals.js";
import { createDoctorCard } from "./components/doctorCard.js";
import { patientSignup, patientLogin } from "./services/patientServices.js";

let latestRequest = 0;

document.addEventListener("DOMContentLoaded", () => {
    const signupButton = document.getElementById("patientSignup");
    const loginButton = document.getElementById("patientLogin");

    if (signupButton) {
        signupButton.addEventListener("click", () => openModal("patientSignup"));
    }

    if (loginButton) {
        loginButton.addEventListener("click", () => openModal("patientLogin"));
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

// Queste funzioni globali sono richiamate dai pulsanti creati da modals.js.
window.signupPatient = async function () {
    const signupButton = document.getElementById("signupBtn");
    signupButton.disabled = true;

    try {
        const data = {
            name: document.getElementById("name").value.trim(),
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value,
            phone: document.getElementById("phone").value.trim(),
            address: document.getElementById("address").value.trim()
        };

        const { success, message } = await patientSignup(data);
        alert(message);

        if (success) {
            document.getElementById("modal").style.display = "none";
            window.location.reload();
        }
    } catch (error) {
        console.error("Signup failed:", error.message);
        alert("An error occurred while signing up.");
    } finally {
        signupButton.disabled = false;
    }
};

window.loginPatient = async function () {
    const loginButton = document.getElementById("loginBtn");
    loginButton.disabled = true;

    try {
        const data = {
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value
        };

        const response = await patientLogin(data);

        if (!response.ok) {
            alert("Invalid credentials!");
            return;
        }

        const result = await response.json();

        if (!result.token) {
            throw new Error("The server did not return a login token.");
        }

        // Salva prima la sessione, poi apri la dashboard autenticata.
        localStorage.setItem("token", result.token);
        localStorage.setItem("userRole", "loggedPatient");
        window.location.href = "/pages/loggedPatientDashboard.html";
    } catch (error) {
        console.error("Patient login failed:", error.message);
        alert("Unable to log in. Please try again later.");
    } finally {
        loginButton.disabled = false;
    }
};
