import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { setUpNativeBackButton } from "./lib/nativeBackButton";
import "./styles/style.css";

setUpNativeBackButton();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
