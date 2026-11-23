# ChristmasFeatures — Paper/Purpur 1.21.10

Samostatný vánoční plugin pro Paper/Purpur 1.21.10. Projekt cílí na Javu 21.

## Funkce

- **Hledání dárků a ozdob:** soutěžní eventy s rozmístěnými sběratelskými předměty. První hráč, který splní cíl, vyhrává; při vypršení limitu rozhoduje nejvyšší skóre.
- **BetterHud panel:** pravý HUD zobrazuje vánoční sezónu, počet klíčů hráče, probíhající event, skóre, čas a úkol ChatGames. Při nedostupném BetterHud se použije BossBar.
- **Grinch na spawnu:** MythicMobs NPC s ModelEngine modelem a průběžným sledováním nejbližšího hráče. ChristmasFeatures zachytí pravý klik přes scoreboard tag NPC a otevře GUI; SupremeTags a BattlePass nabídky se spouštějí v kontextu klikajícího hráče.
- **Vánoční bedna:** nastaví se na existující truhlu nebo sud na spawnu. Otevření vyžaduje speciální vánoční klíč s PDC identifikací.
- **Vánoční klíč a odměny:** klíče získávají vítězové eventů a ChatGames. Bedna losuje vážené odměny; každé otevření garantuje skutečný předmět. Kosmetické nebo neplatné odměny mají fyzickou záložní odměnu a při plném inventáři se předmět položí na zem.
- **ChatGames:** globálně oznámené úkoly. První správná odpověď vyhrává, plugin oznámí vítěze a předá odměnu.
- **Zimní svět:** při načítání chunků přepisuje 3D biomy na `SNOWY_PLAINS`, udržuje sněžení a brání tání sněhu/ledu. Terén ani stavby se negenerují znovu.
- **Resource pack:** vlastní item modely/textury vánočního klíče, dárku a ozdoby.
- Přesný výchozí loot table, váhy, šance a pravidla záložní fyzické odměny jsou popsány v [docs/KEY_CHEST_LOOT.md](docs/KEY_CHEST_LOOT.md).

Podle zadání projekt neobsahuje bossy ani speciální bojové itemy.

## Požadavky

- Paper nebo kompatibilní Purpur 1.21.10
- Java 21
- Maven 3.9+ pro lokální sestavení (v GitHub Actions se sestaví automaticky)

## Sestavení

```bash
mvn clean verify
```

Ve Windows lze spustit `build.bat`, v Linuxu/macOS `./build.sh`.
Výsledný plugin bude `target/ChristmasFeatures-1.21.10.jar`.

GitHub Actions workflow po pushi nebo pull requestu na `main` provede sestavení a zveřejní JAR i ZIP resource packu jako workflow artifact.

## Instalace

1. Vlož `target/ChristmasFeatures-1.21.10.jar` do složky `plugins/`.
2. Spusť server, aby vznikla konfigurace, a uprav `plugins/ChristmasFeatures/config.yml`.
3. Nastav správný název světa v sekci `winter.worlds` a v nastavení světů eventů.
4. Polož prázdnou truhlu nebo sud na požadované místo spawnu. Dívej se na blok a použij `/christmas chest set`.
5. Nahraj ZIP resource pack na HTTPS úložiště a nastav ho hráčům jako serverový resource pack, nebo ho nech hráče nainstalovat ručně.

## Příkazy

| Příkaz | Funkce |
|---|---|
| `/christmas help` | Nápověda |
| `/christmas event start gifts` | Spustí hledání dárků |
| `/christmas event start ornaments` | Spustí hledání ozdob |
| `/christmas event stop` | Ukončí event |
| `/christmas chatgame start` | Spustí ChatGame |
| `/christmas chatgame stop` | Zastaví ChatGame |
| `/christmas key give <hráč> [počet]` | Vydá vánoční klíč |
| `/christmas trail give <hráč> <effect-id>` | Trvale odemkne TrailStudio efekt |
| `/christmas grinch open [hráč]` | Otevře Grinchovo menu s odkazy na úkoly a tagy |
| `/christmas mountains` | Zobrazí bezpečný postup vložení horské schematiky přes FAWE |
| `/christmas chest set` | Nastaví zamčenou bednu na bloku, na který se díváš |
| `/christmas winter apply` | Znovu aplikuje zimu na načtené chunky |
| `/christmas reload` | Znovu načte konfiguraci |

Správcovské příkazy vyžadují `christmas.admin` (defaultně OP).

## Kosmetické pluginy

