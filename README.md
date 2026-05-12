# Trivy Port for z/OS

This repository contains the zopen build configuration for
[Trivy](https://github.com/aquasecurity/trivy) on z/OS.

## Overview

Trivy is a security scanner for container images, filesystems, Git
repositories, Kubernetes clusters, and cloud infrastructure.

- Version: 0.60.0
- License: Apache-2.0
- Categories: security, devops

## Porting Approach

Trivy v0.60.0 depends on `modernc.org/sqlite`, which pulls in
`modernc.org/libc`. That dependency does not currently build on z/OS.

This port follows the workspace pattern used by other zopen Go ports:

1. `zopen-build` clones Trivy from upstream.
2. `buildenv` clones the patched dependencies as sibling directories.
3. Patch files in `patches/` are applied per module.
4. `go work init` creates a workspace over Trivy and the local dependency
   clones.
5. Trivy builds from the workspace without editing Trivy's upstream `go.mod`.

The SQLite workaround uses `github.com/mattn/go-sqlite3` with CGO and a
z/OS-built SQLite side deck.

## Build

```bash
zopen-build -v
```

The installed binary is written to `${ZOPEN_INSTALL_DIR}/bin/trivy`.

## Patches

See [patches/README.md](patches/README.md) for the per-module patch list.
