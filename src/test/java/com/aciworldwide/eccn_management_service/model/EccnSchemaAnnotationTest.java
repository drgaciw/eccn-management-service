package com.aciworldwide.eccn_management_service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit test that locks in the non-deprecated {@code requiredMode} usage on
 * {@link Schema} annotations for {@link Eccn} required fields.
 * <p>
 * The {@code required} boolean attribute on {@code @Schema} is deprecated in
 * swagger-annotations 2.2.x in favour of {@code requiredMode}.
 */
class EccnSchemaAnnotationTest {

    private Schema getSchemaAnnotation(String fieldName) throws NoSuchFieldException {
        Field field = Eccn.class.getDeclaredField(fieldName);
        Schema schema = field.getAnnotation(Schema.class);
        assertNotNull(schema, "@Schema annotation missing on field: " + fieldName);
        return schema;
    }

    @Test
    @DisplayName("code field uses requiredMode=REQUIRED not deprecated required=true")
    void codeField_usesRequiredMode() throws NoSuchFieldException {
        Schema schema = getSchemaAnnotation("code");
        assertEquals(Schema.RequiredMode.REQUIRED, schema.requiredMode());
    }

    @Test
    @DisplayName("description field uses requiredMode=REQUIRED not deprecated required=true")
    void descriptionField_usesRequiredMode() throws NoSuchFieldException {
        Schema schema = getSchemaAnnotation("description");
        assertEquals(Schema.RequiredMode.REQUIRED, schema.requiredMode());
    }

    @Test
    @DisplayName("category field uses requiredMode=REQUIRED not deprecated required=true")
    void categoryField_usesRequiredMode() throws NoSuchFieldException {
        Schema schema = getSchemaAnnotation("category");
        assertEquals(Schema.RequiredMode.REQUIRED, schema.requiredMode());
    }

    @Test
    @DisplayName("licenseRequired field uses requiredMode=REQUIRED not deprecated required=true")
    void licenseRequiredField_usesRequiredMode() throws NoSuchFieldException {
        Schema schema = getSchemaAnnotation("licenseRequired");
        assertEquals(Schema.RequiredMode.REQUIRED, schema.requiredMode());
    }
}
