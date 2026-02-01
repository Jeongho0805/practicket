import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const errorCount = new Counter('errors');
const responseTime = new Trend('response_time');

// export const options = {
//   scenarios: {
//     constant_load: {
//       executor: 'constant-vus',
//       vus: 300,
//       duration: '30s',
//     },
//   },
// };

export const options = {
    scenarios: {
        chat_rate: {
            executor: 'constant-arrival-rate',
            rate: 1000,          // 초당 1000 요청
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 1000,
            maxVUs: 2000,
        },
    }
}

// TODO: DB에서 유효한 token을 하나 가져와서 여기에 입력
const TOKEN = __ENV.TOKEN || 'YOUR_VALID_TOKEN_HERE';

export default function () {
  const url = 'http://localhost:8080/api/chat';
  const payload = JSON.stringify({ text: '테스트 메시지입니다' });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
  };

  const res = http.post(url, payload, params);
  responseTime.add(res.timings.duration);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'no connection error': (r) => r.status !== 500,
  });

  if (!success) {
    errorCount.add(1);
    if (res.status === 500) {
      console.log(`ERROR [${res.status}]: ${res.body}`);
    }
  }
}