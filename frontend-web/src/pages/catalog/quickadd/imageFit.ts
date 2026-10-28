/**
 * Decides what to do with a dropped photo so it passes the server's image rules (ProductImageService.validateGuidelines):
 * width >= minWidth, height >= minHeight, and width / height inside [minAspectRatio, maxAspectRatio]. A photo that is
 * only the wrong shape is cropped from the centre to the nearest allowed shape; one that is too small - including after
 * that crop, which can push it under the minimum - is rejected. Pure maths; the actual cropping is in imageFile.ts.
 */

export interface FitRules {
  minWidth: number;
  minHeight: number;
  minAspectRatio: number;
  maxAspectRatio: number;
}

export interface CropRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export type FitPlan =
  | { status: "ok"; width: number; height: number }
  | { status: "crop"; crop: CropRect }
  /** width/height are the size the photo would end up at (after any crop), which is what falls short. */
  | { status: "tooSmall"; width: number; height: number };

export function planFit(width: number, height: number, rules: FitRules): FitPlan {
  const ratio = width / height;
  let cropWidth = width;
  let cropHeight = height;
  // Floor, so the result is on the allowed side of the limit rather than a rounding error past it.
  if (ratio > rules.maxAspectRatio) cropWidth = Math.floor(height * rules.maxAspectRatio);
  else if (ratio < rules.minAspectRatio) cropHeight = Math.floor(width / rules.minAspectRatio);

  if (cropWidth < rules.minWidth || cropHeight < rules.minHeight) {
    return { status: "tooSmall", width: cropWidth, height: cropHeight };
  }
  if (cropWidth === width && cropHeight === height) return { status: "ok", width, height };
  return {
    status: "crop",
    crop: { x: Math.floor((width - cropWidth) / 2), y: Math.floor((height - cropHeight) / 2), width: cropWidth, height: cropHeight },
  };
}
