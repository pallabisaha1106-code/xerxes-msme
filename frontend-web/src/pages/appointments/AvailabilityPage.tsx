import { PublishedDatesCard } from "./PublishedDatesCard";
import { SlotLengthCard } from "./SlotLengthCard";
import { WeeklyHoursCard } from "./WeeklyHoursCard";
import "./appointments.css";

/** Everything a business sets up before it can take appointment requests: length, weekly hours and open dates. */
export function AvailabilityPage() {
  return (
    <div className="appt-stack">
      <SlotLengthCard />
      <WeeklyHoursCard />
      <PublishedDatesCard />
    </div>
  );
}
