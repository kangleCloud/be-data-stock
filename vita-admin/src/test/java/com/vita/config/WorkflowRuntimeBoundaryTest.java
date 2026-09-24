package com.vita.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vita.workflow.constant.WorkflowConstants;
import com.vita.workflow.controller.WorkflowReimbursementAdminController;
import com.vita.workflow.reimbursement.dto.ReimbursementSearchDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.config
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 工作流运行时代码、JOIN 和数据库边界回归测试
 * @Version: 1.0
 */
class WorkflowRuntimeBoundaryTest {

    private static final List<String> ENGINE_ENTITY_NAMES = List.of(
            "FlowDefinition", "FlowNode", "FlowSkip", "FlowInstance",
            "FlowTask", "FlowHisTask", "FlowUser", "FlowForm"
    );

    @Test
    void generatedEngineCrudShouldBeRemovedAndBlocked() throws IOException {
        Path repository = resolveRepositoryRoot();
        Path workflowJava = repository.resolve("vita-workflow/src/main/java/com/vita/workflow");
        for (String entityName : ENGINE_ENTITY_NAMES) {
            assertThat(workflowJava.resolve("controller/" + entityName + "Controller.java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/entity/" + entityName + ".java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/service/I" + entityName + "Service.java")).doesNotExist();
            assertThat(workflowJava.resolve("flow/mapper/" + entityName + "Mapper.java")).doesNotExist();
        }

        String generator = Files.readString(repository.resolve(
                "vita-generator/src/main/java/com/vita/config/CodeGenerator.java"));
        assertThat(generator)
                .contains("WARM_FLOW_ENGINE_TABLES", "validateGeneratedTables()")
                .doesNotContain(
                        "TABLE_NAMES = {\"flow_definition",
                        "TABLE_NAMES = {\"flow_node",
                        "TABLE_NAMES = {\"flow_skip",
                        "TABLE_NAMES = {\"flow_instance",
                        "TABLE_NAMES = {\"flow_task",
                        "TABLE_NAMES = {\"flow_his_task",
                        "TABLE_NAMES = {\"flow_user",
                        "TABLE_NAMES = {\"flow_form");
    }

    @Test
    void genericControllersShouldBeAggregatedInWorkflowModule() throws IOException {
        Path repository = resolveRepositoryRoot();
        Path workflowController = repository.resolve(
                "vita-workflow/src/main/java/com/vita/workflow/controller");
        Path adminController = repository.resolve(
                "vita-admin/src/main/java/com/vita/controller/workflow");

        List<String> controllerNames = List.of(
                "WorkflowDefinitionController.java",
                "WorkflowInstanceController.java",
                "WorkflowTaskController.java",
                "WorkflowCategoryController.java",
                "WorkflowAssigneeRuleController.java",
                "WorkflowReimbursementController.java",
                "WorkflowReimbursementAdminController.java");
        for (String controllerName : controllerNames) {
            Path controller = workflowController.resolve(controllerName);
            assertThat(controller).exists();
            assertThat(Files.readString(controller))
                    .doesNotContain(
                            ".mapper.",
                            "org.dromara.warm.flow.core.service",
                            "TransactionTemplate",
                            "PlatformTransactionManager",
                            "WorkflowAfterCommitEventPublisher");
        }

        if (Files.isDirectory(adminController)) {
            try (var paths = Files.list(adminController)) {
                assertThat(paths.filter(path -> path.toString().endsWith(".java")).toList()).isEmpty();
            }
        }
        assertThat(workflowController.resolve("api")).doesNotExist();
        assertThat(workflowController.resolve("impl")).doesNotExist();
        assertThat(workflowController.resolve("WorkflowControllerConfiguration.java")).doesNotExist();
        assertThat(repository.resolve(
                "vita-workflow/src/main/resources/META-INF/spring/"
                        + "org.springframework.boot.autoconfigure.AutoConfiguration.imports"))
                .doesNotExist();
    }

