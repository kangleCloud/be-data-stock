package com.vita.poi.excel.model.request;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.vita.poi.excel.processor.ExcelPageProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Excel 分页导出请求。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelPagedExportRequest<T> {

    private OutputStream outputStream;

    private String fileName;

    private Class<T> headClass;

    private String sheetName;

    private Integer pageSize;

    private ExcelPageProvider<T> pageProvider;

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
