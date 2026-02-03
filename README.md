# Improved Grindstone

Lightweight Paper 1.21.11 plugin that reroutes disenchantments from a grindstone directly into an enchanted book whenever an empty book sits in the second slot.

## Features
- Slot 0: item to disenchant; slot 1: normal book.
- Result slot shows the disenchanted item.
- Taking the result consumes the book and gives an enchanted book with the removed enchants.
- Optional XP toggle (default: no XP when creating the book).
- Optional toggle to create cursed books or not.
- Ops commands: `/improvedgrindstone toggle`, `/improvedgrindstone status`, `/improvedgrindstone cursed`, `/improvedgrindstone xp`.

## Building
```bash
./gradlew clean build
```

## Configuration
Configure behavior in `config.yml`:
```yaml
feature-enabled: true
capture-cursed: false
grant-xp-when-book: false
```

## Compatibility
Designed for Paper 1.21.11.
