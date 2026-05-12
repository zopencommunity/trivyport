# Trivy z/OS Port Status

## Current Status

The port now uses a Go workspace and per-module patches instead of modifying
Trivy's upstream `go.mod` during the build.

## Main z/OS Issue

Trivy v0.60.0 imports `modernc.org/sqlite`, which depends on
`modernc.org/libc`. `modernc.org/libc` currently lacks z/OS support, so the
build cannot use that SQLite driver.

## Workaround

The port replaces the SQLite driver path with `github.com/mattn/go-sqlite3`.
`buildenv` clones and patches:

- `github.com/mattn/go-sqlite3`
- `github.com/knqyf263/go-rpmdb`
- `github.com/aquasecurity/trivy`

The build compiles `sqlite3-binding.c` as a shared object, tags the generated
side deck, and builds Trivy with:

```bash
-X=github.com/mattn/go-sqlite3.driverName=sqlite
```

That preserves the `sqlite` driver name used by Trivy and go-rpmdb while using
the CGO-backed implementation.

## Validation

Run the port build on z/OS:

```bash
zopen-build -v
```

The build is complete when `${ZOPEN_INSTALL_DIR}/bin/trivy` is produced and
`trivy --version` runs on z/OS.
