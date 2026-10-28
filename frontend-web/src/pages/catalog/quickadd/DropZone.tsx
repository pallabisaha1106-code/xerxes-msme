import { useRef, useState } from "react";
import { Icon } from "../../../ui";

interface DropZoneProps {
  onFiles: (files: File[]) => void;
  /** True while earlier photos are still being read; new ones are held back. */
  busy: boolean;
  busyText?: string;
  compact?: boolean;
}

const looksLikeImage = (file: File) => file.type.startsWith("image/") || /\.(jpe?g|png|webp|heic)$/i.test(file.name);

/** Drop photos here or click to pick them. Anything that isn't an image is quietly ignored. */
export function DropZone({ onFiles, busy, busyText = "Reading photos...", compact = false }: DropZoneProps) {
  const [over, setOver] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const take = (list: FileList | null) => {
    const files = Array.from(list ?? []).filter(looksLikeImage);
    if (files.length > 0) onFiles(files);
  };

  return (
    <div
      className={["qa-drop", over ? "qa-drop-over" : "", compact ? "qa-drop-compact" : ""].filter(Boolean).join(" ")}
      onDragOver={(event) => {
        event.preventDefault();
        setOver(true);
      }}
      onDragLeave={() => setOver(false)}
      onDrop={(event) => {
        event.preventDefault();
        setOver(false);
        if (!busy) take(event.dataTransfer.files);
      }}
    >
      <Icon name="plus" size={compact ? 18 : 28} />
      <div className="qa-drop-text">
        <strong>{busy ? busyText : compact ? "Add more photos" : "Drop your product photos here"}</strong>
        {!compact && <span>One photo is one product. Name your files like IMG_Kurti_Blue Cotton.jpg to fill the name and category.</span>}
      </div>
      <button type="button" className="qa-drop-pick" disabled={busy} onClick={() => input.current?.click()}>
        Choose photos
      </button>
      <input
        ref={input}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => {
          take(event.target.files);
          event.target.value = "";
        }}
      />
    </div>
  );
}
