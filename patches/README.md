# Trivy z/OS Port Patches

Trivy v0.60.0 imports `modernc.org/sqlite`, which pulls in `modernc.org/libc`.
`modernc.org/libc` does not build on z/OS, so this port uses
`github.com/mattn/go-sqlite3` through CGO instead.

The build keeps upstream `go.mod` files intact where possible. `buildenv`
clones the modules that need z/OS-specific treatment as siblings of the Trivy
source tree, applies these `.gopatch` files, and creates a Go workspace with
`go work`. The `.gopatch` extension is intentional: `zopen-build` should not
auto-apply dependency patches to the Trivy source tree.

## Patch List

### trivy-go-sqlite3-zos.gopatch

Applies to `github.com/mattn/go-sqlite3`.

Adds a z/OS-only cgo LDFLAGS entry for the SQLite side deck generated during
the build. `buildenv` replaces `ZOPEN-REPLACE-DIR` with the local clone path
after compiling `sqlite3-binding.c`.

### trivy-go-rpmdb-sqlite3.gopatch

Applies to `github.com/knqyf263/go-rpmdb`.

Replaces the `github.com/glebarez/go-sqlite` dependency with
`github.com/mattn/go-sqlite3` and registers the CGO SQLite driver. This removes
the transitive `modernc.org/sqlite` path from RPM DB handling.

### trivy-sqlite3-driver.gopatch

Applies to `github.com/aquasecurity/trivy`.

Replaces Trivy's blank `modernc.org/sqlite` driver imports with
`github.com/mattn/go-sqlite3`. The build sets
`github.com/mattn/go-sqlite3.driverName=sqlite` with `-ldflags`, preserving the
driver name expected by Trivy and go-rpmdb.

### Other dependency gopatches

The remaining `.gopatch` files cover z/OS build-tag gaps seen during the Trivy
build:

- `trivy-pb-v3-zos.gopatch`
- `trivy-pb-v1-zos.gopatch`
- `trivy-goleveldb-zos.gopatch`
- `trivy-continuity-zos.gopatch`
- `trivy-containerd-v1-zos.gopatch`
- `trivy-go-git-zos.gopatch`
- `trivy-grpc-zos.gopatch`
- `trivy-squealer-zos.gopatch`
