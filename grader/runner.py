"""Wrapper around the Java HeadlessRunner for MicroSim 8-bit."""

from __future__ import annotations

import json
import os
import shutil
import subprocess
from pathlib import Path
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JAVA_CANDIDATES = [
    "/Users/lehuen/bin/JRE-25.0.1+8/Contents/Home/bin/java",
    "/Users/lehuen/bin/JDK-25.0.1+8/Contents/Home/bin/java",
]


def find_java_executable() -> str:
    """Locate a Java 21+ runtime."""
    env_java = os.environ.get("MICROSIM_JAVA")
    if env_java and Path(env_java).is_file():
        return env_java
    for candidate in DEFAULT_JAVA_CANDIDATES:
        if Path(candidate).is_file():
            return candidate
    path_java = shutil.which("java")
    if path_java:
        return path_java
    raise RuntimeError("No Java executable found. Set MICROSIM_JAVA or add java to PATH.")


def run_asm(
    asm_path: Path,
    *,
    max_cycles: int = 10000,
    keys: list[int] | None = None,
    heater: int | None = None,
    regs: dict[str, int] | None = None,
    mem: dict[int, list[int]] | None = None,
    call_addr: int | str | None = None,
    trace_al: bool = False,
) -> dict[str, Any]:
    """Execute an assembly file in MicroSim's headless Java runner and return parsed JSON."""
    java_bin = find_java_executable()
    jar_path = PROJECT_ROOT / "microsim-headless.jar"
    if jar_path.is_file():
        classpath = str(jar_path)
    else:
        classpath = f"{PROJECT_ROOT / 'class'}:{PROJECT_ROOT / 'data'}:{PROJECT_ROOT / 'lib' / '*'}"
    cmd: list[str] = [
        java_bin,
        "-Djava.awt.headless=true",
        "-cp",
        classpath,
        "microsim.headless.HeadlessRunner",
        "--asm",
        str(asm_path),
        "--max-cycles",
        str(max_cycles),
    ]
    if keys is not None:
        cmd.extend(["--keys", ",".join(str(k & 0xFF) for k in keys)])
    if heater is not None:
        cmd.extend(["--heater", str(heater & 0xFF)])
    if regs:
        for reg_name, val in regs.items():
            cmd.extend(["--reg", f"{reg_name}={val & 0xFF}"])
    if mem:
        for addr, byte_list in mem.items():
            hex_bytes = ",".join(f"0x{b & 0xFF:02X}" for b in byte_list)
            cmd.extend(["--mem", f"0x{addr & 0xFF:02X}={hex_bytes}"])
    if call_addr is not None:
        if isinstance(call_addr, int):
            cmd.extend(["--call", f"0x{call_addr & 0xFF:02X}"])
        else:
            cmd.extend(["--call", str(call_addr)])
    if trace_al:
        cmd.append("--trace-al")

    proc = subprocess.run(
        cmd,
        cwd=str(PROJECT_ROOT),
        capture_output=True,
        text=True,
        timeout=15,
        check=False,
    )
    if proc.returncode != 0 and not proc.stdout.strip():
        return {
            "assembled": False,
            "error": f"Java runner failed (code {proc.returncode}): {proc.stderr.strip()}",
        }
    last_line = proc.stdout.strip().splitlines()[-1]
    return json.loads(last_line)
