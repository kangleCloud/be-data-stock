# vita-file-spring-boot-starter 业务设计文档

## 1. 文档目标

结合 `be-vita` 仓库的工程规范，设计 `vita-file-spring-boot-starter` 的业务方案。

## 2. 参考实现分析（sjfy-file-spring-boot-starter）

### 2.1 核心架构

参考实现采用 **策略模式 + 多存储后端** 的架构：

```
FileApplicationService (应用服务层)
    ├── 动态选择实现类 (通过 file.active 决定)
    ├── FileMinioServiceImpl    — MinIO/S3 存储
    ├── FileNormalServiceImpl   — 本地磁盘存储
    ├── FileCephServiceImpl     — Ceph 存储
    └── FileQiniuServiceImpl    — 七牛云存储
```

### 2.2 业务流程

#### 2.2.1 单文件上传（两步式）

```
GET  /file/upload-id     → 获取 uploadId (Redis 缓存元数据, 7天 TTL)
POST /file/upload        → 携带 uploadId 上传文件
                           → Redis 中读取元数据 → 校验 → 实际上传 → 删除 Redis 缓存
                           → 返回 bucketName, hostName, uri, fileUrl, signUrl
```

#### 2.2.2 分片上传（四步式）

```
GET  /file/upload-id         → 获取 uploadId (初始化分片上传, isPart=1)
POST /file/upload/part       → 上传单个分片 (partNum, totalPart)
GET  /file/upload/complete   → 合并分片 (MinIO: completeMultipartUpload, 本地: cat/Java合并)
GET  /file/parts             → 查询分片列表
```

#### 2.2.3 辅助接口

```
GET  /file/valid/upload-id    → 校验 uploadId 有效性
POST /file/convert/sign-url   → 批量获取私有文件临时签名 URL
POST /file/upload/base64      → Base64 图片上传
```

### 2.3 存储后端对比

| 能力 | MinIO/S3 | 本地磁盘 |
| --- | --- | --- |
| 单文件上传 | `PutObjectArgs` 流式写入 | `MultipartFile.transferTo()` 落盘 |
| 分片上传 | `createMultipartUpload` → `uploadPart` → `completeMultipartUpload` | 分片写入临时目录 → `cat` 命令或 `RandomAccessFile` 合并 |
| 删除分片 | `abortMultipartUpload` | 删除临时目录 |
| 签名 URL | `getPresignedObjectUrl(Method.GET, expiry)` | 自研 MD5 Token 签名 (`uri + timestamp + secretKey`) |
| 公共/私有 | S3 Bucket Policy 控制 | URL 路径前缀区分 (`public/`, `private/`) |
| 分片校验 | 自动校验 Part Size 总和 | 遍历文件列表 |

### 2.4 关键设计模式

1. **uploadId 两阶段设计**：先获取 uploadId（Redis 缓存元数据），再实际上传。避免"上传了文件但元数据丢失"的情况。
2. **公共/私有文件隔离**：通过 `isPublic` 参数区分，影响存储路径前缀（`public/` vs `private/`）和访问方式（直接 URL vs 签名 URL）。
3. **文件名生成策略**：`UUID + yyyy/MM/dd/ + 后缀`，不保留原始文件名，避免冲突和注入。
4. **后缀白名单校验**：通过 starter 内部固定常量维护允许后缀，上传时统一校验。
5. **分片最小尺寸**：默认 5MB，避免小分片过多。
6. **前缀目录**：支持单层 `prefixDir`，最长 30 字符，不支持多级。

### 2.5 配置结构

```yaml
file:
  active: minio                    # 当前激活平台: minio/ceph/qiniu/normal
  accessKey / secretKey            # 凭证
  endpoint / hostname              # S3 endpoint 和公开访问域名
  bucketName                       # 桶名
  mountPath                        # 本地挂载目录 (normal 模式)
  publicRoute / privateRoute       # 公共/私有路径前缀
  signExpireTime                   # 签名有效期(秒)
  suffix:                          # 允许的后缀分类
    video: .mp4.flv...
    audio: .mp3.wma
    image: .jpg.png.jpeg.gif
    document: .doc.docx.csv...
    other: .apk
  part.minSize: 5242880            # 分片最小尺寸(字节)
  private.*                        # 七牛私有化 S3 配置 (仅 qiniu 模式)
```

## 3. be-vita 方案设计

### 3.1 设计原则

基于参考实现和 be-vita 仓库规范，`vita-file-spring-boot-starter` 应：

1. **保留核心业务逻辑**：uploadId 两阶段、分片上传、公共/私有隔离、签名 URL、后缀白名单
2. **适配 be-vita 规范**：`@ConfigurationProperties` 配置、`XxxProperty` 命名、禁止嵌套多层 static class、统一异常体系
3. **精简冗余后端**：优先支持 **MinIO** 和 **本地存储**，移除 Ceph/七牛的独立实现（S3 协议兼容 MinIO）
4. **接口风格**：starter 内部不直接暴露 Controller，而是提供 `FileApplicationService` 供业务模块调用
5. **配置收敛**：starter 仅通过 `@ConfigurationProperties` 读取 `vita.file.*`，不再额外引入 `Environment` 或动态配置覆盖层

