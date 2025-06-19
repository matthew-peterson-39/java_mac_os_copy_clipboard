import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class GlobalHotkeyManager implements NativeKeyListener {
    private ClipboardHistoryGUI gui;
    private boolean cmdPressed = false;
    private boolean shiftPressed = false;
    private long lastHotkeyTime = 0;
    private String originalClipboard = "";
    
    public GlobalHotkeyManager(ClipboardHistoryGUI gui) {
        this.gui = gui;
    }
    
    public void initialize() {
        try {
            // Disable JNativeHook logging to reduce console spam
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);
            
            // Register native hook
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            
            System.out.println("Global hotkey registered: Cmd+Shift+V (GUI only - preventing auto-paste)");
            
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            
            // Show user-friendly error message
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "Could not register global hotkey (Cmd+Shift+V).\n" +
                        "Please grant accessibility permissions:\n\n" +
                        "1. System Preferences → Security & Privacy → Privacy\n" +
                        "2. Select 'Accessibility' on the left\n" +
                        "3. Add Java or your Terminal app\n" +
                        "4. Restart the application\n\n" +
                        "Alternative: You can use the tray icon to access clipboard history.",
                        "Hotkey Setup Required",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                }
            });
        }
    }
    
    public void shutdown() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem unregistering the native hook.");
            System.err.println(ex.getMessage());
        }
    }
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // Track modifier keys
        if (e.getKeyCode() == NativeKeyEvent.VC_META) {  // Cmd key on Mac
            cmdPressed = true;
        }
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = true;
        }
        
        // Check for Cmd+Shift+V combination
        if (cmdPressed && shiftPressed && e.getKeyCode() == NativeKeyEvent.VC_V) {
            long currentTime = System.currentTimeMillis();
            
            // Prevent multiple rapid triggers (debounce)
            if (currentTime - lastHotkeyTime > 500) { // Longer debounce
                lastHotkeyTime = currentTime;
                
                System.out.println("Cmd+Shift+V detected - preventing default paste and opening GUI");
                
                // Method 1: Temporarily clear clipboard to prevent paste
                preventDefaultPaste();
                
                // Show GUI after preventing paste
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        gui.showHistory();
                    }
                });
            }
        }
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Track when modifier keys are released
        if (e.getKeyCode() == NativeKeyEvent.VC_META) {  // Cmd key on Mac
            cmdPressed = false;
        }
        if (e.getKeyCode() == NativeKeyEvent.VC_SHIFT) {
            shiftPressed = false;
        }
        
        // Restore clipboard when all keys are released
        if (!cmdPressed && !shiftPressed) {
            restoreClipboard();
        }
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used for our hotkey implementation
    }
    
    /**
     * Temporarily clears clipboard to prevent default paste behavior
     */
    private void preventDefaultPaste() {
        try {
            // Save current clipboard content
            if (Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                originalClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            }
            
            // Temporarily set clipboard to empty string
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
            
            // Schedule restoration after a brief delay
            javax.swing.Timer restoreTimer = new javax.swing.Timer(200, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    restoreClipboard();
                }
            });
            restoreTimer.setRepeats(false);
            restoreTimer.start();
            
        } catch (Exception e) {
            System.err.println("Error preventing default paste: " + e.getMessage());
        }
    }
    
    /**
     * Restores the original clipboard content
     */
    private void restoreClipboard() {
        try {
            if (originalClipboard != null && !originalClipboard.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(originalClipboard), null);
                originalClipboard = "";
            }
        } catch (Exception e) {
            System.err.println("Error restoring clipboard: " + e.getMessage());
        }
    }
}