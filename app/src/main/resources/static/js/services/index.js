import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

// Collega i pulsanti quando la pagina è stata caricata.
window.onload = function () {
    const adminBtn = document.getElementById("adminLogin");
    const doctorBtn = document.getElementById("doctorLogin");

    if (adminBtn) {
        adminBtn.addEventListener("click", () => {
            openModal("adminLogin");
        });
    }

    if (doctorBtn) {
        doctorBtn.addEventListener("click", () => {
            openModal("doctorLogin");
        });
    }
};

// Login dell'amministratore.
window.adminLoginHandler = async function () {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    const admin = { username, password };

    try {
        const response = await fetch(ADMIN_API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(admin)
        });

        if (!response.ok) {
            alert("Invalid credentials!");
            return;
        }

        const data = await response.json();

        if (!data.token) {
            throw new Error("The server did not return a login token.");
        }

        localStorage.setItem("token", data.token);
        selectRole("admin");
    } catch (error) {
        console.error("Admin login failed:", error);
        alert("Unable to log in. Please try again later.");
    }
};

// Login del medico.
window.doctorLoginHandler = async function () {
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    const doctor = { email, password };

    try {
        const response = await fetch(DOCTOR_API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(doctor)
        });

        if (!response.ok) {
            alert("Invalid credentials!");
            return;
        }

        const data = await response.json();

        if (!data.token) {
            throw new Error("The server did not return a login token.");
        }

        localStorage.setItem("token", data.token);
        selectRole("doctor");
    } catch (error) {
        console.error("Doctor login failed:", error);
        alert("Unable to log in. Please try again later.");
    }
};