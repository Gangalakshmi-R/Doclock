import axios from "axios";

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const apiBaseUrl = (configuredApiBaseUrl || "https://doclock-be.onrender.com/api")
    .replace(/\/$/, "");

const api = axios.create({
    baseURL: apiBaseUrl
});

// =====================================================
// REQUEST INTERCEPTOR
// Automatically attach JWT
// =====================================================

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("doclock_token");

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// =====================================================
// RESPONSE INTERCEPTOR
// If token expires → logout
// =====================================================

api.interceptors.response.use(
    (response) => response,
    (error) => {
        // A failed login is handled by Login so its error can be shown without
        // forcing a page reload. All other unauthorized API calls mean an
        // existing session is no longer usable.
        const isLoginRequest = error.config?.url?.endsWith("/auth/login");

        if (error.response?.status === 401 && !isLoginRequest) {
            localStorage.removeItem("doclock_token");
            localStorage.removeItem("doclock_username");

            window.location.href = "/";
        }

        return Promise.reject(error);
    }
);

export default api;
