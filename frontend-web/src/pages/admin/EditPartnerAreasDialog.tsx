import { useEffect, useState } from "react";
import { Button, Dialog, Select, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminPartnerApi from "../../api/adminPartnerApi";
import type { AdminPartner } from "../../api/adminPartnerApi";
import { listServiceStates, type AdminServiceState } from "../../api/adminServiceStateApi";
import { listServiceAreas, listServiceCities, type AdminServiceArea, type AdminServiceCity } from "../../api/adminServiceLocationApi";

/** Change the areas a partner covers: pick a state and city, tick areas; ticks from other cities are kept. */
export function EditPartnerAreasDialog({
  partner,
  onHide,
  onSaved,
}: {
  partner: AdminPartner | null;
  onHide: () => void;
  onSaved: (updated: AdminPartner) => void;
}) {
  const toast = useToast();
  const [states, setStates] = useState<AdminServiceState[]>([]);
  const [cities, setCities] = useState<AdminServiceCity[]>([]);
  const [areas, setAreas] = useState<AdminServiceArea[]>([]);
  const [stateCode, setStateCode] = useState("");
  const [cityId, setCityId] = useState("");
  const [chosen, setChosen] = useState<Map<string, string>>(new Map());
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!partner) return;
    setChosen(new Map(partner.areas.map((a) => [a.areaId, `${a.areaName}, ${a.cityName}`])));
    setStateCode("");
    setCityId("");
    listServiceStates().then((all) => setStates(all.filter((s) => s.active)));
  }, [partner]);

  useEffect(() => {
    setCityId("");
    setCities([]);
    if (stateCode) listServiceCities(stateCode).then((all) => setCities(all.filter((c) => c.active)));
  }, [stateCode]);

  useEffect(() => {
    setAreas([]);
    if (cityId) listServiceAreas(cityId).then((all) => setAreas(all.filter((a) => a.active)));
  }, [cityId]);

  function toggle(area: AdminServiceArea) {
    const cityName = cities.find((c) => c.id === cityId)?.name ?? "";
    setChosen((current) => {
      const next = new Map(current);
      if (next.has(area.id)) next.delete(area.id);
      else next.set(area.id, `${area.name}, ${cityName}`);
      return next;
    });
  }

  async function handleSave() {
    if (!partner) return;
    setSaving(true);
    try {
      const updated = await adminPartnerApi.replacePartnerAreas(partner.id, [...chosen.keys()]);
      onSaved(updated);
      toast.show({ severity: "success", summary: "Areas updated", detail: partner.name });
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not update areas", detail: extractErrorMessage(err) });
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      visible={partner != null}
      onHide={onHide}
      title={`Areas for ${partner?.name ?? ""}`}
      width={520}
      footer={
        <>
          <Button variant="outlined" severity="secondary" onClick={onHide}>
            Cancel
          </Button>
          <Button disabled={chosen.size === 0} loading={saving} onClick={handleSave}>
            Save areas
          </Button>
        </>
      }
    >
      <p data-testid="partner-chosen-areas">Covers: {chosen.size === 0 ? "none selected" : [...chosen.values()].join("; ")}</p>
      <Select
        id="partner-area-state"
        label="State"
        value={stateCode}
        onChange={(e) => setStateCode(e.target.value)}
        options={[{ label: "Choose a state", value: "" }, ...states.map((s) => ({ label: s.name, value: s.code }))]}
      />
      <Select
        id="partner-area-city"
        label="City"
        value={cityId}
        onChange={(e) => setCityId(e.target.value)}
        options={[{ label: "Choose a city", value: "" }, ...cities.map((c) => ({ label: c.name, value: c.id }))]}
      />
      {areas.map((area) => (
        <label key={area.id} style={{ display: "block" }}>
          <input type="checkbox" checked={chosen.has(area.id)} onChange={() => toggle(area)} /> {area.name} ({area.pincode})
        </label>
      ))}
    </Dialog>
  );
}
