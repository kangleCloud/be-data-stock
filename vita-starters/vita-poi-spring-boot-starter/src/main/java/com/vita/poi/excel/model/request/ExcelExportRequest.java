package com.vita.poi.excel.model.request;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.OutputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * Excel 导出请求。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelExportRequest<T> {

    private OutputStream outputStream;

    private String fileName;

    private Class<T> headClass;

    private String sheetName;

    private Collection<T> data;

    private Boolean needHead;

    private Integer relativeHeadRowIndex;

    private Set<Integer> includeColumnIndexes = new LinkedHashSet<>();

    private Set<String> includeColumnFieldNames = new LinkedHashSet<>();

    private Set<Integer> excludeColumnIndexes = new LinkedHashSet<>();

    private Set<String> excludeColumnFieldNames = new LinkedHashSet<>();

    private List<Converter<?>> converters = new ArrayList<>();

    private List<WriteHandler> writeHandlers = new ArrayList<>();

    private Consumer<ExcelWriterBuilder> writeWorkbookCustomizer;

    private Consumer<ExcelWriterSheetBuilder> writeSheetCustomizer;
}
