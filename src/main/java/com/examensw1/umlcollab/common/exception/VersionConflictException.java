package com.examensw1.umlcollab.common.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String resource) {
        super(resource + " fue modificado por otra persona. Recarga la información antes de volver a guardar.");
    }
}
