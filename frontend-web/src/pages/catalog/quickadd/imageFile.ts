import { planFit, type FitRules } from "./imageFit";

export type PreparedImage =
  /** Ready to upload. "cropped" means it was cut to the allowed shape (shown as a note on the draft card). */
  | { status: "ready" | "cropped"; file: File; width: number; height: number }
  | { status: "tooSmall"; width: number; height: number }
  | { status: "unreadable" };

const JPEG_QUALITY = 0.92;

/**
 * Reads a dropped photo, crops it if its shape is off (see imageFit.ts) and always re-encodes it as an upright JPEG.
 * Always re-encoding matters: a phone photo is often stored sideways with an EXIF "rotate" flag. The browser applies
 * that flag when it decodes, but the server measures the raw pixels and would see a landscape photo and reject it.
 * Drawing through a canvas bakes the right orientation in and drops the metadata. The server keeps and resizes it.
 */
export async function prepareImage(file: File, rules: FitRules): Promise<PreparedImage> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    return { status: "unreadable" };
  }
  try {
    const plan = planFit(bitmap.width, bitmap.height, rules);
    if (plan.status === "tooSmall") return plan;

    const area = plan.status === "crop" ? plan.crop : { x: 0, y: 0, width: bitmap.width, height: bitmap.height };
    const canvas = document.createElement("canvas");
    canvas.width = area.width;
    canvas.height = area.height;
    const context = canvas.getContext("2d");
    if (!context) return { status: "unreadable" };
    context.drawImage(bitmap, area.x, area.y, area.width, area.height, 0, 0, area.width, area.height);

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", JPEG_QUALITY));
    if (!blob) return { status: "unreadable" };
    const name = file.name.replace(/\.[^.]+$/, "") + ".jpg";
    return {
      status: plan.status === "crop" ? "cropped" : "ready",
      file: new File([blob], name, { type: "image/jpeg" }),
      width: area.width,
      height: area.height,
    };
  } finally {
    bitmap.close();
  }
}
