# Trivy z/OS Port Handoff

This file captures the current state of the Trivy z/OS port work so it can be resumed safely.

## Goal

Port Trivy to z/OS using the zopen port layout, but avoid editing Trivy's upstream `go.mod`.
The intended shape is like `murexport`: clone patched dependencies as sibling checkouts, apply
dependency-specific patches, then use `go work init` so Go resolves those local modules.

## Current build strategy

- `buildenv` now uses `zopen_config` plus `go work`, not `replace` directives in Trivy `go.mod`.
- `zopen_init` exports:
  - `CGO_ENABLED=1`
  - `GOMAXPROCS=1`
  - `GOPATH="${PORT_ROOT}/go-path"`
  - `GOMODCACHE="${GOPATH}/pkg/mod"`
  - `GOTMPDIR="${PORT_ROOT}/go-tmpdir"`
- `GOTMPDIR` was moved to the port root because using `trivy/go-tmpdir` broke after the Trivy
  checkout was reset during configure.
- `buildenv` clones/resets Trivy itself and all patched dependency checkouts before applying patches.
- Patch files use `.gopatch` so `zopen-build` does not auto-apply dependency patches to Trivy.
- `wharf` is disabled by default because `wharf ./trivy/...` was crashing. It only runs when
  `TRIVY_RUN_WHARF=1`.

## z/OS sync/build commands

From `/Users/igortodorovski/Projects/trivyport`:

```sh
rsync -avz --delete \
  --rsync-path=/home/itodoro/zopen/usr/local/bin/rsync \
  -e 'ssh -J itodorov@rogi21.fyre.ibm.com' \
  --exclude '.git/' \
  --exclude 'log*/' \
  --exclude 'build/' \
  --exclude 'install/' \
  --exclude 'go/' \
  --exclude 'go-path/' \
  /Users/igortodorovski/Projects/trivyport/trivyport/ \
  itodoro@zoscan2b.pok.stglabs.ibm.com:~/trivyport/
```

Then build:

```sh
SOURCE_PROFILE=1 ZOS_DIR=trivyport ./scripts/ssh-zos.sh --project 'zopen-build -f -v'
```

`SOURCE_PROFILE=1` matters because `scripts/ssh-zos.sh` was updated to source the remote profile
and `.bashrc`, which provide the needed zopen environment.

## Dependency patches currently represented

These patch files exist under `patches/`:

- `trivy-go-sqlite3-zos.gopatch`
- `trivy-go-rpmdb-sqlite3.gopatch`
- `trivy-sqlite3-driver.gopatch`
- `trivy-pb-v3-zos.gopatch`
- `trivy-pb-v1-zos.gopatch`
- `trivy-goleveldb-zos.gopatch`
- `trivy-continuity-zos.gopatch`
- `trivy-containerd-v1-zos.gopatch`
- `trivy-go-git-zos.gopatch`
- `trivy-grpc-zos.gopatch`
- `trivy-squealer-zos.gopatch`

Important fixes covered:

- Replace Trivy/`go-rpmdb` use of `modernc.org/sqlite` with `github.com/mattn/go-sqlite3`.
- Build/link `go-sqlite3` through a z/OS side deck.
- Add z/OS build-tag coverage for `pb`, `goleveldb`, `containerd/continuity`, `containerd`,
  `go-git`, `grpc`, and `squealer`.
- `squealer` error fixed by including `zos` in `internal/pkg/scan/signals_unix.go` so
  `monitorSignals` is built.

## Latest build state

All new-file patches have been regenerated using `git diff` from actual checkouts (2026-05-11).
The previous manually-authored patches had hunk header/content count mismatches that caused z/OS
git apply to truncate the last line of new-file hunks.

Patches regenerated and validated with `git apply --check`:
- `trivy-pb-v3-zos.gopatch` (was blocker: `term_zos.go:68:1 unexpected EOF, expected }`)
- `trivy-containerd-v1-zos.gopatch`
- `trivy-continuity-zos.gopatch`
- `trivy-go-git-zos.gopatch`
- `trivy-grpc-zos.gopatch`

All sentinel lines (`// end *.go`, `// keep final line for z/OS git apply`) have been removed.
Patches are now clean `git diff --binary` output with correct hunk headers.

## Recommended next step

Sync to z/OS and run `zopen-build -f -v`. The pb v3 `term_zos.go` truncation was the last known
compiler error. The build should now advance further; capture any new errors for the next iteration.

## Notes and cautions

- `rsync --delete` prints many warnings about not deleting generated dependency directories on z/OS.
  Those directories contain non-writable files. This has not blocked source sync.
- Avoid deleting `go-path` casually; Go module cache files can be read-only and produce noisy
  permission failures.
- If a z/OS build is clearly doomed, stop it with:

```sh
SOURCE_PROFILE=1 ZOS_DIR=trivyport ./scripts/ssh-zos.sh --project \
  'pkill -f "go build -buildvcs=false" || true; pkill -f "zopen-build -f -v" || true'
```

- The z/OS Go toolchain in use was `/home/itodoro/install_test/go1.26/bin/go`.
- Earlier `go build` runtime/assembler instability was the reason `GOMAXPROCS=1` was added.

## Local state reminder

The repo currently has uncommitted changes across `buildenv`, docs, `.gitignore`, and all new
`.gopatch` files. Before committing, regenerate the manually-created new-file patches with `git diff`
as described above.
