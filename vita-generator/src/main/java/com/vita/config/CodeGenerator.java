package com.vita.config;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import com.vita.core.controller.BaseController;
import com.vita.core.entity.BaseEntity;
import com.vita.engine.VitaVelocityTemplateEngine;
import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.utils.DbUtil;
import org.apache.ibatis.annotations.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-03-04  14:02:55
 * @Description: 代码生成器
 * @Version: 1.0
 */
public class CodeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeGenerator.class);

    /**
     * 生成器配置 - DB HOST
     */
    private static final String DB_HOST = "106.54.205.33";
    /**
     * 生成器配置 - DB PORT
     */
    private static final Integer DB_PORT = 3306;
    /**
     * 生成器配置 - DB USER
     */
    private static final String DB_USER = "root";
    /**
     * 生成器配置 - DB PASSWORD。仅允许在本地临时填写，禁止提交真实密码。
     */
    private static final String DB_PASSWORD = "WQO6D9ax#f*QPJup";
    /**
     * 生成器配置 - DB NAME
     */
    private static final String DB_NAME = "be_vita";
    /**
     * 生成器配置 - CODE AUTHOR
     */
    private static final String AUTHOR = "znk";
    /**
     * 生成器配置 - PROJECT NAME
     */
    private static final String PROJECT_NAME = "vita";

    /**
     * 生成器配置 - TABLE PREFIX
     */
    private static final String TABLE_PREFIX = "tb_";

    /**
     * 生成器配置 - TABLE NAME
     */
    private static final String[] TABLE_NAMES = {"workflow_category"};

    /**
     * 生成器配置 - TARGET MODULE
     */
    private static final String TARGET_MODULE = "vita-workflow";

    /**
     * 生成器配置 - CONTROLLER MODULE
     */
    private static final String CONTROLLER_MODULE = "vita-workflow";

    /**
     * 生成器配置 - BUSINESS MODULE
     */
    private static final String BUSINESS_MODULE = "workflow";

    /**
     * 生成器配置 - DOMAIN MODULE
     */
    private static final String DOMAIN_MODULE = "";

    /**
     * 生成器配置 - BASE PACKAGE
     */
    private static final String BASE_PACKAGE = "com.vita";

    /**
     * 生成器配置 - ROUTE PREFIX。为空时使用 BUSINESS_MODULE。
     */
    private static final String ROUTE_PREFIX = "";

    /**
     * 是否允许覆盖已存在的目标文件。默认关闭，避免覆盖业务定制代码。
     */
    private static final boolean OVERWRITE_EXISTING_FILES = false;

    /**
     * BaseEntity 已承载的数据库字段，实体类不重复生成。
     */
    private static final String[] BASE_ENTITY_COLUMNS = {
            "id", "tenant_id", "create_time", "create_by_id", "create_by",
            "update_time", "update_by_id", "update_by", "is_deleted", "version"
    };

    /**
     * Warm-Flow 官方引擎表，禁止生成业务 CRUD。
     */
    private static final Set<String> WARM_FLOW_ENGINE_TABLES = Set.of(
            "flow_definition", "flow_node", "flow_skip", "flow_instance",
            "flow_task", "flow_his_task", "flow_user", "flow_form"
    );

    /**
     * 认证内部表只允许生成持久化骨架，禁止生成可直接暴露的通用 CRUD。
     */
    private static final Set<String> AUTH_INTERNAL_TABLES = Set.of("app_user", "app_oauth_account");

    private static final Pattern MODULE_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final Pattern PACKAGE_SEGMENT_PATTERN = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern TABLE_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");

    public static void main(String[] args) throws IOException {
        ModuleConfig moduleConfig = ModuleConfig.current();
        validateConfiguration(moduleConfig);

        Path repositoryRoot = Path.of("").toAbsolutePath().normalize();
        validateRepositoryRoot(repositoryRoot, moduleConfig);
        Path stagingRoot = Files.createTempDirectory("vita-code-generator-");
        try {
            LOGGER.info("模块化生成配置: {}", moduleConfig);
            DataSourceConfig.Builder dataSourceConfig = DbUtil.getDataSourceConfig(
                    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
            );
            generateToStaging(
                    stagingRoot,
                    moduleConfig,
                    dataSourceConfig,
                    TABLE_NAMES,
                    TABLE_PREFIX
            );
            publishGeneratedFiles(stagingRoot, repositoryRoot, OVERWRITE_EXISTING_FILES);
            LOGGER.info("代码生成完成。");
        } finally {
            deleteDirectory(stagingRoot);
        }
    }

    private static void generateToStaging(Path stagingRoot, ModuleConfig moduleConfig,
                                          DataSourceConfig.Builder dataSourceConfig,
                                          String[] tableNames, String tablePrefix) {
        String domainOutputJavaDir = stagingRoot.resolve(moduleConfig.targetModule())
                .resolve("src/main/java").toString();
        String controllerOutputJavaDir = stagingRoot.resolve(moduleConfig.controllerModule())
                .resolve("src/main/java").toString();
        String outputMapperDir = stagingRoot.resolve(moduleConfig.targetModule())
                .resolve("src/main/resources/mapper")
                .resolve(moduleConfig.businessModule())
                .toString();
        String domainPackagePrefix = moduleConfig.domainModule().isBlank()
                ? moduleConfig.businessModule()
                : moduleConfig.businessModule() + "." + moduleConfig.domainModule();
        String modulePackage = moduleConfig.basePackage() + "." + domainPackagePrefix;
        String controllerPackage = "controller." + moduleConfig.businessModule();
        String controllerOutputDir = Path.of(controllerOutputJavaDir)
                .resolve(moduleConfig.basePackage().replace('.', '/'))
                .resolve(controllerPackage.replace('.', '/'))
                .toString();

        FastAutoGenerator.create(dataSourceConfig)
                .globalConfig(builder -> builder
                        .outputDir(domainOutputJavaDir)
                        .author(AUTHOR)
                        .disableOpenDir()
                        .dateType(DateType.TIME_PACK)
                        .commentDate(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())))
                .packageConfig(builder -> {
                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    pathInfo.put(OutputFile.xml, outputMapperDir);
                    pathInfo.put(OutputFile.controller, controllerOutputDir);
                    builder.parent(moduleConfig.basePackage())
                            .moduleName("")
                            .entity(domainPackagePrefix + ".entity")
                            .service(domainPackagePrefix + ".service")
                            .serviceImpl(domainPackagePrefix + ".service.impl")
                            .mapper(domainPackagePrefix + ".mapper")
                            .xml("mapper")
                            .controller(controllerPackage)
                            .pathInfo(pathInfo);
                })
                .strategyConfig(builder -> builder
                        .addInclude(tableNames)
                        .addTablePrefix(parseTablePrefixes(tablePrefix))
                        .entityBuilder()
                        .enableFileOverride()
                        .superClass(BaseEntity.class)
                        .addSuperEntityColumns(BASE_ENTITY_COLUMNS)
                        .disableSerialVersionUID()
                        .enableLombok(new ClassAnnotationAttributes("@Data", "lombok.Data"))
                        .enableTableFieldAnnotation()
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .javaTemplate("/templates/mybatis-plus/entity.java.vm")
                        .formatFileName("%s")
                        .mapperBuilder()
                        .enableFileOverride()
                        .superClass(BaseMapperX.class)
                        .mapperAnnotation(Mapper.class)
                        .mapperTemplate("/templates/mybatis-plus/mapper.java.vm")
                        .mapperXmlTemplate("/templates/mybatis-plus/mapper.xml.vm")
                        .formatMapperFileName("%sMapper")
                        .formatXmlFileName("%sMapper")
                        .serviceBuilder()
                        .enableFileOverride()
                        .serviceTemplate("/templates/mybatis-plus/service.java.vm")
                        .serviceImplTemplate("/templates/mybatis-plus/serviceImpl.java.vm")
                        .formatServiceFileName("I%sService")
                        .formatServiceImplFileName("%sServiceImpl")
                        .controllerBuilder()
                        .enableFileOverride()
                        .template("/templates/mybatis-plus/controller.java.vm")
                        .enableRestStyle()
                        .superClass(BaseController.class)
                        .formatFileName("%sController"))
                .injectionConfig(consumer -> {
                    Map<String, Object> customMap = new HashMap<>();
                    customMap.put("projectName", PROJECT_NAME);
                    customMap.put("basePackage", moduleConfig.basePackage());
                    customMap.put("moduleRoute", moduleConfig.routePrefix());
                    customMap.put("dtoPackage", modulePackage + ".dto");
                    customMap.put("voPackage", modulePackage + ".vo");
                    customMap.put("commonPackage", moduleConfig.basePackage() + ".core");
                    consumer.customMap(customMap);
                    consumer.customFile(buildCustomFiles(domainPackagePrefix));
                })
                .templateEngine(new VitaVelocityTemplateEngine())
                .execute();
    }

    private static List<CustomFile> buildCustomFiles(String domainPackagePrefix) {
        String dtoPackage = domainPackagePrefix + ".dto";
        String voPackage = domainPackagePrefix + ".vo";
        List<CustomFile> customFiles = new ArrayList<>();
        customFiles.add(customFile(dtoPackage, "CreateDto.java", "/templates/mybatis-plus/dto/createDto.java.vm"));
        customFiles.add(customFile(dtoPackage, "UpdateDto.java", "/templates/mybatis-plus/dto/updateDto.java.vm"));
        customFiles.add(customFile(dtoPackage, "DeletedDto.java", "/templates/mybatis-plus/dto/deletedDto.java.vm"));
        customFiles.add(customFile(dtoPackage, "SearchDto.java", "/templates/mybatis-plus/dto/searchDto.java.vm"));
        customFiles.add(customFile(voPackage, "DetailVo.java", "/templates/mybatis-plus/vo/detailVo.java.vm"));
        customFiles.add(customFile(voPackage, "ListVo.java", "/templates/mybatis-plus/vo/listVo.java.vm"));
        customFiles.add(customFile(voPackage, "PageVo.java", "/templates/mybatis-plus/vo/pageVo.java.vm"));
        return customFiles;
    }

    private static CustomFile customFile(String packageName, String fileName, String templatePath) {
        return new CustomFile.Builder()
                .packageName(packageName)
                .fileName(fileName)
                .templatePath(templatePath)
                .enableFileOverride()
                .build();
    }

    private static String[] parseTablePrefixes(String tablePrefix) {
        return Arrays.stream(tablePrefix.split(","))
                .map(String::trim)
                .filter(prefix -> !prefix.isEmpty())
                .toArray(String[]::new);
    }

    private static void publishGeneratedFiles(Path stagingRoot, Path repositoryRoot,
                                              boolean overwriteExistingFiles) throws IOException {
        List<Path> stagedFiles;
        try (var paths = Files.walk(stagingRoot)) {
            stagedFiles = paths.filter(Files::isRegularFile).sorted().toList();
        }
        if (stagedFiles.isEmpty()) {
            throw new IllegalStateException("未生成任何文件，请检查表名和数据库连接配置");
        }

        List<Path> targetFiles = stagedFiles.stream()
                .map(stagedFile -> repositoryRoot.resolve(stagingRoot.relativize(stagedFile)).normalize())
                .toList();
        List<Path> conflicts = targetFiles.stream().filter(Files::exists).toList();
        if (!overwriteExistingFiles && !conflicts.isEmpty()) {
            String conflictList = conflicts.stream()
                    .map(repositoryRoot::relativize)
                    .map(Path::toString)
                    .sorted()
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse("");
            throw new IllegalStateException(
                    "目标文件已存在，生成已终止。确认需要整体覆盖后再将 OVERWRITE_EXISTING_FILES 设为 true："
                            + System.lineSeparator() + conflictList
            );
        }

        for (int index = 0; index < stagedFiles.size(); index++) {
            Path targetFile = targetFiles.get(index);
            Files.createDirectories(targetFile.getParent());
            if (overwriteExistingFiles) {
                Files.copy(stagedFiles.get(index), targetFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(stagedFiles.get(index), targetFile);
            }
        }
    }

    private static void validateConfiguration(ModuleConfig moduleConfig) {
        if (DB_HOST.isBlank() || DB_USER.isBlank() || DB_NAME.isBlank()) {
            throw new IllegalStateException("数据库地址、用户名和数据库名不能为空");
        }
        if (DB_PASSWORD.isBlank()) {
            throw new IllegalStateException("请仅在本地临时填写 CodeGenerator.DB_PASSWORD，禁止提交真实密码");
        }
        if (DB_PORT == null || DB_PORT < 1 || DB_PORT > 65535) {
            throw new IllegalStateException("数据库端口必须在 1 到 65535 之间");
        }
        if (TABLE_NAMES.length == 0) {
            throw new IllegalStateException("至少配置一张待生成表");
        }
        for (String tableName : TABLE_NAMES) {
            if (tableName == null || !TABLE_PATTERN.matcher(tableName).matches()) {
                throw new IllegalStateException("表名只允许小写字母、数字和下划线：" + tableName);
            }
        }
        validateGeneratedTables();
        moduleConfig.validate();
    }

    private static void validateGeneratedTables() {
        List<String> invalidTables = Arrays.stream(TABLE_NAMES)
                .filter(tableName -> WARM_FLOW_ENGINE_TABLES.contains(tableName)
                        || AUTH_INTERNAL_TABLES.contains(tableName))
                .toList();
        if (!invalidTables.isEmpty()) {
            throw new IllegalArgumentException("禁止为引擎表或认证内部表生成通用 CRUD：" + invalidTables);
        }
    }

    private static void validateRepositoryRoot(Path repositoryRoot, ModuleConfig moduleConfig) {
        if (!Files.isRegularFile(repositoryRoot.resolve("pom.xml"))) {
            throw new IllegalStateException("请在 be-vita 仓库根目录执行代码生成器");
        }
        if (!Files.isDirectory(repositoryRoot.resolve(moduleConfig.targetModule()))) {
            throw new IllegalStateException("目标模块不存在：" + moduleConfig.targetModule());
        }
        if (!Files.isDirectory(repositoryRoot.resolve(moduleConfig.controllerModule()))) {
            throw new IllegalStateException("Controller 模块不存在：" + moduleConfig.controllerModule());
        }
    }

    private static String normalizeRoutePrefix(String routePrefix, String fallback) {
        String normalized = routePrefix == null ? "" : routePrefix.trim();
        if (normalized.isEmpty()) {
            normalized = fallback;
        }
        normalized = normalized.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record ModuleConfig(String targetModule, String controllerModule, String businessModule,
                                String domainModule, String basePackage, String routePrefix) {

        private static ModuleConfig current() {
            return new ModuleConfig(
                    TARGET_MODULE,
                    CONTROLLER_MODULE,
                    BUSINESS_MODULE,
                    DOMAIN_MODULE,
                    BASE_PACKAGE,
                    normalizeRoutePrefix(ROUTE_PREFIX, BUSINESS_MODULE)
            );
        }

        private void validate() {
            validateModuleName(targetModule, "目标模块");
            validateModuleName(controllerModule, "Controller 模块");
            validatePackageName(basePackage, false, "基础包");
            validatePackageName(businessModule, false, "业务模块包");
            validatePackageName(domainModule, true, "领域模块包");
            if (routePrefix.isBlank() || routePrefix.contains("..")) {
                throw new IllegalStateException("路由前缀不合法：" + routePrefix);
            }
        }

        private static void validateModuleName(String value, String label) {
            if (value == null || !MODULE_PATTERN.matcher(value).matches()) {
                throw new IllegalStateException(label + "不合法：" + value);
            }
        }

        private static void validatePackageName(String value, boolean allowBlank, String label) {
            if (allowBlank && (value == null || value.isBlank())) {
                return;
            }
            if (value == null || Arrays.stream(value.split("\\."))
                    .anyMatch(segment -> !PACKAGE_SEGMENT_PATTERN.matcher(segment).matches())) {
                throw new IllegalStateException(label + "必须由小写包名组成：" + value);
            }
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.ROOT,
                    "targetModule=%s, controllerModule=%s, businessModule=%s, domainModule=%s, "
                            + "basePackage=%s, routePrefix=%s, overwrite=%s",
                    targetModule, controllerModule, businessModule, domainModule, basePackage, routePrefix,
                    OVERWRITE_EXISTING_FILES
            );
        }
    }
}
