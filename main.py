#!/usr/bin/env python3
"""
Canvas: Create + attach rubric from CSV (robust)
- Uses RubricsController#create
- Sends application/x-www-form-urlencoded fields (Canvas-friendly)
- Adds criterion/rating IDs (_0, r0_0, ...) and criterion_id links
- Optionally sync assignment points_possible to rubric total

CSV format:
criterion,criterion_desc,points,rating1,rating1_points,rating1_desc,rating2,rating2_points,rating2_desc,rating3,rating3_points,rating3_desc
"""

from __future__ import annotations

import argparse
import csv
import os
import re
import sys
from typing import Any, Dict, List, Tuple

import requests


def parse_bool(s: str) -> bool:
    s = s.strip().lower()
    if s in {"1", "true", "yes", "y", "on"}:
        return True
    if s in {"0", "false", "no", "n", "off"}:
        return False
    raise argparse.ArgumentTypeError(f"Invalid boolean: {s}")


def is_number(x: str) -> bool:
    try:
        float(x)
        return True
    except Exception:
        return False


def detect_rating_groups(header: List[str]) -> List[Tuple[str, str, str]]:
    groups: List[Tuple[str, str, str]] = []
    rating_name_cols = []
    for col in header:
        m = re.fullmatch(r"rating(\d+)", col.strip())
        if m:
            rating_name_cols.append((int(m.group(1)), col))
    rating_name_cols.sort(key=lambda t: t[0])

    for n, name_col in rating_name_cols:
        pc = f"rating{n}_points"
        dc = f"rating{n}_desc"
        if pc in header and dc in header:
            groups.append((name_col, pc, dc))
    return groups