### 3.2 模块结构

```
vita-file-spring-boot-starter/
├── pom.xml
├── src/main/resources/
│   ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   ├── application-file-dev.yml
│   └── application-file-prod.yml
├── src/main/java/com/vita/file/
│   ├── config/
│   │   ├── FileAutoConfiguration.java          # 自动装配：条件 Bean、启动校验
│   │   ├── MinioClientFactory.java             # MinIO 客户端工厂
│   │   └── ResolvedMinioConfig.java            # MinIO 生效配置快照
│   ├── property/
│   │   ├── FileProperty.java                   # 主配置 (vita.file.*)
│   │   ├── LocalStorageProperty.java            # 本地存储配置
│   │   ├── MinioStorageProperty.java            # MinIO 存储配置
│   │   └── FileSignatureProperty.java           # 签名配置
│   ├── enums/
│   │   ├── FileStorageTypeEnum.java             # local, minio
│   │   └── FileVisibilityEnum.java              # PUBLIC, PRIVATE
│   ├── dto/
│   │   ├── FileUploadRequestDto.java            # 上传请求 DTO
│   │   └── FileAccessUrlRequestDto.java         # 签名 URL 请求 DTO
│   ├── vo/
│   │   ├── FileUploadVo.java                    # 上传结果
│   │   └── FileAccessUrlVo.java                 # 访问 URL 结果
│   ├── storage/
│   │   ├── FileStorage.java                     # 存储接口 (策略模式)
│   │   ├── LocalFileStorage.java                # 本地磁盘实现
│   │   └── MinioFileStorage.java                # MinIO 实现
│   ├── service/
│   │   └── FileApplicationService.java          # 应用服务：uploadId 管理、后缀校验、URL 组装
│   ├── controller/
│   │   ├── FileController.java                  # 核心上传接口 (可选，由业务模块决定是否启用)
│   │   └── LocalFileDownloadController.java     # 本地文件下载 + 签名校验
│   └── support/
│       ├── FileObjectKeyGenerator.java          # 对象键生成器
│       ├── FileSignatureSupport.java            # 签名 URL 生成/校验
│       ├── LocalFileUrlSupport.java             # 本地文件 URL 解析
│       ├── FileUploadCommand.java               # 上传命令对象
│       ├── FileAccessCommand.java               # 访问命令对象
│       └── FileAccessResult.java                # 访问结果对象
```

### 3.3 核心接口设计

#### 3.3.1 存储接口 `FileStorage`

```java
public interface FileStorage {
    FileStorageTypeEnum getType();                              // local / minio

    void upload(InputStream inputStream, FileUploadCommand cmd); // 单文件上传
    String initMultipartUpload(FileUploadCommand cmd);           // 初始化分片上传
    void uploadPart(InputStream inputStream, FileUploadCommand cmd); // 上传分片
    void completeMultipartUpload(FileUploadCommand cmd);         // 合并分片
    void abortMultipartUpload(String uploadId, String objectKey); // 取消分片
    String generateAccessUrl(FileAccessCommand cmd);            // 生成访问 URL
    List<String> listParts(String uploadId, String objectKey);  // 列出分片
}
```

#### 3.3.2 应用服务 `FileApplicationService`

```java
public interface FileApplicationService {
    FileUploadVo upload(MultipartFile file, FileUploadRequestDto dto);  // 单文件上传
    FileUploadVo uploadPart(MultipartFile file, String uploadId, int partNum, int totalParts); // 分片上传
    FileUploadVo completeUpload(String uploadId);                         // 完成分片上传
    void abortUpload(String uploadId);                                    // 取消上传
    FileAccessUrlVo getAccessUrl(String objectKey);                       // 获取访问 URL (私有文件返回签名)
    List<FileAccessUrlVo> batchGetAccessUrls(List<String> objectKeys);    // 批量获取
}
```

### 3.4 业务流程（适配 be-vita）

#### 3.4.1 上传流程

```
1. 调用方传入: FileUploadRequestDto (isPublic, suffix, prefixDir, fileSize, isPart, originName)
2. FileApplicationService:
   a. 校验后缀是否在固定白名单常量中
   b. 如果是分片上传，校验 suffix 必传
   c. 生成 objectKey: {visibilityPrefix}/{yyyy/MM/dd}/{UUID}.{ext}
   d. 如有 prefixDir: {prefixDir}/{visibilityPrefix}/{date}/{UUID}.{ext}
   e. 生成 uploadId (雪花 ID 或 UUID)，写入 Redis (TTL 7天)
   f. 分片模式: 调用 FileStorage.initMultipartUpload()
3. 返回 uploadId

上传文件时:
1. 传入 uploadId + MultipartFile
2. 从 Redis 读取元数据，校验 uploadId 有效性
3. 调用 FileStorage.upload() 或 FileStorage.uploadPart()
4. 完成后删除 Redis 中的 uploadId 缓存
5. 返回 FileUploadVo (objectKey, accessUrl, signUrl)
```

