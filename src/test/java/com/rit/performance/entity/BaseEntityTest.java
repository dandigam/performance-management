package com.rit.performance.entity;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {

    @Test
    void createdFieldsUseSpringDataAuditing() throws NoSuchFieldException {
        assertAnnotated("createdBy", CreatedBy.class);
        assertAnnotated("createdOn", CreatedDate.class);
    }

    @Test
    void updatedFieldsUseSpringDataAuditing() throws NoSuchFieldException {
        assertAnnotated("updatedBy", LastModifiedBy.class);
        assertAnnotated("updatedOn", LastModifiedDate.class);
    }

    private void assertAnnotated(
            String fieldName, Class<? extends Annotation> annotationType)
            throws NoSuchFieldException {
        assertTrue(BaseEntity.class.getDeclaredField(fieldName).isAnnotationPresent(annotationType));
    }
}
