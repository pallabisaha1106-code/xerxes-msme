import { useEffect, useState } from "react";
import { Button, Card, Icon, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as appointmentApi from "../../api/appointmentApi";
import { WEEKDAY_NAMES } from "./appointmentFormat";
import { DAYS, NEW_RANGE, emptyWeek, firstProblem, rangesFromWeek, weekFromRanges, type Week } from "./weeklyHours";

/** The weekly template: when the business takes appointments on each weekday (a day with no ranges is closed). */
export function WeeklyHoursCard() {
  const toast = useToast();
  const [week, setWeek] = useState<Week>(emptyWeek());
  const [saved, setSaved] = useState<Week>(emptyWeek());
  const [saving, setSaving] = useState(false);
  const problem = firstProblem(week);
  const dirty = JSON.stringify(week) !== JSON.stringify(saved);

  useEffect(() => {
    appointmentApi
      .getHours()
      .then((ranges) => {
        const loaded = weekFromRanges(ranges);
        setWeek(loaded);
        setSaved(loaded);
      })
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load hours", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function change(day: number, index: number, field: "opensAt" | "closesAt", value: string) {
    setWeek((current) => ({ ...current, [day]: current[day].map((range, i) => (i === index ? { ...range, [field]: value } : range)) }));
  }

  async function save() {
    setSaving(true);
    try {
      const stored = weekFromRanges(await appointmentApi.saveHours(rangesFromWeek(week)));
      setWeek(stored);
      setSaved(stored);
      toast.show({ severity: "success", summary: "Weekly hours saved" });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save hours", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card title="Weekly hours">
      <p className="field-hint appt-intro">
        The hours you normally take appointments. A day with no hours is closed. Add two ranges for a lunch break. Dates are opened separately below.
      </p>
      <div className="appt-week">
        {DAYS.map((day) => {
          const name = WEEKDAY_NAMES[day - 1];
          return (
            <div className="appt-day" key={day}>
              <div className="appt-day-name">{name}</div>
              <div className="appt-ranges">
                {week[day].length === 0 && <span className="appt-sub">Closed</span>}
                {week[day].map((range, index) => (
                  <div className="appt-range" key={index}>
                    <input
                      className="input"
                      type="time"
                      aria-label={`${name} opens`}
                      value={range.opensAt}
                      onChange={(e) => change(day, index, "opensAt", e.target.value)}
                    />
                    <span>to</span>
                    <input
                      className="input"
                      type="time"
                      aria-label={`${name} closes`}
                      value={range.closesAt}
                      onChange={(e) => change(day, index, "closesAt", e.target.value)}
                    />
                    <button
                      type="button"
                      className="appt-icon-button"
                      aria-label={`Remove ${name} hours ${index + 1}`}
                      onClick={() => setWeek((current) => ({ ...current, [day]: current[day].filter((_, i) => i !== index) }))}
                    >
                      <Icon name="x" size={16} />
                    </button>
                  </div>
                ))}
              </div>
              <Button
                size="sm"
                variant="text"
                icon="plus"
                aria-label={`Add hours for ${name}`}
                onClick={() => setWeek((current) => ({ ...current, [day]: [...current[day], { ...NEW_RANGE }] }))}
              >
                Add hours
              </Button>
            </div>
          );
        })}
      </div>
      {problem && <p className="field-error">{problem}</p>}
      <Button onClick={save} loading={saving} disabled={!dirty || problem != null}>
        Save weekly hours
      </Button>
    </Card>
  );
}
