# Data Dictionary — Analytics Datasets

This document describes the JSON files produced by [`analytics.py`](../analytics.py)
for the CommunityBoard dashboard. Each run writes four files to the output
directory (default `output/`).

Every file is read-only data for the frontend. Files are written atomically, so
the dashboard never sees a half-written file.

---

## Shared envelope

Every file uses the same outer wrapper.

| Field          | Type            | Description                                                        |
|----------------|-----------------|--------------------------------------------------------------------|
| `generated_at` | string (ISO-8601, UTC) | When the file was created, e.g. `2026-06-02T12:15:50+00:00`. |
| `count`        | integer         | Number of records in `data`. Always equals `data` length.          |
| `data`         | array of objects | The records. Shape depends on the file (see below).               |

```json
{
  "generated_at": "2026-06-02T12:15:50+00:00",
  "count": 4,
  "data": [ ... ]
}
```

---

## `totals.json`

Overall counts for the whole board. Always exactly one record.

| Field            | Type    | Description                          |
|------------------|---------|--------------------------------------|
| `total_posts`    | integer | Total number of posts.               |
| `total_comments` | integer | Total number of comments.            |
| `total_users`    | integer | Total number of registered users.    |

```json
{ "total_posts": 52, "total_comments": 220, "total_users": 15 }
```

---

## `posts_per_category.json`

How many posts belong to each category. One record per category. Categories
with no posts still appear, with a count of `0`.

| Field        | Type    | Description                                                       |
|--------------|---------|-------------------------------------------------------------------|
| `category`   | string  | Category name. One of `NEWS`, `EVENT`, `DISCUSSION`, `ALERT`.     |
| `post_count` | integer | Number of posts in that category. `0` or more.                    |

```json
{ "category": "NEWS", "post_count": 13 }
```

---

## `posts_per_day.json`

How many posts were created on each day, over the last N days (default 30).
Every day in the window is present, even days with no posts (filled with `0`),
so the series is always continuous. Sorted oldest to newest.

| Field        | Type            | Description                                            |
|--------------|-----------------|--------------------------------------------------------|
| `day`        | string (`YYYY-MM-DD`) | The calendar day, in UTC.                        |
| `post_count` | integer         | Number of posts created that day. `0` or more.         |

```json
{ "day": "2026-05-09", "post_count": 5 }
```

---

## `top_contributors.json`

The top N users by number of posts (default 5). Sorted by post count, highest
first. Ties are broken by name (A→Z) so the order is stable across runs.

| Field        | Type    | Description                                         |
|--------------|---------|-----------------------------------------------------|
| `id`         | integer | User ID.                                            |
| `name`       | string  | User's display name.                                |
| `email`      | string  | User's email address.                               |
| `post_count` | integer | Number of posts the user has written. `1` or more.  |

```json
{ "id": 14, "name": "Christopher Becker", "email": "ddavis@example.org", "post_count": 6 }
```

---

## Notes

- All counts are whole numbers and never negative.
- All timestamps and dates are in UTC.
- The window size (days) and the number of top contributors are configurable via
  `--days` and `--top`. See the [Analytics Export section of the README](../README.md#analytics-export-analyticspy).
