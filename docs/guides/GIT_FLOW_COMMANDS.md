# ⚡ Git Flow - Quick Reference Commands

## 🚀 Setup Ban Đầu

```bash
# Clone repository
git clone https://github.com/manh20051151/backend-WVideos.git
cd backend-WVideos

# Tạo develop branch (lần đầu)
git checkout -b develop
git push origin develop
```

---

## 🎨 Feature Development

### Tạo Feature

```bash
git checkout develop
git pull origin develop
git checkout -b feature/ten-tinh-nang
```

### Làm Việc Trên Feature

```bash
# Commit changes
git add .
git commit -m "feat: mô tả tính năng"

# Push để backup (optional)
git push origin feature/ten-tinh-nang
```

### Merge Feature vào Develop

```bash
git checkout develop
git pull origin develop
git merge --no-ff feature/ten-tinh-nang -m "merge: tích hợp feature ten-tinh-nang"
git push origin develop

# Xóa feature branch (optional)
git branch -d feature/ten-tinh-nang
git push origin --delete feature/ten-tinh-nang
```

---

## 🚀 Release Process

### Tạo Release

```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0
```

### Chuẩn Bị Release

```bash
# Cập nhật version
git commit -am "chore: cập nhật version 1.0.0"

# Fix bugs nếu cần
git commit -am "fix: sửa lỗi validation"

# Push release branch
git push origin release/v1.0.0
```

### Merge Release vào Master

```bash
# Merge vào master
git checkout master
git pull origin master
git merge --no-ff release/v1.0.0 -m "release: phát hành version 1.0.0"

# Tạo tag
git tag -a v1.0.0 -m "Release 1.0.0"

# Push
git push origin master
git push origin v1.0.0
```

### Merge Release về Develop

```bash
git checkout develop
git pull origin develop
git merge --no-ff release/v1.0.0 -m "merge: đồng bộ release v1.0.0 về develop"
git push origin develop

# Xóa release branch (optional)
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

---

## 🔥 Hotfix Process

### Tạo Hotfix

```bash
git checkout master
git pull origin master
git checkout -b hotfix/v1.0.1
```

### Fix Bug

```bash
git commit -am "fix: sửa lỗi nghiêm trọng"
git commit -am "chore: cập nhật version 1.0.1"
git push origin hotfix/v1.0.1
```

### Merge Hotfix vào Master

```bash
git checkout master
git merge --no-ff hotfix/v1.0.1 -m "hotfix: sửa lỗi v1.0.1"
git tag -a v1.0.1 -m "Hotfix 1.0.1"
git push origin master
git push origin v1.0.1
```

### Merge Hotfix về Develop

```bash
git checkout develop
git merge --no-ff hotfix/v1.0.1 -m "merge: đồng bộ hotfix v1.0.1 về develop"
git push origin develop

# Xóa hotfix branch
git branch -d hotfix/v1.0.1
git push origin --delete hotfix/v1.0.1
```

---

## 📊 Useful Commands

### Xem Git Graph

```bash
# Xem graph đẹp
git log --all --decorate --oneline --graph

# Xem 20 commits gần nhất
git log --all --decorate --oneline --graph -20

# Alias (thêm vào ~/.gitconfig)
git config --global alias.lg "log --all --decorate --oneline --graph"
# Sau đó dùng: git lg
```

### Xem Branches

```bash
# Xem local branches
git branch

# Xem tất cả branches (local + remote)
git branch -a

# Xem branches với commit cuối
git branch -v
```

### Xem Tags

```bash
# Xem tất cả tags
git tag

# Xem tags với message
git tag -n

# Xem tag cụ thể
git show v1.0.0
```

### Xem Status

```bash
# Xem status
git status

# Xem status ngắn gọn
git status -s

# Xem diff
git diff

# Xem diff của staged files
git diff --staged
```

### Sync với Remote

```bash
# Fetch tất cả từ remote
git fetch origin

# Pull branch hiện tại
git pull origin <branch-name>

# Pull tất cả branches
git pull --all

# Push branch hiện tại
git push origin <branch-name>

# Push tất cả branches
git push origin --all

# Push tất cả tags
git push origin --tags
```

---

## 🔧 Troubleshooting Commands

### Undo Changes

```bash
# Undo uncommitted changes
git checkout -- <file>

# Undo tất cả uncommitted changes
git reset --hard HEAD

