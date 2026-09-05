package com.company.casehub.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.casehub.export.service.ExcelCellSafety;
import org.junit.jupiter.api.Test;

class ExcelCellSafetyTest {

    @Test
    void nullBecomesEmptyTextAndNormalTextIsUnchanged() {
        assertThat(ExcelCellSafety.text(null)).isEmpty();
        assertThat(ExcelCellSafety.text("normal")).isEqualTo("normal");
    }

    @Test
    void formulaSignificantPrefixesReceiveExactlyOneAsciiApostrophe() {
        assertThat(ExcelCellSafety.text("=SUM(A1:A2)")).isEqualTo("'=SUM(A1:A2)");
        assertThat(ExcelCellSafety.text("+123")).isEqualTo("'+123");
        assertThat(ExcelCellSafety.text("-123")).isEqualTo("'-123");
        assertThat(ExcelCellSafety.text("@command")).isEqualTo("'@command");
    }
}
