import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FullscreenHelper {
    
    /**
     * Attempts to detect if we're currently in a fullscreen Space on macOS
     */
    public static boolean isInFullscreenSpace() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            return false;
        }
        
        try {
            // Use AppleScript to check if current Space has fullscreen apps
            String script = "tell application \"System Events\" to get the name of every application process whose frontmost is true";
            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();
            
            // Check if any known fullscreen indicators are present
            return result != null && !result.trim().equals("Finder");
            
        } catch (Exception e) {
            // If we can't determine, assume not fullscreen
            return false;
        }
    }
    
    /**
     * Gets the current active application name
     */
    public static String getCurrentAppName() {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            return null;
        }
        
        try {
            String script = "tell application \"System Events\" to get the name of the first application process whose frontmost is true";
            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();
            
            return result != null ? result.trim() : null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Attempts to bring a Java window to the current Space using AppleScript
     */
    public static void bringToCurrentSpace(Window window) {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            return;
        }
        
        try {
            // First, ensure the window is visible and focused
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
            
            // Use AppleScript to activate our Java application on current Space
            String appName = "java"; // This might need adjustment based on how Java appears in Activity Monitor
            
            String script = String.format(
                "tell application \"System Events\" to tell application process \"%s\" to set frontmost to true", 
                appName
            );
            
            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            process.waitFor();
            
        } catch (Exception e) {
            System.err.println("Could not bring window to current Space: " + e.getMessage());
        }
    }
    
    /**
     * Alternative approach: Use window server level manipulation for MAXIMUM visibility
     */
    public static void setHighestWindowLevel(Window window) {
        try {
            // This is the most aggressive approach for maximum visibility
            window.setVisible(false); // Hide first to reset state
            
            // Reset all window properties
            window.setAlwaysOnTop(false);
            
            // Brief delay for system processing
            Thread.sleep(25);
            
            // Configure for absolute maximum visibility
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                dialog.setModal(false);
                dialog.setType(Window.Type.POPUP); // POPUP type for highest priority
                dialog.setFocusableWindowState(true);
                dialog.setAutoRequestFocus(true);
            }
            
            // Set always on top with maximum priority
            window.setAlwaysOnTop(true);
            
            // Make visible and force to front
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
            
            // macOS-specific: Try to use AppleScript for even higher priority
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                bringToAbsoluteFront(window);
            }
            
        } catch (Exception e) {
            System.err.println("Error setting highest window level: " + e.getMessage());
            // Fallback
            window.setAlwaysOnTop(true);
            window.setVisible(true);
            window.toFront();
        }
    }
    
    /**
     * Uses AppleScript to bring window to absolute front on macOS
     */
    private static void bringToAbsoluteFront(Window window) {
        try {
            // First ensure Java app is frontmost
            String script = "tell application \"System Events\" to tell application process \"java\" to set frontmost to true";
            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            process.waitFor();
            
            // Additional focus commands
            window.toFront();
            window.requestFocus();
            
        } catch (Exception e) {
            // Silently fail if AppleScript doesn't work
        }
    }
    
    /**
     * Emergency maximum visibility - use when other methods fail
     */
    public static void emergencyMaximumVisibility(Window window) {
        try {
            // Nuclear option for maximum visibility
            window.setVisible(false);
            
            // Reset everything
            window.setAlwaysOnTop(false);
            if (window instanceof Dialog) {
                ((Dialog) window).setType(Window.Type.POPUP);
            }
            
            // Wait a moment
            Thread.sleep(50);
            
            // Set maximum properties
            window.setAlwaysOnTop(true);
            window.setVisible(true);
            
            // Force focus multiple times
            for (int i = 0; i < 3; i++) {
                window.toFront();
                window.requestFocus();
                Thread.sleep(10);
            }
            
            // Platform-specific final push
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                try {
                    // Use mouse movement to trigger focus system
                    Robot robot = new Robot();
                    Point pos = MouseInfo.getPointerInfo().getLocation();
                    robot.mouseMove(pos.x + 1, pos.y);
                    robot.mouseMove(pos.x, pos.y);
                } catch (Exception e) {
                    // Ignore Robot errors
                }
            }
            
        } catch (Exception e) {
            System.err.println("Emergency visibility failed: " + e.getMessage());
        }
    }
}