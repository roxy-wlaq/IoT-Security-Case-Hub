package com.company.casehub.export.service;

/** Fixed Excel text-safety contract for user-controlled workbook values. */
public final class ExcelCellSafety {

    private ExcelCellSafety() {
    }

    public static String text(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }
}
