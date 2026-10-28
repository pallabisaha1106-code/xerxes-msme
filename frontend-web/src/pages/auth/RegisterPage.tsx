import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button, Input, PasswordInput } from "../../ui";
import { useAuth } from "../../auth/AuthContext";
import { extractErrorMessage } from "../../api/client";
import { isValidEmail } from "../../lib/emailFormat";
import "./auth.css";

const initialForm = {
  businessName: "",
  gstin: "",
  stateCode: "",
  ownerName: "",
  email: "",
  password: "",
};

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function update<K extends keyof typeof initialForm>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!isValidEmail(form.email)) {
      setError("Enter a valid email address, like name@example.com.");
      return;
    }
    setSubmitting(true);
    try {
      await register({
        businessName: form.businessName,
        gstin: form.gstin || undefined,
        stateCode: form.stateCode,
        ownerName: form.ownerName,
        email: form.email.trim(),
        password: form.password,
      });
      navigate("/");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1 className="auth-title">Create your business account</h1>
        <p className="auth-subtitle">Set up Xerxes MSME for your business in a minute.</p>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <Input
            id="businessName"
            label="Business name"
            value={form.businessName}
            onChange={(e) => update("businessName", e.target.value)}
            required
          />

          <div className="auth-row">
            <Input
              id="stateCode"
              label="GST state code"
              placeholder="e.g. 27"
              maxLength={2}
              value={form.stateCode}
              onChange={(e) => update("stateCode", e.target.value)}
              required
            />
            <Input
              id="gstin"
              label="GSTIN (optional)"
              value={form.gstin}
              onChange={(e) => update("gstin", e.target.value)}
            />
          </div>

          <Input
            id="ownerName"
            label="Your name"
            value={form.ownerName}
            onChange={(e) => update("ownerName", e.target.value)}
            required
          />

          <Input
            id="email"
            label="Email"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
            required
          />

          <PasswordInput
            id="password"
            label="Password"
            value={form.password}
            onChange={(e) => update("password", e.target.value)}
            required
          />

          <Button type="submit" fullWidth loading={submitting}>
            Create Account
          </Button>
        </form>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
