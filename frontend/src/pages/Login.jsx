import { useState } from "react";
import api from "../services/api";

function Login({ onLogin }) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleLogin = async (event) => {
        event.preventDefault();

        setError("");
        setLoading(true);

        try {
            const response = await api.post(
                "/auth/login",
                {
                    username,
                    password
                }
            );

            const token = response.data.token;

            localStorage.setItem(
                "doclock_token",
                token
            );

            localStorage.setItem(
                "doclock_username",
                response.data.username
            );

            onLogin();

        } catch (error) {
            console.error("Login failed:", error);
            console.error("Backend response:", error.response?.data);

            if (!error.response) {
                setError("Unable to reach the server. Please try again shortly.");
            } else {
                setError(
                    error.response.data?.message ||
                    "Invalid username or password."
                );
            }

        } finally {
            setLoading(false);
        }
    };


    return (
        <div className="login-page">

            <div className="login-card">


                {/* LOGO */}

                <div className="login-logo">
                    🔐
                </div>


                <p className="login-label">
                    PRIVATE DOCUMENT VAULT
                </p>


                <h1>
                    Welcome to DocLock
                </h1>


                <p className="login-subtitle">
                    Sign in to access your private
                    documents and AI assistant.
                </p>


                <form
                    onSubmit={handleLogin}
                >


                    {/* USERNAME */}

                    <label>
                        Username
                    </label>

                    <input
                        type="text"
                        value={username}
                        onChange={(event) =>
                            setUsername(
                                event.target.value
                            )
                        }
                        placeholder="Enter username"
                        autoComplete="username"
                        required
                    />


                    {/* PASSWORD */}

                    <label>
                        Password
                    </label>

                    <input
                        type="password"
                        value={password}
                        onChange={(event) =>
                            setPassword(
                                event.target.value
                            )
                        }
                        placeholder="Enter password"
                        autoComplete="current-password"
                        required
                    />


                    {/* ERROR */}

                    {error && (

                        <div className="login-error">
                            {error}
                        </div>

                    )}


                    {/* BUTTON */}

                    <button
                        type="submit"
                        disabled={loading}
                    >

                        {loading
                            ? "Signing in..."
                            : "Sign In"}

                    </button>

                </form>


                <div className="login-security">

                    🔒 Only you can access this vault.

                </div>

            </div>

        </div>
    );
}

export default Login;
