package com.vita.poi.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.vita.poi.excel.model.context.ExcelBatchContext;
import com.vita.poi.excel.model.context.ExcelRowContext;
import com.vita.poi.excel.model.request.ExcelImportRequest;
import com.vita.poi.excel.model.result.ExcelImportError;
import com.vita.poi.excel.model.result.ExcelImportResult;
import com.vita.poi.excel.processor.ExcelImportProcessor;
import com.vita.poi.excel.property.PoiExcelProperty;
import com.vita.poi.excel.support.ExcelReadSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 默认导入监听器。
 */
public class DefaultExcelImportListener<T> extends AnalysisEventListener<T> {

    private final ExcelImportRequest<T> request;
    private final ExcelImportProcessor<T> processor;
    private final PoiExcelProperty property;
    private final ExcelReadSupport readSupport;
    private final int batchSize;
    private final List<PendingRow<T>> batchBuffer = new ArrayList<>();
    private final List<ExcelImportError> errors = new ArrayList<>();

    private int totalRows;
    private int successRows;
    private int failedRows;
    private boolean truncated;
    private int batchNo = 1;
    private Integer lastVisitedRowIndex;

    public DefaultExcelImportListener(ExcelImportRequest<T> request,
                                      ExcelImportProcessor<T> processor,
                                      PoiExcelProperty property,
                                      ExcelReadSupport readSupport) {
        this.request = request;
        this.processor = processor;
        this.property = property;
        this.readSupport = readSupport;
        this.batchSize = readSupport.resolveBatchSize(request, property);
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        Integer rowIndex = readSupport.resolveRowIndex(context);
        markVisitedRow(rowIndex);
        Map<Integer, String> rawData = readSupport.extractRawData(context, property.isTrimCellValue());
        ExcelRowContext rowContext = new ExcelRowContext()
                .setFileName(request.getFileName())
                .setSheetNo(readSupport.resolveSheetNo(context))
                .setSheetName(readSupport.resolveSheetName(context))
                .setRowIndex(rowIndex)
                .setRawData(rawData);

        List<String> validateMessages;
        try {
            validateMessages = processor.validate(data, rowContext);
        } catch (Exception exception) {
            failedRows++;
            addError(rowIndex, null, readSupport.resolveErrorMessage(exception), rawData);
            return;
        }

        List<String> normalizedMessages = normalizeMessages(validateMessages);
        if (!normalizedMessages.isEmpty()) {
            failedRows++;
            normalizedMessages.forEach(message -> addError(rowIndex, null, message, rawData));
            return;
        }

        batchBuffer.add(new PendingRow<>(data, rowContext));
        if (batchBuffer.size() >= batchSize) {
            flushBatch();
        }
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        Integer rowIndex = readSupport.resolveRowIndex(exception, context);
        if (rowIndex == null) {
            throw exception;
        }
        markVisitedRow(rowIndex);
        failedRows++;
        addError(
                rowIndex,
                readSupport.resolveFieldName(exception),
                readSupport.resolveErrorMessage(exception),
                readSupport.extractRawData(context, property.isTrimCellValue())
        );
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flushBatch();
    }

    public ExcelImportResult buildResult() {
        return new ExcelImportResult()
                .setTotalRows(totalRows)
                .setSuccessRows(successRows)
                .setFailedRows(failedRows)
                .setTruncated(truncated)
                .setErrors(new ArrayList<>(errors));
    }

    private void flushBatch() {
        if (batchBuffer.isEmpty()) {
            return;
        }

        PendingRow<T> first = batchBuffer.get(0);
        PendingRow<T> last = batchBuffer.get(batchBuffer.size() - 1);
        ExcelBatchContext batchContext = new ExcelBatchContext()
                .setFileName(request.getFileName())
                .setSheetNo(first.context().getSheetNo())
                .setSheetName(first.context().getSheetName())
                .setBatchNo(batchNo)
                .setStartRowIndex(first.context().getRowIndex())
                .setEndRowIndex(last.context().getRowIndex());

        List<T> rows = batchBuffer.stream().map(PendingRow::data).toList();
        try {
            processor.saveBatch(rows, batchContext);
            successRows += batchBuffer.size();
        } catch (Exception exception) {
            failedRows += batchBuffer.size();
            String message = readSupport.resolveErrorMessage(exception);
            batchBuffer.forEach(pendingRow ->
                    addError(pendingRow.context().getRowIndex(), null, message, pendingRow.context().getRawData())
            );
        } finally {
            batchBuffer.clear();
            batchNo++;
        }
    }

    private void markVisitedRow(Integer rowIndex) {
        if (rowIndex == null) {
            return;
        }
        if (!rowIndex.equals(lastVisitedRowIndex)) {
            totalRows++;
            lastVisitedRowIndex = rowIndex;
        }
    }

    private void addError(Integer rowIndex, String fieldName, String message, Map<Integer, String> rawData) {
        if (errors.size() >= property.getMaxErrorRows()) {
            truncated = true;
            return;
        }
        errors.add(new ExcelImportError()
                .setRowIndex(rowIndex)
                .setFieldName(fieldName)
                .setMessage(StringUtils.hasText(message) ? message : "Excel import failed")
                .setRawData(rawData));
    }

    private List<String> normalizeMessages(List<String> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return List.of();
        }
        return messages.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private record PendingRow<T>(T data, ExcelRowContext context) {
    }
}
