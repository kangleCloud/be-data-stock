package com.vita.poi.excel.model.request;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 导入请求。
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelImportRequest<T> {

    private InputStream inputStream;

    private String fileName;

    private Class<T> headClass;

    private Integer sheetNo;

    private String sheetName;

    private Integer headRowNumber;

    private Integer batchSize;

    private List<Converter<?>> converters = new ArrayList<>();

    private Consumer<ExcelReaderBuilder> readWorkbookCustomizer;

    private Consumer<ExcelReaderSheetBuilder> readSheetCustomizer;
}
