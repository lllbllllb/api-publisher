package com.lllbllllb.plugins.publisher.type;

public class OpenApiTypeHandler extends InfoVersionTypeHandler {

    public static final String OPENAPI = "openapi";

    @Override
    public String folderName() {
        return OPENAPI;
    }

    @Override
    public String rootKey() {
        return OPENAPI;
    }
}
