package com.rit.performance.dto.report;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ReportQueryRequestJsonTest {

    @Test
    void deserializesUiPayloadWithStringLookupIds() throws Exception {
        String json = """
                {
                  "columns": ["employeeNumber", "employeeName", "departmentName", "designationName"],
                  "filters": [
                    {"field": "designationId", "operator": "EQUALS", "value": "21"},
                    {"field": "departmentId", "operator": "EQUALS", "value": "48"}
                  ],
                  "sort": [],
                  "page": 0,
                  "size": 25
                }
                """;

        ReportQueryRequest request = JsonMapper.builder().build()
                .readValue(json, ReportQueryRequest.class);

        assertThat(request.filters()).hasSize(2);
        assertThat(request.filters().get(0).value().asText()).isEqualTo("21");
        assertThat(request.filters().get(1).value().asText()).isEqualTo("48");
    }
}
