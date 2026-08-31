package com.examensw1.umlcollab.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " no encontrado: " + id);
    }
}
