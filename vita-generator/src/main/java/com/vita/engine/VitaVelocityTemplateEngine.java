package com.vita.engine;

import com.baomidou.mybatisplus.generator.config.ConstVal;
import com.baomidou.mybatisplus.generator.config.TemplateLoadWay;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.engine.AbstractTemplateEngine;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import java.io.*;
import java.util.Map;
import java.util.Properties;

/**
 * 使用 Velocity 2.4 配置键的模板引擎，避免 MyBatis-Plus 默认实现触发废弃配置警告。
 */
public final class VitaVelocityTemplateEngine extends AbstractTemplateEngine {

    private VelocityEngine velocityEngine;

    @Override
    public VitaVelocityTemplateEngine init(ConfigBuilder configBuilder) {
        if (velocityEngine != null) {
            return this;
        }
        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.INPUT_ENCODING, ConstVal.UTF8);
        if (configBuilder.getTemplateLoadWay().isFile()) {
            properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
            properties.setProperty(
                    "resource.loader.classpath.class",
                    ClasspathResourceLoader.class.getName()
            );
        } else {
            properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, TemplateLoadWay.STRING.getValue());
            properties.setProperty(
                    "resource.loader.string.class",
                    StringResourceLoader.class.getName()
            );
        }
        velocityEngine = new VelocityEngine(properties);
        return this;
    }

    @Override
    public String writer(Map<String, Object> objectMap, String templateName, String templateString) {
        StringWriter writer = new StringWriter();
        velocityEngine.evaluate(new VelocityContext(objectMap), writer, templateName, templateString);
        return writer.toString();
    }

    @Override
    public void writer(Map<String, Object> objectMap, String templatePath, File outputFile) throws Exception {
        Template template = velocityEngine.getTemplate(templatePath, ConstVal.UTF8);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile);
             OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, ConstVal.UTF8);
             BufferedWriter writer = new BufferedWriter(outputWriter)) {
            template.merge(new VelocityContext(objectMap), writer);
        }
        LOGGER.debug("模板:{}; 文件:{}", templatePath, outputFile);
    }

    @Override
    public String templateFilePath(String filePath) {
        return filePath.endsWith(".vm") ? filePath : filePath + ".vm";
    }
}
