#!/usr/bin/env python3
"""Fail when a named Java class is declared inside another type or block."""

import re
import sys
from pathlib import Path


DECLARATION = re.compile(r"\bclass\s+([A-Za-z_$][A-Za-z0-9_$]*)\b|[{}]")


def strip_comments_and_literals(source: str) -> str:
    output = list(source)
    index = 0
    state = "code"
    while index < len(source):
        current = source[index]
        following = source[index : index + 3]
        if state == "code":
            if following == '"""':
                state = "text_block"
                length = 3
            elif source[index : index + 2] == "//":
                state = "line_comment"
                length = 2
            elif source[index : index + 2] == "/*":
                state = "block_comment"
                length = 2
            elif current == '"':
                state = "string"
                length = 1
            elif current == "'":
                state = "character"
                length = 1
            else:
                index += 1
                continue
            for offset in range(length):
                output[index + offset] = " "
            index += length
            continue
        if state == "line_comment" and current == "\n":
            state = "code"
            index += 1
            continue
        if state == "block_comment" and source[index : index + 2] == "*/":
            output[index] = output[index + 1] = " "
            index += 2
            state = "code"
            continue
        if state == "text_block" and following == '"""':
            output[index : index + 3] = [" ", " ", " "]
            index += 3
            state = "code"
            continue
        if state in ("string", "character") and current == "\\":
            output[index] = " "
            if index + 1 < len(source):
                output[index + 1] = " "
            index += 2
            continue
        if state == "string" and current == '"' or state == "character" and current == "'":
            state = "code"
        if current != "\n":
            output[index] = " "
        index += 1
    return "".join(output)


def nested_classes(source: str):
    sanitized = strip_comments_and_literals(source)
    depth = 0
    for match in DECLARATION.finditer(sanitized):
        if match.group(1) and depth > 0:
            line = sanitized.count("\n", 0, match.start()) + 1
            yield line, match.group(1)
        elif match.group() == "{":
            depth += 1
        elif match.group() == "}":
            depth -= 1


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2]
    violations = []
    sources = list(root.glob("vita-*/src/main/java/**/*.java"))
    sources.extend(root.glob("vita-*/src/test/java/**/*.java"))
    for path in sorted(sources):
        for line, name in nested_classes(path.read_text(encoding="utf-8")):
            violations.append(f"{path.relative_to(root)}:{line}: nested class {name}")
    if violations:
        print("\n".join(violations), file=sys.stderr)
        return 1
    print("No nested Java classes found")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
