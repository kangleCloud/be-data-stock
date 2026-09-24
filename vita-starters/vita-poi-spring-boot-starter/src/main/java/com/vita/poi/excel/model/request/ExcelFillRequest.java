package com.vita.poi.excel.model.request;

import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 模板填充请求。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelFillRequest {

    private InputStream templateInputStream;

    private OutputStream outputStream;

    private Integer sheetNo;

    private String sheetName;

    private Object objectData;

    private Collection<?> listData;

    private List<WriteHandler> writeHandlers = new ArrayList<>();

    private FillConfig fillConfig;

    private Consumer<ExcelWriterBuilder> writeCustomizer;
}
