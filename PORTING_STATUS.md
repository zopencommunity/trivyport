# Trivy z/OS Port Status

## Current Status: ⛔ BLOCKED

**Blocker:** `modernc.org/sqlite` dependency on `modernc.org/libc` which lacks z/OS support

---

## Problem Summary

Trivy v0.60.0 uses `modernc.org/sqlite` (a pure-Go SQLite implementation) for RPM and Java database operations. This library depends on `modernc.org/libc`, which has build constraints that **exclude all z/OS files** in 12 critical packages:

- `errno/` - Error number definitions
- `grp/` - Group database operations  
- `limits/` - System limits
- `poll/` - I/O multiplexing
- `pthread/` - POSIX threads
- `pwd/` - Password database
- `signal/` - Signal handling
- `stdio/` - Standard I/O
- `stdlib/` - Standard library functions
- `sys/types/` - System data types
- `time/` - Time operations
- `unistd/` - POSIX operating system API

---

## Build Error

```
package github.com/aquasecurity/trivy/cmd/trivy
	imports modernc.org/sqlite
	imports modernc.org/libc
	imports modernc.org/libc/errno: build constraints exclude all Go files in /path/to/modernc.org/libc@v1.61.13/errno
[... repeated for all 12 packages ...]
```

---

## Attempted Solutions

### ❌ Attempt 1: Replace with go-sqlite3
- Created go.work workspace with `github.com/ncruces/go-sqlite3`
- Added replace directive in go.mod
- **Result:** Failed - APIs are incompatible, trivy uses modernc.org/sqlite-specific features

### ❌ Attempt 2: Wharf patching
- Attempted to use wharf to patch out the dependency
- **Result:** Failed - blank import for driver registration cannot be patched out

### ❌ Attempt 3: CGO-based SQLite
- Considered switching to `github.com/mattn/go-sqlite3` (CGO-based)
- **Result:** Not attempted - requires extensive trivy source modifications (5+ files)

---

## Path Forward: Port modernc.org/libc to z/OS

**Recommendation:** Port `modernc.org/libc` to z/OS as a separate initiative.

**Why this approach:**
1. **Ecosystem Impact:** Benefits all Go applications using modernc.org/sqlite on z/OS
2. **Upstream Contribution:** Can be contributed back to the community
3. **Long-term Solution:** Enables pure-Go SQLite implementations on z/OS

**Estimated Effort:** 4-6 weeks for experienced Go/z/OS developer

**Detailed Plan:** See [MODERNC_LIBC_PORTING_PLAN.md](./MODERNC_LIBC_PORTING_PLAN.md)

---

## Alternative: Patch Trivy for CGO SQLite

If porting modernc.org/libc is not feasible, Trivy can be patched to use CGO-based SQLite:

**Files to modify:**
1. `cmd/trivy/main.go` - Replace `modernc.org/sqlite` with `github.com/mattn/go-sqlite3`
2. `integration/integration_test.go` - Same replacement
3. `pkg/fanal/analyzer/analyzer_test.go` - Same replacement
4. `pkg/fanal/analyzer/language/java/jar/jar_test.go` - Same replacement
5. `pkg/fanal/test/integration/library_test.go` - Same replacement

**Buildenv changes:**
- Set `CGO_ENABLED=1`
- Add `sqlite` to `ZOPEN_STABLE_DEPS`

**Trade-offs:**
- ✅ Unblocks trivy port immediately
- ❌ Requires maintaining patches
- ❌ CGO dependency adds complexity
- ❌ May have performance implications

---

## Dependencies

### Required for Build
- `make`
- `coreutils`
- `wharf`
- `git`
- `gettext`

### Blocked By
- `modernc.org/libc` z/OS support

---

## Timeline

| Date | Event |
|------|-------|
| 2026-05-03 | Initial port attempt - discovered modernc.org/libc blocker |
| 2026-05-04 | Multiple workaround attempts - all failed |
| 2026-05-04 | Created comprehensive porting plan for modernc.org/libc |
| TBD | Begin modernc.org/libc port OR patch trivy for CGO SQLite |

---

## Related Ports

**Also Blocked:**
- Any Go application using `modernc.org/sqlite`
- Potentially other applications using `modernc.org/libc` directly

**Similar Challenges:**
- Grype port (successfully worked around with go-sqlite3 + patches)

---

## Contact

For questions or to contribute to the modernc.org/libc port:
- zopen community Slack
- GitHub: zopencommunity

---

*Last Updated: 2026-05-04*
