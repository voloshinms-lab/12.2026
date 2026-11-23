python
import json
import math
from datetime import datetime, timedelta

Z0 = 2.65
GERTNER_SHIFT_MIN = -11

CONSTITUENTS = [
    ("M2", 28.9841042, 1.18, 214.0),
    ("S2", 30.0000000, 0.44, 258.0),
    ("N2", 28.4397295, 0.23, 190.0),
    ("K1", 15.0410686, 0.56, 312.0),
    ("O1", 13.9430356, 0.41, 289.0),
    ("P1", 14.9589314, 0.18, 308.0),
]

def tide_height(hours_from_start):
    t = hours_from_start - (GERTNER_SHIFT_MIN / 60.0)
    h = Z0
    for _, speed, amp, phase in CONSTITUENTS:
        h += amp * math.cos(math.radians(speed * t - phase))
    return round(h, 2)

start_date = datetime(2026, 12, 1, 0, 0)
end_date = datetime(2027, 1, 1, 0, 0)
minutes_total = int((end_date - start_date).total_seconds() // 60)

heights = [(start_date + timedelta(minutes=m), tide_height(m / 60.0)) for m in range(minutes_total + 1)]
days_data = {}

for i in range(1, len(heights) - 1):
    dt_prev, h_prev = heights[i - 1]
    dt_curr, h_curr = heights[i]
    dt_next, h_next = heights[i + 1]
    if dt_curr.year != 2026:
        continue

    day_key = dt_curr.strftime("%Y-%m-%d")
    if day_key not in days_data:
        days_data[day_key] = {"date": day_key, "events": [], "hourly": []}

    if dt_curr.minute == 0:
        days_data[day_key]["hourly"].append({"hour": dt_curr.hour, "height": h_curr})

    if h_curr >= h_prev and h_curr > h_next:
        days_data[day_key]["events"].append({"time": dt_curr.strftime("%H:%M"), "type": "HIGH", "label": "Полная вода", "height": h_curr})
    elif h_curr <= h_prev and h_curr < h_next:
        days_data[day_key]["events"].append({"time": dt_curr.strftime("%H:%M"), "type": "LOW", "label": "Малая вода", "height": h_curr})

output = {"location": "Бухта Гертнера (Магадан)", "timezone": "UTC+11", "days": list(days_data.values())}
with open("app/src/main/assets/tides_gertner_dec2026.json", "w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False)
