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
```

`special-book-ids` accepts namespaced IDs (for example `dingus:blah`).  
If `special-book-ids` is empty, any non-vanilla namespaced marker is accepted (item model, plugin PDC key, or namespaced display name).

## Compatibility
Designed for Paper 1.21.11.
