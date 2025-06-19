import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClipboardHistoryGUI extends JDialog {
    private final ClipboardMonitor monitor;
    private final JList<ClipboardMonitor.ClipboardEntry> historyList;
    private final DefaultListModel<ClipboardMonitor.ClipboardEntry> listModel;
    private final JTextField searchField;
    private TrayIcon trayIcon; // Reference to tray icon for positioning
    
    // Positioning options
    public enum PositionMode {
        MOUSE_RELATIVE,  // Appear near mouse cursor
        MENU_BAR,        // Appear near menu bar/tray icon
        CENTER           // Default center positioning
    }
    
    private PositionMode positionMode = PositionMode.MOUSE_RELATIVE;
    
    public ClipboardHistoryGUI(ClipboardMonitor monitor) {
        this.monitor = monitor;
        this.listModel = new DefaultListModel<>();
        this.historyList = new JList<>(listModel);
        this.searchField = new JTextField();
        
        setupGUI();
        refreshHistory();
    }
    
    public void setTrayIcon(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
    }
    
    public void setPositionMode(PositionMode mode) {
        this.positionMode = mode;
    }
    
    /**
     * Nuclear option: Force maximum visibility when all else fails
     * Call this if the window is appearing behind other windows
     */
    public void forceMaximumVisibility() {
        FullscreenHelper.emergencyMaximumVisibility(this);
        setMaximumWindowLevel();
        forceToFront();
    }
    
    private void setupGUI() {
        setTitle("Clipboard History");
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setModal(false); // Non-modal for better overlay experience
        setSize(480, 360); // Slightly smaller for overlay feel
        
        // Critical: Configure for maximum window level (highest possible Z-order)
        setMaximumWindowLevel();
        
        // macOS-specific: Ensure window appears on active Space
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                // Make window focusable to ensure it appears on current Space
                setFocusableWindowState(true);
                setAutoRequestFocus(true);
                
            } catch (Exception e) {
                System.err.println("Could not configure fullscreen compatibility: " + e.getMessage());
            }
        }
        
        // Set up the layout
        setLayout(new BorderLayout());
        
        // Search field at top
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(new EmptyBorder(8, 8, 4, 8));
        searchField.setText("Search clipboard history...");
        searchField.setForeground(Color.GRAY);
        
        // Add focus listeners for placeholder effect
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("Search clipboard history...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(Color.GRAY);
                    searchField.setText("Search clipboard history...");
                }
            }
        });
        
        searchPanel.add(new JLabel("🔍 "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);
        
        // Configure the list
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new ClipboardEntryRenderer());
        historyList.setBackground(new Color(248, 248, 248)); // Subtle background
        
        // Add scroll pane for the list
        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(new EmptyBorder(4, 8, 8, 8));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling
        add(scrollPane, BorderLayout.CENTER);
        
        // Add instructions at bottom
        JLabel instructions = new JLabel(
            "<html><center>↵ Enter or double-click to paste instantly • ⎋ Esc to close • ⌘⇧V to reopen</center></html>"
        );
        instructions.setBorder(new EmptyBorder(4, 8, 8, 8));
        instructions.setFont(instructions.getFont().deriveFont(Font.ITALIC, 10f));
        instructions.setHorizontalAlignment(SwingConstants.CENTER);
        instructions.setForeground(Color.GRAY);
        add(instructions, BorderLayout.SOUTH);
        
        // Add event listeners
        setupEventListeners();
        
        // Mac-specific UI improvements
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.setProperty("apple.awt.application.appearance", "system");
        }
        
        // Auto-hide when clicking outside (lose focus)
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                // Auto-hide after a brief delay to allow for interaction
                Timer hideTimer = new Timer(150, evt -> {
                    if (!hasFocus() && !searchField.hasFocus() && !historyList.hasFocus()) {
                        setVisible(false);
                    }
                });
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });
    }
    
    private void setupEventListeners() {
        // Double-click to paste
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    pasteSelected();
                }
            }
        });
        
        // Keyboard navigation
        historyList.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    pasteSelected();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        // Search functionality
        searchField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    historyList.requestFocus();
                    if (historyList.getModel().getSize() > 0) {
                        historyList.setSelectedIndex(0);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Enter in search field pastes first result
                    if (historyList.getModel().getSize() > 0) {
                        historyList.setSelectedIndex(0);
                        pasteSelected();
                    }
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {
                filterHistory();
            }
        });
        
        // Escape key to close dialog
        getRootPane().registerKeyboardAction(
            e -> setVisible(false),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private void filterHistory() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        // Don't filter if showing placeholder text
        if (searchText.equals("search clipboard history...")) {
            searchText = "";
        }
        
        listModel.clear();
        
        List<ClipboardMonitor.ClipboardEntry> allEntries = monitor.getHistory();
        
        for (ClipboardMonitor.ClipboardEntry entry : allEntries) {
            if (searchText.isEmpty() || 
                entry.getContent().toLowerCase().contains(searchText)) {
                listModel.addElement(entry);
            }
        }
        
        // Select first item if available
        if (listModel.getSize() > 0) {
            historyList.setSelectedIndex(0);
        }
    }
    
    private void pasteSelected() {
        ClipboardMonitor.ClipboardEntry selected = historyList.getSelectedValue();
        if (selected != null) {
            // Copy selected text to clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(selected.getContent()), null);
            
            setVisible(false);
            
            // Simple approach: Always paste after a brief delay
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    performPaste();
                }
            });
        }
    }
    
    /**
     * Performs paste operation
     */
    private void performPaste() {
        try {
            Robot robot = new Robot();
            // Small delay to ensure dialog is hidden and focus is back
            Thread.sleep(150);
            
            // Paste using Cmd+V
            robot.keyPress(KeyEvent.VK_META); // Cmd key on Mac
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_META);
            
        } catch (Exception e) {
            System.err.println("Error performing paste: " + e.getMessage());
        }
    }
    
    public void showHistory() {
        refreshHistory();
        
        // Special handling for fullscreen apps on macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            prepareForFullscreenDisplay();
        }
        
        // Ensure maximum window level before positioning
        setMaximumWindowLevel();
        forceToFront();
        
        positionWindow();
        
        // Clear search field and show placeholder
        searchField.setForeground(Color.GRAY);
        searchField.setText("Search clipboard history...");
        
        setVisible(true);
        
        // Critical: Request focus to ensure window appears on current Space
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                forceToFront();
                requestFocus();
                searchField.requestFocus();
                if (listModel.getSize() > 0) {
                    historyList.setSelectedIndex(0);
                }
                
                // Additional safety: ensure we're still on top after a brief delay
                javax.swing.Timer safetyTimer = new javax.swing.Timer(100, new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        forceToFront();
                    }
                });
                safetyTimer.setRepeats(false);
                safetyTimer.start();
            }
        });
    }
    
    /**
     * Sets the window to the absolute highest possible level
     */
    private void setMaximumWindowLevel() {
        try {
            // Reset and configure for maximum visibility
            setAlwaysOnTop(false);
            
            // Set window type for maximum priority
            setType(Window.Type.POPUP); // POPUP often has higher priority than UTILITY
            
            // Enable always on top with maximum priority
            setAlwaysOnTop(true);
            
            // Additional platform-specific optimizations
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                // On macOS, we can try to set an even higher window level
                setMacOSMaximumLevel();
            }
            
        } catch (Exception e) {
            System.err.println("Error setting maximum window level: " + e.getMessage());
            // Fallback to basic always on top
            setAlwaysOnTop(true);
        }
    }
    
    /**
     * macOS-specific maximum window level setting
     */
    private void setMacOSMaximumLevel() {
        try {
            // macOS-specific optimizations for maximum window visibility
            setAlwaysOnTop(true);
            
            // Additional aggressive focus management for macOS
            setFocusableWindowState(true);
            setAutoRequestFocus(true);
            
            // Ensure window is properly configured for maximum visibility
            setType(Window.Type.POPUP);
            
        } catch (Exception e) {
            // If any setting fails, just use standard always on top
            setAlwaysOnTop(true);
        }
    }
    
    /**
     * Aggressively brings window to front
     */
    private void forceToFront() {
        try {
            // Multiple approaches to ensure window comes to front
            toFront();
            requestFocus();
            
            // Additional platform-specific forcing
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                // Use Robot to ensure focus if needed
                try {
                    Robot robot = new Robot();
                    Point currentPos = MouseInfo.getPointerInfo().getLocation();
                    // Tiny mouse movement to trigger focus
                    robot.mouseMove(currentPos.x, currentPos.y);
                } catch (Exception e) {
                    // Ignore if Robot fails
                }
            }
            
            // Ensure visibility
            setVisible(true);
            
        } catch (Exception e) {
            System.err.println("Error forcing window to front: " + e.getMessage());
        }
    }
    
    private void prepareForFullscreenDisplay() {
        try {
            // Use the helper class for maximum window visibility
            if (FullscreenHelper.isInFullscreenSpace()) {
                System.out.println("Detected fullscreen Space - using maximum visibility mode");
                
                // Use the most aggressive approach for fullscreen
                FullscreenHelper.emergencyMaximumVisibility(this);
                FullscreenHelper.bringToCurrentSpace(this);
                
            } else {
                // Use enhanced approach for non-fullscreen
                FullscreenHelper.setHighestWindowLevel(this);
            }
            
        } catch (Exception e) {
            System.err.println("Error preparing for fullscreen display: " + e.getMessage());
            // Fallback to our own maximum level setting
            setMaximumWindowLevel();
        }
    }
    
    private void positionWindow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = getSize();
        
        Point position = new Point();
        
        switch (positionMode) {
            case MOUSE_RELATIVE:
                position = getMouseRelativePosition(windowSize, screenSize);
                break;
                
            case MENU_BAR:
                position = getMenuBarPosition(windowSize, screenSize);
                break;
                
            case CENTER:
            default:
                position = getCenterPosition(windowSize, screenSize);
                break;
        }
        
        setLocation(position);
    }
    
    private Point getMouseRelativePosition(Dimension windowSize, Dimension screenSize) {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        
        // Get the current graphics device (screen) where mouse is located
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice currentScreen = null;
        
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            if (bounds.contains(mousePos)) {
                currentScreen = gd;
                screenSize = bounds.getSize();
                break;
            }
        }
        
        // Offset from mouse position (slightly down and to the right)
        int offsetX = 20;
        int offsetY = 20;
        
        int x = mousePos.x + offsetX;
        int y = mousePos.y + offsetY;
        
        // For fullscreen apps, adjust positioning to be more centered
        if (isLikelyFullscreen(mousePos, screenSize)) {
            // In fullscreen, position more towards center but still near mouse
            x = mousePos.x - windowSize.width / 2;
            y = mousePos.y - windowSize.height / 2;
            offsetX = 50;
            offsetY = 50;
        }
        
        // Keep window on screen
        if (x + windowSize.width > screenSize.width) {
            x = mousePos.x - windowSize.width - offsetX; // Show to the left instead
        }
        if (y + windowSize.height > screenSize.height) {
            y = mousePos.y - windowSize.height - offsetY; // Show above instead
        }
        
        // Ensure minimum position
        x = Math.max(0, x);
        y = Math.max(0, y);
        
        return new Point(x, y);
    }
    
    private boolean isLikelyFullscreen(Point mousePos, Dimension screenSize) {
        // Detect if we're likely in a fullscreen app by checking if mouse is far from edges
        int edgeThreshold = 100;
        return mousePos.x > edgeThreshold && 
               mousePos.y > edgeThreshold && 
               mousePos.x < screenSize.width - edgeThreshold && 
               mousePos.y < screenSize.height - edgeThreshold;
    }
    
    private Point getMenuBarPosition(Dimension windowSize, Dimension screenSize) {
        // Position near the top-right of screen (where menu bar icons are)
        int x = screenSize.width - windowSize.width - 20;
        int y = 30; // Just below menu bar
        
        return new Point(x, y);
    }
    
    private Point getCenterPosition(Dimension windowSize, Dimension screenSize) {
        int x = (screenSize.width - windowSize.width) / 2;
        int y = (screenSize.height - windowSize.height) / 2;
        
        return new Point(x, y);
    }
    
    private void refreshHistory() {
        listModel.clear();
        for (ClipboardMonitor.ClipboardEntry entry : monitor.getHistory()) {
            listModel.addElement(entry);
        }
        
        if (listModel.getSize() > 0) {
            historyList.setSelectedIndex(0);
        }
    }
    
    // Custom renderer for clipboard entries
    private static class ClipboardEntryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ClipboardMonitor.ClipboardEntry) {
                ClipboardMonitor.ClipboardEntry entry = (ClipboardMonitor.ClipboardEntry) value;
                
                // Format timestamp
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(entry.getTimestamp()), 
                    ZoneId.systemDefault()
                );
                String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                // Create HTML formatted text with better styling
                String preview = entry.getPreview().replace("<", "&lt;").replace(">", "&gt;");
                setText(String.format(
                    "<html><div style='padding: 4px;'>" +
                    "<div style='font-size: 12px; color: %s; font-weight: %s;'>%s</div>" +
                    "<div style='font-size: 9px; color: %s; margin-top: 2px;'>⏰ %s</div>" +
                    "</div></html>",
                    isSelected ? "white" : "#333333",
                    isSelected ? "bold" : "normal",
                    preview,
                    isSelected ? "#E0E0E0" : "#888888",
                    timeStr
                ));
                
                setBorder(new EmptyBorder(6, 10, 6, 10));
                
                // Custom selection colors
                if (isSelected) {
                    setBackground(new Color(0, 122, 255)); // macOS blue
                } else {
                    setBackground(index % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                }
            }
            
            return this;
        }
    }
}