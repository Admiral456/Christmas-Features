#!/usr/bin/env python3
"""Fail-closed resource-pack validation and additive merge utility.

The tool never edits its inputs. Merge output must be a new path.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import shutil
import struct
import sys
import zipfile
import zlib
from pathlib import Path, PurePosixPath
from typing import Any

TARGET_FORMAT = (69, 0)  # Java Edition 1.21.9-1.21.10
OWN_NAMESPACE = "christmasfeatures"
ALLOWED_VANILLA_OVERRIDES = {
    "assets/minecraft/items/paper.json",
    "assets/minecraft/items/amethyst_shard.json",
}
MAX_ARCHIVE_FILE = 128 * 1024 * 1024
MAX_ARCHIVE_TOTAL = 1024 * 1024 * 1024
NAMESPACE_RE = re.compile(r"^[a-z0-9._-]+$")
RESOURCE_PATH_RE = re.compile(r"^[a-z0-9/._-]+$")


class GuardError(Exception):
    """Raised when a pack cannot be proven safe to package or merge."""


def _safe_archive_path(name: str) -> str:
    if not name or "\\" in name or name.startswith("/") or "\x00" in name:
        raise GuardError(f"Unsafe archive path: {name!r}")
    path = PurePosixPath(name)
    if path.is_absolute() or any(part in ("", ".", "..") for part in name.split("/")):
        raise GuardError(f"Unsafe archive path: {name!r}")
    if re.match(r"^[A-Za-z]:", name):
        raise GuardError(f"Drive-qualified archive path: {name!r}")
    return path.as_posix()


def read_pack(source: Path) -> dict[str, bytes]:
    """Read a pack directory or ZIP without extracting untrusted archive paths."""
    source = source.expanduser()
    files: dict[str, bytes] = {}
    folded: dict[str, str] = {}

    def add(name: str, data: bytes) -> None:
        safe_name = _safe_archive_path(name)
        key = safe_name.casefold()
        if key in folded:
            raise GuardError(
                f"Duplicate/case-colliding paths: {folded[key]!r} and {safe_name!r}"
            )
        folded[key] = safe_name
        files[safe_name] = data

    if source.is_dir():
        root = source.resolve()
        for path in sorted(source.rglob("*")):
            if path.is_symlink():
                raise GuardError(f"Symlinks are not allowed in packs: {path}")
            if not path.is_file():
                continue
            resolved = path.resolve()
            if not resolved.is_relative_to(root):
                raise GuardError(f"Pack path escapes its root: {path}")
            add(path.relative_to(source).as_posix(), path.read_bytes())
    elif source.is_file() and zipfile.is_zipfile(source):
        total = 0
        with zipfile.ZipFile(source, "r") as archive:
            for info in archive.infolist():
                if info.is_dir():
                    continue
                name = _safe_archive_path(info.filename)
                mode = (info.external_attr >> 16) & 0o170000
                if mode == 0o120000:
                    raise GuardError(f"Symlink entry is not allowed: {name}")
                if info.file_size > MAX_ARCHIVE_FILE:
                    raise GuardError(f"Archive entry is too large: {name}")
                total += info.file_size
                if total > MAX_ARCHIVE_TOTAL:
                    raise GuardError("Archive expands beyond the 1 GiB safety limit.")
                add(name, archive.read(info))
    else:
        raise GuardError(f"Pack source is neither a directory nor a ZIP: {source}")

    if "pack.mcmeta" not in files:
        raise GuardError(
            f"{source} has no pack.mcmeta at its root. ZIPs with an extra wrapper "
            "directory must be repacked with the actual pack root."
        )
    return files


def _version_tuple(value: Any, field: str) -> tuple[int, int]:
    if not isinstance(value, list) or not value or len(value) > 2:
        raise GuardError(f"pack.mcmeta: {field} must be [major] or [major, minor].")
    if any(not isinstance(part, int) or isinstance(part, bool) or part < 0 for part in value):
        raise GuardError(f"pack.mcmeta: invalid {field} value {value!r}.")
    return (value[0], value[1] if len(value) == 2 else 0)


def _format_range(pack: dict[str, Any]) -> tuple[tuple[int, int], tuple[int, int]]:
    if "min_format" in pack or "max_format" in pack:
        if "min_format" not in pack or "max_format" not in pack:
            raise GuardError("pack.mcmeta must define both min_format and max_format.")
        minimum = _version_tuple(pack["min_format"], "min_format")
        maximum = _version_tuple(pack["max_format"], "max_format")
        if minimum > maximum:
            raise GuardError("pack.mcmeta has min_format greater than max_format.")
        return minimum, maximum

    supported = pack.get("supported_formats")
    if isinstance(supported, dict):
        minimum = supported.get("min_inclusive")
        maximum = supported.get("max_inclusive")
        if isinstance(minimum, int) and isinstance(maximum, int) and minimum <= maximum:
            return (minimum, 0), (maximum, 0)

    legacy = pack.get("pack_format")
    if isinstance(legacy, int) and not isinstance(legacy, bool) and legacy >= 0:
        return (legacy, 0), (legacy, 0)
    raise GuardError("pack.mcmeta does not declare a recognizable resource-pack format.")


def _model_refs(value: Any):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in ("parent", "model") and isinstance(child, str):
                yield "model", child
            if key == "textures" and isinstance(child, dict):
                for texture in child.values():
                    if isinstance(texture, str):
                        yield "texture", texture
            yield from _model_refs(child)
    elif isinstance(value, list):
        for child in value:
            yield from _model_refs(child)


def _resource_ref_target(kind: str, identifier: str) -> str | None:
    if kind == "texture" and identifier.startswith("#"):
        return None  # Texture variables are resolved through the model parent chain.
    if ":" in identifier:
        namespace, path = identifier.split(":", 1)
    else:
        namespace, path = "minecraft", identifier
    if (
        not NAMESPACE_RE.fullmatch(namespace)
        or not RESOURCE_PATH_RE.fullmatch(path)
        or any(part in ("", ".", "..") for part in path.split("/"))
    ):
        raise GuardError(f"Invalid {kind} resource identifier: {identifier!r}")
    if namespace == "minecraft":
        return None  # Vanilla resources are supplied by the game or another pack.
    if kind == "model":
        return f"assets/{namespace}/models/{path}.json"
    return f"assets/{namespace}/textures/{path}.png"


def _validate_png(data: bytes, path: str) -> None:
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise GuardError(f"Invalid PNG signature: {path}")
    offset = 8
    seen_ihdr = False
    seen_iend = False
    compressed = bytearray()
    while offset + 12 <= len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        chunk_type = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise GuardError(f"Truncated PNG chunk: {path}")
        payload = data[offset + 8:offset + 8 + length]
        expected_crc = struct.unpack(">I", data[offset + 8 + length:end])[0]
        actual_crc = zlib.crc32(chunk_type + payload) & 0xFFFFFFFF
        if expected_crc != actual_crc:
            raise GuardError(f"PNG CRC mismatch in {path} ({chunk_type!r}).")
        if chunk_type == b"IHDR":
            if seen_ihdr or length != 13:
                raise GuardError(f"Invalid PNG IHDR chunk: {path}")
            width, height = struct.unpack(">II", payload[:8])
            if width < 1 or height < 1 or width > 16384 or height > 16384:
                raise GuardError(f"Invalid PNG dimensions {width}x{height}: {path}")
            seen_ihdr = True
        elif chunk_type == b"IDAT":
            compressed.extend(payload)
        elif chunk_type == b"IEND":
            seen_iend = True
            offset = end
            break
        offset = end

    if not seen_ihdr or not seen_iend or offset != len(data):
        raise GuardError(f"Incomplete or trailing data in PNG: {path}")
    try:
        zlib.decompress(bytes(compressed))
    except zlib.error as exc:
        raise GuardError(f"Corrupt PNG image data in {path}: {exc}") from exc


def validate_pack(
    files: dict[str, bytes],
    *,
    label: str,
    strict_layout: bool,
    validate_pngs: bool,
) -> dict[str, Any]:
    try:
        metadata = json.loads(files["pack.mcmeta"].decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise GuardError(f"{label}: invalid pack.mcmeta JSON: {exc}") from exc
    pack = metadata.get("pack") if isinstance(metadata, dict) else None
    if not isinstance(pack, dict):
        raise GuardError(f"{label}: pack.mcmeta must contain a 'pack' object.")

    minimum, maximum = _format_range(pack)
    if not (minimum <= TARGET_FORMAT <= maximum):
        raise GuardError(
            f"{label}: resource-pack formats {minimum}–{maximum} do not include "
            f"the required Java 1.21.10 format {TARGET_FORMAT}."
        )

    case_paths: dict[str, str] = {}
    json_documents: dict[str, Any] = {}
    vanilla_overrides: list[str] = []

    for path, data in files.items():
        folded = path.casefold()
        if folded in case_paths and case_paths[folded] != path:
            raise GuardError(f"{label}: case-insensitive path collision: {path}")
        case_paths[folded] = path

        if not path.startswith("assets/"):
            continue
        parts = path.split("/")
        if len(parts) < 3 or parts[0] != "assets":
            raise GuardError(f"{label}: invalid asset path: {path}")
        namespace = parts[1]
        resource_path = "/".join(parts[2:])
        if (
            not NAMESPACE_RE.fullmatch(namespace)
            or not RESOURCE_PATH_RE.fullmatch(resource_path)
            or any(part in ("", ".", "..") for part in resource_path.split("/"))
        ):
            raise GuardError(f"{label}: invalid Minecraft resource path: {path}")

        if namespace == "minecraft":
            vanilla_overrides.append(path)
            if strict_layout and path not in ALLOWED_VANILLA_OVERRIDES:
                raise GuardError(
                    f"{label}: unexpected vanilla namespace override {path}. "
                    "Move plugin-owned files into assets/christmasfeatures/."
                )

        if path.endswith(".json"):
            try:
                json_documents[path] = json.loads(data.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise GuardError(f"{label}: invalid JSON in {path}: {exc}") from exc

        if validate_pngs and namespace == OWN_NAMESPACE and path.endswith(".png"):
            _validate_png(data, path)

    if strict_layout:
        expected = {
            "assets/minecraft/items/paper.json",
            "assets/minecraft/items/amethyst_shard.json",
        }
        missing = expected.difference(vanilla_overrides)
        if missing:
            raise GuardError(
                f"{label}: expected vanilla item integration files are missing: "
                + ", ".join(sorted(missing))
            )
        if any(
            not path.startswith(f"assets/{OWN_NAMESPACE}/")
            and path not in ALLOWED_VANILLA_OVERRIDES
            for path in files
            if path.startswith("assets/")
        ):
            raise GuardError(
                f"{label}: plugin assets must be inside assets/{OWN_NAMESPACE}/."
            )

    for path, document in json_documents.items():
        for kind, identifier in _model_refs(document):
            target = _resource_ref_target(kind, identifier)
            if target and target not in files:
                raise GuardError(
                    f"{label}: missing {kind} referenced by {path}: "
                    f"{identifier} (expected {target})"
                )

    if strict_layout:
        _extract_dispatch(files, "assets/minecraft/items/paper.json", 12022)
        _extract_dispatch(files, "assets/minecraft/items/amethyst_shard.json", 12023)

    return {
        "files": len(files),
        "assets": sum(path.startswith("assets/") for path in files),
        "vanilla_overrides": sorted(vanilla_overrides),
        "format_min": minimum,
        "format_max": maximum,
    }


def _extract_dispatch(
    files: dict[str, bytes], path: str, expected_threshold: int
) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    try:
        document = json.loads(files[path].decode("utf-8"))
        model = document["model"]
    except (KeyError, UnicodeDecodeError, json.JSONDecodeError, TypeError) as exc:
        raise GuardError(f"Invalid item model integration in {path}: {exc}") from exc
    if not isinstance(model, dict) or model.get("type") != "minecraft:range_dispatch":
        raise GuardError(f"{path} must use minecraft:range_dispatch.")
    if model.get("property") != "minecraft:custom_model_data" or model.get("index") != 0:
        raise GuardError(f"{path} must use custom_model_data index 0.")
    entries = model.get("entries")
    if not isinstance(entries, list):
        raise GuardError(f"{path} must contain an entries array.")
    thresholds: set[float] = set()
    for entry in entries:
        if not isinstance(entry, dict):
            raise GuardError(f"{path} contains an invalid range_dispatch entry.")
        threshold = entry.get("threshold")
        if (
            not isinstance(threshold, (int, float))
            or isinstance(threshold, bool)
            or not math.isfinite(float(threshold))
        ):
            raise GuardError(f"{path} contains a non-numeric threshold.")
        if float(threshold) in thresholds:
            raise GuardError(f"{path} contains duplicate threshold {threshold}.")
        thresholds.add(float(threshold))
    if not any(float(entry["threshold"]) == expected_threshold for entry in entries):
        raise GuardError(
            f"{path} does not contain its registered CustomModelData threshold "
            f"{expected_threshold}."
        )
    return model, entries


def _merge_item_definition(base_data: bytes, addon_data: bytes, path: str) -> bytes:
    try:
        base_doc = json.loads(base_data.decode("utf-8"))
        addon_doc = json.loads(addon_data.decode("utf-8"))
        base_model = base_doc["model"]
        addon_model = addon_doc["model"]
    except (UnicodeDecodeError, json.JSONDecodeError, KeyError, TypeError) as exc:
        raise GuardError(f"Cannot safely merge item definition {path}: {exc}") from exc

    if not isinstance(base_doc, dict) or not isinstance(addon_doc, dict):
        raise GuardError(f"Cannot safely merge non-object item definition {path}.")
    if not isinstance(base_model, dict) or not isinstance(addon_model, dict):
        raise GuardError(f"Cannot safely merge item definition {path}: missing model object.")
    if (
        addon_model.get("type") != "minecraft:range_dispatch"
        or addon_model.get("property") != "minecraft:custom_model_data"
        or addon_model.get("index") != 0
        or not isinstance(addon_model.get("entries"), list)
    ):
        raise GuardError(f"ChristmasFeatures item definition has an unexpected schema: {path}")

    new_entries = addon_model["entries"]
    for entry in new_entries:
        if (
            not isinstance(entry, dict)
            or not isinstance(entry.get("threshold"), (int, float))
            or isinstance(entry.get("threshold"), bool)
            or not isinstance(entry.get("model"), dict)
            or not math.isfinite(float(entry["threshold"]))
        ):
            raise GuardError(f"Invalid ChristmasFeatures item entry in {path}.")

    if (
        base_model.get("type") == "minecraft:range_dispatch"
        and base_model.get("property") == "minecraft:custom_model_data"
        and base_model.get("index") == 0
    ):
        old_entries = base_model.get("entries")
        if not isinstance(old_entries, list):
            raise GuardError(f"Existing range_dispatch has no entries array: {path}")
        combined = old_entries + new_entries
        seen: set[float] = set()
        for entry in combined:
            if not isinstance(entry, dict):
                raise GuardError(f"Existing item definition has an invalid entry: {path}")
            threshold = entry.get("threshold")
            if (
                not isinstance(threshold, (int, float))
                or isinstance(threshold, bool)
                or not math.isfinite(float(threshold))
                or not isinstance(entry.get("model"), dict)
            ):
                raise GuardError(f"Existing item definition has an invalid entry: {path}")
            numeric = float(threshold)
            if numeric in seen:
                raise GuardError(
                    f"CustomModelData threshold collision at {path}: {threshold}. "
                    "No file was overwritten."
                )
            seen.add(numeric)
        merged_model = dict(base_model)
        merged_model["entries"] = sorted(combined, key=lambda entry: float(entry["threshold"]))
    else:
        # Keep the entire pre-existing item model as fallback. This is additive:
        # only our registered numeric CustomModelData thresholds select our models.
        merged_model = {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "entries": new_entries,
            "fallback": base_model,
        }

    result = dict(base_doc)
    result["model"] = merged_model
    return (json.dumps(result, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def merge_pack_files(
    base_files: dict[str, bytes], addon_files: dict[str, bytes]
) -> tuple[dict[str, bytes], list[str]]:
    """Merge into a new mapping. Never modifies either input mapping."""
    merged = dict(base_files)
    actions: list[str] = []

    for path, addon_data in sorted(addon_files.items()):
        if path == "pack.mcmeta":
            # The existing server pack owns its metadata, description and format range.
            continue
        if path not in merged:
            merged[path] = addon_data
            actions.append(f"added: {path}")
            continue
        if merged[path] == addon_data:
            actions.append(f"identical: {path}")
            continue
        if path in ALLOWED_VANILLA_OVERRIDES:
            merged[path] = _merge_item_definition(merged[path], addon_data, path)
            actions.append(f"merged item definition: {path}")
            continue
        raise GuardError(
            f"Resource collision at {path}. The existing file was preserved. "
            "Review the conflict manually; this path is not on the safe-merge allowlist."
        )

    return merged, actions


def _ensure_output_is_separate(output: Path, *inputs: Path) -> None:
    output_resolved = output.expanduser().resolve()
    for source in inputs:
        source_resolved = source.expanduser().resolve()
        if source_resolved.is_dir() and output_resolved.is_relative_to(source_resolved):
            raise GuardError(
                f"Output {output_resolved} must not be inside input directory {source_resolved}."
            )
        if output_resolved == source_resolved:
            raise GuardError("Output path must differ from every input path.")
    if output.exists():
        raise GuardError(f"Refusing to overwrite existing output: {output}")


def write_zip(files: dict[str, bytes], output: Path) -> str:
    output = output.expanduser()
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        raise GuardError(f"Refusing to overwrite existing output: {output}")
    with zipfile.ZipFile(output, "x", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path, data in sorted(files.items()):
            archive.writestr(path, data)
    with zipfile.ZipFile(output, "r") as archive:
        bad_member = archive.testzip()
        if bad_member is not None:
            output.unlink(missing_ok=True)
            raise GuardError(f"Generated ZIP failed its CRC check at {bad_member}.")
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    checksum_path = Path(str(output) + ".sha256")
    if checksum_path.exists():
        output.unlink(missing_ok=True)
        raise GuardError(f"Refusing to overwrite existing checksum: {checksum_path}")
    checksum_path.write_text(f"{digest}  {output.name}\n", encoding="utf-8")
    return digest


def command_validate(args: argparse.Namespace) -> None:
    files = read_pack(Path(args.source))
    report = validate_pack(
        files,
        label=args.source,
        strict_layout=True,
        validate_pngs=True,
    )
    print(
        f"OK: {report['files']} souborů, {report['assets']} assetů, "
        f"formát {report['format_min']}–{report['format_max']}."
    )
    print("Vanilla item integrace:")
    for path in report["vanilla_overrides"]:
        print(f"  - {path}")


def command_zip(args: argparse.Namespace) -> None:
    source = Path(args.source)
    output = Path(args.output)
    _ensure_output_is_separate(output, source)
    files = read_pack(source)
    validate_pack(
        files,
        label=str(source),
        strict_layout=True,
        validate_pngs=True,
    )
    digest = write_zip(files, output)
    print(f"ZIP OK: {output}")
    print(f"SHA-256: {digest}")
    print(f"Kontrolní součet: {output}.sha256")


def command_merge(args: argparse.Namespace) -> None:
    base_path = Path(args.base)
    addon_path = Path(args.addon)
    output = Path(args.output)
    _ensure_output_is_separate(output, base_path, addon_path)

    base_files = read_pack(base_path)
    addon_files = read_pack(addon_path)
    validate_pack(
        base_files,
        label=f"existing server pack ({base_path})",
        strict_layout=False,
        validate_pngs=False,
    )
    validate_pack(
        addon_files,
        label=f"ChristmasFeatures pack ({addon_path})",
        strict_layout=True,
        validate_pngs=True,
    )
    merged, actions = merge_pack_files(base_files, addon_files)
    validate_pack(
        merged,
        label="merged server pack",
        strict_layout=False,
        validate_pngs=False,
    )
    digest = write_zip(merged, output)
    print(f"Sloučení dokončeno: {output}")
    for action in actions:
        print(f"  {action}")
    print(f"SHA-256: {digest}")
    print(
        "Metadata pack.mcmeta zůstala převzatá z původního serverového packu; "
        "vstupní soubory nebyly upraveny."
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate, package and safely merge the ChristmasFeatures resource pack."
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    validate = subparsers.add_parser("validate", help="Validate the plugin resource pack.")
    validate.add_argument("source")
    validate.set_defaults(func=command_validate)

    package = subparsers.add_parser("zip", help="Validate and create a standalone ZIP.")
    package.add_argument("source")
    package.add_argument("output")
    package.set_defaults(func=command_zip)

    merge = subparsers.add_parser(
        "merge",
        help="Merge into a copy of the final server pack; inputs are never modified.",
    )
    merge.add_argument("--base", required=True, help="Existing combined server pack (directory or ZIP).")
    merge.add_argument("--addon", required=True, help="ChristmasFeatures pack directory or ZIP.")
    merge.add_argument("--output", required=True, help="New ZIP path; it must not already exist.")
    merge.set_defaults(func=command_merge)

    args = parser.parse_args()
    try:
        args.func(args)
    except (GuardError, OSError, zipfile.BadZipFile) as exc:
        print(f"CHYBA: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
