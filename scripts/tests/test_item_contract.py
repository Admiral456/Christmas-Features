from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
from check_item_contract import ContractError, verify_contract


class ItemContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java_source = (
            ROOT / "src/main/java/cz/vanocni/features/HolidayEventManager.java"
        ).read_text(encoding="utf-8")
        cls.pack_root = ROOT / "resourcepack"

    def test_current_java_items_match_resource_pack(self):
        summaries = verify_contract(self.java_source, self.pack_root)
        self.assertEqual(len(summaries), 2)
        self.assertTrue(any("CustomModelData 12022" in line for line in summaries))
        self.assertTrue(any("CustomModelData 12023" in line for line in summaries))

    def test_custom_model_data_mismatch_fails(self):
        changed = self.java_source.replace(
            "setCustomModelData(12022)", "setCustomModelData(12024)", 1
        )
        with self.assertRaisesRegex(ContractError, "Java používá CustomModelData 12024"):
            verify_contract(changed, self.pack_root)

    def test_base_material_mismatch_fails(self):
        changed = self.java_source.replace(
            "Material.PAPER : Material.AMETHYST_SHARD",
            "Material.BOOK : Material.AMETHYST_SHARD",
            1,
        )
        with self.assertRaisesRegex(ContractError, "Material.BOOK"):
            verify_contract(changed, self.pack_root)


if __name__ == "__main__":
    unittest.main()
