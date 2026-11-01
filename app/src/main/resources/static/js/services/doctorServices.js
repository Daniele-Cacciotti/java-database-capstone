import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

// Recupera l'elenco dei medici.
export async function getDoctors() {
    try {
        const response = await fetch(DOCTOR_API);

        if (!response.ok) {
            throw new Error("Unable to load doctors.");
        }

        const data = await response.json();
        return Array.isArray(data.doctors) ? data.doctors : [];
    } catch (error) {
        console.error("getDoctors:", error.message);
        return [];
    }
}

// Elimina un medico usando il token dell'amministratore.
export async function deleteDoctor(id, token) {
    try {
        const response = await fetch(
            `${DOCTOR_API}/${encodeURIComponent(id)}/${encodeURIComponent(token)}`,
            { method: "DELETE" }
        );

        const data = await response.json();

        return {
            success: response.ok,
            message: data.message || (
                response.ok
                    ? "Doctor deleted successfully."
                    : "Unable to delete doctor."
            )
        };
    } catch (error) {
        console.error("deleteDoctor:", error.message);

        return {
            success: false,
            message: "Unable to delete doctor. Please try again."
        };
    }
}

// Invia al backend i dati del nuovo medico.
export async function saveDoctor(doctor, token) {
    try {
        const response = await fetch(
            `${DOCTOR_API}/${encodeURIComponent(token)}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(doctor)
            }
        );

        const data = await response.json();

        return {
            success: response.ok,
            message: data.message || (
                response.ok
                    ? "Doctor added successfully."
                    : "Unable to add doctor."
            )
        };
    } catch (error) {
        console.error("saveDoctor:", error.message);

        return {
            success: false,
            message: "Unable to add doctor. Please try again."
        };
    }
}

// Rappresenta i filtri non selezionati con la stringa "null".
export async function filterDoctors(name, time, specialty) {
    const nameParam = encodeURIComponent(name || "null");
    const timeParam = encodeURIComponent(time || "null");
    const specialtyParam = encodeURIComponent(specialty || "null");

    try {
        const response = await fetch(
            `${DOCTOR_API}/filter/${nameParam}/${timeParam}/${specialtyParam}`
        );

        if (!response.ok) {
            console.error("filterDoctors: HTTP", response.status);
            return { doctors: [] };
        }

        const data = await response.json();

        return {
            doctors: Array.isArray(data.doctors) ? data.doctors : []
        };
    } catch (error) {
        console.error("filterDoctors:", error.message);
        alert("Unable to filter doctors. Please try again.");

        return { doctors: [] };
    }
}