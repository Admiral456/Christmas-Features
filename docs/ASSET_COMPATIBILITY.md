# Audit dodaných vánočních assetů a pluginů

Cíl serveru: **Paper/Purpur 1.21.10**. Soubory byly lokálně zkontrolovány jako ZIP/JAR archivy; všechny archivy prošly kontrolou integrity ZIP. Kompatibilita s běžícím serverem a klientem musí být ověřena v testovací instanci.

## Doporučené využití

| Soubor | Co obsahuje | Stav pro 1.21.10 | Doporučení |
|---|---|---|---|
| `WinterPack_1.21.4_v1.9.zip` | Zimní vanilla textury, modely a OptiFine properties; pack format 46 | Vydání je pro 1.21.4, ne 1.21.10 | Nepoužívat jako hlavní pack; nahrazuje jej novější vánoční pack od stejného autora |
| `CHRISHMAS PACK.zip` | 390 PNG, 30 TGA, 40 OGG, modely truhel a přepsané zvuky | Stránka vydání uvádí podporu 1.21.10; metadata v archivu jsou starší pack_format 34 s rozsahem supported_formats | Obsahově zajímavý, ale 40 zvukových náhrad mění i běžnou hudbu/počasí. Použít jen po testu a souhlasu s licencí |
| `SimpleChristmasChests v2.0.zip` | 41 PNG pro klasické, trapped, ender, copper truhly, chest boats a minecarty | Uvnitř je pack format 69, který odpovídá 1.21.9–1.21.10; nahraný název v2.0 je ale na stránce autora vedený jako novější řada pro 26.2 | Nejlepší kandidát pro samotné vánoční truhly; před nasazením otestovat metadata a zobrazení všech typů truhel |
| `Consistent Christmas Chests.zip` | Textury a modely všech variant truhel | Starší pack metadata; překrývá se s CHRISHMAS PACK i SimpleChristmasChests | Nepoužívat společně s jiným chest packem |
| `ChristmasPack_26.3_v3.3.zip` | Velký vánoční vanilla overhaul, 642 PNG, 138 JSON, OptiFine properties a několik CEM modelů | Je to sestavení pro 26.3, nikoliv pro 1.21.10 | Nedowngradovat ručně. Autor vydal samostatný `ChristmasPack_1.21.10_v3.2.zip` |
| `Christmas mobs.zip` | Vánoční vzhled téměř všech mobů; 84 souborů modelů `.jem` a textury | Existuje vydání pro 1.21.10, ale vyžaduje klientské EMF + ETF | Nehodí se pro hráče s čistým vanilla klientem. Nejde o serverový plugin |
| `ChristmasGift-1.4.0.jar` | Dárkové itemy, ruleta, Common–Mythic odměny, combo bonusy, příkazové rewardy | Plugin metadata uvádí API 1.21; vydavatel uvádí Paper 1.21–1.21.11 a Purpur jako pravděpodobně kompatibilní | Volitelný samostatný doplněk pro dárkové itemy, ruletu a odměny |
| `ChristmasSeason-2.5.0.jar` | Zimní biomy se SQLite snapshoty, sněhové bouře, dekorace, dárky, Wichtel, elfové, sněhuláci a adventní kalendář | Metadata uvádí minimum API 1.21.3; dokumentace autora výslovně uvádí test na Paper 1.21.10 a kompatibilitu s Purpur | Doporučený doplněk pro svět a atmosféru. ChristmasFeatures při jeho načtení vypne vlastní WinterManager |

## Důležité překryvy resource packů

- `WinterPack_1.21.4_v1.9.zip` a `ChristmasPack_26.3_v3.3.zip` mají 267 společných cest; 237 souborů je shodných a 30 se liší.
- `CHRISHMAS PACK.zip` a `Consistent Christmas Chests.zip` sdílejí 95 cest; 93 souborů je shodných.
- `SimpleChristmasChests v2.0.zip` a `Consistent Christmas Chests.zip` sdílejí 30 cest, ale textury se liší.

Proto se nemají všechny chest packy skládat bez pravidel priority. Pokud bude hlavní pack obsahovat vlastní textury truhel, samostatný chest pack je musí cíleně přepsat, nebo se vybere jen jeden systém.

## Licence a distribuce

- CurseForge uvádí **All Rights Reserved** u WinterPack, ChristmasPack, CHRISTMAS PACK, Christmas mobs a Consistent Christmas Chests. Tyto ZIPy proto nebudou kopírovány do tohoto veřejného GitHub repository ani znovu distribuovány v našem serverovém packu bez svolení držitelů práv.
- Simple Christmas Chests má na stránce autora uvedenou licenci MIT. Před případným začleněním zachováme její licenční oznámení.
- ChristmasSeason je vydán pod MIT.
- ChristmasGift je vydán pod GPL-3.0-only. Plugin ponecháme jako samostatný JAR a nebudeme jej vydávat za součást ChristmasFeatures.

