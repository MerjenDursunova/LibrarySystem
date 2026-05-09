import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class AIChatDialog extends JDialog {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendBtn;
    private JButton newChatBtn;
    private JComboBox<String> historyCombo;

    // In-memory session store: each session is the full chat text
    private final List<String> sessions = new ArrayList<>();
    private int currentSessionIndex = -1; // -1 when unsaved

    public AIChatDialog(Frame owner) {
        super(owner, "Library AI Assistant", false);
        setSize(500, 400);
        setLocationRelativeTo(owner);

        setLayout(new BorderLayout(10, 10));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(chatArea);
        add(scroll, BorderLayout.CENTER);

        // Top controls: New Chat and history selector
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        newChatBtn = new JButton("New Chat");
        historyCombo = new JComboBox<>();
        historyCombo.setPreferredSize(new Dimension(220, 24));
        historyCombo.addActionListener(e -> {
            int idx = historyCombo.getSelectedIndex();
            if (idx >= 0 && idx < sessions.size()) {
                loadSession(idx);
            }
        });
        top.add(newChatBtn);
        top.add(new JLabel("History:"));
        top.add(historyCombo);
        add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        sendBtn = new JButton("Ask AI");
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        newChatBtn.addActionListener(e -> startNewChat());
    }

    public void appendMessage(String who, String message) {
        chatArea.append(who + ":\n" + message + "\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void loadSession(int idx) {
        if (idx < 0 || idx >= sessions.size()) return;
        String txt = sessions.get(idx);
        chatArea.setText(txt == null ? "" : txt);
        currentSessionIndex = idx;
    }

    private void saveCurrentSession() {
        String txt = chatArea.getText();
        if (currentSessionIndex >= 0 && currentSessionIndex < sessions.size()) {
            sessions.set(currentSessionIndex, txt);
            historyCombo.removeItemAt(currentSessionIndex);
            historyCombo.insertItemAt("Session " + (currentSessionIndex + 1), currentSessionIndex);
        } else {
            sessions.add(txt);
            historyCombo.addItem("Session " + sessions.size());
            currentSessionIndex = sessions.size() - 1;
        }
        historyCombo.setSelectedIndex(currentSessionIndex);
    }

    private void startNewChat() {
        // Save existing session if it has content
        if (!chatArea.getText().isBlank()) saveCurrentSession();
        chatArea.setText("");
        currentSessionIndex = -1;
        historyCombo.setSelectedIndex(-1);
        inputField.requestFocusInWindow();
    }

    public void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        appendMessage("You", text);
        inputField.setText("");

        // Disable input while waiting
        inputField.setEnabled(false);
        sendBtn.setEnabled(false);

        // Insert a Thinking placeholder that we'll replace when done
        appendMessage("AI", "Thinking...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return AIClient.sendQuery(text);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    replaceLastAIThinking(response);
                } catch (Exception e) {
                    replaceLastAIThinking("[AI Error] " + e.getMessage());
                } finally {
                    inputField.setEnabled(true);
                    sendBtn.setEnabled(true);
                    inputField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    public void askAndShow(String question) {
        // Ensure dialog visible
        if (!isVisible()) setVisible(true);
        appendMessage("You", question);
        // Disable input while waiting
        inputField.setEnabled(false);
        sendBtn.setEnabled(false);
        appendMessage("AI", "Thinking...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return AIClient.sendQuery(question);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    replaceLastAIThinking(response);
                } catch (Exception e) {
                    replaceLastAIThinking("[AI Error] " + e.getMessage());
                } finally {
                    inputField.setEnabled(true);
                    sendBtn.setEnabled(true);
                    inputField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void replaceLastAIThinking(String newText) {
        try {
            String full = chatArea.getText();
            String marker = "AI:\nThinking...\n\n";
            int idx = full.lastIndexOf(marker);
            if (idx >= 0) {
                String before = full.substring(0, idx);
                String after = before + "AI:\n" + newText + "\n\n";
                chatArea.setText(after);
            } else {
                appendMessage("AI", newText);
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        } catch (Exception e) {
            appendMessage("AI", newText);
        }
    }
}
