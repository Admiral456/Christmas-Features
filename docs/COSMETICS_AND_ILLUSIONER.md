# Kosmetika a The Illusioner

Tato část je napsaná pro Paper/Purpur 1.21.10 a nevyžaduje klientské módy.

## Kosmetika

Hráči otevřou menu příkazem `/christmas cosmetics`. Grinchovo menu otevře stejný výběr tagů. Odemčené volby se ukládají do `plugins/ChristmasFeatures/cosmetics.yml`.

### Chat tagy

| Tag | Přístup |
|---|---|
| Zimní poutník | všichni |
| Lovec dárků | všichni |
| Strážce sněhu | všichni |
| Aurora VIP | VIP |
| Ledový vládce | VIP |

Tag se zobrazí v chatu přes PlaceholderAPI placeholder `%christmas_tag%`. Přidej jej do stávajícího formátu chatu v LPC nebo v jiném chat pluginu, který PlaceholderAPI parsuje.

### Trail efekty

| Efekt | Přístup | Vzhled |
|---|---|---|
| Sněhová spirála | všichni | vločky obíhají kolem hráče |
| Polární záře | všichni | tyrkysová a modrá světelná stuha |
| Hvězdný prach | všichni | zlaté hvězdné jiskry |
| Nebeská křídla | VIP | animovaný obrys světelných křídel |
| Koruna prázdnoty | VIP | fialový portálový prstenec nad hlavou |
| Ledový fénix | VIP | ledová křídla, duše a mrazivé výboje |
| Prismatický drak | VIP | vícebarevná spirála s end-rod jiskrami |

Trail efekty jsou serverové částice. Jejich vzhled proto všichni hráči vidí bez instalace OptiFine, EMF nebo jiného klientského módu. VIP efekty používají více částic než běžné efekty; při velmi vysokém počtu současně online hráčů lze snížit frekvenci v další úpravě.

### Čepice a koruny

| Pokrývka | Přístup |
|---|---|
| Santova čepice | všichni |
| Skřítkova čepice | všichni |
| Sněhová čepice | všichni |
| Koruna věčného ledu | VIP |
| Koruna polární záře | VIP |

Aktuální první verze používá barvené vanilla helmy a pojmenované vanilla koruny. Není to ještě vlastní 3D Blockbench model. Při nasazení se uloží původní helma a při sundání kosmetiky se obnoví; kosmetická helma se neztratí při smrti.

### VIP oprávnění

Permission node:

`christmas.cosmetics.vip`

Oprávnění má ve výchozím stavu `false`. Jestliže se tvoje LuckPerms skupina jmenuje `vip`, přiděl jí ho:

```text
/lp group vip permission set christmas.cosmetics.vip true
```

Hráč si může vybrat bezplatné i VIP položky v jednom menu. Zamčené VIP položky jsou viditelné, ale bez oprávnění je nelze vybavit. Pokud hráč o VIP oprávnění přijde, VIP trail/čepice se vypne a VIP tag se přestane zobrazovat.

## The Illusioner

Boss je vytvořený z nativní entity Minecraftu `ILLUSIONER`. Používá vlastní serverovou logiku ChristmasFeatures, nikoli NeoForge mod.

### Nastavení arény

1. Postav arénu a postav se na místo, kde má hráč začít úvodní scénu.
2. Použij `/christmas boss illusioner set`.
3. Při vstupu do okruhu 9 bloků se spustí dialog a bossfight. Automatický vstupní trigger má pětiminutovou globální prodlevu.
4. Pro ruční test použij `/christmas boss illusioner start`.

Nastavení je v `plugins/ChristmasFeatures/config.yml`:

```yaml
bosses:
  illusioner:
    trigger-on-entry: true
    trigger-radius: 9.0
    entry-cooldown-seconds: 300
    spawn:
      world: ""
      x: 0.0
      y: 0.0
      z: 0.0
      yaw: 0.0
      pitch: 0.0
```

### Průběh souboje

- Před soubojem proběhne vlastní úvodní sekvence s dialogem, titulky, zvuky a portálovými částicemi.
- Boss má 650 životů a bossbar.
- Při 50 % zdraví přejde do druhé fáze: změní vzhled bossbaru a jméno, zrychlí útoky a získá novou sadu útoků.
- Útoky zahrnují iluzorní kopie, varované meteoritické zásahy, fantomové tornádo, přízračné Vexy, přízračného Ravagera a Prism Grid.
- Útoky jsou navržené tak, aby dávaly hráči krátkou vizuální výstrahu a možnost uskočit. Plugin neničí bloky arény.

### Příkazy

- `/christmas boss illusioner set` — uloží místo arény a vstupního dialogu.
- `/christmas boss illusioner start` — spustí dialog a souboj ručně.
- `/christmas boss illusioner stop` — ukončí aktivní souboj.

Boss příkazy vyžadují `christmas.admin`.

### Co se z nahraného modu nepřebírá

Soubor `the-illusioner-1.2.0-1.21.11-neoforge.jar` je NeoForge mod pro jinou platformu a novější řadu Minecraftu. Projekt nepřebírá jeho bytecode, model, textury, hudbu, hlasové nahrávky ani shadery. Illusioner boss v ChristmasFeatures je samostatná implementace s vanilla modelem a vlastními texty/útoky.

## Kompatibilita

- Paper/Purpur 1.21.10, Java 21.
- PlaceholderAPI je volitelný soft dependency; bez něj funguje menu, čepice, trail částice i boss. Pouze chat placeholder `%christmas_tag%` vyžaduje PlaceholderAPI.
- MythicMobs a ModelEngine zůstávají volitelné pro Grinch a ostatní modelové integrace; samotný Illusioner je nativní vanilla mob.
