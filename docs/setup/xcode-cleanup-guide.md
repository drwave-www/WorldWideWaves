# Xcode Cleanup Script

## Purpose

The `clean_xcode.sh` script prevents and fixes the recurring Xcode GUID conflict error:

```
Could not compute dependency graph: unable to load transferred PIF:
The workspace contains multiple references with the same GUID 'PACKAGE:...'
```

## Usage

### Quick Fix (When Error Occurs)

```bash
# 1. Close Xcode (Cmd+Q)
# 2. Run cleanup
./scripts/clean_xcode.sh

# 3. Open Xcode and rebuild
open iosApp/worldwidewaves.xcodeproj
# File → Packages → Resolve Package Versions
# Cmd+B to build
```

### Prevention (Run Regularly)

```bash
# Before opening Xcode after git operations
git pull
./scripts/clean_xcode.sh
open iosApp/worldwidewaves.xcodeproj

# Weekly during active development
./scripts/clean_xcode.sh
```

## What It Cleans

1. **Xcode DerivedData** - Removes worldwidewaves build artifacts
2. **Swift Package Manager cache** - Clears SPM state
3. **Xcode user state** - Removes xcuserdata directories
4. **Temporary files** - Removes .DS_Store, .swp files

## When to Run

✅ **Always Run Before**:
- Opening Xcode after `git pull`, `git merge`, `git rebase`
- Opening Xcode after Xcode crash
- Important builds or releases

⚠️ **Run When Seeing**:
- GUID conflict errors
- "Could not compute dependency graph" errors
- Swift Package resolution failures
- Unexplained build failures

🔄 **Run Regularly**:
- Weekly during active development
- After updating dependencies
- After Swift Package Manager updates

## Best Practices

1. **Close Xcode before git operations** - Prevents cache corruption
2. **Don't interrupt SPM resolution** - Let package resolution complete
3. **Run cleanup after crashes** - Xcode crashes corrupt state
4. **Keep .gitignore updated** - Prevents committing cache files

## Related Files

- `.gitignore` - Excludes problematic Xcode files
- `CLAUDE_iOS.md` - Comprehensive troubleshooting guide
- `scripts/verify-ios-safety.sh` - iOS deadlock prevention verification

## Troubleshooting

If cleanup script doesn't resolve GUID error:

```bash
# Nuclear option 1: Clean ALL DerivedData
rm -rf ~/Library/Developer/Xcode/DerivedData/*

# Nuclear option 2: Clean ALL Xcode caches
rm -rf ~/Library/Caches/com.apple.dt.Xcode
rm -rf ~/Library/Caches/org.swift.swiftpm

# Nuclear option 3: Restart Mac
```
