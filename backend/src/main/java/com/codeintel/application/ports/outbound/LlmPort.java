package com.codeintel.application.ports.outbound;

public interface LlmPort {
    String generateRefactor(String context);
}
