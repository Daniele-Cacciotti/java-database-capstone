import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = API_BASE_URL + "/patient";

// Registra un paziente e restituisce esito e messaggio.
export async function patientSignup(data) {
    try {
        const response = await fetch(PATIENT_API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        return {
            success: response.ok,
            message: result.message || (
                response.ok
                    ? "Registration successful."
                    : "Registration failed."
            )
        };
    } catch (error) {
        console.error("patientSignup:", error.message);

        return {
            success: false,
            message: "Unable to register. Please try again."
        };
    }
}

// Restituisce la Response: la dashboard leggerà stato e token.
export async function patientLogin(data) {
    try {
        return await fetch(`${PATIENT_API}/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(data)
        });
    } catch (error) {
        console.error("patientLogin:", error.message);

        // La dashboard gestisce l'errore di rete nel proprio catch.
        throw error;
    }
}

// Recupera il profilo del paziente identificato dal token.
export async function getPatientData(token) {
    try {
        const response = await fetch(
            `${PATIENT_API}/${encodeURIComponent(token)}`
        );

        if (!response.ok) {
            throw new Error("Unable to load patient profile.");
        }

        const data = await response.json();
        return data.patient || null;
    } catch (error) {
        console.error("getPatientData:", error.message);
        return null;
    }
}

// Endpoint condiviso dalle pagine del paziente e del medico.
export async function getPatientAppointments(id, token, user) {
    try {
        const response = await fetch(
            `${PATIENT_API}/${encodeURIComponent(id)}/${encodeURIComponent(user)}/${encodeURIComponent(token)}`
        );

        if (!response.ok) {
            throw new Error("Unable to load patient appointments.");
        }

        const data = await response.json();

        return Array.isArray(data.appointments)
            ? data.appointments
            : null;
    } catch (error) {
        console.error("getPatientAppointments:", error.message);
        return null;
    }
}

// Mantiene il formato { appointments: [...] } usato dalla dashboard.
export async function filterAppointments(condition, name, token) {
    const conditionParam = encodeURIComponent(condition || "null");
    const nameParam = encodeURIComponent(name || "null");

    try {
        const response = await fetch(
            `${PATIENT_API}/filter/${conditionParam}/${nameParam}/${encodeURIComponent(token)}`
        );

        if (!response.ok) {
            console.error("filterAppointments: HTTP", response.status);
            return { appointments: [] };
        }

        const data = await response.json();

        return {
            appointments: Array.isArray(data.appointments)
                ? data.appointments
                : []
        };
    } catch (error) {
        console.error("filterAppointments:", error.message);
        alert("Unable to filter appointments. Please try again.");

        return { appointments: [] };
    }
}