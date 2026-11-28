# ChristmasFeatures — Paper/Purpur 1.21.10

Samostatný vánoční plugin pro Paper/Purpur 1.21.10. Projekt cílí na Javu 21.

## Funkce

- **Hledání dárků:** 10 ukrytých dárků na 10 minut v kruhové oblasti. Dárky mají bezpečný prostor nad sebou a drží nastavený odstup, takže neleží v jedné hromadě. Předměty jemně jiskří a při sebrání se spustí částicový efekt i osobní ukazatel postupu. Výhra dává 10 diamantů a trvale odemyká TrailStudio efekt trail-aurora-braid (Polární stuha).
- **Hledání ozdob:** samostatný soutěžní event s vlastní konfigurací a odměnou.
- **Grinch NPC:** /christmas npc spawn umístí Grinche na aktuální pozici hráče. S importovanými modely ModelEngine + MythicMobs použije 3D model; bez nich vytvoří persistentní vestavěné NPC, které pravým kliknutím otevře vánoční GUI.
- **Grinchovo GUI:** obsahuje hledání dárků, ChatGames, kosmetické tagy a trail efekty. Hledání dárků i ChatGame lze spustit přímo z menu.
- **ChatGames:** rozšířená sada více než 20 početních úloh, hádanek a Minecraft/vánočních otázek. Odpovědi se kontrolují bez ohledu na velikost písmen, českou diakritiku a běžnou interpunkci. Běžný hráč získá 3 smaragdy a 12 XP; VIP má navíc 2 smaragdy, 2 zlaté ingoty a zvláštní TrailStudio auru.
- **BetterHud panel:** pravý HUD zobrazuje vánoční sezónu, probíhající event, skóre, čas a úkol ChatGames. Při nedostupnosti BetterHud se použije BossBar.
- **Vánoční kosmetika:** 7 běžných a 9 VIP propracovaných částicových trailů, chat tagy, čepice a koruny. Pravým kliknutím v menu si lze zobrazit dočasný náhled bez vybavení. Nové traily zahrnují Polární lišku, Srdce z křišťálu, Sněžítko, Aurorového serafa, Astrálního strážce a Ledového leviatana.
- **TrailStudio kosmetika:** vítězové eventů a ChatGames mohou trvale odemykat částicové efekty.
- **Zimní svět:** při načítání chunků přepisuje 3D biomy na SNOWY_PLAINS, udržuje sněžení, brání tání sněhu/ledu a přidává decentní venkovní ledové třpytky kolem hráčů. Efekt je omezený počtem částic a nevykresluje se pod střechou, v jeskyních ani ve spectator režimu. Pokud biomy a počasí spravuje ChristmasSeason, třpytky z ChristmasFeatures zůstávají aktivní. Terén ani stavby se znovu negenerují.
- **Resource pack:** vlastní item modely/textury vánočního dárku a ozdoby.
- **Stav systému:** /christmas status ověří běh eventů, počet úkolů, zimní správu a dostupnost integrací.

## Požadavky

- Paper nebo kompatibilní Purpur 1.21.10
- Java 21
- Maven 3.9+ pro lokální sestavení
- Python 3.9+ pouze pro kontroly a bezpečné sloučení resource packu

## Sestavení

~~~bash
mvn clean verify
~~~

Výsledný plugin bude target/ChristmasFeatures-1.21.10.jar. GitHub Actions po pushi nebo pull requestu provede sestavení a zveřejní JAR, samostatný ZIP resource packu a jeho SHA-256 kontrolní součet jako workflow artifact.

## Instalace

1. Vlož target/ChristmasFeatures-1.21.10.jar do složky plugins/.
2. Spusť server. Starší konfigurace 1.1.0 se automaticky doplní o nové ChatGames úkoly a nastavení hledání dárků.
3. Ověř název světa v winter.worlds a events.gifts.world.
4. Resource pack nepoužívej jako náhradu již existujícího serverového packu. Před nasazením jej bezpečně sluč s kopií finálního serverového packu podle [návodu na kompatibilitu assetů](docs/ASSET_COMPATIBILITY.md).

## Příkazy

| Příkaz | Funkce |
|---|---|
| /christmas help | Nápověda |
| /christmas event start gifts | Spustí hledání dárků |
| /christmas event start ornaments | Spustí hledání ozdob |
| /christmas event stop | Ukončí event |
| /christmas chatgame start | Spustí ChatGame |
| /christmas chatgame stop | Zastaví ChatGame |
| /christmas cosmetics | Otevře menu kosmetiky |
| /christmas status | Zobrazí stav hlavních systémů a počet úkolů |
| /christmas trail give <hráč> <effect-id> | Trvale odemkne TrailStudio efekt |
| /christmas grinch open [hráč] | Otevře Grinchovo menu |
| /christmas npc spawn | Umístí Grinche na aktuální pozici hráče |
| /christmas winter apply | Znovu aplikuje zimu na načtené chunky |
| /christmas reload | Znovu načte konfiguraci |

Správcovské příkazy vyžadují christmas.admin (defaultně OP). Grinchovo menu je dostupné hráčům přímo po kliknutí na NPC.

## Odměny

- Hledání dárků: 10 diamantů a odemčení trail-aurora-braid (Polární stuha).
- Hledání ozdob: 5 smaragdů a odemčení burst-frost-flower.
- ChatGames běžný hráč: 3 smaragdy a 12 XP; VIP navíc 2 smaragdy, 2 zlaté ingoty a aura-astral-star. Všichni vítězové ChatGames odemykají wings-ice-dragon.

## Důležité poznámky

- Při staré konfiguraci se výchozí původní hodnoty hledání dárků (18 kusů, 5 minut, poloměr 32 a cíl 5) převedou na nové nastavení (10 kusů, 10 minut, poloměr 45 a cíl 10). Ostatní vlastní nastavení se zachovají.
- Hledání dárků se centruje na světový spawn, pokud není nastaveno events.gifts.center.x/y/z.
- Hledání dárků se nespustí, pokud plugin bezpečně nerozmístí všech 10 samostatných předmětů. Minimální odstup mezi dárky a ozdobami lze upravit pomocí `events.gifts.min-distance-between-items` a `events.ornaments.min-distance-between-items`. Zimní třpytky lze vypnout přes `winter.ambience.enabled: false`.
- Pokud je nainstalován ChristmasSeason, přebírá správu zimních biomů a počasí, aby se systémy nepřepisovaly.
- TrailStudio je volitelný plugin. Odemčení se ukládají do plugins/ChristmasFeatures/trail-unlocks.yml, takže se neztratí při restartu. Bez TrailStudio se uloží a aktivují po jeho instalaci.
- Frost Emperor – zimní boss – byl odstraněn z podporovaného nastavení. Již nainstalovanou externí MythicMobs definici a její random-spawn soubor je nutné odstranit ručně; viz docs/CHRISTMAS_MOBS_INTEGRATION.md.

## Modelovaný Grinch

Modelovaný 3D Grinch vyžaduje samostatný import Blockbench modelu do ModelEngine a MythicMobs. Postup a ruční odstranění starého Frost Emperor random spawnu jsou v docs/CHRISTMAS_MOBS_INTEGRATION.md.
