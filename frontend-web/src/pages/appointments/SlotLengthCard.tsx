import { useEffect, useState } from "react";
import { Button, Card, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as appointmentApi from "../../api/appointmentApi";
import type { AppointmentSettings } from "../../api/appointmentApi";

/** How long each appointment is - also the step start times follow (10:00, 10:30, 11:00 for 30 minutes). */
export function SlotLengthCard() {
  const toast = useToast();
  const [settings, setSettings] = useState<AppointmentSettings | null>(null);
  const [minutes, setMinutes] = useState("30");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    appointmentApi
      .getSettings()
      .then((loaded) => {
        setSettings(loaded);
        setMinutes(String(loaded.slotMinutes));
      })
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load settings", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function save() {
    setSaving(true);
    try {
      const saved = await appointmentApi.saveSettings(Number(minutes));
      setSettings(saved);
      toast.show({ severity: "success", summary: "Appointment length saved", detail: `${saved.slotMinutes} minutes` });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card title="Appointment length">
      <p className="field-hint appt-intro">
        Every appointment is this long, and start times follow it. Your weekly hours must have room for at least one appointment.
      </p>
      <div className="appt-inline">
        <Select
          id="appointment-length"
          label="Each appointment lasts"
          value={minutes}
          onChange={(e) => setMinutes(e.target.value)}
          options={(settings?.allowedSlotMinutes ?? [30]).map((value) => ({ label: `${value} minutes`, value: String(value) }))}
        />
        <Button onClick={save} loading={saving} disabled={settings == null || Number(minutes) === settings.slotMinutes}>
          Save length
        </Button>
      </div>
    </Card>
  );
}
