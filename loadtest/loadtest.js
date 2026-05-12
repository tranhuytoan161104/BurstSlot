import http from "k6/http";
import { check } from "k6";

export const options = {
  vus: 500,
  duration: "10s",
};

export default function () {
  const url = "http://localhost:8080/api/v1/bookings";

  const payload = JSON.stringify({
    eventId: 1,
    userId: Math.floor(Math.random() * 10000) + 1,
    quantity: 1,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    "Booking successful": (r) => r.status === 200,
  });
}
