# modernc.org/libc z/OS Porting Plan

## Executive Summary

Porting `modernc.org/libc` to z/OS is a **major undertaking** that will enable pure-Go SQLite implementations (like `modernc.org/sqlite`) to work on z/OS. This benefits not just Trivy, but the entire Go ecosystem on z/OS.

**Estimated Effort:** 4-6 weeks for experienced Go/z/OS developer  
**Complexity:** High - requires deep understanding of z/OS syscalls and Go runtime  
**Impact:** High - unblocks multiple ports (Trivy, Grype, and any Go app using modernc.org/sqlite)

---

## Phase 1: Repository Setup & Analysis (Week 1)

### 1.1 Fork and Clone modernc.org/libc
```bash
# Fork https://gitlab.com/cznic/libc to your account
git clone https://gitlab.com/YOUR_USERNAME/libc.git
cd libc
git checkout -b zos-port
```

### 1.2 Analyze Build Constraints
The following packages exclude z/OS via build tags:
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

### 1.3 Study Existing Implementations
Review existing platform implementations:
```bash
# Study Linux implementation as reference
find . -name "*_linux_*.go" | head -10
# Study Darwin (macOS) for BSD-like patterns
find . -name "*_darwin_*.go" | head -10
```

---

## Phase 2: Core Infrastructure (Week 1-2)

### 2.1 Create z/OS Build Files
For each package, create `*_zos_s390x.go` files:

**Template Structure:**
```go
//go:build zos && s390x

package errno

import (
    "syscall"
)

// z/OS-specific errno mappings
const (
    EPERM   = syscall.EPERM
    ENOENT  = syscall.ENOENT
    // ... map all errno values
)
```

### 2.2 Implement sys/types Package
**Priority: HIGH** - Foundation for all other packages

Create `sys/types/types_zos_s390x.go`:
```go
//go:build zos && s390x

package types

import "syscall"

// Map z/OS types to Go types
type (
    X__dev_t     = uint32
    X__ino_t     = uint32
    X__mode_t    = uint32
    X__nlink_t   = uint32
    X__uid_t     = uint32
    X__gid_t     = uint32
    X__off_t     = int64
    X__time_t    = int64
    X__pid_t     = int32
    // ... complete type mappings
)
```

### 2.3 Implement errno Package
Create `errno/errno_zos_s390x.go`:
```go
//go:build zos && s390x

package errno

import "syscall"

// Map all POSIX errno values to z/OS equivalents
// Reference: z/OS XL C/C++ Runtime Library Reference
const (
    EPERM           = syscall.Errno(0x00000001)
    ENOENT          = syscall.Errno(0x00000002)
    ESRCH           = syscall.Errno(0x00000003)
    // ... complete errno mappings (100+ values)
)
```

---

## Phase 3: File & I/O Operations (Week 2-3)

### 3.1 Implement stdio Package
Create `stdio/stdio_zos_s390x.go`:
```go
//go:build zos && s390x

package stdio

import (
    "syscall"
    "unsafe"
)

// Implement FILE operations using z/OS syscalls
type X__sFILE struct {
    // z/OS FILE structure
}

func Xfopen(tls *TLS, pathname, mode uintptr) uintptr {
    // Use syscall.Open with z/OS-specific flags
}

func Xfclose(tls *TLS, stream uintptr) int32 {
    // Use syscall.Close
}

// ... implement all stdio functions
```

### 3.2 Implement unistd Package
Create `unistd/unistd_zos_s390x.go`:
```go
//go:build zos && s390x

package unistd

import "syscall"

func Xread(tls *TLS, fd int32, buf uintptr, count uint64) int64 {
    n, err := syscall.Read(int(fd), (*[1<<30]byte)(unsafe.Pointer(buf))[:count])
    if err != nil {
        tls.setErrno(err)
        return -1
    }
    return int64(n)
}

func Xwrite(tls *TLS, fd int32, buf uintptr, count uint64) int64 {
    n, err := syscall.Write(int(fd), (*[1<<30]byte)(unsafe.Pointer(buf))[:count])
    if err != nil {
        tls.setErrno(err)
        return -1
    }
    return int64(n)
}

// ... implement all unistd functions
```

---

