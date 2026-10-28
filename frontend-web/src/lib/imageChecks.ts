export interface ImageRules {
  minWidth?: number;
  minHeight?: number;
  /** width / height */
  minAspect?: number;
  maxAspect?: number;
  /** Plain-words description of the accepted shape, shown when the aspect ratio is off. */
  aspectHint?: string;
  maxBytes?: number;
}

export interface ImageSize {
  width: number;
  height: number;
}

export function readImageSize(file: File): Promise<ImageSize> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve({ width: img.naturalWidth, height: img.naturalHeight });
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("not-an-image"));
    };
    img.src = url;
  });
}

/** Mirrors what the backend enforces, so the user hears about a bad photo before uploading it - the server stays the source of truth. */
export function checkImage(size: ImageSize, bytes: number, rules: ImageRules): string[] {
  const problems: string[] = [];
  const { width, height } = size;

  if (rules.minWidth != null && rules.minHeight != null && (width < rules.minWidth || height < rules.minHeight)) {
    problems.push(`Too small (${width}×${height}px). Needs at least ${rules.minWidth}×${rules.minHeight}px.`);
  }

  const aspect = width / height;
  const badShape =
    (rules.minAspect != null && aspect < rules.minAspect) || (rules.maxAspect != null && aspect > rules.maxAspect);
  if (badShape) {
    problems.push(`The shape doesn't fit${rules.aspectHint ? " - use " + rules.aspectHint : ""}.`);
  }
  if (rules.maxBytes != null && bytes > rules.maxBytes) {
    problems.push(`File is too large (${(bytes / 1024 / 1024).toFixed(1)}MB). Maximum is ${(rules.maxBytes / 1024 / 1024).toFixed(0)}MB.`);
  }
  return problems;
}
