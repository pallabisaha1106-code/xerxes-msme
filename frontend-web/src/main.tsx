import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";

// Our own theme (inspired by the PrimeReact "Verona" template, but no
// dependency on PrimeReact itself) + the plain-React UI kit's styles.
import "./theme/tokens.css";
import "./theme/base.css";
import "./ui/ui.css";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
