import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class App {
    private ClipboardMonitor monitor;
    private ClipboardHistoryGUI gui;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private GlobalHotkeyManager hotkeyManager;
    private AlternativeHotkeyManager altHotkeyManager;
    private boolean usingAlternativeHotkey = false;
    
    public App() {
        setupSystemLookAndFeel();
        initializeComponents();
        setupSystemTray();
        setupGlobalHotkey();
        startApplication();
    }
    
    private void setupSystemLookAndFeel() {
        try {
            // Use system look and feel for better Mac integration
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Mac-specific properties
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Clipboard Manager");
                System.setProperty("apple.awt.application.name", "Clipboard Manager");
            }
        } catch (Exception e) {
            System.err.println("Failed to set system look and feel: " + e.getMessage());
        }
    }
    
    private void initializeComponents() {
        monitor = new ClipboardMonitor();
        gui = new ClipboardHistoryGUI(monitor);
        
        // Set default positioning mode (you can change this)
        gui.setPositionMode(ClipboardHistoryGUI.PositionMode.MOUSE_RELATIVE);
    }
    
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported");
            return;
        }
        
        systemTray = SystemTray.getSystemTray();
        
        // Create tray icon
        Image image = createTrayIcon();
        trayIcon = new TrayIcon(image, "Clipboard Manager");
        trayIcon.setImageAutoSize(true);
        
        // Create popup menu
        PopupMenu popup = new PopupMenu();
        
        MenuItem showHistoryItem = new MenuItem("Show Clipboard History");
        showHistoryItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHistory();
            }
        });
        popup.add(showHistoryItem);
        
        popup.addSeparator();
        
        // Positioning options submenu
        Menu positionMenu = new Menu("Window Position");
        
        MenuItem mousePositionItem = new MenuItem("Near Mouse Cursor");
        mousePositionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setPositionMode(ClipboardHistoryGUI.PositionMode.MOUSE_RELATIVE);
                showStatusMessage("Window will appear near mouse cursor");
            }
        });
        positionMenu.add(mousePositionItem);
        
        MenuItem menuBarPositionItem = new MenuItem("Near Menu Bar");
        menuBarPositionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setPositionMode(ClipboardHistoryGUI.PositionMode.MENU_BAR);
                showStatusMessage("Window will appear near menu bar");
            }
        });
        positionMenu.add(menuBarPositionItem);
        
        MenuItem centerPositionItem = new MenuItem("Screen Center");
        centerPositionItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.setPositionMode(ClipboardHistoryGUI.PositionMode.CENTER);
                showStatusMessage("Window will appear in screen center");
            }
        });
        positionMenu.add(centerPositionItem);
        
        popup.add(positionMenu);
        
        popup.addSeparator();
        
        // Emergency visibility option
        MenuItem forceVisibilityItem = new MenuItem("Force Maximum Visibility");
        forceVisibilityItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.forceMaximumVisibility();
                showStatusMessage("Applied maximum visibility settings");
            }
        });
        popup.add(forceVisibilityItem);
        
        popup.addSeparator();
        
        // Hotkey options submenu
        Menu hotkeyMenu = new Menu("Hotkey Options");
        
        MenuItem cmdShiftVItem = new MenuItem("Use Cmd+Shift+V (with paste prevention)");
        cmdShiftVItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToStandardHotkey();
            }
        });
        hotkeyMenu.add(cmdShiftVItem);
        
        MenuItem cmdShiftCItem = new MenuItem("Use Cmd+Shift+C (conflict-free)");
        cmdShiftCItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                switchToAlternativeHotkey();
            }
        });
        hotkeyMenu.add(cmdShiftCItem);
        
        popup.add(hotkeyMenu);
        
        popup.addSeparator();
        
        MenuItem clearHistoryItem = new MenuItem("Clear History");
        clearHistoryItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearHistory();
            }
        });
        popup.add(clearHistoryItem);
        
        popup.addSeparator();
        
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAbout();
            }
        });
        popup.add(aboutItem);
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitApplication();
            }
        });
        popup.add(exitItem);
        
        trayIcon.setPopupMenu(popup);
        
        // Double-click to show history
        trayIcon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHistory();
            }
        });
        
        try {
            systemTray.add(trayIcon);
            
            // Pass tray icon reference to GUI for positioning
            gui.setTrayIcon(trayIcon);
            
        } catch (AWTException e) {
            System.err.println("Failed to add tray icon: " + e.getMessage());
        }
    }
    
    private Image createTrayIcon() {
        // Create a simple icon programmatically
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // Anti-aliasing for smooth edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw clipboard icon
        g2d.setColor(Color.BLACK);
        g2d.fillRoundRect(2, 1, 12, 14, 2, 2);
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(3, 2, 10, 12, 1, 1);
        
        // Draw clip
        g2d.setColor(Color.GRAY);
        g2d.fillRoundRect(6, 0, 4, 4, 1, 1);
        
        // Draw lines representing text
        g2d.setColor(Color.BLACK);
        g2d.fillRect(5, 6, 6, 1);
        g2d.fillRect(5, 8, 4, 1);
        g2d.fillRect(5, 10, 5, 1);
        
        g2d.dispose();
        return image;
    }
    
    private void setupGlobalHotkey() {
        // Initialize global hotkey manager
        // Option 1: Try Cmd+Shift+V with paste prevention
        hotkeyManager = new GlobalHotkeyManager(gui);
        hotkeyManager.initialize();
        
        // Show startup information with troubleshooting using high-level dialog
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                HighLevelDialogUtils.showHighLevelMessageDialog(
                    null,
                    "Clipboard Manager is now running!\n\n" +
                    "• Copy text normally (Cmd+C)\n" +
                    "• Press Cmd+Shift+V to open clipboard history\n" +
                    "• Select any item to paste it immediately\n" +
                    "• Window will appear near your mouse cursor\n" +
                    "• Right-click tray icon for settings\n\n" +
                    "⚠️ TROUBLESHOOTING:\n" +
                    "If Cmd+Shift+V still auto-pastes, use the menu option\n" +
                    "'Switch to Cmd+Shift+C' for a conflict-free hotkey.\n\n" +
                    "If the hotkey doesn't work, check accessibility permissions.",
                    "Clipboard Manager Started",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }
    
    private void startApplication() {
        monitor.startMonitoring();
        showWelcomeMessage();
    }
    
    private void showWelcomeMessage() {
        if (trayIcon != null) {
            trayIcon.displayMessage(
                "Clipboard Manager Started",
                "Press Cmd+Shift+V to open clipboard history or right-click this icon.",
                TrayIcon.MessageType.INFO
            );
        }
    }
    
    private void showStatusMessage(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(
                "Clipboard Manager",
                message,
                TrayIcon.MessageType.INFO
            );
        }
    }
    
    private void switchToStandardHotkey() {
        if (usingAlternativeHotkey) {
            // Shutdown alternative hotkey
            if (altHotkeyManager != null) {
                altHotkeyManager.shutdown();
            }
            
            // Start standard hotkey
            hotkeyManager = new GlobalHotkeyManager(gui);
            hotkeyManager.initialize();
            
            usingAlternativeHotkey = false;
            showStatusMessage("Switched to Cmd+Shift+V hotkey");
        } else {
            showStatusMessage("Already using Cmd+Shift+V hotkey");
        }
    }
    
    private void switchToAlternativeHotkey() {
        if (!usingAlternativeHotkey) {
            // Shutdown standard hotkey
            if (hotkeyManager != null) {
                hotkeyManager.shutdown();
            }
            
            // Start alternative hotkey
            altHotkeyManager = new AlternativeHotkeyManager(gui);
            altHotkeyManager.initialize();
            
            usingAlternativeHotkey = true;
            showStatusMessage("Switched to Cmd+Shift+C hotkey (conflict-free)");
        } else {
            showStatusMessage("Already using Cmd+Shift+C hotkey");
        }
    }
    
    private void showHistory() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.showHistory();
            }
        });
    }
    
    private void clearHistory() {
    // Use high-level dialog that appears above clipboard GUI
    int result = HighLevelDialogUtils.showHighLevelConfirmDialog(
        null,
        "Are you sure you want to clear all clipboard history?",
        "Clear History",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );
    
    if (result == JOptionPane.YES_OPTION) {
        // Use the monitor's clearHistory method instead of getHistory().clear()
        monitor.clearHistory();
        
        // Refresh the GUI if it's currently visible
        if (gui != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    gui.refreshHistoryDisplay();
                }
            });
        }
        
        if (trayIcon != null) {
            trayIcon.displayMessage(
                "History Cleared",
                "All clipboard history has been cleared.",
                TrayIcon.MessageType.INFO
            );
        }
    }
}
    
    private void showAbout() {
        String currentHotkey = usingAlternativeHotkey ? "Cmd+Shift+C" : "Cmd+Shift+V";
        
        HighLevelDialogUtils.showHighLevelMessageDialog(
            null,
            "<html><h2>Clipboard Manager</h2>" +
            "<p>Version 1.0</p>" +
            "<p>A clipboard history manager for Mac</p>" +
            "<br>" +
            "<p><b>Current Hotkey:</b> " + currentHotkey + "</p>" +
            "<br>" +
            "<p><b>Features:</b></p>" +
            "<ul>" +
            "<li>Tracks clipboard history automatically</li>" +
            "<li>Dual hotkey support (Cmd+Shift+V or Cmd+Shift+C)</li>" +
            "<li>Search through clipboard entries</li>" +
            "<li>Quick paste functionality</li>" +
            "<li>System tray integration</li>" +
            "<li>Fullscreen app compatibility</li>" +
            "</ul>" +
            "<br>" +
            "<p><b>Usage:</b></p>" +
            "<ul>" +
            "<li>Copy text normally (Cmd+C)</li>" +
            "<li>Press " + currentHotkey + " to open history</li>" +
            "<li>Or right-click tray icon</li>" +
            "<li>Double-click entries to paste</li>" +
            "</ul></html>",
            "About Clipboard Manager",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    private void exitApplication() {
        int result = HighLevelDialogUtils.showHighLevelConfirmDialog(
            null,
            "Are you sure you want to exit Clipboard Manager?",
            "Exit Application",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            if (monitor != null) {
                monitor.shutdown();
            }
            if (hotkeyManager != null) {
                hotkeyManager.shutdown();
            }
            if (altHotkeyManager != null) {
                altHotkeyManager.shutdown();
            }
            if (systemTray != null && trayIcon != null) {
                systemTray.remove(trayIcon);
            }
            System.exit(0);
        }
    }
    public static void main(String[] args) {
        // Ensure we're running on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new App();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                        null,
                        "Failed to start Clipboard Manager: " + e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    System.exit(1);
                }
            }
        });
    }
}