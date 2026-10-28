import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Icon, type IconName } from "./Icon";

export type ButtonSeverity = "primary" | "secondary" | "success" | "danger" | "warning" | "help";
export type ButtonVariant = "solid" | "outlined" | "text";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  severity?: ButtonSeverity;
  variant?: ButtonVariant;
  size?: "sm" | "md";
  icon?: IconName;
  loading?: boolean;
  fullWidth?: boolean;
  children?: ReactNode;
}

export function Button({
  severity = "primary",
  variant = "solid",
  size = "md",
  icon,
  loading = false,
  fullWidth = false,
  className,
  children,
  disabled,
  ...rest
}: ButtonProps) {
  const classes = [
    "btn",
    `btn-${variant}`,
    `btn-${severity}`,
    size === "sm" ? "btn-sm" : "",
    fullWidth ? "btn-full" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button className={classes} disabled={disabled || loading} {...rest}>
      {loading ? <span className="btn-spinner" aria-hidden="true" /> : icon ? <Icon name={icon} size={16} /> : null}
      {children && <span>{children}</span>}
    </button>
  );
}
