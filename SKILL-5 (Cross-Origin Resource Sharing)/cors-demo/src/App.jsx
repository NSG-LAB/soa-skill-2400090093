import { useEffect, useState } from "react";
import axios from "axios";

function App() {

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    useEffect(() => {

        axios
            .get("http://localhost:8080/message")
            .then(response => {
                setMessage(response.data);
            })
            .catch(error => {
                console.error(error);
                setError("Unable to connect to Spring Boot API.");
            });

    }, []);

    return (
        <div>
            <h1>React Vite CORS Demo</h1>

            <h2>{message}</h2>

            {error && <p>{error}</p>}
        </div>
    );
}

export default App;
