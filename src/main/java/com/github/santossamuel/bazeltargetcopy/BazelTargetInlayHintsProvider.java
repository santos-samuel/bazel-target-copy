package com.github.santossamuel.bazeltargetcopy;

import com.intellij.codeInsight.hints.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Registers the inlay hints provider for BUILD.bazel files.
 * Supports both the JetBrains Bazel (EAP) plugin (language: "Starlark")
 * and the Google Bazel for IntelliJ plugin (language: "BUILD").
 * Both registrations point to this same provider class via plugin.xml.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BazelTargetInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    public static final SettingsKey<NoSettings> KEY = new SettingsKey<>("bazel.target.copy");

    @Override
    public @NotNull SettingsKey<NoSettings> getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Bazel Target Copy";
    }

    @Override
    public @NotNull String getDescription() {
        return "Shows a clickable inlay hint to copy the Bazel target label to clipboard";
    }

    @Override
    public @Nullable String getPreviewText() {
        return null;
    }

    @Override
    public @NotNull NoSettings createSettings() {
        return new NoSettings();
    }

    @Override
    public @NotNull InlayHintsCollector getCollectorFor(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull NoSettings settings,
            @NotNull InlayHintsSink sink
    ) {
        return new BazelTargetCollector(editor, file);
    }

    @Override
    public boolean isVisibleInSettings() {
        return true;
    }

    @Override
    public @NotNull ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return changeListener -> new JPanel();
    }
}
