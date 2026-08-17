import axios from "axios";

const api = axios.create({
    baseURL: "http://localhost:8080/api",
    headers: {
        "Content-Type": "application/json"
    }
});


// =====================================================
// REQUEST INTERCEPTOR
// Automatically attach JWT
// =====================================================

api.interceptors.request.use(
    (config) => {

        const token =
            localStorage.getItem("doclock_token");

        if (token) {

            config.headers.Authorization =
                `Bearer ${token}`;
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

        if (
            error.response?.status === 401
        ) {

            localStorage.removeItem(
                "doclock_token"
            );

            localStorage.removeItem(
                "doclock_username"
            );

            window.location.href = "/";
        }

        return Promise.reject(error);
    }
);


export default api;