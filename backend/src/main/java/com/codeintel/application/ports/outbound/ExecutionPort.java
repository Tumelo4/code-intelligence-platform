package com.codeintel.application.ports.outbound;

import java.util.List;

public interface ExecutionPort {
    int execute(String sandboxId, List<String> command);
}
