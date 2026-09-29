"""
Realme Buds T200x - Official Clean Realme Link Desktop UI
Pixel-perfect replication of the official Realme Link mobile interface for Windows 10/11.
Only includes verified, working hardware controls: Noise Control, Game Mode, and Battery/Connection.
"""

import os
import sys
import json
import time
import queue
from typing import Optional, Dict, Any, Tuple

import tkinter as tk
from tkinter import messagebox
from PIL import Image
import customtkinter as ctk

from t200x_core import (
    ANC_MODES,
    BudsController,
    PORT_LABEL_SEPARATOR,
    format_port_label,
    get_available_ports,
    pick_default_port,
)


# ============================================================
# APP IDENTITY & BEHAVIOUR CONSTANTS
# ============================================================

APP_NAME = "realme Buds T200x"
APP_VERSION = "1.0.0"

UI_PUMP_MS = 20          # ui_queue pump interval
LOG_MAX_LINES = 1000     # hard cap for the activity log textbox
LOG_TRIM_TO = 800        # trim back down to this many lines when exceeded
CMD_TIMEOUT_MS = 6000    # watchdog for a command that never reports back
NO_PORTS_PLACEHOLDER = "No COM ports found"


# Clean Realme Link Signature Palette
COLOR_BG = "#D5DADF"             # Soft matte silver-grey background
COLOR_CARD = "#FFFFFF"           # Pure white cards
COLOR_CARD_BORDER = "#E2E8F0"
COLOR_DIVIDER = "#EEF1F5"        # Subtle in-card divider
COLOR_TEXT_PRIMARY = "#111827"   # Dark charcoal text
COLOR_TEXT_SECONDARY = "#6B7280" # Slate grey text
COLOR_TEXT_MUTED = "#9CA3AF"     # Light grey text

# Control States
COLOR_PRIMARY_BTN = "#111111"
COLOR_PRIMARY_BTN_HOVER = "#27272A"

COLOR_SUCCESS = "#10B981"
COLOR_WARNING = "#F59E0B"
COLOR_DANGER = "#EF4444"
COLOR_DANGER_HOVER = "#DC2626"

# Noise-control tile interaction states
COLOR_TILE_BG_HOVER = "#EDF1F6"
COLOR_TILE_BG_PRESSED = "#E1E6ED"
COLOR_TILE_BG_SELECTED = "#F4F6F9"
COLOR_RING = "#E5E9EF"
COLOR_RING_HOVER = "#B9C2CE"

# Soft grey surfaces (pills, badges, secondary buttons)
COLOR_SOFT = "#EEF1F6"
COLOR_SOFT_HOVER = "#E1E6ED"


# ============================================================
# SHARED HELPERS (cached objects -> no per-update churn)
# ============================================================

_FONT_CACHE: Dict[Tuple[str, int, str], ctk.CTkFont] = {}


def ui_font(size: int, weight: str = "normal", family: str = "Segoe UI") -> ctk.CTkFont:
    """
    Cached CTkFont factory. Fonts are expensive to create and must never be
    re-created on every state update.
    """
    key = (family, size, weight)
    font = _FONT_CACHE.get(key)
    if font is None:
        font = ctk.CTkFont(family=family, size=size, weight=weight)
        _FONT_CACHE[key] = font
    return font


def configure_changed(widget: Any, **kwargs: Any) -> None:
    """
    configure() that skips keys whose value did not change, so frequent state
    updates (battery, status, tile painting) do not force a canvas redraw.
    """
    changed: Dict[str, Any] = {}
    for key, new_value in kwargs.items():
        try:
            current = widget.cget(key)
        except Exception:
            changed[key] = new_value
            continue
        if isinstance(current, (tuple, list)) and not isinstance(new_value, (tuple, list)):
            # CTk stores colours as (light, dark) pairs
            same = len(current) > 0 and all(item == new_value for item in current)
        else:
            same = current == new_value
        if not same:
            changed[key] = new_value
    if changed:
        widget.configure(**changed)


def battery_color(pct: Optional[int]) -> str:
    """Colour threshold: green >= 50 %, amber 20-49 %, red < 20 %, grey unknown."""
    if pct is None:
        return COLOR_TEXT_MUTED
    if pct >= 50:
        return COLOR_SUCCESS
    if pct >= 20:
        return COLOR_WARNING
    return COLOR_DANGER


def load_config() -> dict:
    """
    Load config.json. Tolerates missing files, malformed JSON, missing keys
    and unknown/extra keys (only the known keys are kept).
    """
    default_cfg = {
        "last_port": "COM4",
        "auto_connect": True,
        "show_logs": False,
    }
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
            if isinstance(data, dict):
                for key in default_cfg:
                    if key in data:
                        default_cfg[key] = data[key]
        except Exception:
            pass
    return default_cfg


def save_config(cfg: dict) -> None:
    try:
        clean = {key: cfg.get(key) for key in ("last_port", "auto_connect", "show_logs")}
        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            json.dump(clean, f, indent=2)
    except Exception:
        pass


def get_app_dir() -> str:
    """Directory where .exe or main script is located."""
    if getattr(sys, "frozen", False):
        return os.path.dirname(os.path.abspath(sys.executable))
    return os.path.dirname(os.path.abspath(__file__))


def get_bundle_dir() -> str:
    """Directory where bundled internal resources are located (PyInstaller MEIPASS)."""
    return getattr(sys, "_MEIPASS", get_app_dir())


CONFIG_FILE = os.path.join(get_app_dir(), "config.json")


# ============================================================
# MAIN APPLICATION WINDOW
# ============================================================

