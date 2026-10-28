import { useEffect, useState } from "react";
import { Button, Card, Table, useToast, type Column } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import * as adminPartnerApi from "../../api/adminPartnerApi";
import type { WaitingArea } from "../../api/adminPartnerApi";

/** Areas with shops that need a partner (none yet, or theirs was switched off): one click hands an area's shops out. */
export function AdminWaitingShopsPage() {
  const toast = useToast();
  const [areas, setAreas] = useState<WaitingArea[]>([]);
  const [loading, setLoading] = useState(true);
  const [assigningId, setAssigningId] = useState<string | null>(null);

  async function load() {
    try {
      setAreas(await adminPartnerApi.listWaitingAreas());
    } catch (err) {
      toast.show({ severity: "error", summary: "Failed to load areas", detail: extractErrorMessage(err) });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleAssign(area: WaitingArea) {
    setAssigningId(area.areaId);
    try {
      const result = await adminPartnerApi.assignWaitingArea(area.areaId);
      toast.show({
        severity: "success",
        summary: `${result.assigned} ${result.assigned === 1 ? "shop" : "shops"} assigned`,
        detail: `${area.areaName}: ${result.stillWaiting} still waiting`,
      });
      await load();
    } catch (err) {
      toast.show({ severity: "error", summary: "Could not assign shops", detail: extractErrorMessage(err) });
    } finally {
      setAssigningId(null);
    }
  }

  const columns: Column<WaitingArea>[] = [
    { field: "areaName", header: "Area" },
    { field: "cityName", header: "City", body: (a) => a.cityName ?? "-" },
    { field: "waitingShops", header: "Shops waiting" },
    { field: "activePartners", header: "Active partners" },
    {
      field: "actions",
      header: "",
      body: (a) => (
        <Button
          size="sm"
          disabled={a.activePartners === 0}
          loading={assigningId === a.areaId}
          onClick={() => handleAssign(a)}
        >
          Assign shops
        </Button>
      ),
    },
  ];

  return (
    <Card title="Shops Needing a Partner">
      <Table columns={columns} data={areas} rowKey={(row) => row.areaId} loading={loading} emptyMessage="Every shop with an area has a partner." />
    </Card>
  );
}
