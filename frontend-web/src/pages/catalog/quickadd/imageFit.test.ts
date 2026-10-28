import { describe, expect, it } from "vitest";
import { planFit, type FitRules } from "./imageFit";

// The shipped defaults: DETAIL (portrait, fashion) and COMPACT (squarer, small pack shots).
const DETAIL: FitRules = { minWidth: 720, minHeight: 900, minAspectRatio: 0.55, maxAspectRatio: 0.9 };
const COMPACT: FitRules = { minWidth: 800, minHeight: 800, minAspectRatio: 0.5, maxAspectRatio: 2 };

/** The server's own check, restated: a plan's final size must pass it. */
function passesServerRule(width: number, height: number, rules: FitRules): boolean {
  const ratio = width / height;
  return width >= rules.minWidth && height >= rules.minHeight && ratio >= rules.minAspectRatio && ratio <= rules.maxAspectRatio;
}

describe("planFit", () => {
  it("leaves a photo that already fits alone", () => {
    expect(planFit(1080, 1440, DETAIL)).toEqual({ status: "ok", width: 1080, height: 1440 });
  });
  it("accepts a photo exactly on either ratio limit", () => {
    expect(planFit(900, 1000, DETAIL).status).toBe("ok"); // 0.90
    expect(planFit(1100, 2000, DETAIL).status).toBe("ok"); // 0.55
  });
  it("accepts a photo exactly at the minimum size", () => {
    expect(planFit(720, 900, DETAIL).status).toBe("ok");
  });
  it("crops a landscape photo from the centre to the widest allowed shape", () => {
    expect(planFit(4000, 3000, DETAIL)).toEqual({ status: "crop", crop: { x: 650, y: 0, width: 2700, height: 3000 } });
  });
  it("crops a square photo", () => {
    expect(planFit(1200, 1200, DETAIL)).toEqual({ status: "crop", crop: { x: 60, y: 0, width: 1080, height: 1200 } });
  });
  it("crops a too-tall photo from the centre", () => {
    const plan = planFit(1000, 3000, DETAIL);
    expect(plan).toEqual({ status: "crop", crop: { x: 0, y: 591, width: 1000, height: 1818 } });
  });
  it("rejects a photo below the minimum size, with its size", () => {
    expect(planFit(400, 500, DETAIL)).toEqual({ status: "tooSmall", width: 400, height: 500 });
  });
  it("rejects a photo that is only big enough before the crop", () => {
    // 800x800 is square: cropped to 720x800 it is too short for DETAIL's 900px minimum.
    expect(planFit(800, 800, DETAIL)).toEqual({ status: "tooSmall", width: 720, height: 800 });
  });
  it("uses the compact rules for compact shops", () => {
    expect(planFit(1000, 1000, COMPACT).status).toBe("ok");
    expect(planFit(2000, 900, COMPACT)).toEqual({ status: "crop", crop: { x: 100, y: 0, width: 1800, height: 900 } });
    // 1600x700 would crop to 1400x700, under the 800px minimum height.
    expect(planFit(1600, 700, COMPACT)).toEqual({ status: "tooSmall", width: 1400, height: 700 });
    expect(planFit(600, 600, COMPACT).status).toBe("tooSmall");
  });
  it("never returns a plan the server would reject, across many sizes", () => {
    for (const rules of [DETAIL, COMPACT]) {
      for (let width = 300; width <= 5000; width += 137) {
        for (let height = 300; height <= 5000; height += 211) {
          const plan = planFit(width, height, rules);
          if (plan.status === "ok") expect(passesServerRule(plan.width, plan.height, rules)).toBe(true);
          if (plan.status === "crop") {
            const { x, y, width: w, height: h } = plan.crop;
            expect(passesServerRule(w, h, rules)).toBe(true);
            expect(x).toBeGreaterThanOrEqual(0);
            expect(y).toBeGreaterThanOrEqual(0);
            expect(x + w).toBeLessThanOrEqual(width);
            expect(y + h).toBeLessThanOrEqual(height);
          }
          if (plan.status === "tooSmall") expect(passesServerRule(plan.width, plan.height, rules)).toBe(false);
        }
      }
    }
  });
});