def read_rubric_csv(path: str) -> Tuple[List[Dict[str, Any]], float]:
    with open(path, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        if not reader.fieldnames:
            raise ValueError("CSV has no header row.")
        header = [h.strip() for h in reader.fieldnames]

        if "criterion" not in header or "points" not in header:
            raise ValueError("CSV must contain 'criterion' and 'points' columns.")

        rating_groups = detect_rating_groups(header)
        if len(rating_groups) < 2:
            raise ValueError(
                "CSV must have at least rating1/rating1_points/rating1_desc etc."
            )

        rows: List[Dict[str, Any]] = []
        total = 0.0
        row_num = 1
        for row in reader:
            row_num += 1
            crit = (row.get("criterion") or "").strip()
            if not crit:
                raise ValueError(f"Row {row_num}: empty criterion.")

            pts_raw = (row.get("points") or "").strip()
            if not is_number(pts_raw):
                raise ValueError(f"Row {row_num}: points must be numeric.")
            pts = float(pts_raw)
            total += pts

            crit_desc = (row.get("criterion_desc") or "").strip()

            ratings = []
            for name_col, pts_col, desc_col in rating_groups:
                r_name = (row.get(name_col) or "").strip()
                r_pts_raw = (row.get(pts_col) or "").strip()
                r_desc = (row.get(desc_col) or "").strip()

                if not r_name and not r_pts_raw and not r_desc:
                    continue

                if not r_name:
                    raise ValueError(f"Row {row_num}: {name_col} empty.")
                if not is_number(r_pts_raw):
                    raise ValueError(f"Row {row_num}: {pts_col} must be numeric.")
                ratings.append(
                    {
                        "description": r_name,
                        "points": float(r_pts_raw),
                        "long_description": r_desc,
                    }
                )

            if len(ratings) < 2:
                raise ValueError(f"Row {row_num}: need >=2 ratings.")

            rows.append(
                {
                    "criterion": crit,
                    "criterion_desc": crit_desc,
                    "points": pts,
                    "ratings": ratings,
                }
            )

        return rows, total


def to_form_fields_for_rubric_create(
    title: str,
    free_form_comments: bool,
    rows: List[Dict[str, Any]],
    association_id: int,
    use_for_grading: bool,
    hide_score_total: bool,
    purpose: str,
) -> Dict[str, str]:
    """
    Build form fields expected by RubricsController#create.

    Key trick: include ids like docs:
      rubric[criteria][0][id]=_0
      rubric[criteria][0][ratings][0][id]=r0_0
      rubric[criteria][0][ratings][0][criterion_id]=_0
    """
    data: Dict[str, str] = {}

    data["rubric[title]"] = title
    data["rubric[free_form_criterion_comments]"] = (
        "true" if free_form_comments else "false"
    )

    # Association
    data["rubric_association[association_id]"] = str(association_id)
    data["rubric_association[association_type]"] = "Assignment"
    data["rubric_association[use_for_grading]"] = "true" if use_for_grading else "false"
    data["rubric_association[hide_score_total]"] = (
        "true" if hide_score_total else "false"
    )
    data["rubric_association[purpose]"] = purpose  # usually "grading" or "bookmark"

    # Criteria
    for i, row in enumerate(rows):
        crit_id = f"_{i}"  # matches doc pattern like "_10"
        base = f"rubric[criteria][{i}]"
        data[f"{base}[id]"] = crit_id
        data[f"{base}[description]"] = row["criterion"]
        data[f"{base}[long_description]"] = row.get("criterion_desc", "")
        data[f"{base}[points]"] = str(row["points"])
        data[f"{base}[criterion_use_range]"] = "false"

        for j, r in enumerate(row["ratings"]):
            rbase = f"{base}[ratings][{j}]"
            data[f"{rbase}[id]"] = f"r{i}_{j}"  # matches doc-ish "name_2" style
            data[f"{rbase}[criterion_id]"] = crit_id
            data[f"{rbase}[description]"] = r["description"]
            data[f"{rbase}[long_description]"] = r.get("long_description", "")
            data[f"{rbase}[points]"] = str(r["points"])

    return data


def update_assignment_points(
    base_url: str, token: str, course_id: str, assignment_id: str, points: float
) -> None:
    url = (
        f"{base_url.rstrip('/')}/api/v1/courses/{course_id}/assignments/{assignment_id}"
    )
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    payload = {"assignment": {"points_possible": points, "grading_type": "points"}}
    r = requests.put(url, headers=headers, json=payload, timeout=30)
    if r.status_code >= 400:
        raise RuntimeError(
            f"Failed to update assignment points: HTTP {r.status_code} {r.text[:1000]}"
        )


def create_rubric(
    base_url: str, token: str, course_id: str, form_fields: Dict[str, str]
) -> Dict[str, Any]:
    url = f"{base_url.rstrip('/')}/api/v1/courses/{course_id}/rubrics"
    headers = {
        "Authorization": f"Bearer {token}"
    }  # let requests set content-type for form
    r = requests.post(url, headers=headers, data=form_fields, timeout=60)
    if r.status_code >= 400:
        raise RuntimeError(
            f"Rubric create failed: HTTP {r.status_code}\n{r.text[:2000]}"
        )
    return r.json()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-url", default="https://canvas.aubh.edu.bh")
    ap.add_argument("--course-id", required=True)
    ap.add_argument("--assignment-id", required=True)
    ap.add_argument("--title", required=True)
    ap.add_argument("--csv", required=True)
    ap.add_argument("--token", default=os.getenv("CANVAS_TOKEN", ""))

    ap.add_argument("--free-form-comments", type=parse_bool, default=True)
    ap.add_argument("--use-for-grading", type=parse_bool, default=True)
    ap.add_argument("--hide-score-total", type=parse_bool, default=False)
    ap.add_argument("--purpose", default="grading")
    ap.add_argument("--sync-assignment-points", type=parse_bool, default=False)
    ap.add_argument("--dry-run", action="store_true")

    args = ap.parse_args()
    if not args.token:
        print("Missing token (set CANVAS_TOKEN or pass --token).", file=sys.stderr)
        return 2

    rows, total = read_rubric_csv(args.csv)

    # If you're targeting "out of 20", total should be 20.0. We'll use it as-is.
    total_points = total

    form_fields = to_form_fields_for_rubric_create(
        title=args.title,
        free_form_comments=args.free_form_comments,
        rows=rows,
        association_id=int(args.assignment_id),
        use_for_grading=args.use_for_grading,
        hide_score_total=args.hide_score_total,
        purpose=args.purpose,
    )

    if args.dry_run:
        print(f"# Rubric total (sum of criterion points): {total_points}")
        # show a few fields to sanity-check
        sample_keys = sorted(
            k for k in form_fields.keys() if k.startswith("rubric[criteria][0]")
        )[:20]
        print("# Sample fields:", sample_keys)
        return 0

    if args.sync_assignment_points:
        update_assignment_points(
            args.base_url, args.token, args.course_id, args.assignment_id, total_points
        )
        print(f"Updated assignment points_possible to {total_points}")

    resp = create_rubric(args.base_url, args.token, args.course_id, form_fields)
    rubric_id = resp.get("rubric", {}).get("id")
    assoc_id = resp.get("rubric_association", {}).get("id")
    print("Created rubric + association.")
    print("Rubric ID:", rubric_id)
    print("Association ID:", assoc_id)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
