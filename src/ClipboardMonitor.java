import java.awt.*;
import java.awt.datatransfer.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClipboardMonitor {
    private final Clipboard clipboard;
    private final List<ClipboardEntry> history;
    private String lastClipboardContent;
    private final ScheduledExecutorService scheduler;
    
    public ClipboardMonitor() {
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        this.history = new ArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.lastClipboardContent = "";
    }
    
    public void startMonitoring() {
        // Check clipboard every 500ms for changes
        scheduler.scheduleAtFixedRate(this::checkClipboard, 0, 500, TimeUnit.MILLISECONDS);
        System.out.println("Clipboard monitoring started...");
    }
    
    private void checkClipboard() {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String currentContent = (String) clipboard.getData(DataFlavor.stringFlavor);
                
                // Only add to history if content has changed and is not empty
                if (currentContent != null && 
                    !currentContent.trim().isEmpty() && 
                    !currentContent.equals(lastClipboardContent)) {
                    
                    addToHistory(currentContent);
                    lastClipboardContent = currentContent;
                }
            }
        } catch (Exception e) {
            System.err.println("Error accessing clipboard: " + e.getMessage());
        }
    }
    
    private void addToHistory(String content) {
        ClipboardEntry entry = new ClipboardEntry(content, System.currentTimeMillis());
        
        // Remove duplicate if it exists
        history.removeIf(e -> e.getContent().equals(content));
        
        // Add to beginning of list (most recent first)
        history.add(0, entry);
        
        // Limit history size to 50 items
        if (history.size() > 50) {
            history.remove(history.size() - 1);
        }
        
        System.out.println("Added to clipboard history: " + 
                          (content.length() > 50 ? content.substring(0, 50) + "..." : content));
    }
    
    public List<ClipboardEntry> getHistory() {
        return new ArrayList<>(history);
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    // Inner class to represent clipboard entries
    public static class ClipboardEntry {
        private final String content;
        private final long timestamp;
        
        public ClipboardEntry(String content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getPreview() {
            if (content.length() <= 60) {
                return content.replaceAll("\\s+", " ").trim();
            }
            return content.substring(0, 60).replaceAll("\\s+", " ").trim() + "...";
        }
    }
}