import { forwardRef, type ButtonHTMLAttributes } from "react";

export const Avatar = forwardRef<HTMLButtonElement, { label: string } & ButtonHTMLAttributes<HTMLButtonElement>>(
  function Avatar({ label, className, ...rest }, ref) {
    return (
      <button ref={ref} type="button" className={["avatar", className ?? ""].filter(Boolean).join(" ")} {...rest}>
        {label}
      </button>
    );
  },
);
