import { useEffect, useState } from "react";
import { Button, Card, Dialog, Input, Select, Table, Tag, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminServiceLocationApi from "../../api/adminServiceLocationApi";
import type { AdminServiceArea, AdminServiceCity } from "../../api/adminServiceLocationApi";
import * as adminServiceStateApi from "../../api/adminServiceStateApi";
import type { AdminServiceState } from "../../api/adminServiceStateApi";

interface CityFormState {
  name: string;
  displayOrder: string;
  active: boolean;
}

interface AreaFormState {
  name: string;
  pincode: string;
  displayOrder: string;
  active: boolean;
}

export function AdminServiceLocationsPage() {
  const toast = useToast();
  const [states, setStates] = useState<AdminServiceState[]>([]);
  const [stateCode, setStateCode] = useState("");

  const [cities, setCities] = useState<AdminServiceCity[]>([]);
  const [citiesLoading, setCitiesLoading] = useState(false);
  const [selectedCityId, setSelectedCityId] = useState<string | null>(null);

  const [areas, setAreas] = useState<AdminServiceArea[]>([]);
  const [areasLoading, setAreasLoading] = useState(false);

  const [cityDialog, setCityDialog] = useState<{ editing: AdminServiceCity | null } | null>(null);
  const [cityForm, setCityForm] = useState<CityFormState>({ name: "", displayOrder: "0", active: true });
  const [citySaving, setCitySaving] = useState(false);

  const [areaDialog, setAreaDialog] = useState<{ editing: AdminServiceArea | null } | null>(null);
  const [areaForm, setAreaForm] = useState<AreaFormState>({ name: "", pincode: "", displayOrder: "0", active: true });
  const [areaSaving, setAreaSaving] = useState(false);

  useEffect(() => {
    adminServiceStateApi
      .listServiceStates()
      .then((loaded) => {
        setStates(loaded);
        setStateCode((current) => current || (loaded[0]?.code ?? ""));
      })
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load states", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadCities(forStateCode: string) {
    setCitiesLoading(true);
    setSelectedCityId(null);
    setAreas([]);
    try {
      setCities(await adminServiceLocationApi.listServiceCities(forStateCode));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load cities", detail: extractErrorMessage(err) });
    } finally {
      setCitiesLoading(false);
    }
  }

  useEffect(() => {
    if (stateCode) loadCities(stateCode);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stateCode]);

  async function loadAreas(cityId: string) {
    setAreasLoading(true);
    try {
      setAreas(await adminServiceLocationApi.listServiceAreas(cityId));
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load areas", detail: extractErrorMessage(err) });
    } finally {
      setAreasLoading(false);
    }
  }

  function selectCity(city: AdminServiceCity) {
    setSelectedCityId(city.id);
    loadAreas(city.id);
  }

  function openCityDialog(editing: AdminServiceCity | null) {
    setCityForm(
      editing
        ? { name: editing.name, displayOrder: String(editing.displayOrder), active: editing.active }
        : { name: "", displayOrder: "0", active: true },
    );
    setCityDialog({ editing });
  }

  async function handleSaveCity() {
    setCitySaving(true);
    try {
      if (cityDialog?.editing) {
        await adminServiceLocationApi.updateServiceCity(
          cityDialog.editing.id,
          cityForm.name,
          Number(cityForm.displayOrder) || 0,
          cityForm.active,
        );
      } else {
        await adminServiceLocationApi.createServiceCity(stateCode, cityForm.name, Number(cityForm.displayOrder) || 0);
      }
      toast.show({ severity: "success", summary: "City saved" });
      setCityDialog(null);
      await loadCities(stateCode);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save city", detail: extractErrorMessage(err) });
    } finally {
      setCitySaving(false);
    }
  }

  function openAreaDialog(editing: AdminServiceArea | null) {
    setAreaForm(
      editing
        ? { name: editing.name, pincode: editing.pincode, displayOrder: String(editing.displayOrder), active: editing.active }
        : { name: "", pincode: "", displayOrder: "0", active: true },
    );
    setAreaDialog({ editing });
  }

  async function handleSaveArea() {
    if (!selectedCityId) return;
    setAreaSaving(true);
    try {
      if (areaDialog?.editing) {
        await adminServiceLocationApi.updateServiceArea(
          areaDialog.editing.id,
          areaForm.name,
          areaForm.pincode,
          Number(areaForm.displayOrder) || 0,
          areaForm.active,
        );
      } else {
        await adminServiceLocationApi.createServiceArea(
          selectedCityId,
          areaForm.name,
          areaForm.pincode,
          Number(areaForm.displayOrder) || 0,
        );
      }
      toast.show({ severity: "success", summary: "Area saved" });
      setAreaDialog(null);
      await loadAreas(selectedCityId);
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not save area", detail: extractErrorMessage(err) });
    } finally {
      setAreaSaving(false);
    }
  }

  const cityColumns: Column<AdminServiceCity>[] = [
    {
      field: "name",
      header: "City",
      body: (c) => (
        <Button variant="text" severity="primary" size="sm" onClick={() => selectCity(c)}>
          {c.name}
        </Button>
      ),
    },
    { field: "displayOrder", header: "Order" },
    { field: "active", header: "Status", body: (c) => (c.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">INACTIVE</Tag>) },
    {
      field: "actions",
      header: "",
      body: (c) => (
        <Button size="sm" variant="outlined" severity="secondary" onClick={() => openCityDialog(c)}>
          Edit
        </Button>
      ),
    },
  ];

  const areaColumns: Column<AdminServiceArea>[] = [
    { field: "name", header: "Area" },
    { field: "pincode", header: "Pincode" },
    { field: "displayOrder", header: "Order" },
    { field: "active", header: "Status", body: (a) => (a.active ? <Tag severity="success">ACTIVE</Tag> : <Tag severity="secondary">INACTIVE</Tag>) },
    {
      field: "actions",
      header: "",
      body: (a) => (
        <Button size="sm" variant="outlined" severity="secondary" onClick={() => openAreaDialog(a)}>
          Edit
        </Button>
      ),
    },
  ];

  const selectedCity = cities.find((c) => c.id === selectedCityId) ?? null;

  return (
    <>
      <Card
        title="Service Locations — Cities"
        actions={
          <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
            <Select
              id="service-location-state"
              value={stateCode}
              onChange={(e) => setStateCode(e.target.value)}
              options={states.map((s) => ({ label: s.active ? s.name : `${s.name} (disabled)`, value: s.code }))}
            />
            <Button size="sm" onClick={() => openCityDialog(null)} disabled={!stateCode}>
              Add City
            </Button>
          </div>
        }
      >
        <p className="field-hint" style={{ marginBottom: "1rem" }}>
          Vendor registration is gated on this list - a business can only pick a city/area that&rsquo;s here and
          active. Disabling a city hides every area under it from registration too.
        </p>
        <Table
          data={cities}
          columns={cityColumns}
          rowKey={(row) => row.id}
          loading={citiesLoading}
          emptyMessage="No cities yet for this state."
        />
      </Card>

      <Dialog
        visible={selectedCity != null}
        onHide={() => setSelectedCityId(null)}
        title={`Areas under ${selectedCity?.name ?? ""}`}
        width={640}
        footer={
          <Button variant="outlined" severity="secondary" onClick={() => setSelectedCityId(null)}>
            Close
          </Button>
        }
      >
        <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: "1rem" }}>
          <Button size="sm" onClick={() => openAreaDialog(null)}>
            Add Area
          </Button>
        </div>
        <Table
          data={areas}
          columns={areaColumns}
          rowKey={(row) => row.id}
          loading={areasLoading}
          emptyMessage="No areas yet under this city."
        />
      </Dialog>

      <Dialog
        visible={cityDialog != null}
        onHide={() => setCityDialog(null)}
        title={cityDialog?.editing ? "Edit City" : "Add City"}
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setCityDialog(null)}>
              Cancel
            </Button>
            <Button loading={citySaving} onClick={handleSaveCity}>
              Save
            </Button>
          </>
        }
      >
        <Input
          id="city-name"
          label="Name"
          value={cityForm.name}
          onChange={(e) => setCityForm((f) => ({ ...f, name: e.target.value }))}
        />
        <Input
          id="city-display-order"
          label="Display order"
          type="number"
          value={cityForm.displayOrder}
          onChange={(e) => setCityForm((f) => ({ ...f, displayOrder: e.target.value }))}
        />
        {cityDialog?.editing && (
          <Select
            id="city-active"
            label="Status"
            value={cityForm.active ? "true" : "false"}
            onChange={(e) => setCityForm((f) => ({ ...f, active: e.target.value === "true" }))}
            options={[
              { label: "Active", value: "true" },
              { label: "Inactive", value: "false" },
            ]}
          />
        )}
      </Dialog>

      <Dialog
        visible={areaDialog != null}
        onHide={() => setAreaDialog(null)}
        title={areaDialog?.editing ? "Edit Area" : "Add Area"}
        width={420}
        footer={
          <>
            <Button variant="outlined" severity="secondary" onClick={() => setAreaDialog(null)}>
              Cancel
            </Button>
            <Button loading={areaSaving} onClick={handleSaveArea}>
              Save
            </Button>
          </>
        }
      >
        <Input
          id="area-name"
          label="Name"
          value={areaForm.name}
          onChange={(e) => setAreaForm((f) => ({ ...f, name: e.target.value }))}
        />
        <Input
          id="area-pincode"
          label="Pincode"
          value={areaForm.pincode}
          onChange={(e) => setAreaForm((f) => ({ ...f, pincode: e.target.value }))}
        />
        <Input
          id="area-display-order"
          label="Display order"
          type="number"
          value={areaForm.displayOrder}
          onChange={(e) => setAreaForm((f) => ({ ...f, displayOrder: e.target.value }))}
        />
        {areaDialog?.editing && (
          <Select
            id="area-active"
            label="Status"
            value={areaForm.active ? "true" : "false"}
            onChange={(e) => setAreaForm((f) => ({ ...f, active: e.target.value === "true" }))}
            options={[
              { label: "Active", value: "true" },
              { label: "Inactive", value: "false" },
            ]}
          />
        )}
      </Dialog>
    </>
  );
}
