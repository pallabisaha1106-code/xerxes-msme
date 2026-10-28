import { useEffect, useState } from "react";
import { checkImage, readImageSize, type ImageRules, type ImageSize } from "../lib/imageChecks";
import { Button } from "./Button";
import { ImageCropper } from "./ImageCropper";

interface ImagePickerProps {
  id: string;
  label?: string;
  rules?: ImageRules;
  disabled?: boolean;
  /** When set (width / height), the vendor can re-frame the photo to this shape before it is used. */
  cropAspect?: number;
  /** Called with the chosen file once it passes the checks, or null when cleared or rejected. */
  onChange: (file: File | null) => void;
}

interface Picked {
  file: File;
  previewUrl: string;
  size: ImageSize | null;
  problems: string[];
}

export function ImagePicker({ id, label, rules = {}, disabled, cropAspect, onChange }: ImagePickerProps) {
  const [picked, setPicked] = useState<Picked | null>(null);
  const [cropping, setCropping] = useState(false);
  // Re-framing always starts from the photo the vendor originally picked, never from an earlier crop of it.
  const [source, setSource] = useState<File | null>(null);

  useEffect(() => {
    return () => {
      if (picked) URL.revokeObjectURL(picked.previewUrl);
    };
  }, [picked]);

  async function process(file: File) {
    const previewUrl = URL.createObjectURL(file);
    try {
      const size = await readImageSize(file);
      const problems = checkImage(size, file.size, rules);
      setPicked({ file, previewUrl, size, problems });
      onChange(problems.length === 0 ? file : null);
    } catch {
      setPicked({ file, previewUrl, size: null, problems: ["This file could not be read as an image."] });
      onChange(null);
    }
  }

  async function handleSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0] ?? null;
    setCropping(false);
    setSource(file);
    if (!file) {
      setPicked(null);
      onChange(null);
      return;
    }
    await process(file);
  }

  async function handleCropped(cropped: File) {
    setCropping(false);
    await process(cropped);
  }

  const shapeProblem = picked?.problems.some((p) => p.startsWith("The shape doesn't fit")) ?? false;

  return (
    <div className="field">
      {label && (
        <label className="field-label" htmlFor={id}>
          {label}
        </label>
      )}
      <input id={id} type="file" accept="image/jpeg,image/png,image/webp" onChange={handleSelect} disabled={disabled} />
      {picked && cropping && cropAspect && source && (
        <ImageCropper file={source} aspect={cropAspect} onApply={handleCropped} onCancel={() => setCropping(false)} />
      )}
      {picked && !cropping && (
        <div className="image-picker-preview">
          <img src={picked.previewUrl} alt="Selected photo preview" />
          <div>
            {picked.size && (
              <span className="field-hint">
                {picked.size.width}×{picked.size.height}px
              </span>
            )}
            {picked.problems.length === 0 ? (
              <span className="field-hint">Looks good.</span>
            ) : (
              picked.problems.map((problem) => (
                <span key={problem} className="field-error">
                  {problem}
                </span>
              ))
            )}
            {cropAspect && picked.size && (
              <Button
                type="button"
                size="sm"
                variant={shapeProblem ? "solid" : "outlined"}
                severity={shapeProblem ? "primary" : "secondary"}
                disabled={disabled}
                onClick={() => setCropping(true)}
              >
                {shapeProblem ? "Fix the framing" : "Adjust framing"}
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
