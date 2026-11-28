# Vánoční kosmetika

Tato část je napsaná pro Paper/Purpur 1.21.10 a nevyžaduje klientské módy.

## Otevření menu

Hráči otevřou menu příkazem `/christmas cosmetics`. Grinchovo menu otevře stejný výběr tagů. Odemčené volby se ukládají do `plugins/ChristmasFeatures/cosmetics.yml`.

## Chat tagy

| Tag | Přístup |
|---|---|
| Zimní poutník | všichni |
| Lovec dárků | všichni |
| Strážce sněhu | všichni |
| Aurora VIP | VIP |
| Ledový vládce | VIP |

Tag se zobrazí v chatu přes PlaceholderAPI placeholder `%christmas_tag%`. Přidej jej do stávajícího formátu chatu v LPC nebo v jiném chat pluginu, který PlaceholderAPI parsuje.

## Trail efekty

| Efekt | Přístup | Vzhled |
|---|---|---|
| Sněhová spirála | všichni | vločky obíhají kolem hráče |
| Polární záře | všichni | tyrkysová a modrá světelná stuha |
| Hvězdný prach | všichni | zlaté hvězdné jiskry |
| Kometa zimy | všichni | ledová kometa a světelná stopa za hráčem |
| Nebeská křídla | VIP | animovaný obrys světelných křídel |
| Koruna prázdnoty | VIP | fialový portálový prstenec nad hlavou |
| Ledový fénix | VIP | ledová křídla, duše a mrazivé výboje |
| Prismatický drak | VIP | vícebarevná spirála s end-rod jiskrami |
| Nebeská bouře | VIP | zlatá a modrá koruna s fialovými oblouky |
| Drak zatmění | VIP | temně fialová aura, ledová křídla a portálové částice |

Trail efekty jsou serverové částice. Menu zobrazí až 14 položek na stránce a automaticky nabídne další stránku, pokud bude efektů více. Jejich vzhled proto všichni hráči vidí bez instalace OptiFine, EMF nebo jiného klientského módu. VIP efekty používají více částic než běžné efekty; při velmi vysokém počtu současně online hráčů lze upravit frekvenci vykreslování v další verzi.

## Čepice a koruny

| Pokrývka | Přístup |
|---|---|
| Santova čepice | všichni |
| Skřítkova čepice | všichni |
| Sněhová čepice | všichni |
| Koruna věčného ledu | VIP |
| Koruna polární záře | VIP |

Aktuální verze používá barvené vanilla helmy a pojmenované vanilla koruny. Nejde zatím o vlastní 3D Blockbench model. Při nasazení se uloží původní helma a při sundání kosmetiky se obnoví; kosmetická helma se neztratí při smrti.

## VIP oprávnění

Permission node:

`christmas.cosmetics.vip`

Oprávnění má ve výchozím stavu `false`. Jestliže se tvoje LuckPerms skupina jmenuje `vip`, přiděl jí ho:

```text
/lp group vip permission set christmas.cosmetics.vip true
```

Hráč si může vybrat bezplatné i VIP položky v jednom menu. Zamčené VIP položky jsou viditelné, ale bez oprávnění je nelze vybavit. Pokud hráč o VIP oprávnění přijde, VIP trail/čepice se vypne a VIP tag se přestane zobrazovat.

## Kompatibilita

- Paper/Purpur 1.21.10, Java 21.
- PlaceholderAPI je volitelný soft dependency; bez něj funguje menu, čepice a trail částice. Pouze chat placeholder `%christmas_tag%` vyžaduje PlaceholderAPI.
- MythicMobs a ModelEngine zůstávají volitelné pro Grinch a ostatní modelové integrace.