## Phase 4: Process & User Management (Week 3-4)

### 4.1 Implement pwd Package (Password Database)
Create `pwd/pwd_zos_s390x.go`:
```go
//go:build zos && s390x

package pwd

import (
    "os/user"
    "strconv"
)

type Passwd struct {
    Fpw_name   uintptr
    Fpw_passwd uintptr
    Fpw_uid    uint32
    Fpw_gid    uint32
    Fpw_gecos  uintptr
    Fpw_dir    uintptr
    Fpw_shell  uintptr
}

func Xgetpwuid(tls *TLS, uid uint32) uintptr {
    u, err := user.LookupId(strconv.Itoa(int(uid)))
    if err != nil {
        return 0
    }
    // Convert user.User to Passwd struct
    // Allocate memory and populate fields
}

// ... implement getpwnam, getpwent, etc.
```

### 4.2 Implement grp Package (Group Database)
Create `grp/grp_zos_s390x.go`:
```go
//go:build zos && s390x

package grp

import (
    "os/user"
    "strconv"
)

type Group struct {
    Fgr_name   uintptr
    Fgr_passwd uintptr
    Fgr_gid    uint32
    Fgr_mem    uintptr
}

func Xgetgrgid(tls *TLS, gid uint32) uintptr {
    g, err := user.LookupGroupId(strconv.Itoa(int(gid)))
    if err != nil {
        return 0
    }
    // Convert user.Group to Group struct
}

// ... implement getgrnam, getgrent, etc.
```

### 4.3 Implement pthread Package
Create `pthread/pthread_zos_s390x.go`:
```go
//go:build zos && s390x

package pthread

import "sync"

// Use Go's sync primitives to implement pthread operations
type X__pthread_mutex_t struct {
    mu sync.Mutex
}

func Xpthread_mutex_init(tls *TLS, mutex, attr uintptr) int32 {
    // Initialize mutex
    return 0
}

func Xpthread_mutex_lock(tls *TLS, mutex uintptr) int32 {
    m := (*X__pthread_mutex_t)(unsafe.Pointer(mutex))
    m.mu.Lock()
    return 0
}

// ... implement all pthread functions
```

---

## Phase 5: Advanced I/O & Signals (Week 4-5)

### 5.1 Implement poll Package
Create `poll/poll_zos_s390x.go`:
```go
//go:build zos && s390x

package poll

import "syscall"

type PollFd struct {
    Ffd      int32
    Fevents  int16
    Frevents int16
}

func Xpoll(tls *TLS, fds uintptr, nfds uint64, timeout int32) int32 {
    // Use syscall.Select or implement using z/OS poll()
    // z/OS supports poll() via BPX1POL
}
```

### 5.2 Implement signal Package
Create `signal/signal_zos_s390x.go`:
```go
//go:build zos && s390x

package signal

import (
    "os/signal"
    "syscall"
)

func Xsignal(tls *TLS, signum int32, handler uintptr) uintptr {
    // Use Go's signal.Notify for signal handling
    // Map POSIX signals to z/OS signals
}

// ... implement sigaction, kill, etc.
```

---

## Phase 6: Time & Limits (Week 5)

### 6.1 Implement time Package
Create `time/time_zos_s390x.go`:
```go
//go:build zos && s390x

package time

import (
    "syscall"
    "time"
)

type Timespec struct {
    Ftv_sec  int64
    Ftv_nsec int64
}

func Xgettimeofday(tls *TLS, tv, tz uintptr) int32 {
    now := time.Now()
    // Convert to timespec
    return 0
}

// ... implement clock_gettime, nanosleep, etc.
```

### 6.2 Implement limits Package
Create `limits/limits_zos_s390x.go`:
```go
//go:build zos && s390x

package limits

// z/OS system limits
const (
    X_POSIX_PATH_MAX = 1024
    X_POSIX_NAME_MAX = 255
    X_POSIX_OPEN_MAX = 256
    // ... all POSIX limits
)
```

