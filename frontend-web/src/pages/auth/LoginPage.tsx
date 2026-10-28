import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Input, PasswordInput } from "../../ui";
import { useAuth } from "../../auth/AuthContext";
import { extractErrorMessage } from "../../api/client";
import "./auth.css";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login({ email, password });
      navigate(user.role === "SUPER_ADMIN" || user.role === "PARTNER" ? "/admin" : "/");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Xerxes MSME</h1>
        <p className="auth-subtitle">Sign in to manage your business.</p>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <Input
            id="email"
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <PasswordInput
            id="password"
            label="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <Button type="submit" fullWidth loading={submitting}>
            Sign In
          </Button>
        </form>

        <p className="auth-footer">
          Don&apos;t have a business account? <Link to="/register">Register</Link>
        </p>
      </div>
    </div>
  );
}
