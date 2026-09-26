package com.lllbllllb.plugins.publisher.type;

public class AsyncApiTypeHandler extends InfoVersionTypeHandler {

    public static final String ASYNCAPI = "asyncapi";

    @Override
    public String folderName() {
        return ASYNCAPI;
    }

    @Override
    public String rootKey() {
        return ASYNCAPI;
    }
}
