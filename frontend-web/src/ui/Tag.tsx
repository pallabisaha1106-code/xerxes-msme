export type TagSeverity = "success" | "info" | "warning" | "danger" | "secondary";

export function Tag({ severity = "secondary", children }: { severity?: TagSeverity; children: React.ReactNode }) {
  return <span className={`tag tag-${severity}`}>{children}</span>;
}
