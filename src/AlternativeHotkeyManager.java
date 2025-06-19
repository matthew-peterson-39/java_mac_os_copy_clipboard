import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlternativeHotkeyManager implements NativeKeyListener {
    private ClipboardHistoryGUI gui;
    private boolean cmdPressed = false;
    private boolean shiftPressed = false;
    private long lastHotkeyTime = 0;
    
    public AlternativeHotkeyManager(ClipboardHistoryGUI gui) {
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
            
            System.out.println("Alternative global hotkey registered: Cmd+Shift+C (Clipboard History)");
            
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            
            // Show user-friendly error message
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    HighLevelDialogUtils.showHighLevelMessageDialog(
                        null,
                        "Could not register global hotkey (Cmd+Shift+C).\n" +
                        "Please grant accessibility permissions:\n\n" +
                        "1. System Preferences → Security & Privacy → Privacy\n" +
                        "2. Select 'Accessibility' on the left\n" +
                        "3. Add Java or your Terminal app\n" +
                        "4. Restart the application\n\n" +
                        "You can still use the tray icon to access clipboard history.",
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
        
        // Check for Cmd+Shift+C combination (C for Clipboard history)
        if (cmdPressed && shiftPressed && e.getKeyCode() == NativeKeyEvent.VC_C) {
            long currentTime = System.currentTimeMillis();
            
            // Prevent multiple rapid triggers (debounce)
            if (currentTime - lastHotkeyTime > 300) {
                lastHotkeyTime = currentTime;
                
                System.out.println("Cmd+Shift+C detected - opening clipboard history");
                
                // Show the GUI - this key combination has no conflicts!
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
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Not used for our hotkey implementation
    }
}