    @Test
    void reimbursementManagementMethodsShouldDeclarePermissionDirectly() throws NoSuchMethodException {
        assertPermission("page", WorkflowConstants.WORKFLOW_REIMBURSEMENT_VIEW_PERMISSION,
                ReimbursementSearchDto.class);
        assertPermission("detail", WorkflowConstants.WORKFLOW_REIMBURSEMENT_VIEW_PERMISSION,
                Long.class);
        assertPermission("resync", WorkflowConstants.WORKFLOW_REIMBURSEMENT_RESYNC_PERMISSION,
                String.class);
    }

    @Test
    void joinShouldOnlyExistInWorkflowReadOnlyMapper() throws IOException {
        Path repository = resolveRepositoryRoot();
        List<Path> joinFiles = new ArrayList<>();
        for (Path scanRoot : sourceAndSqlRoots(repository)) {
            try (var paths = Files.walk(scanRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".xml") || path.toString().endsWith(".sql"))
                        .filter(this::containsJoin)
                        .forEach(joinFiles::add);
            }
        }

        assertThat(joinFiles).allMatch(path -> repository.relativize(path).toString()
                .equals("vita-workflow/src/main/resources/mapper/workflow/WorkflowRuntimeQueryMapper.xml"));

        String mapperXml = Files.readString(repository.resolve(
                "vita-workflow/src/main/resources/mapper/workflow/WorkflowRuntimeQueryMapper.xml"));
        assertThat(mapperXml.toLowerCase(Locale.ROOT))
                .contains(" join ")
                .doesNotContain("<insert", "<update", "<delete");
    }

    @Test
    void workflowUpgradeSqlShouldBeConsolidatedAndRespectDatabaseBoundaries() throws IOException {
        Path repository = resolveRepositoryRoot();
        String initSql = Files.readString(repository.resolve("sql/init/workflow.sql"));
        String warmFlowSql = Files.readString(repository.resolve("sql/upgrade/warm_flow_upgrade.sql"));
        String vitaWorkflowSql = Files.readString(repository.resolve("sql/upgrade/vita_workflow_upgrade.sql"));

        assertThat(initSql).doesNotContain(
                "uk_flow_instance_business_id",
                "idx_flow_instance_create_by_update",
                "idx_flow_task_instance",
                "idx_flow_his_task_approver_update"
        );
        assertThat(warmFlowSql)
                .doesNotContain("ALTER TABLE", "information_schema.statistics", "SIGNAL SQLSTATE")
                .contains("DROP TABLE IF EXISTS `flow_definition`", "DROP TABLE IF EXISTS `flow_form`");
        assertThat(vitaWorkflowSql)
                .doesNotContain(
                        " JOIN ",
                        "ALTER TABLE",
                        "information_schema.statistics",
                        "DROP TABLE IF EXISTS `workflow_category`",
                        "DROP TABLE IF EXISTS `sys_role`",
                        "DROP TABLE IF EXISTS `sys_permission`",
                        "DROP TABLE IF EXISTS `sys_role_permission`"
                )
                .contains(
                        "CREATE TABLE IF NOT EXISTS `workflow_category`",
                        "@next_role_id",
                        "@next_permission_id",
                        "@next_role_permission_id",
                        "WHERE NOT EXISTS",
                        "FINANCE_APPROVER",
                        "GENERAL_AFFAIRS_APPROVER",
                        "GENERAL_MANAGER_APPROVER",
                        "SUB_ADMIN",
                        "system:workflow:import",
                        "system:workflow:definition:view",
                        "system:workflow:definition:manage",
                        "system:workflow:instance:view",
                        "system:workflow:instance:manage",
                        "system:workflow:task:view",
                        "system:workflow:task:manage",
                        "system:workflow:category:view",
                        "system:workflow:category:manage",
                        "system:workflow:assignee-rule:view",
                        "system:workflow:assignee-rule:manage",
                        "CREATE TABLE IF NOT EXISTS `workflow_reimbursement`",
                        "CREATE TABLE IF NOT EXISTS `workflow_instance_metadata`",
                        "CREATE TABLE IF NOT EXISTS `workflow_task_copy`",
                        "CREATE TABLE IF NOT EXISTS `workflow_assignee_rule`",
                        "system:workflow:reimbursement:view",
                        "system:workflow:reimbursement:resync"
                );
        assertThat(vitaWorkflowSql).doesNotContain(
                "DROP TABLE IF EXISTS `workflow_reimbursement`",
                "DROP TABLE IF EXISTS `workflow_instance_metadata`",
                "DROP TABLE IF EXISTS `workflow_task_copy`",
                "DROP TABLE IF EXISTS `workflow_assignee_rule`");
        assertThat(initSql).contains(
                "CREATE TABLE `workflow_reimbursement`",
                "CREATE TABLE IF NOT EXISTS `workflow_instance_metadata`",
                "CREATE TABLE IF NOT EXISTS `workflow_task_copy`",
                "CREATE TABLE IF NOT EXISTS `workflow_assignee_rule`");

        try (var paths = Files.list(repository.resolve("sql/upgrade"))) {
            List<String> workflowScripts = paths
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .filter(name -> name.contains("workflow") || name.contains("warm_flow"))
                    .sorted()
                    .toList();
            assertThat(workflowScripts).containsExactly("vita_workflow_upgrade.sql", "warm_flow_upgrade.sql");
        }
    }

    @Test
    void workflowWithdrawalShouldTerminateInstanceAndProtectHandlerVariables() throws IOException {
        String runtimeService = Files.readString(resolveRepositoryRoot().resolve(
                "vita-workflow/src/main/java/com/vita/workflow/runtime/service/impl/WorkflowRuntimeServiceImpl.java"));

        assertThat(runtimeService)
                .contains("taskService.terminationByInsId", "PROTECTED_TASK_VARIABLES")
                .doesNotContain("taskService.revoke(");
    }

    @Test
    void advancedOperationsShouldUseWarmFlowServicesWithoutEngineWriteMappers() throws IOException {
        Path repository = resolveRepositoryRoot();
        String runtimeService = Files.readString(repository.resolve(
                "vita-workflow/src/main/java/com/vita/workflow/runtime/service/impl/WorkflowRuntimeServiceImpl.java"));

        assertThat(runtimeService).contains(
                "taskService.updateHandler",
                "taskService.termination",
                "nodeService.getNextNodeList");
        assertThat(repository.resolve(
                "vita-workflow/src/main/java/com/vita/workflow/runtime/mapper/FlowTaskMapper.java"))
                .doesNotExist();
        assertThat(repository.resolve(
                "vita-workflow/src/main/java/com/vita/workflow/runtime/mapper/FlowUserMapper.java"))
                .doesNotExist();
    }

    private boolean containsJoin(Path path) {
        try {
            String normalized = Files.readString(path).toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
            return normalized.matches(".*\\b(left |right |inner |outer |cross )?join\\b.*");
        } catch (IOException ex) {
            throw new IllegalStateException("读取文件失败：" + path, ex);
        }
    }

    private void assertPermission(String methodName, String permission,
                                  Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = WorkflowReimbursementAdminController.class.getMethod(methodName, parameterTypes);
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertThat(annotation).as("WorkflowReimbursementAdminController#" + methodName).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private List<Path> sourceAndSqlRoots(Path repository) throws IOException {
        List<Path> roots = new ArrayList<>();
        try (var paths = Files.list(repository)) {
            paths.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("vita-"))
                    .map(path -> path.resolve("src"))
                    .filter(Files::isDirectory)
                    .forEach(roots::add);
        }
        roots.add(repository.resolve("sql"));
        return roots;
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("vita-workflow"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位仓库根目录");
    }
}
