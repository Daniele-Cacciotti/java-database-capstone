import { openModal } from "./modals.js";

function renderHeader() {
    const headerDiv = document.getElementById("header");

    if (!headerDiv) {
        return;
    }

    // Sulla homepage mostriamo soltanto logo e titolo.
    const pathname = window.location.pathname;
    const isHomePage = pathname === "/" || pathname === "/index.html";

    if (isHomePage) {
        localStorage.removeItem("userRole");
        localStorage.removeItem("token");
    }

    const role = localStorage.getItem("userRole");
    const token = localStorage.getItem("token");

    // Il lab richiede di verificare la presenza del token
    // per i ruoli che rappresentano utenti autenticati.
    const requiresLogin =
        role === "admin" ||
        role === "doctor" ||
        role === "loggedPatient";

    if (requiresLogin && !token) {
        localStorage.removeItem("userRole");
        alert("Session expired or invalid login. Please log in again.");
        window.location.href = "/";
        return;
    }

    let headerContent = `
        <header class="header">
            <div class="logo-section logo-link">
                <img
                    src="/assets/images/logo/logo.png"
                    alt="Hospital CMS Logo"
                    class="logo-img">
                <span class="logo-title">Hospital CMS</span>
            </div>
    `;

    if (!isHomePage) {
        headerContent += `<nav aria-label="Main navigation">`;

        if (role === "admin") {
            headerContent += `
                <button type="button" id="addDocBtn" class="adminBtn">
                    Add Doctor
                </button>
                <a href="/" id="logoutLink">Logout</a>
            `;
        } else if (role === "doctor") {
            headerContent += `
                <button type="button" id="doctorHome" class="adminBtn">
                    Home
                </button>
                <a href="/" id="logoutLink">Logout</a>
            `;
        } else if (role === "patient") {
            headerContent += `
                <button type="button" id="patientLogin" class="adminBtn">
                    Login
                </button>
                <button type="button" id="patientSignup" class="adminBtn">
                    Sign Up
                </button>
            `;
        } else if (role === "loggedPatient") {
            headerContent += `
                <button type="button" id="home" class="adminBtn">
                    Home
                </button>
                <button
                    type="button"
                    id="patientAppointments"
                    class="adminBtn">
                    Appointments
                </button>
                <a
                    href="/pages/patientDashboard.html"
                    id="logoutPatientLink">
                    Logout
                </a>
            `;
        }

        headerContent += `</nav>`;
    }

    headerContent += `</header>`;

    // Prima creiamo gli elementi, poi colleghiamo gli eventi.
    headerDiv.innerHTML = headerContent;
    attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
    // Collega un evento soltanto se l'elemento esiste nella pagina.
    function onClick(id, handler) {
        const element = document.getElementById(id);

        if (element) {
            element.addEventListener("click", handler);
        }
    }

    onClick("adminLogin", () => {
        openModal("adminLogin");
    });

    onClick("doctorLogin", () => {
        openModal("doctorLogin");
    });

    onClick("addDocBtn", () => {
        openModal("addDoctor");
    });

    onClick("doctorHome", () => {
        selectRole("doctor");
    });

    onClick("home", () => {
        window.location.href = "/pages/loggedPatientDashboard.html";
    });

    onClick("patientAppointments", () => {
        window.location.href = "/pages/patientAppointments.html";
    });

    onClick("logoutLink", (event) => {
        event.preventDefault();
        logout();
    });

    onClick("logoutPatientLink", (event) => {
        event.preventDefault();
        logoutPatient();
    });
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    window.location.href = "/";
}

function logoutPatient() {
    localStorage.removeItem("token");
    localStorage.setItem("userRole", "patient");
    window.location.href = "/pages/patientDashboard.html";
}

renderHeader();