package com.codeintel.infrastructure.inventory;

import java.util.Locale;
import java.util.Map;

/** Deterministic extension-based language classification; no repository code is executed. */
final class LanguageCatalog {
    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("java", "JAVA"), Map.entry("kt", "KOTLIN"), Map.entry("kts", "KOTLIN"),
            Map.entry("scala", "SCALA"), Map.entry("sc", "SCALA"), Map.entry("groovy", "GROOVY"),
            Map.entry("js", "JAVASCRIPT"), Map.entry("jsx", "JAVASCRIPT"),
            Map.entry("mjs", "JAVASCRIPT"), Map.entry("cjs", "JAVASCRIPT"),
            Map.entry("ts", "TYPESCRIPT"), Map.entry("tsx", "TYPESCRIPT"),
            Map.entry("mts", "TYPESCRIPT"), Map.entry("cts", "TYPESCRIPT"),
            Map.entry("vue", "VUE"), Map.entry("svelte", "SVELTE"),
            Map.entry("py", "PYTHON"), Map.entry("pyi", "PYTHON"),
            Map.entry("go", "GO"), Map.entry("rs", "RUST"),
            Map.entry("c", "C"), Map.entry("h", "C"),
            Map.entry("cc", "C++"), Map.entry("cpp", "C++"), Map.entry("cxx", "C++"),
            Map.entry("hpp", "C++"), Map.entry("hh", "C++"), Map.entry("hxx", "C++"),
            Map.entry("cs", "C#"), Map.entry("fs", "F#"), Map.entry("fsx", "F#"),
            Map.entry("vb", "VISUAL_BASIC"), Map.entry("swift", "SWIFT"),
            Map.entry("m", "OBJECTIVE_C"), Map.entry("mm", "OBJECTIVE_C++"),
            Map.entry("dart", "DART"), Map.entry("php", "PHP"),
            Map.entry("rb", "RUBY"), Map.entry("rake", "RUBY"),
            Map.entry("pl", "PERL"), Map.entry("pm", "PERL"),
            Map.entry("ex", "ELIXIR"), Map.entry("exs", "ELIXIR"),
            Map.entry("erl", "ERLANG"), Map.entry("hrl", "ERLANG"),
            Map.entry("clj", "CLOJURE"), Map.entry("cljs", "CLOJURE"),
            Map.entry("cljc", "CLOJURE"), Map.entry("edn", "CLOJURE"),
            Map.entry("ps1", "POWERSHELL"), Map.entry("psm1", "POWERSHELL"),
            Map.entry("tf", "TERRAFORM"), Map.entry("tfvars", "TERRAFORM"),
            Map.entry("sql", "SQL"), Map.entry("sh", "SHELL"),
            Map.entry("bash", "SHELL"), Map.entry("zsh", "SHELL"),
            Map.entry("lua", "LUA"), Map.entry("r", "R"),
            Map.entry("jl", "JULIA"), Map.entry("hs", "HASKELL"),
            Map.entry("elm", "ELM"), Map.entry("ml", "OCAML"),
            Map.entry("re", "REASON"), Map.entry("zig", "ZIG"),
            Map.entry("sol", "SOLIDITY"), Map.entry("apex", "APEX"),
            Map.entry("html", "HTML"), Map.entry("css", "CSS"), Map.entry("scss", "SCSS")
    );
    private static final Map<String, String> FILENAMES = Map.ofEntries(
            Map.entry("dockerfile", "DOCKERFILE"), Map.entry("makefile", "MAKEFILE"),
            Map.entry("rakefile", "RUBY"), Map.entry("gemfile", "RUBY")
    );

    private LanguageCatalog() { }

    static String detect(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        String named = FILENAMES.get(lower);
        if (named != null) return named;
        int dot = lower.lastIndexOf('.');
        return dot < 0 ? null : EXTENSIONS.get(lower.substring(dot + 1));
    }
}
