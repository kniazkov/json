#!/usr/bin/env python3
"""Verify that Maven rejects formatting, code-style and Javadoc regressions."""

import os
from pathlib import Path
import subprocess
import shutil
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = Path("com/kniazkov/json/StyleGateProbe.java")
MAVEN = os.environ.get("MAVEN", "mvn")


def rejects(name, source_set, source, goals, diagnostic):
    """Create an isolated probe, require its diagnostic, and always remove it."""
    logs = ROOT / "target" / "style-gate"
    logs.mkdir(parents=True, exist_ok=True)
    # Each probe gets its own project copy; developer sources are never edited.
    with tempfile.TemporaryDirectory(prefix=name + "-", dir=logs) as temporary:
        project = Path(temporary)
        for directory in ("src", "config"):
            shutil.copytree(ROOT / directory, project / directory)
        shutil.copy2(ROOT / "pom.xml", project / "pom.xml")
        path = project / "src" / source_set / "java" / PACKAGE
        with path.open("x", encoding="utf-8") as output:
            output.write(source)
        result = subprocess.run(
            [MAVEN, "-B", "-ntp", *goals],
            cwd=project,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            check=False,
        )
        (logs / (name + ".log")).write_text(result.stdout, encoding="utf-8")
        if result.returncode == 0 or diagnostic not in result.stdout:
            raise RuntimeError(
                f"{name}: expected a failing check with {diagnostic!r}\n{result.stdout}"
            )
        print(f"PASS: {name} rejected with {diagnostic}", flush=True)


rejects(
    "formatting",
    "main",
    "package com.kniazkov.json;\nfinal class StyleGateProbe{int value;}\n",
    ["spotless:check"],
    "StyleGateProbe.java",
)
rejects(
    "test-code-style",
    "test",
    """package com.kniazkov.json;
import java.util.*;
final class StyleGateProbe {}
""",
    ["checkstyle:check"],
    "AvoidStarImport",
)
rejects(
    "javadoc-tags",
    "main",
    """package com.kniazkov.json;

/** Probe for public API documentation checks. */
public final class StyleGateProbe {
    /** Creates a probe. */
    public StyleGateProbe() {}

    /**
     * Returns the supplied value.
     *
     * @return the supplied value
     */
    public int value(int input) {
        return input;
    }
}
""",
    ["checkstyle:check"],
    "Expected @param tag for 'input'",
)
rejects(
    "javadoc-links",
    "main",
    """package com.kniazkov.json;

/** Links to a missing type: {@link StyleGateTypeThatDoesNotExist}. */
public final class StyleGateProbe {
    /** Creates a probe. */
    public StyleGateProbe() {}
}
""",
    ["javadoc:javadoc"],
    "reference not found",
)
