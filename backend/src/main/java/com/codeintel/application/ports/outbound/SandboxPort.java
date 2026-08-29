package com.codeintel.application.ports.outbound;

import java.nio.file.Path;

public interface SandboxPort {
    String create(Path source, boolean readOnly);
}
