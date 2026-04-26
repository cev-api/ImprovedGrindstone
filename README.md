# Improved Grindstone

Lightweight Paper 1.21.11 plugin that reroutes grindstone disenchantments into books, with optional cross-material transfer support.

## Features
- Slot 0: enchanted source item.
- Slot 1 can be either:
  - A normal book (optionally can be a special book) to extract enchantments into an enchanted book.
  - A compatible target item (same archetype, different material allowed) to transfer enchantments directly.
- Extraction keeps the source item (disenchanted) and gives an enchanted book with removed enchants.
- Transfer keeps the source item (disenchanted) and returns the target item with transferred enchants.
- Optional cursed-enchantment capture toggle.
- Separate XP-cost toggles for extraction and transfer.
- Separate configurable XP percentages (default 50%) based on estimated enchanting-table cost of the moved enchants.

- Commands:
  - `/improvedgrindstone toggle`
  - `/improvedgrindstone status`
  - `/improvedgrindstone cursed`
  - `/improvedgrindstone xp`
  - `/improvedgrindstone transfer`
  - `/improvedgrindstone transferxp`
  - `/improvedgrindstone specialbook`
  - `/improvedgrindstone reload`

## Building
```bash
./gradlew clean build
```

## Configuration
Configure behavior in `config.yml`:
```yaml
feature-enabled: true
capture-cursed: true
book-xp-cost-enabled: true
book-xp-cost-percent: 50.0
transfer-enabled: true
transfer-xp-cost-enabled: true
transfer-xp-cost-percent: 50.0
require-special-book: false
special-book-ids: []
max-enchants-per-operation: 16
max-operation-item-bytes: 24576
```

`special-book-ids` accepts namespaced IDs (for example `dingus:blah`).  
If `special-book-ids` is empty, any non-vanilla namespaced marker is accepted (item model, plugin PDC key, or namespaced display name).

`max-enchants-per-operation` is a safety cap. If a source item has more enchantments than this value, the grindstone operation is blocked to avoid oversized/unstable enchant payload handling.
`max-operation-item-bytes` is an additional safety cap on serialized item size (source/result/bonus). If any item involved exceeds this size, the operation is blocked to prevent unsafe payload handling.

XP-cost notes:
- Vanilla enchantments use their estimated enchanting-table costs.
- Custom enchantments are assigned a rough vanilla-equivalent cost from their level, using the highest matching vanilla cost as the baseline.

## Compatibility
Designed for Paper 1.21.11.
