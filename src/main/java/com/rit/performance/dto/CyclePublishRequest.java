package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CyclePublishRequest {
    public enum Mode { ALL, SELECTED, EXCLUDE }
    @NotNull
    private Mode mode = Mode.ALL;
    private List<@NotNull @Positive Long> employeeList = List.of();
}