### 6.3 Implement stdlib Package
Create `stdlib/stdlib_zos_s390x.go`:
```go
//go:build zos && s390x

package stdlib

import (
    "os"
    "strconv"
)

func Xgetenv(tls *TLS, name uintptr) uintptr {
    s := GoString(name)
    val := os.Getenv(s)
    if val == "" {
        return 0
    }
    return CString(val)
}

func Xatoi(tls *TLS, nptr uintptr) int32 {
    s := GoString(nptr)
    i, _ := strconv.Atoi(s)
    return int32(i)
}

// ... implement malloc, free, exit, etc.
```

---

## Phase 7: Testing & Integration (Week 6)

### 7.1 Unit Tests
Create test files for each package:
```go
//go:build zos && s390x

package errno_test

import "testing"

func TestErrnoValues(t *testing.T) {
    // Verify errno mappings
}
```

### 7.2 Integration Testing with modernc.org/sqlite
```bash
cd /path/to/sqlite-test
go mod init test
go get modernc.org/sqlite@latest
# Write test program
go test -v
```

### 7.3 Test with Trivy
```bash
cd /home/itodoro/projects/zos-porting/trivyport
zopen-build -v
```

---

## Phase 8: Upstream Contribution (Week 6+)

### 8.1 Prepare Merge Request
1. Clean up code and add documentation
2. Run all tests on z/OS
3. Create comprehensive commit messages
4. Submit MR to https://gitlab.com/cznic/libc

### 8.2 Community Engagement
1. Post to zopen community about the port
2. Document learnings in blog post
3. Present at z/OS Open Source meetup

---

## Key Challenges & Solutions

### Challenge 1: EBCDIC vs ASCII
**Solution:** Use Go's built-in encoding conversion or rely on z/OS's automatic conversion

### Challenge 2: z/OS-specific Syscalls
**Solution:** Study z/OS XL C/C++ Runtime Library Reference for syscall mappings

### Challenge 3: Memory Management
**Solution:** Use Go's memory management; avoid direct malloc/free where possible

### Challenge 4: Thread Safety
**Solution:** Leverage Go's goroutines and sync primitives instead of pthreads

---

## Resources

### Documentation
- [z/OS XL C/C++ Runtime Library Reference](https://www.ibm.com/docs/en/zos)
- [modernc.org/libc GitLab](https://gitlab.com/cznic/libc)
- [Go syscall package](https://pkg.go.dev/syscall)

### Similar Ports
- Study how other platforms are implemented in modernc.org/libc
- Review z/OS Go runtime source code

### Community
- zopen Slack channel
- z/OS Open Source community forums

---

## Success Criteria

- [ ] All 12 packages build successfully on z/OS
- [ ] modernc.org/sqlite compiles and runs on z/OS
- [ ] Trivy builds successfully using modernc.org/sqlite
- [ ] All unit tests pass
- [ ] Integration tests with SQLite pass
- [ ] Upstream merge request accepted

---

## Next Steps

1. **Immediate:** Fork modernc.org/libc repository
2. **Week 1:** Implement sys/types and errno packages
3. **Week 2:** Implement stdio and unistd packages
4. **Week 3-4:** Implement pwd, grp, pthread packages
5. **Week 5:** Implement poll, signal, time, limits, stdlib packages
6. **Week 6:** Testing and upstream contribution

---

## Estimated Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Phase 1 | 3 days | Analysis complete, repo forked |
| Phase 2 | 5 days | Core infrastructure (types, errno) |
| Phase 3 | 7 days | File I/O (stdio, unistd) |
| Phase 4 | 7 days | Process/user mgmt (pwd, grp, pthread) |
| Phase 5 | 7 days | Advanced I/O (poll, signal) |
| Phase 6 | 5 days | Time/limits (time, limits, stdlib) |
| Phase 7 | 5 days | Testing & integration |
| Phase 8 | Ongoing | Upstream contribution |
| **Total** | **~6 weeks** | Full z/OS port |

---

## Risk Mitigation

**Risk:** z/OS syscalls differ significantly from POSIX  
**Mitigation:** Use Go's standard library where possible; document deviations

**Risk:** Upstream maintainer may not accept z/OS port  
**Mitigation:** Maintain fork; engage early with maintainer

**Risk:** Performance issues with pure-Go implementation  
**Mitigation:** Profile and optimize; consider CGO fallback for critical paths

---

*This is a living document. Update as implementation progresses.*
