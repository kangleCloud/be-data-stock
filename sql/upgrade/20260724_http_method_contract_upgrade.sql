-- 将历史权限方法统一收口到 GET/POST 白名单。
-- 本脚本可重复执行：已是 GET/POST 或为空的记录不会被修改。
UPDATE `sys_permission`
SET `api_method` = 'POST'
WHERE `api_method` IS NOT NULL
  AND UPPER(`api_method`) NOT IN ('GET', 'POST');
