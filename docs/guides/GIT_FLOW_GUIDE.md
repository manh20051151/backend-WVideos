# 🌿 Quy Trình Git Flow - snha Backend

## 📌 Tổng Quan

Git Flow là quy trình quản lý branch chuẩn cho team development. Document này hướng dẫn chi tiết cách áp dụng Git Flow trong dự án snha Backend.

## 🎯 Cấu Trúc Branches

### Branches Chính (Permanent)

#### 1. **master** (Production)
- Code production-ready, luôn ổn định
- Mỗi commit là một version release
- Được bảo vệ, không commit trực tiếp
- Chỉ merge từ: release branches, hotfix branches

#### 2. **develop** (Development)
- Integration branch cho development
- Base cho tất cả feature branches
- Có thể chứa code chưa ổn định
- Merge từ: feature branches, release branches, hotfix branches

### Branches Tạm Thời (Temporary)

#### 3. **feature/** (Feature Development)
- Phát triển tính năng mới
- Tạo từ: `develop`
- Merge vào: `develop`
- Naming: `feature/ten-tinh-nang`

#### 4. **release/** (Release Preparation)
- Chuẩn bị cho production release
- Tạo từ: `develop`
- Merge vào: `master` VÀ `develop`
- Naming: `release/v{major}.{minor}.{patch}`

#### 5. **hotfix/** (Emergency Fixes)
- Sửa bug khẩn cấp trên production
- Tạo từ: `master`
- Merge vào: `master` VÀ `develop`
- Naming: `hotfix/v{major}.{minor}.{patch}`

---

## 🔄 Quy Trình Chi Tiết

### 1️⃣ Khởi Tạo Git Flow (Lần Đầu)

```bash
# Clone repository
git clone https://github.com/manh20051151/backend-WVideos.git
cd backend-WVideos

# Tạo develop branch từ master
git checkout master
git checkout -b develop
git push origin develop
```

---

### 2️⃣ Phát Triển Feature Mới

#### Bước 1: Tạo Feature Branch

```bash
# Đảm bảo develop là mới nhất
git checkout develop
git pull origin develop

# Tạo feature branch
git checkout -b feature/ten-tinh-nang
```

#### Bước 2: Phát Triển Feature

```bash
# Làm việc trên feature
# ... code code code ...

# Commit thường xuyên
git add .
git commit -m "feat: mô tả tính năng"

# Push lên remote (optional, để backup)
git push origin feature/ten-tinh-nang
```

#### Bước 3: Merge Feature vào Develop

```bash
# Cập nhật develop mới nhất
git checkout develop
git pull origin develop

# Merge feature với --no-ff để giữ lịch sử
git merge --no-ff feature/ten-tinh-nang -m "merge: tích hợp feature ten-tinh-nang vào develop"

# Push develop
git push origin develop

# Xóa feature branch (optional)
git branch -d feature/ten-tinh-nang
git push origin --delete feature/ten-tinh-nang
```

**Ví dụ thực tế:**
```bash
git checkout develop
git checkout -b feature/comment-system

# Phát triển feature
git add .
git commit -m "feat: thêm hệ thống comment cho video"
git commit -m "feat: thêm reply comment"
git commit -m "feat: thêm like/dislike comment"

# Merge vào develop
git checkout develop
git merge --no-ff feature/comment-system -m "merge: tích hợp comment system vào develop"
git push origin develop
```

---

### 3️⃣ Chuẩn Bị Release

#### Bước 1: Tạo Release Branch

```bash
# Từ develop, tạo release branch
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0
```

#### Bước 2: Chuẩn Bị Release

```bash
# Cập nhật version trong pom.xml
# <version>1.2.0</version>

# Cập nhật version trong README.md
# **Version: 1.2.0**

# Cập nhật CHANGELOG.md (nếu có)

# Commit changes
git add .
git commit -m "chore: cập nhật version 1.2.0 cho release"

# Fix bug nếu phát hiện trong testing
git commit -m "fix: sửa lỗi validation trong comment"

# Push release branch
git push origin release/v1.2.0
```

#### Bước 3: Merge Release vào Master

```bash
# Merge vào master
git checkout master
git pull origin master
git merge --no-ff release/v1.2.0 -m "release: phát hành version 1.2.0"

# Tạo tag
git tag -a v1.2.0 -m "Release version 1.2.0 - Comment System"

# Push master và tags
git push origin master
git push origin v1.2.0
```

#### Bước 4: Merge Release về Develop

```bash
# Merge về develop để đồng bộ bug fixes
git checkout develop
git pull origin develop
git merge --no-ff release/v1.2.0 -m "merge: đồng bộ release v1.2.0 về develop"
git push origin develop
```

#### Bước 5: Xóa Release Branch (Optional)

```bash
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

---

### 4️⃣ Hotfix Khẩn Cấp

#### Bước 1: Tạo Hotfix Branch

```bash
# Từ master, tạo hotfix branch
git checkout master
git pull origin master
git checkout -b hotfix/v1.2.1
```

#### Bước 2: Fix Bug

```bash
# Fix bug khẩn cấp
git add .
git commit -m "fix: sửa lỗi bảo mật SQL injection"

# Cập nhật version
git commit -m "chore: cập nhật version 1.2.1"

# Push hotfix branch
git push origin hotfix/v1.2.1
```

#### Bước 3: Merge Hotfix vào Master

```bash
# Merge vào master
git checkout master
git merge --no-ff hotfix/v1.2.1 -m "hotfix: sửa lỗi bảo mật v1.2.1"

# Tạo tag
git tag -a v1.2.1 -m "Hotfix 1.2.1 - Security Fix"

