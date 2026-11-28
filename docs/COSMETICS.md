# Vánoční kosmetika

Tato část je napsaná pro Paper/Purpur 1.21.10 a nevyžaduje klientské módy ani vlastní resource pack.

## Otevření menu

Hráči otevřou menu příkazem \`/christmas cosmetics\`. Grinchovo menu otevře stejný výběr tagů a trail efektů. Odemčené volby se ukládají do \`plugins/ChristmasFeatures/cosmetics.yml\`.

## Trail efekty

Všechny efekty jsou vykreslované serverovými částicemi. Jsou tedy viditelné ostatním hráčům v okolí a nevyžadují OptiFine, EMF ani klientský mod. Animace se aktualizují každé čtyři tick-y; neviditelné hráče a hráče ve spectator režimu plugin nevykresluje.

### Dostupné všem hráčům

| Efekt | Vzhled |
|---|---|
| Sněhová spirála | Čtyři obíhající proudy vloček s jemnými ledovými jiskrami |
| Polární záře | Dvě vícevrstvé světelné stuhy v tyrkysové, zelené a ledově modré |
| Hvězdný prach | Zlatá hvězdná stopa, která při pohybu září za hráčem |
| Kometa zimy | Jasná hlava komety s dlouhým, zvlněným ledovým ocasem |
| Polární liška | Modrobílý spektrální ocas a dvě jiskřící ouška |
| Srdce z křišťálu | Samostatný obrys srdce z ledových krystalických částic před hráčem |
| Sněžítko | Kulový prstenec s drobnými sněhovými vločkami uvnitř |

### VIP efekty

| Efekt | Vzhled |
|---|---|
| Nebeská křídla | Zlatá rozevírací křídla s obrysem a vykreslenými pery |
| Koruna prázdnoty | Dvojitá fialová koruna z portálových částic a run |
| Ledový fénix | Rozsáhlá ledová křídla, světelný hřeben a pravidelné mrazivé výbuchy |
| Prismatický drak | Dvě propletené duhové spirály s krystalovými jiskrami |
| Nebeská bouře | Zlatá a modrá koruna se dvěma protínajícími se orbitálními proudy |
| Drak zatmění | Temná dračí křídla, portálová aura a duše |
| Aurorový seraf | Velká polární křídla se zlatou svatozáří |
| Astrální strážce | Tři rotující ochranné prstence a hvězdné body kolem hráče |
| Ledový leviatan | Ledový had stoupající kolem těla a zakončený zářícími rohy |

Nabídka obsahuje celkem **7 běžných a 9 VIP trailů**. Menu zobrazuje až 14 položek na stránce a automaticky nabídne další stránku.

## Chat tagy

| Tag | Přístup |
|---|---|
| Zimní poutník | všichni |
| Lovec dárků | všichni |
| Strážce sněhu | všichni |
| Aurora VIP | VIP |
| Ledový vládce | VIP |

Tag se zobrazí v chatu přes PlaceholderAPI placeholder \`%christmas_tag%\`. Přidej jej do stávajícího formátu chatu v LPC nebo v jiném chat pluginu, který PlaceholderAPI parsuje.

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

\`christmas.cosmetics.vip\`

Oprávnění má ve výchozím stavu \`false\`. Jestliže se tvoje LuckPerms skupina jmenuje \`vip\`, přiděl jí ho:

~~~text
/lp group vip permission set christmas.cosmetics.vip true
~~~

Hráč si může vybrat běžné i VIP položky v jednom menu. Zamčené VIP položky jsou viditelné, ale bez oprávnění je nelze vybavit. Pokud hráč o VIP oprávnění přijde, VIP trail/čepice se vypne a VIP tag se přestane zobrazovat.

## Kompatibilita

- Paper/Purpur 1.21.10, Java 21.
- PlaceholderAPI je volitelný soft dependency; bez něj funguje menu, čepice a trail částice. Pouze chat placeholder \`%christmas_tag%\` vyžaduje PlaceholderAPI.
- MythicMobs a ModelEngine zůstávají volitelné pro Grinch a ostatní modelové integrace.