class RealmeBudsApp(ctk.CTk):
    def __init__(self):
        super().__init__()

        # Appearance & Window Configuration (Clean Light Mode)
        ctk.set_appearance_mode("light")
        ctk.set_default_color_theme("blue")
        self._apply_dpi_scaling()

        self.title(APP_NAME)
        self.geometry("540x760")
        self.minsize(480, 700)
        self.configure(fg_color=COLOR_BG)

        # Set Window Icon if exists
        icon_path = os.path.join(get_bundle_dir(), "app_icon.ico")
        if not os.path.exists(icon_path):
            icon_path = os.path.join(get_app_dir(), "app_icon.ico")
        if os.path.exists(icon_path):
            try:
                self.iconbitmap(icon_path)
            except Exception:
                pass

        # Load persisted configuration
        self.cfg = load_config()

        # Controller & State
        self.controller = BudsController()
        self.controller.on_connection_change = self._on_core_connection_change
        self.controller.on_state_change = self._on_core_state_change
        self.controller.on_battery_change = self._on_core_battery_change
        self.controller.on_packet_log = self._on_core_packet_log
        self.controller.on_command_result = self._on_core_command_result

        # Thread-safe UI update queue
        self.ui_queue: queue.Queue = queue.Queue()

        # UI State variables
        self.var_auto_connect = tk.BooleanVar(value=self.cfg.get("auto_connect", True))
        self.var_game_mode = tk.BooleanVar(value=False)
        self.show_logs_var = tk.BooleanVar(value=self.cfg.get("show_logs", False))

        # Noise control: optimistic selection vs. last device-confirmed mode
        self.active_anc_key: str = "normal"
        self.confirmed_anc: Optional[str] = "normal"
        self.confirmed_game: Optional[bool] = None

        # Commands in flight
        self.pending_anc: Optional[str] = None
        self.pending_game: Optional[bool] = None
        self._busy = False
        self._busy_token = 0
        self._awaiting_battery = False
        self._closing = False

        self.anc_tiles: Dict[str, dict] = {}
        self.anc_images: Dict[str, dict] = {}
        self.battery_labels: Dict[str, ctk.CTkLabel] = {}

        # Port dropdown label ("COM4 — ...") -> device ("COM4")
        self._port_labels: Dict[str, str] = {}
        self._port_count = 0

        # Batched, capped activity log
        self._log_pending: list = []
        self._log_line_count = 0

        # Preload ANC Mode circular icons (once)
        self._load_anc_icons()

        # Build UI
        self._build_ui()

        # Start UI event pump
        self._process_ui_queue()

        # Port detection & Auto-connect
        self._refresh_ports(initial=True)

        # Handle window close
        self.protocol("WM_DELETE_WINDOW", self._on_closing)

    # ============================================================
    # HIGH-DPI SCALING
    # ============================================================

    def _apply_dpi_scaling(self) -> None:
        """
        Set ctk scaling for high-DPI displays.

        customtkinter activates process DPI awareness itself and multiplies
        every widget/font size by the per-monitor DPI factor it detects, so
        the *user* scaling factor has to stay neutral there (setting the DPI
        factor again would scale everything twice). When the automatic
        detection is unavailable we fall back to an explicit system-DPI
        factor so the UI is still readable on scaled displays.
        """
        detected: Optional[float] = None
        try:
            from customtkinter.windows.widgets.scaling import ScalingTracker
            detected = ScalingTracker.get_window_dpi_scaling(self)
        except Exception:
            detected = None

        if detected is None:
            scale = 1.0
            try:
                import ctypes
                dpi = ctypes.windll.user32.GetDpiForSystem() or 96
                scale = round(max(0.5, min(3.0, dpi / 96.0)), 2)
            except Exception:
                scale = 1.0
        else:
            scale = 1.0  # automatic per-monitor DPI scaling is active

        ctk.set_widget_scaling(scale)
        ctk.set_window_scaling(scale)

    # ============================================================
    # RESOURCE LOADING
    # ============================================================

    def _load_anc_icons(self):
        bundle_dir = get_bundle_dir()
        app_dir = get_app_dir()

        modes = [
            ("noise_cancelling", "anc_noise_cancel"),
            ("normal", "anc_off"),
            ("transparency", "anc_transparency"),
        ]

        for mode_key, file_prefix in modes:
            act_path = os.path.join(bundle_dir, f"{file_prefix}_active.png")
            if not os.path.exists(act_path):
                act_path = os.path.join(app_dir, f"{file_prefix}_active.png")

            inact_path = os.path.join(bundle_dir, f"{file_prefix}_inactive.png")
            if not os.path.exists(inact_path):
                inact_path = os.path.join(app_dir, f"{file_prefix}_inactive.png")

            try:
                img_act = Image.open(act_path)
                img_inact = Image.open(inact_path)
                self.anc_images[mode_key] = {
                    "active": ctk.CTkImage(light_image=img_act, dark_image=img_act, size=(58, 58)),
                    "inactive": ctk.CTkImage(light_image=img_inact, dark_image=img_inact, size=(58, 58)),
                }
            except Exception as e:
                print(f"Error loading icon for {mode_key}: {e}")

    # ============================================================
    # UI CONSTRUCTION (CLEAN, PROPORTIONAL, ALIGNED)
    # ============================================================

    def _build_ui(self):
        # Main symmetrical container
        self.main_container = ctk.CTkFrame(self, fg_color="transparent")
        self.main_container.pack(fill="both", expand=True, padx=20, pady=(16, 12))

        # Header box: title row + (hidden) busy strip underneath. Keeping them
        # in one container guarantees the busy strip always sits below the
        # header, no matter when it is packed.
        self.header_box = ctk.CTkFrame(self.main_container, fg_color="transparent")
        self.header_box.pack(fill="x", pady=(0, 8))

        # 1. Top Header Bar (title + connection badge + last action)
        self._build_top_bar()

        # 2. Thin busy indicator (only visible while a command is in flight)
        self._build_busy_bar()

        # 3. Hero Buds & Battery pills
        self._build_hero_section()

        # 4. Noise Control Card (100% functional)
        self._build_noise_control_card()

        # 5. Game Mode Card (100% functional)
        self._build_game_mode_card()

        # 6. Bluetooth Connection & COM Port Card
        self._build_connection_card()

        # 7. Activity Log Card (Collapsible)
        self._build_log_card()

        # 8. Discreet footer
        self._build_footer()

    def _divider(self, parent: Any) -> None:
        """Subtle hairline divider used inside cards."""
        line = ctk.CTkFrame(parent, height=1, fg_color=COLOR_DIVIDER, corner_radius=0)
        line.pack_propagate(False)
        line.pack(fill="x", padx=18, pady=(10, 0))

    def _build_top_bar(self):
        top_frame = ctk.CTkFrame(self.header_box, fg_color="transparent")
        top_frame.pack(fill="x")

        # Left: Title + "last action" status line
        left_col = ctk.CTkFrame(top_frame, fg_color="transparent")
        left_col.pack(side="left", fill="x", expand=True)

        title_lbl = ctk.CTkLabel(
            left_col,
            text=APP_NAME,
            font=ui_font(17, "bold"),
            text_color=COLOR_TEXT_PRIMARY,
            anchor="w",
        )
        title_lbl.pack(anchor="w")

        # Feedback line with timestamp - visible even when the log is collapsed
        self.last_action_lbl = ctk.CTkLabel(
            left_col,
            text="Ready",
            font=ui_font(11),
            text_color=COLOR_TEXT_MUTED,
            anchor="w",
        )
        self.last_action_lbl.pack(anchor="w", pady=(2, 0))

        # Right: Connection Status Badge
        self.status_badge = ctk.CTkFrame(
            top_frame,
            fg_color="#E5EAF0",
            corner_radius=15,
        )
        self.status_badge.pack(side="right", padx=(10, 0))

        self.status_dot = ctk.CTkLabel(
            self.status_badge,
            text="●",
            font=ui_font(10),
            text_color=COLOR_DANGER,
        )
        self.status_dot.pack(side="left", padx=(11, 4), pady=4)

        self.status_text = ctk.CTkLabel(
            self.status_badge,
            text="Disconnected",
            font=ui_font(11, "bold"),
            text_color=COLOR_TEXT_SECONDARY,
        )
        self.status_text.pack(side="left", padx=(0, 11), pady=4)

    def _build_busy_bar(self):
        """Indeterminate progress strip shown while a command is in flight."""
        self.busy_bar = ctk.CTkProgressBar(
            self.header_box,
            height=4,
            mode="indeterminate",
            indeterminate_speed=1.4,
            corner_radius=2,
            fg_color="#E4E9EF",
            progress_color=COLOR_PRIMARY_BTN,
        )
        # Deliberately not packed until a command is in flight (it lives in
        # header_box, so it always appears directly below the title row).

    def _build_hero_section(self):
        hero_box = ctk.CTkFrame(self.main_container, fg_color="transparent")
        hero_box.pack(fill="x", pady=(0, 12))

        buds_img_path = os.path.join(get_bundle_dir(), "realme_buds_case.png")
        if not os.path.exists(buds_img_path):
            buds_img_path = os.path.join(get_app_dir(), "realme_buds_case.png")

        if os.path.exists(buds_img_path):
            try:
                pil_img = Image.open(buds_img_path)
                ctk_img = ctk.CTkImage(light_image=pil_img, dark_image=pil_img, size=(150, 126))
                img_lbl = ctk.CTkLabel(hero_box, image=ctk_img, text="")
                img_lbl.pack(pady=(0, 8))
            except Exception:
                pass

        # Battery pills (L / R / Case) + manual refresh
        battery_row = ctk.CTkFrame(hero_box, fg_color="transparent")
        battery_row.pack()

        for key, caption, icon in (
            ("left", "Left", "🎧"),
            ("right", "Right", "🎧"),
            ("case", "Case", "🔋"),
        ):
            self._build_battery_pill(battery_row, key, caption, icon)

        self.btn_battery_refresh = ctk.CTkButton(
            battery_row,
            text="⟳",
            width=32,
            height=32,
            corner_radius=16,
            font=ui_font(15),
            fg_color=COLOR_SOFT,
            hover_color=COLOR_SOFT_HOVER,
            text_color=COLOR_TEXT_PRIMARY,
            command=self._on_battery_refresh,
        )
        self.btn_battery_refresh.pack(side="left", padx=(8, 0))

    def _build_battery_pill(self, parent: Any, key: str, caption: str, icon: str):
        pill = ctk.CTkFrame(parent, fg_color=COLOR_SOFT, corner_radius=14)
        pill.pack(side="left", padx=(0, 8))

        icon_lbl = ctk.CTkLabel(
            pill, text=icon, font=ui_font(14), text_color=COLOR_TEXT_SECONDARY,
        )
        icon_lbl.pack(side="left", padx=(11, 5), pady=(7, 7))

        text_col = ctk.CTkFrame(pill, fg_color="transparent")
        text_col.pack(side="left", padx=(0, 11), pady=(6, 6))

        cap_lbl = ctk.CTkLabel(
            text_col, text=caption, font=ui_font(9),
            text_color=COLOR_TEXT_MUTED, anchor="w",
        )
        cap_lbl.pack(anchor="w")

        val_lbl = ctk.CTkLabel(
            text_col, text="--", font=ui_font(14, "bold"),
            text_color=COLOR_TEXT_MUTED, anchor="w",
        )
        val_lbl.pack(anchor="w")

        self.battery_labels[key] = val_lbl

    def _build_noise_control_card(self):
        card = ctk.CTkFrame(
            self.main_container,
            fg_color=COLOR_CARD,
            corner_radius=18,
        )
        card.pack(fill="x", pady=(0, 12))

        # Header
        header = ctk.CTkFrame(card, fg_color="transparent")
        header.pack(fill="x", padx=18, pady=(14, 0))

        title_lbl = ctk.CTkLabel(
            header,
            text="Noise control",
            font=ui_font(15, "bold"),
            text_color=COLOR_TEXT_PRIMARY,
            anchor="w",
        )
        title_lbl.pack(side="left")

        hint_lbl = ctk.CTkLabel(
            header,
            text="Tap a mode",
            font=ui_font(10),
            text_color=COLOR_TEXT_MUTED,
            anchor="e",
        )
        hint_lbl.pack(side="right")

        self._divider(card)

        # 3 Circular Mode Selectors
        modes_row = ctk.CTkFrame(card, fg_color="transparent")
        modes_row.pack(fill="x", padx=8, pady=(8, 10))
        modes_row.columnconfigure((0, 1, 2), weight=1)

        modes_config = [
            ("noise_cancelling", "Noise cancellation"),
            ("normal", "Off"),
            ("transparency", "Transparency"),
        ]

        for col, (mode_key, label_text) in enumerate(modes_config):
            tile = ctk.CTkFrame(
                modes_row,
                fg_color="transparent",
                corner_radius=16,
                cursor="hand2",
            )
            tile.grid(row=0, column=col, sticky="nsew", padx=5, pady=4)

            # Circular selection ring around the icon
            ring = ctk.CTkFrame(
                tile,
                fg_color="transparent",
                corner_radius=34,
                border_width=2,
                border_color=COLOR_RING,
                width=66,
                height=66,
                cursor="hand2",
            )
            ring.pack_propagate(False)
            ring.pack(pady=(10, 6))

            btn_img = None
            if mode_key in self.anc_images:
                init_state = "active" if mode_key == self.active_anc_key else "inactive"
                btn_img = self.anc_images[mode_key][init_state]

            icon_lbl = ctk.CTkLabel(ring, text="", image=btn_img, cursor="hand2")
            icon_lbl.place(relx=0.5, rely=0.5, anchor="center")

            text_lbl = ctk.CTkLabel(
                tile,
                text=label_text,
                font=ui_font(12),
                text_color=COLOR_TEXT_SECONDARY,
                justify="center",
                cursor="hand2",
            )
            text_lbl.pack(padx=4, pady=(0, 10))

            # Store references for state styling
            self.anc_tiles[mode_key] = {
                "container": tile,
                "ring": ring,
                "icon": icon_lbl,
                "label": text_lbl,
                "hover": False,
                "pressed": False,
            }

            # Bind click / hover / press events
            for widget in (tile, ring, icon_lbl, text_lbl):
                widget.bind("<Button-1>", lambda e, k=mode_key: self._on_anc_clicked(k))
                widget.bind("<Enter>", lambda e, k=mode_key: self._on_anc_enter(k))
                widget.bind("<Leave>", lambda e, k=mode_key: self._on_anc_leave(k))

        self._paint_all_anc_tiles()

    def _build_game_mode_card(self):
        card = ctk.CTkFrame(
            self.main_container,
            fg_color=COLOR_CARD,
            corner_radius=18,
        )
        card.pack(fill="x", pady=(0, 12))

        row = ctk.CTkFrame(card, fg_color="transparent")
        row.pack(fill="x", padx=18, pady=(14, 14))

        text_col = ctk.CTkFrame(row, fg_color="transparent")
        text_col.pack(side="left", fill="x", expand=True)

        title_lbl = ctk.CTkLabel(
            text_col,
            text="Game mode",
            font=ui_font(15, "bold"),
            text_color=COLOR_TEXT_PRIMARY,
            anchor="w",
        )
        title_lbl.pack(anchor="w")

        desc_lbl = ctk.CTkLabel(
            text_col,
            text="Reduces latency for audio and video synchronization",
            font=ui_font(11),
            text_color=COLOR_TEXT_SECONDARY,
            anchor="w",
        )
        desc_lbl.pack(anchor="w", pady=(2, 0))

        right_col = ctk.CTkFrame(row, fg_color="transparent")
        right_col.pack(side="right", padx=(12, 0))

        # Confirmed state text (On / Off / Unknown / pending)
        self.game_state_lbl = ctk.CTkLabel(
            right_col,
            text="Unknown",
            font=ui_font(12, "bold"),
            text_color=COLOR_TEXT_MUTED,
            anchor="e",
        )
        self.game_state_lbl.pack(anchor="e")

        self.game_switch = ctk.CTkSwitch(
            right_col,
            text="",
            variable=self.var_game_mode,
            command=self._on_game_switch_toggled,
            progress_color=COLOR_PRIMARY_BTN,
            button_color="#FFFFFF",
            button_hover_color="#F8FAFC",
            switch_width=44,
            switch_height=24,
        )
        self.game_switch.pack(anchor="e", pady=(5, 0))

    def _build_connection_card(self):
        card = ctk.CTkFrame(
            self.main_container,
            fg_color=COLOR_CARD,
            corner_radius=18,
        )
        card.pack(fill="x", pady=(0, 12))

        # Header
        title_lbl = ctk.CTkLabel(
            card,
            text="Device connection",
            font=ui_font(15, "bold"),
            text_color=COLOR_TEXT_PRIMARY,
            anchor="w",
        )
        title_lbl.pack(fill="x", padx=18, pady=(14, 0))

        self._divider(card)

        # Controls Row
        top_row = ctk.CTkFrame(card, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=(12, 4))

        port_lbl = ctk.CTkLabel(
            top_row,
            text="Port",
            font=ui_font(12, "bold"),
            text_color=COLOR_TEXT_SECONDARY,
        )
        port_lbl.pack(side="left", padx=(0, 6))

        self.port_combobox = ctk.CTkComboBox(
            top_row,
            values=["Scanning..."],
            width=200,
            font=ui_font(12),
            fg_color="#F3F4F6",
            border_color="#D1D5DB",
            text_color=COLOR_TEXT_PRIMARY,
            dropdown_fg_color="#FFFFFF",
            dropdown_text_color=COLOR_TEXT_PRIMARY,
            button_color="#E5E7EB",
            button_hover_color="#D1D5DB",
        )
        self.port_combobox.pack(side="left", padx=(0, 6))

        self.btn_refresh = ctk.CTkButton(
            top_row,
            text="🔄",
            width=34,
            height=30,
            fg_color=COLOR_SOFT,
            hover_color="#E1E5EA",
            text_color=COLOR_TEXT_PRIMARY,
            command=self._refresh_ports,
        )
        self.btn_refresh.pack(side="left", padx=(0, 8))

        self.btn_connect = ctk.CTkButton(
            top_row,
            text="Connect",
            width=92,
            height=30,
            font=ui_font(12, "bold"),
            fg_color=COLOR_PRIMARY_BTN,
            hover_color=COLOR_PRIMARY_BTN_HOVER,
            text_color="#FFFFFF",
            corner_radius=15,
            command=self._toggle_connection,
        )
        self.btn_connect.pack(side="right")

        # Options Row: Auto-connect on left, Log toggle on right
        opt_row = ctk.CTkFrame(card, fg_color="transparent")
        opt_row.pack(fill="x", padx=16, pady=(8, 14))

        self.chk_auto = ctk.CTkCheckBox(
            opt_row,
            text="Auto-connect on launch",
            variable=self.var_auto_connect,
            command=self._on_auto_connect_toggle,
            font=ui_font(11),
            text_color=COLOR_TEXT_SECONDARY,
            fg_color=COLOR_PRIMARY_BTN,
            hover_color=COLOR_PRIMARY_BTN_HOVER,
            checkmark_color="#FFFFFF",
            border_color="#9CA3AF",
            checkbox_width=16,
            checkbox_height=16,
        )
        self.chk_auto.pack(side="left")

        self.btn_toggle_log = ctk.CTkButton(
            opt_row,
            text="Activity Log ▼",
            font=ui_font(11),
            fg_color="transparent",
            hover_color="#F3F4F6",
            text_color=COLOR_TEXT_SECONDARY,
            width=88,
            height=20,
            command=self._toggle_log_view,
        )
        self.btn_toggle_log.pack(side="right")

    def _build_log_card(self):
        self.log_card = ctk.CTkFrame(
            self.main_container,
            fg_color=COLOR_CARD,
            corner_radius=18,
        )
        # Initially not packed unless show_logs is true

        toggle_row = ctk.CTkFrame(self.log_card, fg_color="transparent")
        toggle_row.pack(fill="x", padx=16, pady=(10, 6))

        log_title = ctk.CTkLabel(
            toggle_row,
            text="Protocol activity monitor",
            font=ui_font(12, "bold"),
            text_color=COLOR_TEXT_SECONDARY,
        )
        log_title.pack(side="left")

        self.btn_clear_log = ctk.CTkButton(
            toggle_row,
            text="Clear",
            font=ui_font(11),
            width=46,
            height=22,
            fg_color=COLOR_SOFT,
            hover_color="#E1E5EA",
            text_color=COLOR_TEXT_SECONDARY,
            command=self._clear_log,
        )
        self.btn_clear_log.pack(side="right")

        self.log_box = ctk.CTkTextbox(
            self.log_card,
            height=110,
            font=ui_font(10, family="Consolas"),
            fg_color="#18181B",
            text_color="#A1A1AA",
            border_width=0,
            corner_radius=12,
            activate_scrollbars=True,
        )
        self.log_box.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        if self.show_logs_var.get():
            self._show_log_view()

    def _build_footer(self):
        self.footer_lbl = ctk.CTkLabel(
            self.main_container,
            text="",
            font=ui_font(10),
            text_color=COLOR_TEXT_MUTED,
            anchor="center",
        )
        self.footer_lbl.pack(fill="x", pady=(4, 0))
        self._update_footer()

    def _update_footer(self):
        count = self._port_count
        ports_txt = f"{count} COM port{'s' if count != 1 else ''} discovered"
        configure_changed(
            self.footer_lbl,
            text=f"{APP_NAME} v{APP_VERSION} · {ports_txt}",
        )

    # ============================================================
    # LOG VIEW TOGGLE
    # ============================================================

    def _toggle_log_view(self):
        if self.show_logs_var.get():
            self._hide_log_view()
        else:
            self._show_log_view()

    def _show_log_view(self):
        self.show_logs_var.set(True)
        self.btn_toggle_log.configure(text="Activity Log ▲")
        # Always keep the log card above the footer, even when re-packed later
        if hasattr(self, "footer_lbl"):
            self.log_card.pack(fill="x", pady=(0, 12), before=self.footer_lbl)
        else:
            self.log_card.pack(fill="x", pady=(0, 12))
        self.cfg["show_logs"] = True
        save_config(self.cfg)

    def _hide_log_view(self):
        self.show_logs_var.set(False)
        self.btn_toggle_log.configure(text="Activity Log ▼")
        self.log_card.pack_forget()
        self.cfg["show_logs"] = False
        save_config(self.cfg)

    def _clear_log(self):
        self._log_pending = []
        self._log_line_count = 0
        self.log_box.delete("1.0", "end")

    # ============================================================
    # PORT MANAGEMENT & CONNECTIVITY
    # ============================================================

    def _refresh_ports(self, initial: bool = False):
        ports = get_available_ports()

        self._port_labels = {}
        labels = []
        for p in ports:
            label = format_port_label(p)
            self._port_labels[label] = p["device"]
            labels.append(label)
        if not labels:
            labels = [NO_PORTS_PLACEHOLDER]

        self.port_combobox.configure(values=labels)

        chosen = pick_default_port(ports, preferred=self.cfg.get("last_port", ""))
        if chosen:
            label = next(
                (lbl for lbl, dev in self._port_labels.items() if dev == chosen),
                labels[0],
            )
        else:
            label = labels[0]
        self.port_combobox.set(label)

        self._port_count = len(ports)
        self._update_footer()

        if not initial:
            if ports:
                names = ", ".join(p["device"] for p in ports)
                self._append_log_message(
                    "INFO", f"Discovered {len(ports)} serial port(s): {names}"
                )
            else:
                self._append_log_message("WARN", "No serial ports discovered.")

        if initial and self.var_auto_connect.get() and chosen:
            self._append_log_message("INFO", f"Auto-connecting to {chosen}...")
            self._connect(chosen)

    def _selected_port(self) -> str:
        """Resolve the combobox selection ('COM4 — ...') to a device ('COM4')."""
        selection = (self.port_combobox.get() or "").strip()
        if not selection or selection in ("Scanning...", NO_PORTS_PLACEHOLDER):
            return ""
        device = self._port_labels.get(selection)
        if device:
            return device
        return selection.split(PORT_LABEL_SEPARATOR)[0].strip()

    def _toggle_connection(self):
        if self.controller.is_connected:
            self._disconnect()
            return

        selected_port = self._selected_port()
        if not selected_port:
            messagebox.showwarning("Port Missing", "Please select a valid COM port first.")
            return
        self._connect(selected_port)

    def _connect(self, port: str):
        self._update_status_badge("Connecting…", COLOR_WARNING)
        self._set_last_action(f"Connecting to {port}…", None)
        self.btn_connect.configure(state="disabled", text="Connecting...")
        self.cfg["last_port"] = port
        save_config(self.cfg)
        self.controller.connect(port)

    def _disconnect(self):
        self._set_last_action("Disconnecting…", None)
        self.controller.disconnect()

    def _on_auto_connect_toggle(self):
        self.cfg["auto_connect"] = self.var_auto_connect.get()
        save_config(self.cfg)

    # ============================================================
    # NOISE CONTROL: PAINTING, HOVER & CLICK HANDLING
    # ============================================================

    def _update_anc_selection(self, active_key: Optional[str]):
        """Show `active_key` as the selected mode (optimistic or confirmed)."""
        self.active_anc_key = active_key or "normal"
        self._paint_all_anc_tiles()

    def _paint_all_anc_tiles(self):
        for mode_key in list(self.anc_tiles.keys()):
            self._paint_anc_tile(mode_key)

    def _paint_anc_tile(self, mode_key: str):
        tile = self.anc_tiles.get(mode_key)
        if not tile:
            return

        selected = (mode_key == self.active_anc_key)
        if tile["pressed"]:
            bg, ring_color = COLOR_TILE_BG_PRESSED, COLOR_RING_HOVER
        elif tile["hover"]:
            bg, ring_color = COLOR_TILE_BG_HOVER, COLOR_RING_HOVER
        elif selected:
            bg, ring_color = COLOR_TILE_BG_SELECTED, COLOR_PRIMARY_BTN
        else:
            bg, ring_color = "transparent", COLOR_RING

        cursor = "arrow" if self._busy else "hand2"

        configure_changed(
            tile["container"],
            fg_color=bg,
            cursor=cursor,
        )
        configure_changed(
            tile["ring"],
            border_color=ring_color,
            cursor=cursor,
        )
        configure_changed(
            tile["label"],
            cursor=cursor,
            font=ui_font(12, "bold" if selected else "normal"),
            text_color=COLOR_TEXT_PRIMARY if selected else COLOR_TEXT_SECONDARY,
        )

        images = self.anc_images.get(mode_key)
        if images:
            configure_changed(
                tile["icon"],
                image=images["active" if selected else "inactive"],
                cursor=cursor,
            )

    def _on_anc_enter(self, mode_key: str):
        tile = self.anc_tiles.get(mode_key)
        if not tile:
            return
        tile["hover"] = True
        self._paint_anc_tile(mode_key)

    def _on_anc_leave(self, mode_key: str):
        tile = self.anc_tiles.get(mode_key)
        if not tile:
            return
        tile["hover"] = False
        tile["pressed"] = False
        self._paint_anc_tile(mode_key)

    def _clear_tile_pressed(self, mode_key: str):
        tile = self.anc_tiles.get(mode_key)
        if not tile:
            return
        tile["pressed"] = False
        self._paint_anc_tile(mode_key)

    def _on_anc_clicked(self, mode_key: str):
        # Tiles are disabled while a command is in flight (no device spamming)
        if self._busy:
            return

        if not self.controller.is_connected:
            messagebox.showinfo(
                "Not Connected",
                "Please connect to your Realme Buds Bluetooth port before changing modes."
            )
            return

        info = ANC_MODES.get(mode_key, {})
        name = info.get("name", mode_key)

        # Brief pressed feedback
        tile = self.anc_tiles.get(mode_key)
        if tile:
            tile["pressed"] = True
            self._paint_anc_tile(mode_key)
            try:
                self.after(140, lambda k=mode_key: self._clear_tile_pressed(k))
            except Exception:
                pass

        self.pending_anc = mode_key
        self._set_busy(True)
        # Optimistic selection: reverted by _handle_cmd_result() on failure
        self._update_anc_selection(mode_key)

        self._append_log_message("ACTION", f"Setting ANC mode to {name}...")
        self._set_last_action(f"Setting {name}…", None)
        self.controller.set_anc(mode_key)

    # ============================================================
    # GAME MODE ACTION
    # ============================================================

    def _set_game_state_text(self, state: Any):
        """state: True / False / None (unknown) / 'pending'."""
        if state == "pending":
            text, color, weight = "…", COLOR_WARNING, "bold"
        elif state is True:
            text, color, weight = "On", COLOR_SUCCESS, "bold"
        elif state is False:
            text, color, weight = "Off", COLOR_TEXT_SECONDARY, "bold"
        else:
            text, color, weight = "Unknown", COLOR_TEXT_MUTED, "normal"
        configure_changed(
            self.game_state_lbl,
            text=text,
            text_color=color,
            font=ui_font(12, weight),
        )

    def _revert_game_switch(self):
        """Put the switch back to the last device-confirmed state."""
        if self.confirmed_game is None:
            self.var_game_mode.set(False)
            self._set_game_state_text(None)
        else:
            self.var_game_mode.set(self.confirmed_game)
            self._set_game_state_text(self.confirmed_game)

    def _on_game_switch_toggled(self):
        # The switch is disabled while a command is in flight
        if self._busy:
            self._revert_game_switch()
            return

        if not self.controller.is_connected:
            messagebox.showinfo(
                "Not Connected",
                "Please connect to your Realme Buds Bluetooth port before toggling Game Mode."
            )
            self._revert_game_switch()
            return

        enabled = bool(self.var_game_mode.get())
        self.pending_game = enabled
        self._set_game_state_text("pending")
        self._set_busy(True)

        self._append_log_message("ACTION", f"Turning Game Mode {'ON' if enabled else 'OFF'}...")
        self._set_last_action(f"Setting game mode {'ON' if enabled else 'OFF'}…", None)
        self.controller.set_game_mode(enabled)

    # ============================================================
    # IN-FLIGHT COMMAND (BUSY) STATE
    # ============================================================

    def _set_busy(self, busy: bool):
        if busy:
            if self._busy:
                return
            self._busy = True
            self._busy_token += 1
            token = self._busy_token
            try:
                self.busy_bar.pack(fill="x", pady=(0, 8))
                self.busy_bar.start()
            except Exception:
                pass
            self.game_switch.configure(state="disabled")
            self.btn_battery_refresh.configure(state="disabled")
            self._paint_all_anc_tiles()
            try:
                self.after(CMD_TIMEOUT_MS, lambda t=token: self._on_command_timeout(t))
            except Exception:
                pass
        else:
            self._busy = False
            self._busy_token += 1
            try:
                self.busy_bar.stop()
                self.busy_bar.pack_forget()
            except Exception:
                pass
            self.game_switch.configure(state="normal")
            self.btn_battery_refresh.configure(
                state="normal" if self.controller.is_connected else "disabled"
            )
            self.pending_anc = None
            self.pending_game = None
            self._paint_all_anc_tiles()

    def _on_command_timeout(self, token: int):
        """Safety net: the device never confirmed the command."""
        if token != self._busy_token or not self._busy:
            return

        pending_anc = self.pending_anc
        pending_game = self.pending_game

        if pending_anc is not None:
            name = ANC_MODES.get(pending_anc, {}).get("name", "Mode")
            self._update_anc_selection(self.confirmed_anc)
            self._set_last_action(f"{name} failed · no response", False)
        if pending_game is not None:
            self._revert_game_switch()
            self._set_last_action("Game mode change failed · no response", False)

        self._append_log_message("WARN", "No confirmation from the device (command timed out).")
        self._set_busy(False)

    # ============================================================
    # BACKGROUND WORKER EVENT DISPATCH (THREAD SAFE)
    # ============================================================

    def _on_core_connection_change(self, connected: bool, port: str, error_msg: Optional[str]):
        self.ui_queue.put(("CONN", (connected, port, error_msg)))

    def _on_core_state_change(self, game_mode: Optional[bool], anc_mode: Optional[str]):
        self.ui_queue.put(("STATE", (game_mode, anc_mode)))

    def _on_core_battery_change(self, left: Optional[int], right: Optional[int], case: Optional[int]):
        self.ui_queue.put(("BATTERY", (left, right, case)))

    def _on_core_packet_log(self, level: str, message: str, raw_bytes: Optional[bytes]):
        self.ui_queue.put(("LOG", (level, message, raw_bytes)))

    def _on_core_command_result(self, action: str, ok: bool, detail: str):
        self.ui_queue.put(("CMD_RESULT", (action, ok, detail)))

    def _process_ui_queue(self):
        try:
            while True:
                evt, data = self.ui_queue.get_nowait()
                if evt == "CONN":
                    connected, port, error = data
                    self._handle_conn_update(connected, port, error)
                elif evt == "STATE":
                    game_mode, anc_mode = data
                    self._handle_state_update(game_mode, anc_mode)
                elif evt == "BATTERY":
                    left, right, case = data
                    self._handle_battery_update(left, right, case)
                elif evt == "CMD_RESULT":
                    action, ok, detail = data
                    self._handle_cmd_result(action, ok, detail)
                elif evt == "LOG":
                    level, msg, raw = data
                    self._append_log_message(level, msg)
                self.ui_queue.task_done()
        except queue.Empty:
            pass

        # Batched log insertion (cheap: one insert per pump tick)
        if self._log_pending:
            self._flush_log()

        try:
            self.after(UI_PUMP_MS, self._process_ui_queue)
        except Exception:
            pass

    def _handle_cmd_result(self, action: str, ok: bool, detail: str):
        pending_anc = self.pending_anc
        pending_game = self.pending_game

        if action == "ANC_MODE":
            mode_key = pending_anc or self.active_anc_key
            name = ANC_MODES.get(mode_key, {}).get("name", "Mode")
            if ok:
                self.confirmed_anc = mode_key
                self._update_anc_selection(mode_key)
                self._set_last_action(f"{name} set", True)
            else:
                # Revert to the last confirmed mode + visible failure status
                self._update_anc_selection(self.confirmed_anc)
                self._set_last_action(f"{name} failed · no response", False)
        elif action == "GAME_MODE":
            if ok:
                enabled = pending_game if pending_game is not None else bool(self.confirmed_game)
                self.confirmed_game = enabled
                self.var_game_mode.set(enabled)
                self._set_game_state_text(enabled)
                self._set_last_action(f"Game mode {'ON' if enabled else 'OFF'}", True)
            else:
                self._revert_game_switch()
                self._set_last_action("Game mode change failed · no response", False)

        self._set_busy(False)

    def _handle_battery_update(self, left: Optional[int], right: Optional[int], case: Optional[int]):
        if not self.controller.is_connected:
            self._set_battery_pills(left=None, right=None, case=None)
            return

        # A reported case level of 0 means "not present / not readable"
        case_pct = case if (case is not None and case > 0) else None
        self._set_battery_pills(left=left, right=right, case=case_pct)

        if self._awaiting_battery:
            self._awaiting_battery = False
            parts = [
                f"L {_pct_text(left)}",
                f"R {_pct_text(right)}",
                f"Case {_pct_text(case_pct)}",
            ]
            self._set_last_action("Battery updated · " + "  ".join(parts), True)

    def _set_battery_pills(self, left: Optional[int], right: Optional[int], case: Optional[int]):
        values = {"left": left, "right": right, "case": case}
        for key, label in self.battery_labels.items():
            pct = values.get(key)
            if pct is None:
                configure_changed(label, text="--", text_color=COLOR_TEXT_MUTED)
            else:
                configure_changed(label, text=f"{pct}%", text_color=battery_color(pct))

    def _on_battery_refresh(self):
        if not self.controller.is_connected:
            messagebox.showinfo(
                "Not Connected",
                "Please connect to your Realme Buds Bluetooth port first."
            )
            return
        self._awaiting_battery = True
        self._set_last_action("Refreshing battery…", None)
        self._append_log_message("ACTION", "Manual battery refresh requested")
        self.controller.query_battery()

    def _handle_conn_update(self, connected: bool, port: str, error_msg: Optional[str]):
        self.btn_connect.configure(state="normal")

        if connected:
            self._update_status_badge(f"Connected ({port})", COLOR_SUCCESS)
            self._set_last_action(f"Connected to {port}", True)

            if self.controller.battery_left is not None:
                self._handle_battery_update(
                    self.controller.battery_left,
                    self.controller.battery_right,
                    self.controller.battery_case,
                )
            else:
                self._set_battery_pills(left=None, right=None, case=None)
                self.controller.query_battery()

            self.btn_connect.configure(
                text="Disconnect",
                fg_color=COLOR_DANGER,
                hover_color=COLOR_DANGER_HOVER,
                text_color="#FFFFFF",
            )
            self.port_combobox.configure(state="disabled")
            self.btn_refresh.configure(state="disabled")
            self.btn_battery_refresh.configure(state="normal")
        else:
            # Abandon anything still in flight (its result may never come)
            self._set_busy(False)

            if error_msg:
                self._update_status_badge("Error", COLOR_DANGER)
                self._set_last_action(f"Connection error · {error_msg}", False)
                self._append_log_message("ERROR", f"Connection error on {port}: {error_msg}")
            else:
                self._update_status_badge("Disconnected", COLOR_DANGER)
                self._set_last_action(f"Disconnected from {port}" if port else "Disconnected", None)

            self._set_battery_pills(left=None, right=None, case=None)
            self._awaiting_battery = False

            self.btn_connect.configure(
                text="Connect",
                fg_color=COLOR_PRIMARY_BTN,
                hover_color=COLOR_PRIMARY_BTN_HOVER,
                text_color="#FFFFFF",
            )
            self.port_combobox.configure(state="normal")
            self.btn_refresh.configure(state="normal")
            self.btn_battery_refresh.configure(state="disabled")

            self.confirmed_anc = "normal"
            self._update_anc_selection("normal")
            self.confirmed_game = None
            self.var_game_mode.set(False)
            self._set_game_state_text(None)

    def _handle_state_update(self, game_mode: Optional[bool], anc_mode: Optional[str]):
        # Only called when the device actually confirmed a state
        if game_mode is not None:
            self.confirmed_game = game_mode
            self.var_game_mode.set(game_mode)
            self._set_game_state_text(game_mode)

        if anc_mode is not None:
            self.confirmed_anc = anc_mode
            self._update_anc_selection(anc_mode)

    def _update_status_badge(self, text: str, dot_color: str):
        text_color = COLOR_TEXT_PRIMARY if dot_color == COLOR_SUCCESS else COLOR_TEXT_SECONDARY
        configure_changed(self.status_dot, text_color=dot_color)
        configure_changed(self.status_text, text=text, text_color=text_color)

    def _set_last_action(self, text: str, ok: Optional[bool] = None):
        stamp = time.strftime("%H:%M:%S")
        if ok is False:
            color = COLOR_DANGER
        elif ok is True:
            color = COLOR_TEXT_SECONDARY
        else:
            color = COLOR_TEXT_MUTED
        configure_changed(self.last_action_lbl, text=f"{stamp} · {text}", text_color=color)

    # ============================================================
    # ACTIVITY LOG (BATCHED + CAPPED)
    # ============================================================

    def _append_log_message(self, level: str, message: str):
        # Buffered; flushed in one insert per UI pump tick, so a burst of
        # packets can never make the UI stutter.
        self._log_pending.append(f"[{time.strftime('%H:%M:%S')}] [{level}] {message}")

    def _flush_log(self):
        if not self._log_pending or not hasattr(self, "log_box"):
            return

        lines, self._log_pending = self._log_pending, []
        self.log_box.insert("end", "".join(line + "\n" for line in lines))
        self._log_line_count += len(lines)

        # Cap the log: trim from the top, in chunks, to stay ~1000 lines
        if self._log_line_count > LOG_MAX_LINES:
            cut = self._log_line_count - LOG_TRIM_TO
            if cut > 0:
                try:
                    self.log_box.delete("1.0", f"{cut + 1}.0")
                    self._log_line_count = LOG_TRIM_TO
                except Exception:
                    pass

        try:
            self.log_box.see("end")
        except Exception:
            pass

    # ============================================================
    # WINDOW LIFECYCLE
    # ============================================================

    def _on_closing(self):
        if self._closing:
            return
        self._closing = True
        try:
            self.controller.close()
        finally:
            self.destroy()


def _pct_text(pct: Optional[int]) -> str:
    return f"{pct}%" if pct is not None else "--"


def main():
    app = RealmeBudsApp()
    app.mainloop()


if __name__ == "__main__":
    main()
