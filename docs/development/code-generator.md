# 代码生成器使用指南

## 适用边界

`vita-generator` 为仓库自建普通业务表生成标准 CRUD 骨架。纯关联表、Warm-Flow 官方引擎表、认证内部表以及明确不需要通用 CRUD 的表不适用；工作流只读关联查询和所有业务特有校验仍需人工设计。

生成器只维护 `vita-generator/src/main/resources/templates/mybatis-plus/` 一套当前模板，不提供旧模板、版本切换或兼容回退。

## 运行前配置

在 `CodeGenerator` 顶部常量区设置数据库连接、表名和输出模块。仓库保留当前 MySQL、`workflow_category` 和 `vita-workflow` 配置作为默认结构，真实数据库密码不得提交：

```java
private static final String DB_PASSWORD = "";
private static final boolean OVERWRITE_EXISTING_FILES = false;
```

本地运行前可临时填写 `DB_PASSWORD`，完成后必须恢复为空字符串并检查 Git 差异。生成器不读取环境变量或 JVM 系统属性。

`OVERWRITE_EXISTING_FILES` 默认为 `false`。生成结果先写入系统临时目录，全部完成后统一检查目标文件；任一目标文件已存在时，本次发布整体终止，不会写入仓库。只有明确审查现有业务改造并决定整体覆盖时，才可在本地临时设置为 `true`。

## 执行

在仓库根目录运行：

```bash
mvn -pl vita-generator -DskipTests exec:java -Dexec.mainClass=com.vita.config.CodeGenerator
```

生成器会拒绝 Warm-Flow 官方引擎表和 `app_user`、`app_oauth_account` 认证内部表，并校验模块、包名和表名格式。

## 固定输出

每张表生成以下文件：

- Entity、Mapper 和 Mapper XML
- CreateDto、UpdateDto、DeletedDto、SearchDto
- DetailVo、ListVo、PageVo
- Service、ServiceImpl、Controller

Entity 继承 `BaseEntity`，不重复声明 id、租户、审计、逻辑删除和 version 字段。ListVo 输出主键 id 和业务字段；Mapper XML 即使没有自定义 SQL 也会保留空 namespace 骨架。

写操作 Service 默认声明回滚事务，Controller 默认增加 `@RepeatSubmit`，但模板不生成权限注解。删除接口固定为：

```http
POST /{route}/{resource}/delete
Content-Type: application/json

{"id": 1}
```

Controller 和 Service 均使用 `DeletedDto`，Service 校验 DTO、主键和数据存在性后按主键删除。

SearchDto 保留 `keyword`，ServiceImpl 同时保留关键字条件的 TODO。生成后必须根据实际可搜索字段完成该条件，不得直接把占位实现作为最终业务代码交付。

## 生成后检查

1. 确认只生成预期表和模块文件，没有覆盖未审查的人工改造。
2. 补充 DTO 业务校验、唯一性检查、状态约束、关键字字段和必要的 Mapper SQL。
3. 按接口场景补充权限、日志和其他业务注解；不得在生成器中固化具体权限码。
4. 编译受影响模块并运行相关测试，检查 `DB_PASSWORD` 已恢复为空、`OVERWRITE_EXISTING_FILES` 已恢复为 `false`。
