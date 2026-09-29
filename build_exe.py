"""
Build script to compile Realme Buds T200x GUI into a standalone Windows .exe
"""

import os
import sys
import shutil
import subprocess

def build():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)

    print("=" * 60)
    print("Building standalone Realme Buds T200x Executable...")
    print("=" * 60)

    # Clean previous builds
    for folder in ["build", "dist"]:
        if os.path.exists(folder):
            print(f"Cleaning {folder}/...")
            shutil.rmtree(folder, ignore_errors=True)

    # PyInstaller command
    cmd = [
        sys.executable,
        "-m",
        "PyInstaller",
        "--noconsole",
        "--onefile",
        "--name=Realme_Buds_T200x",
        "--icon=app_icon.ico",
        "--add-data=app_icon.ico;.",
        "--add-data=realme_buds_case.png;.",
        "--add-data=anc_noise_cancel_active.png;.",
        "--add-data=anc_noise_cancel_inactive.png;.",
        "--add-data=anc_off_active.png;.",
        "--add-data=anc_off_inactive.png;.",
        "--add-data=anc_transparency_active.png;.",
        "--add-data=anc_transparency_inactive.png;.",
        "--collect-all=customtkinter",
        "--clean",
        "t200x_gui.py",
    ]

    print("Running command:")
    print(" ".join(cmd))
    print("-" * 60)

    res = subprocess.run(cmd)

    if res.returncode != 0:
        print("\nERROR: PyInstaller build failed!")
        sys.exit(res.returncode)

    dist_exe = os.path.join(script_dir, "dist", "Realme_Buds_T200x.exe")
    root_exe = os.path.join(script_dir, "Realme_Buds_T200x.exe")

    if os.path.exists(dist_exe):
        # Copy to root directory for easy single-click access
        try:
            shutil.copy2(dist_exe, root_exe)
            print(f"\nSUCCESS! Executable created and copied to:")
            print(f"  --> {root_exe}")
            print(f"Size: {os.path.getsize(root_exe) / (1024 * 1024):.1f} MB")
        except Exception as e:
            print(f"\nBuilt in dist/ folder: {dist_exe} ({e})")
    else:
        print(f"\nWarning: Could not locate {dist_exe}")

if __name__ == "__main__":
    build()
