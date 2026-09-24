# vita-poi-spring-boot-starter Excel 设计文档

## 1. 文档目标

结合 `be-vita` 仓库的工程规范，补齐 `vita-poi-spring-boot-starter` 的 Excel 处理方案，明确后续业务在导入、导出、模板填充场景下如何接入。

当前 `vita-poi-spring-boot-starter` 只有 `pom.xml` 和 `easyexcel` 依赖，没有任何对外 API、配置约定和业务调用规范。本设计文档需要同时承担两类职责：

1. 作为后续 starter 实现的蓝图
2. 作为业务模块接入 Excel 能力的使用说明

## 2. 官方 API 基线（EasyExcel）

本方案以 EasyExcel 官方 API 文档为能力边界：

- 读 Excel：[https://easyexcel.opensource.alibaba.com/docs/current/api/](https://easyexcel.opensource.alibaba.com/docs/current/api/)
- 写 Excel：[https://easyexcel.opensource.alibaba.com/docs/current/api/write](https://easyexcel.opensource.alibaba.com/docs/current/api/write)
- 填充 Excel：[https://easyexcel.opensource.alibaba.com/docs/current/api/fill](https://easyexcel.opensource.alibaba.com/docs/current/api/fill)

### 2.1 读 Excel（ReadWorkbook / ReadSheet）

官方读能力的核心组成如下：

- 注解模型：`@ExcelProperty`、`@ExcelIgnore`、`@ExcelIgnoreUnannotated`、`@DateTimeFormat`、`@NumberFormat`
- Workbook 级参数：`file/inputStream`、`excelType`、`charset`、`autoCloseStream`、`ignoreEmptyRow`、`useDefaultListener`、`readDefaultReturn`
- Sheet 级参数：`sheetNo`、`sheetName`、`headRowNumber`
- 扩展点：`readListener`、`converter`

对 starter 的直接启发：

- 导入默认必须基于 listener 流式读取，而不是默认 `syncRead`
- 业务侧 DTO 的表头映射和字段格式化应直接复用 EasyExcel 注解
- 自定义 Converter 必须作为一级扩展点纳入 starter 的请求模型

### 2.2 写 Excel（WriteWorkbook / WriteSheet / WriteTable）

官方写能力的核心组成如下：

- Workbook 级参数：`outputStream/file`、`excelType`、`templateInputStream/templateFile`、`inMemory`、`autoCloseStream`
- Sheet 级参数：`sheetNo`、`sheetName`
- Table 级参数：`tableNo`
- 通用扩展点：`converter`、`writeHandler`、`needHead`、`relativeHeadRowIndex`、`includeColumn*`、`excludeColumn*`

对 starter 的直接启发：

- 导出需要同时支持小数据量一次性写出和大数据量分页写出
- 样式、列宽、冻结、合并等自定义能力不能被 starter 吃掉，必须保留 `writeHandler` 扩展口
- 字段裁剪能力要直接映射 `include/excludeColumn*`

### 2.3 填充 Excel（Fill）

官方文档对 fill 的说明很简洁，本质是复用写 Excel API 和模板能力完成填充，不需要单独造一套与写入完全不同的抽象。

对 starter 的直接启发：

- 模板填充单独提供 `ExcelFillService`
- 但底层能力仍复用 EasyExcel 写入模型
- 模板文件由业务提供，starter 不负责模板仓库和模板版本管理

## 3. starter 定位与设计原则

### 3.1 定位

`vita-poi-spring-boot-starter` 首版只负责 **Excel**，不扩展到 Word / PPT / PDF，也不封装通用 Apache POI 低层 API。

starter 对外只暴露 **service/facade + 请求结果模型**，不内置通用导入导出 Controller。业务模块自行决定：

- HTTP 路由
- 鉴权与权限码
- 审计日志
- `CommonResult` 包装
- `HttpServletResponse` 响应头

### 3.2 设计原则

1. **Service only**
   - starter 只提供 Java API，不直接提供 Web API
2. **EasyExcel first**
   - 所有抽象必须能回落到官方 `read/write/fill` 语义
3. **高频参数类型化**
   - 业务最常用的能力放到 request 模型里
4. **长尾能力透传**
   - 不把 EasyExcel 的 builder 参数重新发明一遍
5. **导入默认流式处理**
   - 默认 listener 模式，避免大文件 OOM
6. **业务 DTO 与领域实体隔离**
   - 不建议直接用数据库实体承载 Excel 读写

## 4. 与当前仓库现状的关系

当前仓库中：

- `vita-starters/vita-poi-spring-boot-starter` 仅声明 `easyexcel` 依赖
- `vita-admin`、`vita-service`、`vita-common` 中没有现成 Excel 导入导出实现
- 仓库内已有 starter 设计文档风格以 `docs/architecture/starter/file/vita-file-starter-design.md` 为参考

因此 `vita-poi-spring-boot-starter` 文档需要先把以下内容一次性补齐：

- 模块边界
- 推荐包结构
- 公共 API 草案
- 请求与结果模型
- 业务接入示例
- 验收用例

## 5. 配置模型设计

配置前缀统一采用 `vita.poi.excel.*`，为未来可能扩展的非 Excel 能力预留 `vita.poi.*` 命名空间。

### 5.1 推荐配置

```yaml
vita:
  poi:
    excel:
      enabled: true
      default-head-row-number: 1
      default-import-batch-size: 100
      default-export-page-size: 5000
      max-error-rows: 200
      trim-cell-value: true
      ignore-empty-row: true
      auto-close-stream: false
      in-memory: false
      use-default-style: true
      default-sheet-name: Sheet1
```

### 5.2 配置项说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 Excel starter |
| `default-head-row-number` | `1` | 默认表头行数 |
| `default-import-batch-size` | `100` | 默认导入批次大小 |
| `default-export-page-size` | `5000` | 默认分页导出每页记录数 |
| `max-error-rows` | `200` | 导入错误明细最大保留条数，避免超大响应 |
| `trim-cell-value` | `true` | 默认开启 trim |
| `ignore-empty-row` | `true` | 默认忽略空行 |
| `auto-close-stream` | `false` | 默认不关闭调用方传入的流，由调用方管理生命周期 |
| `in-memory` | `false` | 写入默认不走全内存模式，降低 OOM 风险 |
| `use-default-style` | `true` | 导出默认使用 EasyExcel 默认样式 |
| `default-sheet-name` | `Sheet1` | 默认 sheet 名称 |

### 5.3 Property 类建议

```java
public class PoiExcelProperty {
    private Boolean enabled = true;
    private Integer defaultHeadRowNumber = 1;
    private Integer defaultImportBatchSize = 100;
    private Integer defaultExportPageSize = 5000;
    private Integer maxErrorRows = 200;
    private Boolean trimCellValue = true;
    private Boolean ignoreEmptyRow = true;
    private Boolean autoCloseStream = false;
    private Boolean inMemory = false;
    private Boolean useDefaultStyle = true;
    private String defaultSheetName = "Sheet1";
}
```

## 6. 推荐模块结构

推荐包结构如下：

```text
vita-poi-spring-boot-starter/
├── pom.xml
├── src/main/resources/
│   ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
│   └── application-poi.yml
└── src/main/java/com/vita/poi/excel/
    ├── config/
    │   └── PoiExcelAutoConfiguration.java
    ├── property/
    │   └── PoiExcelProperty.java
    ├── service/
    │   ├── ExcelImportService.java
    │   ├── ExcelExportService.java
    │   ├── ExcelFillService.java
    │   ├── impl/DefaultExcelImportService.java
    │   ├── impl/DefaultExcelExportService.java
    │   └── impl/DefaultExcelFillService.java
    ├── model/
    │   ├── request/
    │   │   ├── ExcelImportRequest.java
    │   │   ├── ExcelExportRequest.java
    │   │   ├── ExcelPagedExportRequest.java
    │   │   └── ExcelFillRequest.java
    │   ├── result/
    │   │   ├── ExcelImportResult.java
    │   │   └── ExcelImportError.java
    │   └── context/
    │       ├── ExcelRowContext.java
    │       └── ExcelBatchContext.java
    ├── processor/
    │   ├── ExcelImportProcessor.java
    │   └── ExcelPageProvider.java
    ├── listener/
    │   └── DefaultExcelImportListener.java
    ├── support/
    │   ├── ExcelReadSupport.java
    │   ├── ExcelWriteSupport.java
    │   ├── ExcelFillSupport.java
    │   ├── ExcelFilenameSupport.java
    │   └── ExcelResponseHeaderExample.java
    ├── converter/
    └── handler/
```

说明：

- 不建议使用 `controller` 包
- 请求/结果模型更推荐放在 `model` 包，而不是 `dto/vo`
- `processor` 包用于承载业务回调接口，不和 listener/support 混在一起

## 7. 公共 API 草案

## 7.1 核心 service

```java
public interface ExcelImportService {
    <T> ExcelImportResult importExcel(ExcelImportRequest<T> request,
                                      ExcelImportProcessor<T> processor);
}

public interface ExcelExportService {
    <T> void export(ExcelExportRequest<T> request);

    <T> void exportByPage(ExcelPagedExportRequest<T> request);
}

public interface ExcelFillService {
    void fill(ExcelFillRequest request);
}
```

### 7.2 导入请求模型

```java
public class ExcelImportRequest<T> {
    private InputStream inputStream;
    private String fileName;
    private Class<T> headClass;
    private Integer sheetNo;
    private String sheetName;
    private Integer headRowNumber;
    private Integer batchSize;
    private List<Converter<?>> converters;
    private Consumer<ExcelReaderBuilder> readWorkbookCustomizer;
    private Consumer<ExcelReaderSheetBuilder> readSheetCustomizer;
}
```

说明：

- `inputStream` 必填
- `headClass` 首版要求必填，采用 class + 注解映射模式
- `sheetNo` 和 `sheetName` 二选一，若同时存在，以 `sheetNo` 为准
- `headRowNumber` 为空时回退到全局默认配置

### 7.3 导出请求模型

```java
public class ExcelExportRequest<T> {
    private OutputStream outputStream;
    private String fileName;
    private Class<T> headClass;
    private String sheetName;
    private Collection<T> data;
    private Boolean needHead;
    private Integer relativeHeadRowIndex;
    private Set<Integer> includeColumnIndexes;
    private Set<String> includeColumnFieldNames;
    private Set<Integer> excludeColumnIndexes;
    private Set<String> excludeColumnFieldNames;
    private List<Converter<?>> converters;
    private List<WriteHandler> writeHandlers;
    private Consumer<ExcelWriterBuilder> writeWorkbookCustomizer;
    private Consumer<ExcelWriterSheetBuilder> writeSheetCustomizer;
}
```

### 7.4 分页导出请求模型

```java
public class ExcelPagedExportRequest<T> {
    private OutputStream outputStream;
    private String fileName;
    private Class<T> headClass;
    private String sheetName;
    private Integer pageSize;
    private ExcelPageProvider<T> pageProvider;
    private Boolean needHead;
    private Integer relativeHeadRowIndex;
    private Set<Integer> includeColumnIndexes;
    private Set<String> includeColumnFieldNames;
    private Set<Integer> excludeColumnIndexes;
    private Set<String> excludeColumnFieldNames;
    private List<Converter<?>> converters;
    private List<WriteHandler> writeHandlers;
    private Consumer<ExcelWriterBuilder> writeWorkbookCustomizer;
    private Consumer<ExcelWriterSheetBuilder> writeSheetCustomizer;
}
```

分页 provider 建议定义为：

```java
public interface ExcelPageProvider<T> {
    List<T> fetch(int pageNo, int pageSize);
}
```

分页停止规则：

- `pageNo` 从 `1` 开始
- 返回空集合时结束
- 返回数量小于 `pageSize` 时结束
- 返回 `null` 视为非法实现，starter 直接抛业务异常

### 7.5 模板填充请求模型

```java
public class ExcelFillRequest {
    private InputStream templateInputStream;
    private OutputStream outputStream;
    private Integer sheetNo;
    private String sheetName;
    private Object objectData;
    private Collection<?> listData;
    private List<WriteHandler> writeHandlers;
    private FillConfig fillConfig;
    private Consumer<ExcelWriterBuilder> writeCustomizer;
}
```

说明：

- `templateInputStream` 必填
- `objectData` 用于单对象填充
- `listData` 用于列表填充
- 两者可以同时存在
- `sheetNo` 与 `sheetName` 可同时传，首版实现中优先使用 `sheetNo`

### 7.6 导入结果模型

```java
public class ExcelImportResult {
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private Boolean truncated;
    private List<ExcelImportError> errors;
}

public class ExcelImportError {
    private Integer rowIndex;
    private String fieldName;
    private String message;
    private Map<Integer, String> rawData;
}
```

设计约束：

- `rowIndex` 采用 **Excel 中的真实行号（1-based）**
- `rawData` 采用用户可读的 `Map<Integer, String>`，不直接暴露 `CellData`
- 当失败明细超过 `max-error-rows` 时，仅保留前 N 条，同时 `truncated=true`
- `processor.validate` 返回的错误统一映射为行级错误，`fieldName = null`
- 只有 EasyExcel 转换/解析异常能定位字段时，`fieldName` 才回填具体字段名

### 7.7 业务回调接口

```java
public interface ExcelImportProcessor<T> {
    List<String> validate(T row, ExcelRowContext context);

    void saveBatch(List<T> rows, ExcelBatchContext context);
}
```

辅助上下文建议定义为：

```java
public class ExcelRowContext {
    private String fileName;
    private Integer sheetNo;
    private String sheetName;
    private Integer rowIndex;
    private Map<Integer, String> rawData;
}

public class ExcelBatchContext {
    private String fileName;
    private Integer sheetNo;
    private String sheetName;
    private Integer batchNo;
    private Integer startRowIndex;
    private Integer endRowIndex;
}
```

## 8. 导入设计

### 8.1 核心流程

```text
Controller / Service
    -> 组装 ExcelImportRequest
    -> ExcelImportService#importExcel
    -> EasyExcel.read(...).sheet(...).doRead()
    -> DefaultExcelImportListener
         -> DTO 映射
         -> 逐行 validate
         -> 错误行写入 ExcelImportError
         -> 成功行放入 batch buffer
         -> 达到 batchSize 后调用 saveBatch
         -> saveBatch 异常则整批转失败
    -> 汇总 ExcelImportResult
```

### 8.2 处理模型

首版导入默认采用 **行级汇总** 模型：

1. 逐行读取
2. 每行先进行 DTO 映射
3. 再调用 `processor.validate`
4. 校验失败则记录错误并继续下一行
5. 校验通过的数据进入批次缓冲区
6. 达到 `batchSize` 后调用 `saveBatch`
7. `saveBatch` 异常时，当前批次所有行统一转为失败
8. 文件读取结束后返回汇总结果

补充约束：

- `failedRows` 统计失败数据行数，不按错误消息条数累计
- `processor.validate` 可以返回多条消息，starter 会为同一行记录多条 `ExcelImportError`
- 转换异常不会直接中断整份文件，listener 会转成 `ExcelImportError` 后继续读取后续行

### 8.3 事务约束

`saveBatch` 的事务由业务实现负责，推荐：

- 每个 batch 自己开启事务
- batch 失败则整批回滚
- 不建议在 starter 里强行控制业务事务边界

这是因为 starter 不知道业务是否还要联动其他表、消息或校验规则。

### 8.4 为什么不用 `syncRead`

首版不把 `syncRead` 作为默认方案，原因如下：

- 大文件场景下容易占用大量内存
- 与“逐行校验 + 批量落库 + 错误汇总”的目标不匹配
- listener 流式读取更适合后台管理导入场景

如果后续确有小文件全量处理需求，可以在 customizer 或二期扩展中增加同步读取模式，但不作为默认设计。

## 9. 导出设计

### 9.1 小数据量导出

适用于列表页导出、少量字典导出、单次报表导出：

```text
业务模块查询 List<T>
    -> 组装 ExcelExportRequest
    -> ExcelExportService#export
    -> EasyExcel.write(outputStream, headClass).sheet(sheetName).doWrite(data)
```

### 9.2 大数据量分页导出

适用于台账、日志、历史记录等大数据量场景：

```text
Controller
    -> 组装 ExcelPagedExportRequest
    -> ExcelExportService#exportByPage
    -> 创建 ExcelWriter / WriteSheet
    -> pageProvider.fetch(pageNo, pageSize)
    -> writer.write(pageData, writeSheet)
    -> 循环直至返回空集合或不足 pageSize
    -> finish()
```

设计约束：

- 首版分页导出只处理单 sheet
- `pageProvider` 返回结果不能为 `null`，空集合表示结束
- `pageProvider.fetch(pageNo, pageSize)` 中的 `pageNo` 从 `1` 开始递增
- 每页默认大小使用 `vita.poi.excel.default-export-page-size`

### 9.3 字段裁剪策略

导出字段裁剪直接映射 EasyExcel 原生参数：

- `includeColumnIndexes`
- `includeColumnFieldNames`
- `excludeColumnIndexes`
- `excludeColumnFieldNames`

推荐优先使用 `fieldNames` 版本，避免 index 受 DTO 调整影响。

实现约束：

- 同一维度的 `include/exclude` 不能同时传入
- 即 `includeColumnIndexes` 与 `excludeColumnIndexes` 互斥
- `includeColumnFieldNames` 与 `excludeColumnFieldNames` 互斥

### 9.4 响应流处理边界

starter 不直接操作 `HttpServletResponse`。业务 Controller 需要自行：

1. 设置 `Content-Type`
2. 设置 `Content-Disposition`
3. 获取 `response.getOutputStream()`
4. 将 `OutputStream` 交给 `ExcelExportService`

这样可以让业务自由决定：

- 文件名策略
- 权限校验
- 下载审计
- 特定浏览器兼容处理

## 10. 模板填充设计

### 10.1 适用场景

模板填充适用于：

- 固定版式导出
- 带表头说明、封面、签章位置的报表
- 单对象 + 列表混合输出

### 10.2 流程

```text
业务准备模板 InputStream
    -> 组装 ExcelFillRequest
    -> ExcelFillService#fill
    -> EasyExcel.write(outputStream)
         .withTemplate(templateInputStream)
         .sheet(sheetName)
         .fill(objectData)
         .fill(listData)
```

首版固定执行顺序：

1. 先填充 `objectData`
2. 再填充 `listData`

这样可以避免对象占位与列表占位在同一模板中的覆盖顺序不一致。

### 10.3 设计约束

- starter 不管理模板文件来源
- 模板既可以来自 `classpath`，也可以来自文件系统或远程流
- 首版默认单模板、单 sheet 调用模型
- 多 sheet 模板填充通过重复 fill 调用扩展，不在首版单独设计新的 facade
- 首版不额外提供 fill 专属 sheet customizer，只保留 `Consumer<ExcelWriterBuilder> writeCustomizer`

## 11. 高频参数类型化与长尾参数透传

这是 `vita-poi-spring-boot-starter` 的关键设计策略。

### 11.1 高频参数类型化

以下参数直接进入 request 模型：

- `headClass`
- `sheetNo/sheetName`
- `headRowNumber`
- `batchSize`
- `data`
- `needHead`
- `include/excludeColumn*`
- `writeHandlers`
- `converters`

这样业务最常见场景不需要理解底层 builder。

### 11.2 长尾参数透传

EasyExcel 官方 builder 参数较多，不适合全部在 starter 中重新抽象一遍。因此保留以下 customizer：

- `readWorkbookCustomizer`
- `readSheetCustomizer`
- `writeWorkbookCustomizer`
- `writeSheetCustomizer`
- `writeCustomizer`（fill）

建议类型直接使用：

```java
Consumer<ExcelReaderBuilder>
Consumer<ExcelReaderSheetBuilder>
Consumer<ExcelWriterBuilder>
Consumer<ExcelWriterSheetBuilder>
```

这样后续即使 EasyExcel 新增参数，也不需要频繁修改 starter API。

实现顺序约束：

- starter 先应用自身默认配置与 typed request 参数
- `readWorkbookCustomizer / readSheetCustomizer / writeWorkbookCustomizer / writeSheetCustomizer / writeCustomizer` 最后执行
- 因此业务可以通过 customizer 覆盖 starter 默认值，而不是被 starter 二次改写

## 12. 业务 DTO 建模规范

### 12.1 不直接复用实体类

推荐规则：

- Excel 导入 DTO 和数据库实体分离
- Excel 导出 DTO 和页面 VO 分离
- 不要直接把 `entity` 当作 Excel 读写对象

原因：

- Excel 字段往往有格式化和展示语义
- 数据库实体可能包含不应暴露的字段
- 导入 DTO 往往需要额外校验字段

### 12.2 注解使用约定

推荐：

- 所有导入导出 DTO 使用 `@ExcelIgnoreUnannotated`
- 需要参与读写的字段显式标记 `@ExcelProperty`
- 日期字符串统一用 `@DateTimeFormat`
- 数字格式统一用 `@NumberFormat`
- 非标准类型统一使用 `Converter`

示例：

```java
@ExcelIgnoreUnannotated
public class SysUserExcelImportDto {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("昵称")
    private String nickName;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("状态")
    private String statusText;
}
```

## 13. 业务接入示例

### 13.1 基础列表导出

```java
@GetMapping("/export")
public void export(HttpServletResponse response) throws IOException {
    String fileName = URLEncoder.encode("系统用户.xlsx", StandardCharsets.UTF_8);
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);

    List<SysUserExcelExportDto> rows = sysUserService.listForExcel();

    excelExportService.export(
            ExcelExportRequest.<SysUserExcelExportDto>builder()
                    .outputStream(response.getOutputStream())
                    .fileName("系统用户.xlsx")
                    .headClass(SysUserExcelExportDto.class)
                    .sheetName("用户列表")
                    .data(rows)
                    .needHead(true)
                    .build()
    );
}
```

### 13.2 分页大数据导出

```java
@GetMapping("/export/page")
public void exportPage(HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''sys-user-page.xlsx");

    excelExportService.exportByPage(
            ExcelPagedExportRequest.<SysUserExcelExportDto>builder()
                    .outputStream(response.getOutputStream())
                    .fileName("sys-user-page.xlsx")
                    .headClass(SysUserExcelExportDto.class)
                    .sheetName("用户台账")
                    .pageSize(5000)
                    .pageProvider((pageNo, pageSize) -> sysUserService.pageForExcel(pageNo, pageSize))
                    .build()
    );
}
```

### 13.3 导入并逐行返回错误明细

```java
@PostMapping("/import")
public CommonResult<ExcelImportResult> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
    ExcelImportResult result = excelImportService.importExcel(
            ExcelImportRequest.<SysUserExcelImportDto>builder()
                    .inputStream(file.getInputStream())
                    .fileName(file.getOriginalFilename())
                    .headClass(SysUserExcelImportDto.class)
                    .sheetNo(0)
                    .headRowNumber(1)
                    .batchSize(100)
                    .build(),
            new ExcelImportProcessor<SysUserExcelImportDto>() {
                @Override
                public List<String> validate(SysUserExcelImportDto row, ExcelRowContext context) {
                    List<String> errors = new ArrayList<>();
                    if (StrUtil.isBlank(row.getUsername())) {
                        errors.add("用户名不能为空");
                    }
                    return errors;
                }

                @Override
                public void saveBatch(List<SysUserExcelImportDto> rows, ExcelBatchContext context) {
                    sysUserImportService.saveBatch(rows);
                }
            }
    );
    return CommonResult.success(result);
}
```

## 14. 未来实现的验收场景

### 14.1 导入

- 表头通过 `@ExcelProperty` 正确映射
- 能指定 `sheetNo` 或 `sheetName`
- 支持 `headRowNumber > 1`
- 支持自定义 `Converter`
- 空行默认忽略
- 单行脏数据不会阻断整文件导入
- `saveBatch` 异常会把当前批次转为失败行
- 错误明细会保留真实 Excel 行号

### 14.2 导出

- 能基于注解 DTO 正常导出
- 能通过 `include/excludeColumn*` 裁剪字段
- 能注册 `WriteHandler` 自定义样式、列宽、冻结或批注
- 能按分页 provider 持续写出大数据量
- 能通过 customizer 透传原生 builder 参数

### 14.3 模板填充

- 支持对象填充
- 支持列表填充
- 支持对象 + 列表混合填充
- 模板缺少占位符时行为可控
- 支持指定模板 sheet

### 14.4 透传能力

- `readWorkbookCustomizer`
- `readSheetCustomizer`
- `writeWorkbookCustomizer`
- `writeSheetCustomizer`
- `writeCustomizer`

必须真实作用于 EasyExcel builder，而不是只保留字段却不生效。

## 15. 非目标与边界

首版明确不做：

- 通用导入导出 Controller
- 自动封装 `CommonResult`
- 异步导入任务
- 导入文件落库
- 导入历史记录追踪
- 错误文件回传
- Excel 之外的 POI 低层能力封装
- Word / PPT / PDF

如果后续有这些需求，应作为二期能力单独设计，而不是把首版 starter 扩张成“大而全”的办公文档平台。

## 16. 最终建议

`vita-poi-spring-boot-starter` 首版最适合做成一套 **轻封装、强约定、保留 EasyExcel 原生扩展能力** 的 service-only starter：

- 对业务足够简单
- 对底层 EasyExcel 足够透明
- 对未来能力扩展足够稳定

这也是当前 `be-vita` 仓库里最稳妥、最容易推广给后续业务模块的 Excel 接入方式。
