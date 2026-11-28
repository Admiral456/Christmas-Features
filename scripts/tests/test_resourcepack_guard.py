from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
import resourcepack_guard as guard


def encoded(value):
    return json.dumps(value, separators=(",", ":")).encode("utf-8")


def metadata(description="test pack"):
    return encoded({
        "pack": {
            "description": description,
            "min_format": [69, 0],
            "max_format": [69, 0],
        }
    })


def item_model(model, entries=None):
    value = {"model": model}
    return encoded(value)


def dispatch(threshold, model_id):
    return {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:custom_model_data",
        "index": 0,
        "entries": [{"threshold": threshold, "model": {
            "type": "minecraft:model", "model": model_id
        }}],
        "fallback": {"type": "minecraft:model", "model": "minecraft:item/paper"},
    }


class ResourcePackGuardTests(unittest.TestCase):
    def test_repository_pack_passes_strict_validation(self):
        files = guard.read_pack(ROOT / "resourcepack")
        report = guard.validate_pack(
            files,
            label="repository resourcepack",
            strict_layout=True,
            validate_pngs=True,
        )
        self.assertEqual(report["vanilla_overrides"], [
            "assets/minecraft/items/amethyst_shard.json",
            "assets/minecraft/items/paper.json",
        ])
        self.assertEqual(report["format_min"], (69, 0))
        self.assertEqual(report["format_max"], (69, 0))

    def test_existing_vanilla_model_is_preserved_as_fallback(self):
        old_model = {
            "type": "minecraft:model",
            "model": "minecraft:item/paper",
        }
        base_meta = metadata("existing server pack")
        base = {
            "pack.mcmeta": base_meta,
            "assets/minecraft/items/paper.json": encoded({"model": old_model}),
        }
        addon = {
            "pack.mcmeta": metadata("ChristmasFeatures"),
            "assets/minecraft/items/paper.json": encoded({
                "model": dispatch(12022, "christmasfeatures:item/christmas_gift")
            }),
            "assets/christmasfeatures/models/item/christmas_gift.json": encoded({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": "christmasfeatures:item/christmas_gift"},
            }),
        }

        merged, actions = guard.merge_pack_files(base, addon)
        result = json.loads(merged["assets/minecraft/items/paper.json"])
        self.assertEqual(result["model"]["entries"][0]["threshold"], 12022)
        self.assertEqual(result["model"]["fallback"], old_model)
        self.assertEqual(merged["pack.mcmeta"], base_meta)
        self.assertEqual(base["assets/minecraft/items/paper.json"], encoded({"model": old_model}))
        self.assertIn("merged item definition: assets/minecraft/items/paper.json", actions)
        self.assertIn(
            "assets/christmasfeatures/models/item/christmas_gift.json", merged
        )

    def test_existing_custom_model_data_dispatch_is_extended(self):
        existing = dispatch(1000, "serverpack:item/server_paper")
        existing["entries"] = [{
            "threshold": 1000,
            "model": {"type": "minecraft:model", "model": "serverpack:item/server_paper"},
        }]
        existing["fallback"] = {
            "type": "minecraft:model", "model": "minecraft:item/paper"
        }
        base = {
            "pack.mcmeta": metadata("server"),
            "assets/minecraft/items/paper.json": encoded({"model": existing}),
        }
        addon = {
            "pack.mcmeta": metadata("addon"),
            "assets/minecraft/items/paper.json": encoded({
                "model": dispatch(12022, "christmasfeatures:item/christmas_gift")
            }),
        }

        merged, _ = guard.merge_pack_files(base, addon)
        model = json.loads(merged["assets/minecraft/items/paper.json"])["model"]
        self.assertEqual(
            [entry["threshold"] for entry in model["entries"]],
            [1000, 12022],
        )
        self.assertEqual(model["fallback"], existing["fallback"])

    def test_unknown_asset_collision_stops_merge(self):
        base = {
            "pack.mcmeta": metadata("server"),
            "assets/example/models/item/shared.json": b'{"parent":"minecraft:item/generated"}',
        }
        addon = {
            "pack.mcmeta": metadata("addon"),
            "assets/example/models/item/shared.json": b'{"parent":"minecraft:item/handheld"}',
        }
        with self.assertRaisesRegex(guard.GuardError, "Resource collision"):
            guard.merge_pack_files(base, addon)
        self.assertEqual(
            base["assets/example/models/item/shared.json"],
            b'{"parent":"minecraft:item/generated"}',
        )

    def test_duplicate_custom_model_data_threshold_stops_merge(self):
        existing = dispatch(12022, "serverpack:item/server_paper")
        base = {
            "pack.mcmeta": metadata("server"),
            "assets/minecraft/items/paper.json": encoded({"model": existing}),
        }
        addon = {
            "pack.mcmeta": metadata("addon"),
            "assets/minecraft/items/paper.json": encoded({
                "model": dispatch(12022, "christmasfeatures:item/christmas_gift")
            }),
        }
        with self.assertRaisesRegex(guard.GuardError, "threshold collision"):
            guard.merge_pack_files(base, addon)

    def test_missing_non_vanilla_namespace_reference_is_rejected(self):
        files = {
            "pack.mcmeta": metadata(),
            "assets/example/models/item/sample.json": encoded({
                "parent": "missing_namespace:item/base",
            }),
        }
        with self.assertRaisesRegex(guard.GuardError, "missing model referenced"):
            guard.validate_pack(
                files,
                label="test pack",
                strict_layout=False,
                validate_pngs=False,
            )

    def test_texture_variable_reference_is_not_treated_as_missing_file(self):
        files = {
            "pack.mcmeta": metadata(),
            "assets/example/models/item/parent.json": encoded({
                "textures": {"layer0": "example:item/texture"},
            }),
            "assets/example/models/item/child.json": encoded({
                "parent": "example:item/parent",
                "textures": {"layer0": "#layer0"},
            }),
            "assets/example/textures/item/texture.png": b"not validated in this test",
        }
        report = guard.validate_pack(
            files,
            label="test pack",
            strict_layout=False,
            validate_pngs=False,
        )
        self.assertEqual(report["files"], 4)

    def test_merge_dry_run_does_not_write_or_modify_inputs(self):
        import argparse
        import io
        import tempfile
        from contextlib import redirect_stdout

        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory) / "server-pack"
            base.mkdir()
            metadata_path = base / "pack.mcmeta"
            metadata_path.write_bytes(metadata("existing server pack"))
            original_metadata = metadata_path.read_bytes()
            output_path = Path(directory) / "must-not-be-created.zip"

            args = argparse.Namespace(
                base=str(base),
                addon=str(ROOT / "resourcepack"),
                output=str(output_path),
                dry_run=True,
            )
            captured = io.StringIO()
            with redirect_stdout(captured):
                guard.command_merge(args)

            result = captured.getvalue()
            self.assertIn("DRY-RUN", result)
            self.assertIn("Žádný ZIP ani kontrolní součet nebyl vytvořen", result)
            self.assertFalse(output_path.exists())
            self.assertFalse(Path(str(output_path) + ".sha256").exists())
            self.assertEqual(metadata_path.read_bytes(), original_metadata)

    def test_merge_requires_output_when_not_dry_run(self):
        import argparse

        args = argparse.Namespace(
            base="unused-base",
            addon="unused-addon",
            output=None,
            dry_run=False,
        )
        with self.assertRaisesRegex(guard.GuardError, "--output is required"):
            guard.command_merge(args)


    def test_archive_path_traversal_is_rejected(self):
        import tempfile
        import zipfile

        with tempfile.TemporaryDirectory() as directory:
            archive_path = Path(directory) / "unsafe.zip"
            with zipfile.ZipFile(archive_path, "w") as archive:
                archive.writestr("../outside.txt", "unsafe")
            with self.assertRaisesRegex(guard.GuardError, "Unsafe archive path"):
                guard.read_pack(archive_path)


if __name__ == "__main__":
    unittest.main()
