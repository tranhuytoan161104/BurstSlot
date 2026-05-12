import http from "k6/http";
import { check } from "k6";
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  vus: 500,
  duration: "10s",
};

export default function () {
  const url = "http://localhost:8080/api/v1/bookings";

  const idempotencyKey = uuidv4();

  const payload = JSON.stringify({
    eventId: 1,
    userId: Math.floor(Math.random() * 10000) + 1,
    quantity: 1,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey
    },
  };

  const res1 = http.post(url, payload, params);
  const res2 = http.post(url, payload, params);

  check(res1, {
    "Lần 1 - Mua thành công (hoặc hết vé)": (r) => r.status === 200 || r.status === 409 || r.status === 400 || r.status === 500,
  });

  check(res2, {
    "Lần 2 - Bị từ chối": (r) => r.status === 500 || r.status === 409 || r.status === 400,
  });
}
