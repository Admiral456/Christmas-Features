# ChristmasFeatures — Paper/Purpur 1.21.10

Samostatný vánoční plugin pro Paper/Purpur 1.21.10. Projekt cílí na Javu 21.

## Funkce

- **Hledání dárků a ozdob:** soutěžní eventy s rozmístěnými sběratelskými předměty. První hráč, který splní cíl, vyhrává; při vypršení limitu rozhoduje nejvyšší skóre.
- **Event HUD:** pravý postranní panel se jménem eventu, odpočtem a skóre. Alternativou je BossBar, pokud sidebar používá jiný plugin.
- **Vánoční bedna:** nastaví se na existující truhlu nebo sud na spawnu. Otevření vyžaduje speciální vánoční klíč s PDC identifikací.
- **Vánoční klíč a odměny:** klíče získávají vítězové eventů a ChatGames. Bedna losuje odměny podle vah v konfiguraci; umí také spouštět příkazy externích kosmetických pluginů.
- **ChatGames:** globálně oznámené úkoly. První správná odpověď vyhrává, plugin oznámí vítěze a předá odměnu.
- **Zimní svět:** při načítání chunků přepisuje 3D biomy na `SNOWY_PLAINS`, udržuje sněžení a brání tání sněhu/ledu. Terén ani stavby se negenerují znovu.
- **Resource pack:** vlastní item modely/textury vánočního klíče, dárku a ozdoby.

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
| `/christmas chest set` | Nastaví zamčenou bednu na bloku, na který se díváš |
| `/christmas winter apply` | Znovu aplikuje zimu na načtené chunky |
| `/christmas reload` | Znovu načte konfiguraci |

Správcovské příkazy vyžadují `christmas.admin` (defaultně OP).

## Kosmetické pluginy

Konfigurace obsahuje příkazové hooky pro odměny z eventů, ChatGames a bedny. Do nich doplň přesné příkazy vašeho pluginu na tagy/traily/čepice. ChristmasFeatures spouští zadané příkazy jako konzole; používá placeholdery `%player%`, `%uuid%`, `%display_name%` a podle kontextu také `%event%`, `%score%`, `%rank%`, `%reward%`. Kosmetické odměny jsou ve výchozím stavu vypnuté, dokud se nedoplní integrační příkaz.

## Důležité poznámky

- `hud.mode: SIDEBAR` používá jediný pravý scoreboard slot Minecraftu. Pokud jej používá jiný plugin, nastav `hud.mode: BOSSBAR`.
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