import { forwardRef, useState, type InputHTMLAttributes } from "react";
import { Icon } from "./Icon";

export interface PasswordInputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(function PasswordInput(
  { label, error, id, className, ...rest },
  ref,
) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="field">
      {label && (
        <label htmlFor={id} className="field-label">
          {label}
        </label>
      )}
      <div className="input-with-icon">
        <input
          ref={ref}
          id={id}
          type={visible ? "text" : "password"}
          className={["input", error ? "input-error" : "", className ?? ""].filter(Boolean).join(" ")}
          {...rest}
        />
        <button
          type="button"
          className="input-icon-button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Hide password" : "Show password"}
          tabIndex={-1}
        >
          <Icon name={visible ? "eyeOff" : "eye"} size={16} />
        </button>
      </div>
      {error && <span className="field-error">{error}</span>}
    </div>
  );
});
