# Trivy z/OS Port Patches

This directory contains patches required to port Trivy to z/OS.

## Overview

The main challenge in porting Trivy to z/OS was the dependency on `modernc.org/sqlite`, which transitively depends on `modernc.org/libc` - a pure Go implementation of libc that does not support z/OS. The solution involved replacing the SQLite driver with `github.com/mattn/go-sqlite3`, which uses CGO and can be compiled with z/OS-specific flags.

## Patches

### PR1-replace-modernc-sqlite-with-mattn.patch

**Purpose**: Replace all `modernc.org/sqlite` imports with `github.com/mattn/go-sqlite3` in Trivy source code.

**Files Modified**:
- `cmd/trivy/main.go`
- `integration/integration_test.go`
- `pkg/fanal/analyzer/analyzer_test.go`
- `pkg/fanal/analyzer/language/java/jar/jar_test.go`
- `pkg/fanal/test/integration/library_test.go`

**Changes**: Changed blank import from `_ "modernc.org/sqlite"` to `_ "github.com/mattn/go-sqlite3"` to register the CGO-based SQLite driver instead of the pure Go driver.

### PR2-go-rpmdb-use-mattn-sqlite3.patch

**Purpose**: Modify the `github.com/knqyf263/go-rpmdb` dependency to use `mattn/go-sqlite3` instead of `glebarez/go-sqlite`.

**Files Modified**:
- `go.mod` - Changed dependency from `github.com/glebarez/go-sqlite v1.20.3` to `github.com/mattn/go-sqlite3 v1.14.24`
- `pkg/sqlite3/sqlite3.go` - Added blank import `_ "github.com/mattn/go-sqlite3"` for driver registration

**Rationale**: The `go-rpmdb` library originally used `glebarez/go-sqlite`, which is a wrapper around `modernc.org/sqlite`. By switching to `mattn/go-sqlite3`, we eliminate the transitive dependency on `modernc.org/libc`.

## Build Configuration

The `buildenv` file includes a `zopen_wharf()` function that:

1. **Clones and compiles go-sqlite3**:
   - Clones `github.com/mattn/go-sqlite3` locally
   - Compiles `sqlite3-binding.c` as a shared library with z/OS-specific flags
   - Uses the ZOPEN-REPLACE-DIR placeholder pattern (similar to murexport)

2. **Sets up Go workspace**:
   - Creates a Go workspace with local copies of `go-sqlite3`, `go-rpmdb`, and `trivy`
   - Applies go.mod modifications (replace directive and exclude directive)
   - Runs wharf to apply z/OS platform patches

3. **Key z/OS compilation flags**:
   ```bash
   -DPATH_MAX=1023
   -D_AE_BIMODAL=1
   -D_ALL_SOURCE
   -U_ENHANCED_ASCII_EXT
   -D_ENHANCED_ASCII_EXT=0xFFFFFFFF
   -D_ISOC99_SOURCE
   -D_LARGE_TIME_API
   -D_OPEN_MSGQ_EXT
   -D_OPEN_SYS_FILE_EXT
   -D_OPEN_SYS_SOCK_EXT3
   -D_OPEN_SYS_SOCK_IPV6
   -D_UNIX03_SOURCE
   -D_UNIX03_THREADS
   -D_UNIX03_WITHDRAWN
   -D_XOPEN_SOURCE=600
   -D_XOPEN_SOURCE_EXTENDED
   -fasm
   -fzos-le-char-mode=ascii
   -isystem/usr/include
   -m64
   ```

## Application

These patches are automatically applied during the build process via the `zopen_wharf()` function in `buildenv`. The function:

1. Applies patches to the local `go-rpmdb` clone
2. Applies patches to the `trivy` source tree
3. Sets up the Go workspace with replace directives
4. Runs wharf for additional z/OS platform patches

## Status

✅ **SQLite dependency issue resolved** - No more `modernc.org/libc` errors
⏳ **Platform-specific compilation errors** - Remaining errors are standard z/OS porting issues that wharf should handle

## References

- Inspired by the murexport port's approach to handling SQLite on z/OS
- Uses the same ZOPEN-REPLACE-DIR placeholder pattern for dynamic path replacement
- Follows zopen porting best practices for CGO-based dependencies
