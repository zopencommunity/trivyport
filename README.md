# Trivy Port for z/OS

## ⛔ Status: BLOCKED - Awaiting modernc.org/libc z/OS Support

This port is currently **blocked** due to a fundamental dependency issue. See [PORTING_STATUS.md](./PORTING_STATUS.md) for details.

---

## Overview

[Trivy](https://github.com/aquasecurity/trivy) is a comprehensive security scanner for vulnerabilities in:
- Container images
- Filesystems
- Git repositories
- Kubernetes clusters
- Cloud infrastructure

**Version:** 0.60.0  
**License:** Apache-2.0  
**Categories:** security, devops

---

## The Blocker

Trivy depends on `modernc.org/sqlite` (pure-Go SQLite implementation) which in turn depends on `modernc.org/libc`. The latter has **no z/OS support** - it excludes all z/OS files via build constraints in 12 critical packages.

### Impact

Without `modernc.org/libc` z/OS support, Trivy cannot be built on z/OS. This affects:
- RPM database scanning
- Java database operations
- Any feature requiring SQLite

---

## Documentation

- **[PORTING_STATUS.md](./PORTING_STATUS.md)** - Detailed status, attempted solutions, and timeline
- **[MODERNC_LIBC_PORTING_PLAN.md](./MODERNC_LIBC_PORTING_PLAN.md)** - Comprehensive 6-week plan to port modernc.org/libc to z/OS

---

## Options

### Option 1: Port modernc.org/libc to z/OS (Recommended)
**Effort:** 4-6 weeks  
**Impact:** Unblocks entire Go ecosystem on z/OS  
**See:** [MODERNC_LIBC_PORTING_PLAN.md](./MODERNC_LIBC_PORTING_PLAN.md)

### Option 2: Patch Trivy for CGO-based SQLite
**Effort:** 1-2 days  
**Impact:** Trivy-specific solution, requires maintaining patches  
**Trade-off:** Adds CGO dependency, may affect performance

### Option 3: Wait for Upstream
**Effort:** None  
**Impact:** Indefinite wait  
**Risk:** May never happen without community initiative

---

## How to Help

### For Go Developers
1. Review the [porting plan](./MODERNC_LIBC_PORTING_PLAN.md)
2. Fork [modernc.org/libc](https://gitlab.com/cznic/libc)
3. Implement z/OS support for one or more packages
4. Submit merge requests upstream

### For z/OS Experts
1. Provide guidance on z/OS syscall mappings
2. Review z/OS-specific implementations
3. Test on real z/OS systems

### For Community Members
1. Star/watch the modernc.org/libc repository
2. Engage with upstream maintainers
3. Share this documentation with interested parties

---

## Build Instructions (When Unblocked)

Once `modernc.org/libc` has z/OS support:

```bash
cd trivyport
zopen-build -v
```

The buildenv is already configured and ready to go.

---

## Related Ports

**Also Blocked:**
- Any Go application using `modernc.org/sqlite`

**Successfully Worked Around:**
- Grype (used go-sqlite3 + patches)

---

## Contact

- **zopen Community:** [Slack](https://zopencommunity.slack.com)
- **GitHub:** [zopencommunity](https://github.com/zopencommunity)
- **Issues:** Report to zopencommunity/meta

---

## License

This port configuration is provided under the Apache-2.0 license, matching Trivy's upstream license.

---

*Last Updated: 2026-05-04*