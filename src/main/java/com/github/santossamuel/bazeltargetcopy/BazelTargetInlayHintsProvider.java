package com.github.santossamuel.bazeltargetcopy;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public final class BazelTargetInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    // Matches the opening line of a Bazel rule, e.g.: go_test(
    private static final Pattern RULE_START_PATTERN = Pattern.compile("^\\s*\\w+\\s*\\(\\s*$");

    // Matches: name = "some_target"
    private static final Pattern NAME_PATTERN = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");

    public static final SettingsKey<NoSettings> KEY =
            new SettingsKey<>("bazel.target.copy");

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
    public @Nullable InlayHintsCollector getCollectorFor(
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

    // -------------------------------------------------------------------------

    private static final class BazelTargetCollector implements InlayHintsCollector {

        private final Editor editor;
        private final PsiFile file;

        BazelTargetCollector(Editor editor, PsiFile file) {
            this.editor = editor;
            this.file = file;
        }

        @Override
        public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
            if (element != file) {
                return true;
            }

            String packagePath = resolvePackagePath(file);
            String text = file.getText();
            String[] lines = text.split("\n", -1);

            int offset = 0;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (RULE_START_PATTERN.matcher(line).matches()) {
                    String targetName = findTargetName(lines, i + 1);
                    if (targetName != null) {
                        String label = "//" + packagePath + ":" + targetName;
                        int lineEndOffset = offset + line.length();
                        addHint(sink, lineEndOffset, label);
                    }
                }

                offset += line.length() + 1; // +1 for the newline
            }

            return false;
        }

        private void addHint(InlayHintsSink sink, int offset, String label) {
            PresentationFactory factory = new PresentationFactory(editor);

            InlayPresentation text = factory.smallText("⎘ " + label);
            InlayPresentation padded = factory.inset(text, 8, 0, 0, 0);
            InlayPresentation clickable = factory.onClick(padded, MouseButton.Left, (event, point) -> {
                CopyPasteManager.getInstance().setContents(new StringSelection(label));
                JBPopupFactory.getInstance()
                        .createHtmlTextBalloonBuilder("Copied: <b>" + label + "</b>", MessageType.INFO, null)
                        .setFadeoutTime(2500)
                        .createBalloon()
                        .showInCenterOf(editor.getComponent());
                return null;
            });

            sink.addInlineElement(offset, true, clickable, false);
        }

        private @Nullable String findTargetName(String[] lines, int fromLine) {
            int depth = 1;
            for (int i = fromLine; i < lines.length && depth > 0; i++) {
                String line = lines[i];

                Matcher nameMatcher = NAME_PATTERN.matcher(line);
                if (nameMatcher.find()) {
                    return nameMatcher.group(1);
                }

                for (char c : line.toCharArray()) {
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                }
            }
            return null;
        }

        private String resolvePackagePath(PsiFile file) {
            VirtualFile vFile = file.getVirtualFile();
            if (vFile == null) return "";

            VirtualFile dir = vFile.getParent();
            if (dir == null) return "";

            VirtualFile workspace = findWorkspaceRoot(dir);
            if (workspace == null) return dir.getName();

            String workspacePath = workspace.getPath();
            String dirPath = dir.getPath();

            if (dirPath.startsWith(workspacePath)) {
                String relative = dirPath.substring(workspacePath.length());
                if (relative.startsWith("/")) {
                    relative = relative.substring(1);
                }
                return relative;
            }

            return dir.getName();
        }

        private @Nullable VirtualFile findWorkspaceRoot(VirtualFile dir) {
            VirtualFile current = dir;
            while (current != null) {
                if (current.findChild("WORKSPACE") != null
                        || current.findChild("WORKSPACE.bazel") != null) {
                    return current;
                }
                current = current.getParent();
            }
            return null;
        }
    }
}