# Push master và tags
git push origin master
git push origin v1.2.1
```

#### Bước 4: Merge Hotfix về Develop

```bash
# Merge về develop
git checkout develop
git merge --no-ff hotfix/v1.2.1 -m "merge: đồng bộ hotfix v1.2.1 về develop"
git push origin develop
```

#### Bước 5: Xóa Hotfix Branch

```bash
git branch -d hotfix/v1.2.1
git push origin --delete hotfix/v1.2.1
```

---

## 📊 Workflow Diagram

```
master ─────●─────────────────●─────────●─────────●────
            │              v1.0.0    v1.1.0    v1.2.0
            │                 │         │         │
develop ────●──●──●──●─────────●─────────●─────────●────
            │  │  │  │         │         │         │
feature/A ──●──┘  │  │         │         │         │
feature/B ────────●──┘         │         │         │
release/v1.0.0 ────────────────●─┘       │         │
hotfix/v1.1.0 ──────────────────────────●─┘        │
release/v1.2.0 ─────────────────────────────────────●─┘
```

---

## ✅ Best Practices

### 1. Commit Messages

Sử dụng Conventional Commits:

```bash
feat: thêm tính năng mới
fix: sửa bug
docs: cập nhật documentation
chore: công việc maintenance
refactor: refactor code
style: format code
perf: cải thiện performance
test: thêm tests
```

**Ví dụ:**
```bash
git commit -m "feat: thêm API upload video"
git commit -m "fix: sửa lỗi validation email"
git commit -m "docs: cập nhật API documentation"
```

### 2. Branch Naming

```bash
# Feature branches
feature/user-profile
feature/video-streaming
feature/payment-integration

# Release branches
release/v1.0.0
release/v2.1.0

# Hotfix branches
hotfix/v1.0.1
hotfix/v2.1.1
```

### 3. Merge Strategy

**Luôn sử dụng `--no-ff`** để giữ lịch sử branch:

```bash
# ✅ ĐÚNG
git merge --no-ff feature/my-feature

# ❌ SAI
git merge feature/my-feature
```

### 4. Pull Before Push

```bash
# Luôn pull trước khi push
git pull origin develop
git push origin develop
```

### 5. Testing

- Test kỹ trên feature branch trước khi merge
- Test lại trên release branch trước khi merge vào master
- Chạy full test suite trước mỗi release

---

## 🚫 Những Điều KHÔNG NÊN Làm

1. ❌ **KHÔNG commit trực tiếp vào master**
   ```bash
   # SAI
   git checkout master
   git commit -m "fix something"
   ```

2. ❌ **KHÔNG merge develop trực tiếp vào master**
   ```bash
   # SAI
   git checkout master
   git merge develop
   ```

3. ❌ **KHÔNG thêm feature mới vào release branch**
   ```bash
   # SAI - chỉ fix bug trên release branch
   git checkout release/v1.0.0
   git commit -m "feat: thêm feature mới"
   ```

4. ❌ **KHÔNG force push lên master/develop**
   ```bash
   # SAI
   git push -f origin master
   ```

5. ❌ **KHÔNG xóa branch trước khi merge**
   ```bash
   # SAI
   git branch -D feature/my-feature  # Chưa merge
   ```

---

## 📝 Checklist

### Trước Khi Merge Feature

- [ ] Code đã được review
- [ ] Tests đã pass
- [ ] Không có conflicts với develop
- [ ] Commit messages rõ ràng
- [ ] Documentation đã cập nhật

### Trước Khi Release

- [ ] Tất cả features đã merge vào develop
- [ ] Version number đã cập nhật
- [ ] CHANGELOG đã cập nhật
- [ ] Full test suite đã pass
- [ ] Documentation đã hoàn chỉnh
- [ ] Database migrations đã sẵn sàng

### Sau Khi Release

- [ ] Tag đã được tạo
- [ ] Release notes đã publish
- [ ] Master và develop đã đồng bộ
- [ ] Release branch đã xóa (optional)
- [ ] Team đã được thông báo

---

## 🆘 Troubleshooting

### Conflict Khi Merge

```bash
# Nếu có conflict
git merge --no-ff feature/my-feature
# CONFLICT (content): Merge conflict in file.txt

# Giải quyết conflict
# 1. Mở file và sửa conflicts
# 2. Sau khi sửa xong:
git add file.txt
git commit -m "merge: giải quyết conflicts khi merge feature/my-feature"
```

### Merge Nhầm Branch

```bash
# Nếu merge nhầm và chưa push
git reset --hard HEAD~1

# Nếu đã push (cẩn thận!)
git revert -m 1 HEAD
git push origin develop
```

### Quên Tạo Tag

```bash
# Tạo tag cho commit cũ
git tag -a v1.0.0 <commit-hash> -m "Release 1.0.0"
git push origin v1.0.0
```

---

## 📚 Tài Liệu Tham Khảo

- [Git Flow Original](https://nvie.com/posts/a-successful-git-branching-model/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)

---

## 👥 Team Workflow

### Developer

1. Pull develop mới nhất
2. Tạo feature branch
3. Phát triển feature
4. Commit và push
5. Tạo Pull Request (nếu có)
6. Merge vào develop sau khi review

### Tech Lead

1. Review Pull Requests
2. Quyết định khi nào release
3. Tạo release branch
4. Merge release vào master
5. Tạo tags và release notes

### DevOps

1. Monitor master branch
2. Deploy khi có tag mới
3. Rollback nếu cần
4. Thông báo team về deployment

---

**Cập nhật lần cuối**: 2026-01-29
**Version**: 1.0.0
