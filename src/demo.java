package com.example.test;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;

import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import com.intellij.openapi.util.Key;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.AttributedCharacterIterator;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.time.LocalDateTime;

import org.eclipse.jgit.api.Git;

import com.github.difflib.DiffUtils;
import java.util.Arrays;
import java.util.List;


public class LocalHistoryDocumentListener implements DocumentListener, InputMethodListener {

    private VersionControlSystem vcs;
    private final Project project;
    private static final Logger logger = Logger.getInstance(LocalHistoryDocumentListener.class);
    private static final long SAVE_DELAY = 5000; // 延迟1秒保存，减少输入法中间状态的捕获
    private Timer timer;
    private StringBuilder buffer; // 用来暂存变化内容
    private boolean inputMethodComposing = false; // 标记输入法是否在组合字符
    public static final Key<LocalHistoryDocumentListener> KEY = new Key<>("LocalHistoryInputMethodListener");

    public LocalHistoryDocumentListener(Project project, String filePath) {
        vcs = new VersionControlSystem("Initial content.", filePath);
        this.project = project;
        buffer = new StringBuilder();
        timer = new Timer();
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {

        Document document = event.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);


        if (psiFile != null) {
            // 取消之前的计时器任务，避免频繁保存
            resetSaveTimer();

            System.out.println("documentChanged executed");

        }
    }

    // 重置计时器，延迟处理
    private void resetSaveTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                saveEditorContent();
            }
        }, SAVE_DELAY);
    }

    // 保存变化


    // 输入法组合开始时触发
    @Override
    public void inputMethodTextChanged(InputMethodEvent event) {
        if (event.getCommittedCharacterCount() == 0) {
            setInputMethodComposing(true); // 开始组合输入
        } else {
            setInputMethodComposing(false); // 结束组合输入
        }
    }

    // 输入法组合开始时触发
    @Override
    public void caretPositionChanged(InputMethodEvent event) {
        // 这里不需要处理
    }

    // 处理输入法组合事件
    public void setInputMethodComposing(boolean composing) {
        this.inputMethodComposing = composing;
        System.out.println("inputMethodComposing: " + composing);
    }

    public void saveEditorContent() {
        // 获取当前打开的编辑器
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        if (editor != null) {
            Document document = editor.getDocument();
            String content = document.getText();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = LocalDateTime.now().format(formatter);

            // 获取当前文件名或路径（根据需求修改保存的路径）
            VirtualFile file = FileEditorManager.getInstance(project).getSelectedFiles()[0];
            String fileName = file.getNameWithoutExtension();

            // 定义保存的路径（如：项目根目录下的一个 txt 文件）
            String filePath = project.getBasePath() + "/.history/" + fileName + "_record_"+timestamp+".txt";

            // 使用 OutputStreamWriter 指定编码为 UTF-8 来避免乱码
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")) {
                writer.write(content);
                System.out.println("Saved to: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No opened editor");
        }
    }



}