# Undo last commit (giữ changes)
git reset --soft HEAD~1

# Undo last commit (xóa changes)
git reset --hard HEAD~1
```

### Fix Conflicts

```bash
# Khi có conflict
git merge feature/my-feature
# CONFLICT!

# Xem files có conflict
git status

# Sau khi sửa conflicts
git add <file>
git commit -m "merge: giải quyết conflicts"
```

### Revert Merge

```bash
# Revert merge commit (chưa push)
git reset --hard HEAD~1

# Revert merge commit (đã push)
git revert -m 1 HEAD
git push origin <branch>
```

### Clean Up

```bash
# Xóa local branch
git branch -d feature/my-feature

# Force xóa local branch
git branch -D feature/my-feature

# Xóa remote branch
git push origin --delete feature/my-feature

# Xóa tất cả merged branches
git branch --merged | grep -v "\*" | xargs -n 1 git branch -d
```

---

## 📝 Commit Message Templates

### Feature

```bash
git commit -m "feat: thêm API upload video"
git commit -m "feat: thêm validation cho user input"
git commit -m "feat: tích hợp payment gateway"
```

### Fix

```bash
git commit -m "fix: sửa lỗi SQL injection"
git commit -m "fix: sửa lỗi validation email"
git commit -m "fix: sửa memory leak trong video processing"
```

### Chore

```bash
git commit -m "chore: cập nhật version 1.0.0"
git commit -m "chore: cập nhật dependencies"
git commit -m "chore: cleanup code"
```

### Docs

```bash
git commit -m "docs: cập nhật API documentation"
git commit -m "docs: thêm Git Flow guide"
git commit -m "docs: cập nhật README"
```

### Refactor

```bash
git commit -m "refactor: tối ưu video processing service"
git commit -m "refactor: cải thiện code structure"
```

### Merge

```bash
git commit -m "merge: tích hợp feature video-upload vào develop"
git commit -m "merge: đồng bộ release v1.0.0 về develop"
```

---

## 🎯 One-Liners

### Complete Feature Workflow

```bash
# Tạo, làm việc, và merge feature
git checkout develop && \
git pull origin develop && \
git checkout -b feature/my-feature && \
# ... code code code ... && \
git add . && \
git commit -m "feat: my feature" && \
git checkout develop && \
git merge --no-ff feature/my-feature -m "merge: tích hợp my-feature" && \
git push origin develop && \
git branch -d feature/my-feature
```

### Complete Release Workflow

```bash
# Tạo release, merge vào master và develop
git checkout develop && \
git pull origin develop && \
git checkout -b release/v1.0.0 && \
# ... cập nhật version ... && \
git commit -am "chore: cập nhật version 1.0.0" && \
git checkout master && \
git merge --no-ff release/v1.0.0 -m "release: phát hành v1.0.0" && \
git tag -a v1.0.0 -m "Release 1.0.0" && \
git push origin master v1.0.0 && \
git checkout develop && \
git merge --no-ff release/v1.0.0 -m "merge: đồng bộ release v1.0.0" && \
git push origin develop && \
git branch -d release/v1.0.0
```

---

## 🔍 Git Aliases (Optional)

Thêm vào `~/.gitconfig`:

```ini
[alias]
    # Git Flow shortcuts
    co = checkout
    br = branch
    ci = commit
    st = status
    
    # Git Flow commands
    feature-start = "!f() { git checkout develop && git pull && git checkout -b feature/$1; }; f"
    feature-finish = "!f() { git checkout develop && git merge --no-ff feature/$1 -m \"merge: tích hợp feature/$1\"; }; f"
    
    release-start = "!f() { git checkout develop && git pull && git checkout -b release/$1; }; f"
    release-finish = "!f() { \
        git checkout master && git merge --no-ff release/$1 -m \"release: phát hành $1\" && \
        git tag -a $1 -m \"Release $1\" && \
        git checkout develop && git merge --no-ff release/$1 -m \"merge: đồng bộ release $1\"; \
    }; f"
    
    # Useful aliases
    lg = log --all --decorate --oneline --graph
    last = log -1 HEAD
    unstage = reset HEAD --
    undo = reset --soft HEAD~1
```

Sử dụng:

```bash
git feature-start my-feature
git feature-finish my-feature

git release-start v1.0.0
git release-finish v1.0.0
```

---

**Cập nhật lần cuối**: 2026-01-29
