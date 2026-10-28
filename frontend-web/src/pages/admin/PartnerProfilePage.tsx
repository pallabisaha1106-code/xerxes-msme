import { useEffect, useState } from "react";
import { Card, Tag, useToast } from "../../ui";
import { extractErrorMessage } from "../../api/client";
import { getMyPartnerProfile, type PartnerProfile } from "../../api/partnerApi";

/** The partner's landing page: who they are, the areas they cover and how many shops they look after. */
export function PartnerProfilePage() {
  const toast = useToast();
  const [profile, setProfile] = useState<PartnerProfile | null>(null);

  useEffect(() => {
    getMyPartnerProfile()
      .then(setProfile)
      .catch((err) => toast.show({ severity: "error", summary: "Failed to load profile", detail: extractErrorMessage(err) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!profile) return null;

  return (
    <Card title="My profile">
      <p>
        <strong>{profile.name}</strong> <Tag severity={profile.status === "ACTIVE" ? "success" : "secondary"}>{profile.status}</Tag>
      </p>
      <p>Email: {profile.email}</p>
      <p>WhatsApp: +{profile.whatsapp}</p>
      <p data-testid="partner-shop-count">Shops you look after: {profile.assignedShopCount}</p>
      <h3>Areas you cover</h3>
      <ul>
        {profile.areas.map((area) => (
          <li key={area.areaId}>
            {area.areaName}, {area.cityName}
          </li>
        ))}
      </ul>
    </Card>
  );
}