#### 3.4.2 访问流程

```
1. 公开文件: 直接拼接 `public-endpoint` + objectKey（避免重复拼接 public/ 前缀）
2. 私有文件: 调用 FileStorage.generateAccessUrl() 生成临时签名 URL
   - MinIO: `MinioClientFactory` 统一创建上传 client 与签名 client，签名 URL 使用 `private-endpoint`
   - 本地: MD5(uri + timestamp + secretKey) 自研签名
3. 本地存储下载: LocalFileDownloadController 校验签名后流式返回
```

### 3.5 与 sjfy 参考实现的主要差异

| 差异点 | sjfy-file | vita-file (设计) |
| --- | --- | --- |
| 配置方式 | `@Value` 平铺 + SpEL 动态解析 | `@ConfigurationProperties` + 独立 Property 类 |
| Controller 位置 | 写在 starter 内部 | starter 提供可选 Controller，业务模块可选择性启用或自行封装 |
| 存储后端 | MinIO/Ceph/七牛/本地 (4 个实现) | MinIO/本地 (2 个实现)，S3 协议通过 MinIO SDK 覆盖 |
| 分片合并 | 系统命令 (`cat`) + Java (`RandomAccessFile`) 二选一 | MinIO 服务端合并；本地仅 `RandomAccessFile` 合并 |
| 返回体 | `ResponseData<T>` (sjfy 自定义) | `CommonResult<T>` (be-vita 标准) |
| 异常体系 | `ServiceException(ResultCode, message)` | `ServiceException(GlobalErrorCode)` |
| 嵌套类 | `FileProperty` 中多层嵌套 | 拆分为独立 Property 类 (`LocalStorageProperty`, `MinioStorageProperty`) |
| 校验分组 | `NormalCheck/UploadCheck/UploadPartCheck/BucketCheck` | 保留，命名调整为 `FileNormalCheck` 等避免冲突 |
| uploadId 存储 | Redis JSON 序列化 (FastJSON) | Redis JSON 序列化 (be-vita 已有 `RedisCache`) |
| 文件名生成 | `UUID.randomUUID() + yyyy/MM/dd/` | `UUID + yyyy/MM/dd/`，保持一致 |

### 3.6 配置示例

```yaml
vita:
  file:
    enabled: true
    active: minio                          # local | minio，仅选择当前激活实现
    public-prefix: public
    private-prefix: private
    max-file-size: 10485760                # 10MB
    sign-expire-seconds: 300               # 签名有效期 5 分钟
    local:
      base-path: /data/vita/uploads        # 本地存储根目录 (active=local 时生效)
      public-domain: https://files.kangle.local/public
      private-domain: https://files.kangle.local/private
    minio:
      endpoint: http://10.144.144.2:9000
      access-key: change-me-access-key
      secret-key: change-me-secret-key
      bucket: vita-file
      region: us-east-1
      secure: false
      auto-create-bucket: true
      presigned-expire-seconds: 3600
      path-style-access: true
      public-endpoint: https://oss.kangle.cloud/public
      private-endpoint: https://oss.kangle.cloud/private
    signature:
      secret: change-me-local-signature-secret     # 本地模式签名密钥
```

### 3.7 依赖关系

```
vita-file-spring-boot-starter -> vita-common
```

依赖说明：
- `vita-common`：提供 `RedisCache`、`ServiceException`、`GlobalErrorCode`、`CommonResult`、`PageRequest/PageResponse`
- MinIO SDK：通过 `io.minio:minio` 引入（`active=minio` 时生效）
- Spring Boot Starter 基础：`spring-boot-starter`、`spring-boot-autoconfigure`

### 3.8 启动校验

`FileAutoConfiguration` 的 `InitializingBean` 中校验：

1. `enabled=true` 时才激活
2. `active` 必须为 `local` 或 `minio`
3. `local` 模式下 `local.base-path`、`local.public-domain` 不能为空，且目录需可创建
4. `minio` 模式下通过 `MinioStorageProperty` 解析当前 YML 配置，必填项为 `endpoint`、`access-key`、`secret-key`、`bucket`、`public-endpoint`
5. `signature.secret` 需要显式配置，示例值仅用于样例文件
6. `max-file-size > 0`
7. `sign-expire-seconds > 0`
8. `local` 和 `minio` 配置允许同时存在，仅根据 `active` 选择实际启用实现
