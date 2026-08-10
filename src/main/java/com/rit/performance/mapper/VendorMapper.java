package com.rit.performance.mapper;

import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.VendorResponse;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Vendor;

import java.util.Comparator;

public final class VendorMapper {
    private VendorMapper() {
    }

    public static VendorResponse toResponse(Vendor vendor) {
        return VendorResponse.builder()
                .id(vendor.getId())
                .vendorCode(vendor.getVendorCode())
                .companyName(vendor.getCompanyName())
                .primaryContact(vendor.getPrimaryContact())
                .contactEmail(vendor.getContactEmail())
                .phoneNumber(vendor.getPhoneNumber())
                .paymentTerms(vendor.getPaymentTerms())
                .currency(vendor.getCurrency())
                .status(vendor.getStatus())
                .address(vendor.getAddress())
                .documentList(vendor.getDocuments().stream()
                        .sorted(Comparator.comparing(
                                Document::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(VendorMapper::toDocumentResponse)
                        .toList())
                .createdDate(vendor.getCreatedDate())
                .updatedDate(vendor.getUpdatedDate())
                .build();
    }

    private static DocumentResponse toDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileType(document.getFileType())
                .documentType(document.getDocumentType())
                .fileUrl(document.getFileUrl())
                .module(document.getModule())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
