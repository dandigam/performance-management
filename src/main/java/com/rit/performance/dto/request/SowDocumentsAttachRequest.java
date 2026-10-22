package com.rit.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SowDocumentsAttachRequest {
    @NotNull(message = "documentList is required")
    private List<@NotNull @Valid SowDocumentRequest> documentList;
}
