# Vánoční bedna – výchozí loot table

Vánoční bedna spotřebuje jeden označený Vánoční klíč a náhodně vybere jednu odměnu podle váhy v `plugins/ChristmasFeatures/config.yml`.

## Výchozí šance

| Odměna | Množství | Váha | Přibližná šance |
|---|---:|---:|---:|
| Zlaté ingoty | 12 | 45 | 42,45 % |
| Ametystové úlomky | 8 | 30 | 28,30 % |
| Diamanty | 2 | 15 | 14,15 % |
| Smaragdy | 5 | 8 | 7,55 % |
| Zlaté jablko (očarované) | 1 | 3 | 2,83 % |
| Netheritový úlomek | 1 | 2 | 1,89 % |
| TrailStudio hvězdná aura | trvalé odemknutí + záložní loot | 3 | 2,83 % |

Součet vah je 106. Šance jsou relativní váhy převedené na procenta a mohou se změnit, když správce upraví konfiguraci. Váha 3 tedy neznamená tři procenta.

## Záruka fyzické odměny

Každé otevření bedny dá hráči skutečný předmět:
- běžná materiálová odměna se vloží do inventáře;
- kosmetická odměna přidá odemknutí a současně fyzickou záložní odměnu;
- příkazová nebo kosmetická odměna, jejíž externí plugin není dostupný, stále přidá fyzickou záložní odměnu;
- pokud je tabulka prázdná nebo obsahuje jen neplatné odměny, vydá se záložní odměna;
- pokud je inventář plný, přebytečné předměty se položí na zem u hráče.

Výchozí záložní loot jsou 3 zlaté ingoty pojmenované `Vánoční jistota`. Materiál, počet, název a lore lze upravit v `chest.fallback-reward`.

Před spotřebováním klíče se nejprve vybere platná odměna. Nulové a záporné váhy a položky, které nemají platný materiál, žádný příkaz ani kosmetický efekt, se z losování vynechají.
