# vita-file-spring-boot-starter MinIO AK/SK 接入指南

## 1. 创建 MinIO alias

```bash
mc alias set myminio http://10.144.144.2:9000 minioadmin minioadmin
```

## 2. 创建业务专用用户

```bash
mc admin user add myminio file-starter-user '请替换为强密码'
```

## 3. 创建最小权限策略

保存策略文件 `file-starter-policy.json`：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": [
        "arn:aws:s3:::vita-file"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::vita-file/*"
      ]
    }
  ]
}
```

创建并绑定策略：

```bash
mc admin policy create myminio file-starter-policy file-starter-policy.json
mc admin policy attach myminio file-starter-policy --user file-starter-user
```

## 4. 创建 AK/SK

自动生成：

```bash
mc admin accesskey create myminio/ file-starter-user
```

指定生成：

```bash
mc admin accesskey create myminio/ file-starter-user \
  --access-key '请替换为ACCESS_KEY' \
  --secret-key '请替换为SECRET_KEY'
```

## 5. 推荐配置样例

```yaml
vita:
  file:
    enabled: true
    active: minio
    public-prefix: public
    private-prefix: private
    max-file-size: 10485760
    sign-expire-seconds: 300

    local:
      base-path: /data/vita/uploads
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
      secret: change-me-local-signature-secret
```

## 6. 说明

- 业务应用只使用专用 AK/SK，不使用管理员账号。
- starter 仅从 `vita.file.*` 的 YML 配置读取 MinIO 参数，不额外做 `Environment` 或配置中心覆盖。
- 对外访问地址与 MinIO API `endpoint` 分离：
  - API：`http://10.144.144.2:9000`
  - 公开访问：`https://oss.kangle.cloud/public`
  - 私有访问：`https://oss.kangle.cloud/private`