## Doporučená sestava

1. **ChristmasSeason 2.5.0** pro zimní svět a atmosféru. Nastavit snapshoty, zálohování a okamžité zamrzání vody/sněžení; jeho vlastní náhodné dárky zapnout pouze tehdy, pokud je na serveru chceš používat.
2. **ChristmasFeatures** pro soutěžní eventy, ChatGames a odměny s napojením na vánoční kosmetiku.
3. **SimpleChristmasChests** jako volitelná textura běžných truhel, až po testu na 1.21.10.
4. **BetterHud** je vhodný další krok pro propracovaný animovaný event HUD. Podporuje Paper/Purpur, verzi pluginu 1.20.4–1.21.11, animace a automatické generování resource packu.
5. **Christmas mobs** zatím nenasazovat hráčům bez EMF+ETF. Pro čistý vanilla klient by bylo potřeba moby předělat na serverové modely/entitní prezentace, například přes kompatibilní modelový plugin.

## Konflikty příkazů

- ChristmasSeason vlastní příkaz `/xmas`. ChristmasFeatures proto odstranil alias `/xmas` a používá `/vanocni` a `/xfeatures`.
- ChristmasSeason má příkaz `/xmasgift`; ChristmasGift jej také registruje jako alias. Při společném použití se má volat `/christmasgift` nebo `/cgift`; alias `/xmasgift` nepoužívat.

## Verze resource packu

Java Edition 1.21.9–1.21.10 používá resource-pack format **69**. Pack format popisuje strukturu resource packu, nikoliv přímo verzi pluginu nebo serveru. Nízký pack format se proto nedá bezpečně „opravit“ jen přepsáním čísla, pokud pack obsahuje staré nebo přesunuté cesty a modelová schémata.

## Bezpečné slučování ChristmasFeatures s hlavním serverovým packem

ChristmasFeatures obsahuje dvě cílené definice ve vanilla namespace:

- `assets/minecraft/items/paper.json` pro dárek (CustomModelData **12022**)
- `assets/minecraft/items/amethyst_shard.json` pro ozdobu (CustomModelData **12023**)

Tyto soubory nejsou samostatné textury v odděleném namespace: nahrazují definici daného vanilla předmětu. Při prostém rozbalení ZIPu přes existující serverový pack by se mohly ztratit modely, které do stejných položek přidaly jiné packy.

V repozitáři je proto `scripts/resourcepack_guard.py`. Nástroj:

- kontroluje JSON, formát packu 69, cesty a odkazy na vlastní modely/textury;
- ověřuje integritu PNG a CRC vytvořeného ZIPu;
- povoluje pouze dvě výše uvedené vanilla položky, ostatní pluginové assety musí být v namespace `christmasfeatures`;
- při slučování zachová původní `pack.mcmeta` i jeho popis a formát;
- u `paper.json` a `amethyst_shard.json` přidá naše prahy do existujícího `range_dispatch`, případně zachová celý původní model jako fallback;
- při duplicitním CustomModelData nebo neznámé kolizi skončí chybou. Nepoužívá automatické přepisování ani volbu „vítězného“ packu.

### Kontrola samotného pluginového packu

```bash
python scripts/resourcepack_guard.py validate resourcepack
python -m unittest discover -s scripts/tests -v
```

### Slučování do kopie finálního serverového packu

Nejprve si stáhni nebo zkopíruj skutečný výsledný serverový pack, který už obsahuje assety všech aktivních pluginů (například ItemsAdder, Oraxen, ModelEngine a BetterHud). Pracuj s kopií, nikoliv se složkou pluginu nebo jediným produkčním souborem.

```bash
python scripts/resourcepack_guard.py merge --base ./server-pack-copy.zip --addon ./resourcepack --output ./dist/ChristmasFeatures-Merged-1.21.10.zip
```

Výstupní ZIP je nový soubor a vedle něj vznikne `.sha256` kontrolní součet. Vstupní packy se nemění. Pokud nástroj narazí na neznámou kolizi nebo konflikt prahů, nic nepřepíše a vyžádá ruční kontrolu.

**Důležité:** výstup se automaticky nenahrává na server a nemění URL serverového resource packu. Před změnou produkční URL jej otestuj na kopii serveru s reálnými pluginy a klientem. Samotný úspěšný GitHub Actions build potvrzuje kontrolu souborů a sestavení, nikoliv kompatibilitu s konkrétní instalací serveru.

Resource packy generované pluginy nikdy neupravuj přímo. Vždy pracuj s kopií finálního sloučeného packu a zachovej původní vstupy i možnost návratu.
