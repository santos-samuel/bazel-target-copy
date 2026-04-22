package com.github.santossamuel.bazeltargetcopy;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans a BUILD.bazel file line by line and registers an inlay hint on the
 * opening line of each Bazel rule. The hint displays the fully-qualified target
 * label (e.g. //path/to/package:target_name) and copies it to the clipboard on click.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BazelTargetCollector implements InlayHintsCollector {

    // Matches the opening line of a Bazel rule, e.g.: go_test(
    private static final Pattern RULE_START_PATTERN = Pattern.compile("^\\s*\\w+\\s*\\(\\s*$");

    // Matches the name attribute inside a rule block, e.g.: name = "my_target"
    private static final Pattern NAME_PATTERN = Pattern.compile("name\\s*=\\s*\"([^\"]+)\"");

    private final Editor editor;
    private final PsiFile file;

    public BazelTargetCollector(Editor editor, PsiFile file) {
        this.editor = editor;
        this.file = file;
    }

    /**
     * Called once per PSI element. We only act on the file root element to avoid
     * scanning the file multiple times.
     */
    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
        if (element != file) {
            // Return true to keep visiting child elements; we bail early for non-root elements.
            return true;
        }

        String packagePath = resolvePackagePath(file);
        String text = file.getText();
        String[] lines = text.split("\n", -1);

        // Walk lines tracking character offset so we can place hints at the right position.
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (RULE_START_PATTERN.matcher(line).matches()) {
                String targetName = findTargetName(lines, i + 1);
                if (targetName != null) {
                    String label = "//" + packagePath + ":" + targetName;
                    // Place the hint at the end of the rule's opening line.
                    int lineEndOffset = offset + line.length();
                    addHint(sink, lineEndOffset, label);
                }
            }

            offset += line.length() + 1; // +1 for the newline character
        }

        // Return false: we handled the file root, no need to visit children.
        return false;
    }

    /**
     * Builds and registers the inlay hint presentation for a single target label.
     * The hint shows a copy icon followed by the label, with a hover effect and
     * a "Copied!" tooltip on click.
     */
    private void addHint(InlayHintsSink sink, int offset, String label) {
        PresentationFactory factory = new PresentationFactory(editor);

        // Compute top inset to vertically center the hint within the line.
        int lineHeight = editor.getLineHeight();
        int fontSize = editor.getColorsScheme().getEditorFontSize();
        int topInset = Math.max(0, (lineHeight - fontSize) / 2);

        InlayPresentation text = factory.smallText("⎘ " + label);

        // referenceOnHover applies an underline and hand cursor when hovering.
        InlayPresentation withHover = factory.referenceOnHover(text, (event, point) -> {});

        // onClick copies the label to the clipboard and shows a brief tooltip.
        InlayPresentation clickable = factory.onClick(withHover, MouseButton.Left, (event, point) -> {
            CopyPasteManager.getInstance().setContents(new StringSelection(label));
            HintManager.getInstance().showInformationHint(editor, "Copied!");
            return null;
        });

        InlayPresentation padded = factory.inset(clickable, 8, 0, topInset, 0);

        sink.addInlineElement(offset, true, padded, false);
    }

    /**
     * Scans forward from {@code fromLine} to find the {@code name = "..."} attribute
     * of the current rule block. Stops when the block closes (depth reaches 0).
     */
    private @Nullable String findTargetName(String[] lines, int fromLine) {
        int depth = 1;
        for (int i = fromLine; i < lines.length && depth > 0; i++) {
            String line = lines[i];

            Matcher nameMatcher = NAME_PATTERN.matcher(line);
            if (nameMatcher.find()) {
                return nameMatcher.group(1);
            }

            // Track parenthesis depth to know when the rule block ends.
            for (char c : line.toCharArray()) {
                if (c == '(') depth++;
                else if (c == ')') depth--;
            }
        }
        return null;
    }

    /**
     * Derives the Bazel package path from the file's location relative to the
     * workspace root. The workspace root is the nearest ancestor directory that
     * contains a WORKSPACE or WORKSPACE.bazel file.
     * <p>
     * Example: if the workspace root is /repo and the file is at
     * /repo/foo/bar/BUILD.bazel, this returns "foo/bar".
     */
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

    /**
     * Walks up the directory tree to find the workspace root, identified by
     * the presence of a WORKSPACE or WORKSPACE.bazel file.
     */
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