TrailStudio je napojené přímo: vítězové eventů, ChatGames a speciální odměna z bedny mohou trvale odemykat efekty. ChristmasFeatures používá vlastní Bukkit PermissionAttachment a ukládá odemknutí do trail-unlocks.yml, takže není nutný LuckPerms. Další tag/trail pluginy lze připojit přes konzolové command hooky s placeholdery `%player%`, `%uuid%`, `%display_name%`, `%event%`, `%score%`, `%rank%` a `%reward%`.

## Důležité poznámky

- `hud.mode: BETTERHUD` používá samostatný pravý panel BetterHud. Pokud BetterHud není dostupný, plugin použije `hud.fallback-mode: BOSSBAR`. V již existujícím `config.yml` ověř, že `hud.mode` je nastaveno na `BETTERHUD`.
- Biomy se mění postupně při načítání chunků, aby nedocházelo k hromadnému načítání celého světa. Dosud nenačtené oblasti se upraví až při návštěvě.
- Změna biomu se ukládá do světa a po odebrání pluginu zůstane zachována.
- Voda mrzne podle standardních pravidel Minecraftu pro chladné biomy; plugin nepřeměňuje násilně každou vodu na led.


## Doporučená integrace s dodanými pluginy

- **ChristmasSeason 2.5.0**: doporučeno pro zimní biomy, sněhové bouře, dekorace, adventní kalendář a vánoční moby. Je-li plugin načten, ChristmasFeatures automaticky vypne svůj jednodušší WinterManager, aby se dva systémy nepřepisovaly. Zimní systém spusť a nastav přes `/xmas on`, `/xmas biome convert-all <radius> <world> confirm` a `/xmas reload`. ChristmasSeason ukládá původní biomy do SQLite snapshotů; před hromadnou konverzí přesto zazálohuj svět.
- **ChristmasGift 1.4.0**: volitelný systém dárkových itemů s ruletou a váženými Common–Mythic odměnami. Nenahrazuje fyzickou bednu vyžadující klíč v ChristmasFeatures. Pokud používáš zároveň ChristmasSeason, nepoužívej alias `/xmasgift` z ChristmasGift; příkazy `/christmasgift` a `/cgift` zůstávají dostupné.
- V konfiguraci ChristmasSeason vypni `gifts.enabled`, pokud nechceš další volně otevíratelné dárkové truhly vedle naší klíčové bedny.
- Vlastní příkazy ChristmasFeatures nepoužívají alias `/xmas`, aby nekolidovaly s ChristmasSeason.

## Přehled dodaných assetů

Viz [docs/ASSET_COMPATIBILITY.md](docs/ASSET_COMPATIBILITY.md). Cizí resource packy nejsou kopírovány do veřejného repozitáře; jejich licence a požadované klientské modifikace je nutné respektovat.

ChristmasFeatures 1.0.6 navíc zachovává horské biomy (`SNOWY_SLOPES`, `FROZEN_PEAKS`, `JAGGED_PEAKS`, `GROVE`) při vlastní zimní konverzi, takže podmínky spawnování horských mobů zůstávají platné.

## Christmas NPC models

See [docs/CHRISTMAS_MOBS_INTEGRATION.md](docs/CHRISTMAS_MOBS_INTEGRATION.md) for the Grinch spawn NPC, Grinch tag/quest menu hooks, and the Yeti/Frost Emperor mountain spawns.

## BetterHud integration

BetterHud integration registers live event, ChatGame and player key-count placeholders. It installs versioned assets, safely migrates only the ChristmasFeatures HUD entry, and keeps other HUD entries and admin customizations. See [docs/BETTERHUD_INTEGRATION.md](docs/BETTERHUD_INTEGRATION.md).

## Grinch, Yeti and Frost Emperor

The Blockbench models and MythicMobs definitions are supplied as a separate import package. Install them into ModelEngine and MythicMobs using the instructions in `docs/CHRISTMAS_MOBS_INTEGRATION.md`. The Grinch menu is wired to the installed SupremeTags (`/tags`) and BattlePass (`/bp open quests`) player commands in config.yml. The `greet` animation is played through ModelEngine's API on right-click. `/christmas mountains` prints the FAWE load/paste instructions for the `world` world.

## TrailStudio integration

TrailStudio is an optional separate plugin. ChristmasFeatures can permanently unlock its effects from event, ChatGame and key-chest rewards using Bukkit permission attachments; LuckPerms is not required. See docs/TRAILSTUDIO_INTEGRATION.md for the reviewed effect IDs and setup.

## ChristmasSeason setup

ChristmasSeason owns winter biome conversion and restoration when installed. ChristmasFeatures automatically pauses its fallback WinterManager to avoid competing biome edits. See docs/CHRISTMASSEASON_SETUP.md before enabling global snow and instant water freezing.