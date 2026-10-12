#!/usr/bin/env python3
#
# Copyright (c) 2026 Ivan Kniazkov
#
"""Check guide links and execute its Java, JSON and JSON5 examples on the built library."""

import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
DOCUMENTS = [ROOT / "README.md", *sorted((ROOT / "docs").glob("*.md"))]
OUTPUT = ROOT / "target" / "doc-examples"
CLASSES = ROOT / "target" / "classes"
FENCE = chr(96) * 3


def tool(name):
    """Locate a JDK executable, preferring JAVA_HOME."""
    home = os.environ.get("JAVA_HOME")
    executable = Path(home) / "bin" / name if home else shutil.which(name)
    if not executable:
        raise RuntimeError(f"Cannot find {name}; set JAVA_HOME or PATH")
    return str(executable)


def anchors(text):
    """Collect GitHub-style anchors for the guide's simple Markdown headings."""
    found = set()
    counts = {}
    for heading in re.findall(r"^#{1,6} +(.+)$", text, re.MULTILINE):
        base = re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")
        count = counts.get(base, 0)
        found.add(base if count == 0 else f"{base}-{count}")
        counts[base] = count + 1
    return found


def check_links(document, text):
    """Require relative Markdown links and their local heading targets to exist."""
    without_code = re.sub(rf"^{FENCE}.*?^{FENCE}[ \t]*$", "", text,
                          flags=re.MULTILINE | re.DOTALL)
    for link in re.findall(r"\[[^\]]+\]\(([^)]+)\)", without_code):
        if re.match(r"[a-zA-Z][a-zA-Z0-9+.-]*:", link):
            continue
        filename, _, fragment = link.partition("#")
        target = (document.parent / filename).resolve() if filename else document
        if not target.is_file():
            raise RuntimeError(f"{document.relative_to(ROOT)}: broken link {link}")
        if fragment and target.suffix == ".md" and fragment not in anchors(target.read_text()):
            raise RuntimeError(f"{document.relative_to(ROOT)}: broken anchor {link}")


def main():
    """Compile and execute examples, with failures attributed to their Markdown location."""
    if not CLASSES.is_dir():
        raise RuntimeError("Build the project first: mvn -Dgpg.skip=true package")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    methods = []
    calls = []
    counts = {"java": 0, "json": 0, "json5": 0}
    for document in DOCUMENTS:
        text = document.read_text(encoding="utf-8")
        check_links(document, text)
        for block in re.finditer(rf"^{FENCE}(java|json|json5)\n(.*?)^{FENCE}[ \t]*$",
                                 text, re.MULTILINE | re.DOTALL):
            language, source = block.groups()
            number = len(methods)
            line = text.count("\n", 0, block.start()) + 1
            label = f"{document.relative_to(ROOT)}:{line}"
            if language != "java":
                filename = OUTPUT / f"document-{number}.{language}"
                filename.write_text(source, encoding="utf-8")
                mode = "STRICT" if language == "json" else "JSON5"
                source = (
                    f"Json.parse(new File({json.dumps(str(filename))}), "
                    f"JsonParsingMode.{mode});"
                )
            methods.append(f"static void example{number}() throws Exception {{\n{source}\n}}\n")
            calls.append(f'System.out.println({json.dumps("Checking " + label)});')
            calls.append(f"example{number}();")
            counts[language] += 1
    harness = (
        "import com.kniazkov.json.*;\nimport java.io.*;\nimport java.math.*;\n"
        "import java.nio.charset.*;\nimport java.nio.file.*;\nimport java.util.*;\n"
        "public final class DocumentationExamples {\n"
        + "".join(methods)
        + "public static void main(String[] args) throws Exception {\n"
        + "\n".join(calls) + "\n}\n}\n"
    )
    source_file = OUTPUT / "DocumentationExamples.java"
    source_file.write_text(harness, encoding="utf-8")
    subprocess.run([tool("javac"), "--release", "21", "-encoding", "UTF-8",
                    "-cp", str(CLASSES), "-d", str(OUTPUT), str(source_file)], check=True)
    subprocess.run([tool("java"), "-ea", "-cp", os.pathsep.join(map(str, (CLASSES, OUTPUT))),
                    "DocumentationExamples"], check=True)
    print(f"PASS: local links and documentation examples: {counts}")


if __name__ == "__main__":
    main()
