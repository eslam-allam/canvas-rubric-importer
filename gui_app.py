#!/usr/bin/env python3

import json
import os
import threading
import tkinter as tk
from tkinter import filedialog, messagebox

import requests

from main import (
    create_rubric,
    read_rubric_csv,
    to_form_fields_for_rubric_create,
    update_assignment_points,
)

CONFIG_PATH = os.path.join(os.path.expanduser("~"), ".canvas_rubric_gui.json")


def load_config() -> dict:
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


def save_config(cfg: dict) -> None:
    try:
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(cfg, f, indent=2)
    except Exception:
        pass


class CanvasRubricGUI(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("Canvas Rubric Uploader")
        self.geometry("800x520")

        self.config_data = load_config()

        self.base_url_var = tk.StringVar(
            value=self.config_data.get("base_url", "https://canvas.aubh.edu.bh")
        )
        self.token_var = tk.StringVar(
            value=self.config_data.get("access_token", os.getenv("CANVAS_TOKEN", ""))
        )

        self.course_id_var = tk.StringVar()
        self.assignment_id_var = tk.StringVar()
        self.title_var = tk.StringVar()
        self.csv_path_var = tk.StringVar()

        self.free_form_var = tk.BooleanVar(value=True)
        self.use_for_grading_var = tk.BooleanVar(value=True)
        self.hide_score_total_var = tk.BooleanVar(value=False)
        self.sync_points_var = tk.BooleanVar(value=False)

        self.courses = []
        self.assignments = []

        self._build_widgets()

    # ---------------- GUI layout -----------------
    def _build_widgets(self) -> None:
        pad = {"padx": 6, "pady": 4}

        root = tk.Frame(self)
        root.pack(fill=tk.BOTH, expand=True)

        # --- Connection / token section ---
        conn_frame = tk.LabelFrame(root, text="Canvas Connection")
        conn_frame.pack(fill=tk.X, **pad)

        tk.Label(conn_frame, text="Base URL:").grid(row=0, column=0, sticky="e", **pad)
        tk.Entry(conn_frame, textvariable=self.base_url_var, width=40).grid(
            row=0, column=1, columnspan=2, sticky="we", **pad
        )

        tk.Label(conn_frame, text="Access token:").grid(
            row=1, column=0, sticky="e", **pad
        )
        tk.Entry(conn_frame, textvariable=self.token_var, show="*", width=40).grid(
            row=1, column=1, columnspan=2, sticky="we", **pad
        )

        tk.Button(
            conn_frame,
            text="Save Settings",
            command=self.on_save_settings,
        ).grid(row=0, column=3, rowspan=2, sticky="nswe", **pad)

        for c in range(4):
            conn_frame.grid_columnconfigure(c, weight=1)

        # --- Course / Assignment selection ---
        mid_frame = tk.Frame(root)
        mid_frame.pack(fill=tk.BOTH, expand=True, **pad)

        # Courses list
        course_frame = tk.LabelFrame(mid_frame, text="Courses")
        course_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, **pad)

        self.course_list = tk.Listbox(course_frame, height=10)
        self.course_list.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, **pad)
        self.course_list.bind("<<ListboxSelect>>", self.on_course_selected)

        sb_course = tk.Scrollbar(course_frame, orient=tk.VERTICAL)
        sb_course.pack(side=tk.RIGHT, fill=tk.Y)
        self.course_list.config(yscrollcommand=sb_course.set)
        sb_course.config(command=self.course_list.yview)

        tk.Button(
            course_frame,
            text="Load Courses",
            command=self.on_load_courses,
        ).pack(fill=tk.X, **pad)

        # Assignments list
        assign_frame = tk.LabelFrame(mid_frame, text="Assignments")
        assign_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, **pad)

        self.assignment_list = tk.Listbox(assign_frame, height=10)
        self.assignment_list.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, **pad)
        self.assignment_list.bind("<<ListboxSelect>>", self.on_assignment_selected)

        sb_assign = tk.Scrollbar(assign_frame, orient=tk.VERTICAL)
        sb_assign.pack(side=tk.RIGHT, fill=tk.Y)
        self.assignment_list.config(yscrollcommand=sb_assign.set)
        sb_assign.config(command=self.assignment_list.yview)

        tk.Button(
            assign_frame,
            text="Load Assignments",
            command=self.on_load_assignments,
        ).pack(fill=tk.X, **pad)

        # --- Rubric details ---
        bottom_frame = tk.LabelFrame(root, text="Rubric")
        bottom_frame.pack(fill=tk.X, **pad)

        tk.Label(bottom_frame, text="Selected Course ID:").grid(
            row=0, column=0, sticky="e", **pad
        )
        tk.Entry(bottom_frame, textvariable=self.course_id_var, state="readonly").grid(
            row=0, column=1, sticky="we", **pad
        )

        tk.Label(bottom_frame, text="Selected Assignment ID:").grid(
            row=1, column=0, sticky="e", **pad
        )
        tk.Entry(
            bottom_frame,
            textvariable=self.assignment_id_var,
            state="readonly",
        ).grid(row=1, column=1, sticky="we", **pad)

        tk.Label(bottom_frame, text="Rubric Title:").grid(
            row=2, column=0, sticky="e", **pad
        )
        tk.Entry(bottom_frame, textvariable=self.title_var, width=40).grid(
            row=2, column=1, columnspan=2, sticky="we", **pad
        )

        tk.Label(bottom_frame, text="CSV File:").grid(
            row=3, column=0, sticky="e", **pad
        )
        tk.Entry(bottom_frame, textvariable=self.csv_path_var, width=40).grid(
            row=3, column=1, sticky="we", **pad
        )
        tk.Button(bottom_frame, text="Browse...", command=self.browse_csv).grid(
            row=3, column=2, **pad
        )

        tk.Button(
            bottom_frame,
            text="Download CSV Template",
            command=self.on_download_template,
        ).grid(row=4, column=2, **pad)



        # Options
        tk.Checkbutton(
            bottom_frame,
            text="Free-form comments",
            variable=self.free_form_var,
        ).grid(row=5, column=0, columnspan=2, sticky="w", **pad)


        tk.Checkbutton(
            bottom_frame,
            text="Use for grading",
            variable=self.use_for_grading_var,
        ).grid(row=6, column=0, columnspan=2, sticky="w", **pad)


        tk.Checkbutton(
            bottom_frame,
            text="Hide score total",
            variable=self.hide_score_total_var,
        ).grid(row=7, column=0, columnspan=2, sticky="w", **pad)


        tk.Checkbutton(
            bottom_frame,
            text="Sync assignment points to rubric total",
            variable=self.sync_points_var,
        ).grid(row=8, column=0, columnspan=3, sticky="w", **pad)


        # Status + actions
        self.status_var = tk.StringVar(value="Idle")
        tk.Label(bottom_frame, textvariable=self.status_var, anchor="w", fg="blue").grid(
            row=9, column=0, columnspan=3, sticky="we", **pad
        )

        tk.Button(bottom_frame, text="Create Rubric", command=self.on_create).grid(
            row=10, column=1, sticky="e", **pad
        )
        tk.Button(bottom_frame, text="Quit", command=self.destroy).grid(
            row=10, column=2, sticky="w", **pad
        )


        for c in range(3):
            bottom_frame.grid_columnconfigure(c, weight=1)

    # ---------------- Canvas API helpers -----------------
    def _auth_headers(self) -> dict:
        tok = self.token_var.get().strip()
        if not tok:
            raise RuntimeError("No access token; please enter one in the connection section.")
        return {"Authorization": f"Bearer {tok}"}

    def _get_json_paginated(self, url: str) -> list:
        """Fetch all pages from a Canvas listing endpoint."""
        out = []
        session = requests.Session()
        while url:
            resp = session.get(url, headers=self._auth_headers(), timeout=30)
            if resp.status_code >= 400:
                raise RuntimeError(
                    f"HTTP {resp.status_code} while calling {url}: {resp.text[:1000]}"
                )
            data = resp.json()
            if isinstance(data, list):
                out.extend(data)
            else:
                out.append(data)

            # Very small, naive Link-header pagination parser
            link = resp.headers.get("Link", "")
            next_url = None
            for part in link.split(","):
                if 'rel="next"' in part:
                    sec = part.split(";")[0].strip().lstrip("<").rstrip(">")
                    next_url = sec
                    break
            url = next_url
        return out

    def fetch_courses(self) -> None:
        base = self.base_url_var.get().strip().rstrip("/")
        url = f"{base}/api/v1/courses?enrollment_state=active"
        self._set_status("Loading courses...")
        self.courses = self._get_json_paginated(url)

        def update_list():
            self.course_list.delete(0, tk.END)
            for c in self.courses:
                name = c.get("name") or str(c.get("id"))
                code = c.get("course_code") or ""
                display = f"{name} [{code}]" if code else name
                self.course_list.insert(tk.END, display)
            self._set_status(f"Loaded {len(self.courses)} courses")



        self.after(0, update_list)

    def fetch_assignments(self, course_id: str) -> None:
        base = self.base_url_var.get().strip().rstrip("/")
        url = f"{base}/api/v1/courses/{course_id}/assignments"
        self._set_status("Loading assignments...")
        self.assignments = self._get_json_paginated(url)

        def update_list():
            self.assignment_list.delete(0, tk.END)
            for a in self.assignments:
                name = a.get("name") or str(a.get("id"))
                self.assignment_list.insert(tk.END, name)
            self._set_status(f"Loaded {len(self.assignments)} assignments")


        self.after(0, update_list)

    # ---------------- Event handlers -----------------
    def on_save_settings(self) -> None:
        base = self.base_url_var.get().strip()
        token = self.token_var.get().strip()
        if not base or not token:
            messagebox.showerror(
                "Error", "Base URL and Access token are both required to save settings."
            )
            return
        self.config_data.update({"base_url": base, "access_token": token})
        save_config(self.config_data)
        messagebox.showinfo("Saved", "Settings saved successfully.")

    def on_load_courses(self) -> None:
        if not self.token_var.get().strip():
            messagebox.showerror("Error", "You must enter an access token first.")
            return
        threading.Thread(target=self.fetch_courses, daemon=True).start()

    def on_course_selected(self, _event=None) -> None:
        sel = self.course_list.curselection()
        if not sel:
            return
        idx = sel[0]
        course = self.courses[idx]
        cid = str(course["id"])
        self.course_id_var.set(cid)
        # Clear assignments when course changes
        self.assignment_list.delete(0, tk.END)
        self.assignments = []
        self.assignment_id_var.set("")

    def on_load_assignments(self) -> None:
        cid = self.course_id_var.get().strip()
        if not cid:
            messagebox.showerror("Error", "Please select a course first.")
            return
        threading.Thread(target=self.fetch_assignments, args=(cid,), daemon=True).start()

    def on_assignment_selected(self, _event=None) -> None:
        sel = self.assignment_list.curselection()
        if not sel:
            return
        idx = sel[0]
        assignment = self.assignments[idx]
        aid = str(assignment["id"])
        self.assignment_id_var.set(aid)

        # Always (re)generate a default rubric title based on the selected assignment.
        # If the user wants a completely custom title, they can edit it after selection.
        name = assignment.get("name") or aid
        self.title_var.set(f"Rubric for {name}")


    def browse_csv(self) -> None:
        path = filedialog.askopenfilename(
            filetypes=[("CSV files", "*.csv"), ("All files", "*.*")]
        )
        if path:
            self.csv_path_var.set(path)

    def on_download_template(self) -> None:
        if not self.assignment_id_var.get().strip():
            messagebox.showerror("Error", "Please select an assignment first.")
            return

        rubric_title = self.title_var.get().strip()
        if not rubric_title:
            messagebox.showerror(
                "Error",
                "Rubric title is required before downloading a template.",
            )
            return

        # Derive a safe file name from the rubric title.
        import re

        safe_name = re.sub(r"[^A-Za-z0-9_.-]+", "_", rubric_title) or "rubric"

        path = filedialog.asksaveasfilename(
            defaultextension=".csv",
            filetypes=[("CSV files", "*.csv"), ("All files", "*.*")],
            initialfile=f"{safe_name}.csv",
        )

        if not path:
            return

        header = [
            "criterion",
            "criterion_desc",
            "points",
            "rating1",
            "rating1_points",
            "rating1_desc",
            "rating2",
            "rating2_points",
            "rating2_desc",
        ]

        import csv

        try:
            with open(path, "w", encoding="utf-8", newline="") as f:
                writer = csv.writer(f)
                writer.writerow(header)
            messagebox.showinfo(
                "Template saved",
                f"Saved rubric CSV template to:\n{path}",
            )
        except Exception as e:  # noqa: BLE001
            messagebox.showerror("Error", f"Could not save template: {e}")


    def on_create(self) -> None:
        if not self.token_var.get().strip():
            messagebox.showerror("Error", "You must enter an access token first.")
            return
        if not self.course_id_var.get().strip():
            messagebox.showerror("Error", "Please select a course.")
            return
        if not self.assignment_id_var.get().strip():
            messagebox.showerror("Error", "Please select an assignment.")
            return
        if not self.title_var.get().strip():
            messagebox.showerror("Error", "Rubric title is required.")
            return
        if not self.csv_path_var.get().strip():
            messagebox.showerror("Error", "CSV file is required.")
            return

        threading.Thread(target=self._worker_create, daemon=True).start()

    def _worker_create(self) -> None:
        try:
            self._set_status("Reading CSV...")
            rows, total = read_rubric_csv(self.csv_path_var.get())
            self._set_status(f"Rubric total points: {total}")

            form_fields = to_form_fields_for_rubric_create(
                title=self.title_var.get().strip(),
                free_form_comments=self.free_form_var.get(),
                rows=rows,
                association_id=int(self.assignment_id_var.get().strip()),
                use_for_grading=self.use_for_grading_var.get(),
                hide_score_total=self.hide_score_total_var.get(),
                purpose="grading",
            )

            base_url = self.base_url_var.get().strip()
            token = self.token_var.get().strip()
            course_id = self.course_id_var.get().strip()
            assignment_id = self.assignment_id_var.get().strip()

            if self.sync_points_var.get():
                self._set_status("Updating assignment points...")
                update_assignment_points(
                    base_url, token, course_id, assignment_id, total
                )

            self._set_status("Creating rubric...")
            resp = create_rubric(base_url, token, course_id, form_fields)
            rubric_id = resp.get("rubric", {}).get("id")
            assoc_id = resp.get("rubric_association", {}).get("id")

            self._set_status("Done")
            messagebox.showinfo(
                "Success",
                f"Rubric created successfully!\nRubric ID: {rubric_id}\nAssociation ID: {assoc_id}",
            )
        except Exception as e:  # noqa: BLE001
            self._set_status("Error")
            messagebox.showerror("Error", str(e))

    def _set_status(self, msg: str) -> None:
        def _update() -> None:
            self.status_var.set(msg)

        self.after(0, _update)


if __name__ == "__main__":
    app = CanvasRubricGUI()
    app.mainloop()
