import javax.swing.*;
import java.awt.*;

/**
 * Utility class for creating dialogs that appear at the highest possible window level,
 * ensuring they appear above even our own clipboard GUI window.
 */
public class HighLevelDialogUtils {
    
    /**
     * Shows a confirmation dialog at maximum window level
     */
    public static int showHighLevelConfirmDialog(Component parentComponent, 
                                                String message, 
                                                String title, 
                                                int optionType) {
        return showHighLevelConfirmDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }
    
    /**
     * Shows a confirmation dialog at maximum window level with custom message type
     */
    public static int showHighLevelConfirmDialog(Component parentComponent, 
                                                String message, 
                                                String title, 
                                                int optionType, 
                                                int messageType) {
        // Create a custom dialog instead of using JOptionPane directly
        JDialog dialog = new JDialog((Frame) null, title, true);
        
        // Set maximum window level - even higher than our clipboard GUI
        setMaximumDialogLevel(dialog);
        
        // Create the content
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Add icon based on message type
        JLabel iconLabel = new JLabel(getIconForMessageType(messageType));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        contentPanel.add(iconLabel, BorderLayout.WEST);
        
        // Add message
        JLabel messageLabel = new JLabel("<html><body style='width: 250px'>" + message + "</body></html>");
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        
        // Create buttons based on option type
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final int[] result = {JOptionPane.CLOSED_OPTION};
        
        if (optionType == JOptionPane.YES_NO_OPTION) {
            JButton yesButton = new JButton("Yes");
            JButton noButton = new JButton("No");
            
            yesButton.addActionListener(e -> {
                result[0] = JOptionPane.YES_OPTION;
                dialog.dispose();
            });
            
            noButton.addActionListener(e -> {
                result[0] = JOptionPane.NO_OPTION;
                dialog.dispose();
            });
            
            // Make "No" the default (safer choice)
            noButton.requestFocusInWindow();
            dialog.getRootPane().setDefaultButton(noButton);
            
            buttonPanel.add(yesButton);
            buttonPanel.add(noButton);
            
        } else if (optionType == JOptionPane.OK_CANCEL_OPTION) {
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            
            okButton.addActionListener(e -> {
                result[0] = JOptionPane.OK_OPTION;
                dialog.dispose();
            });
            
            cancelButton.addActionListener(e -> {
                result[0] = JOptionPane.CANCEL_OPTION;
                dialog.dispose();
            });
            
            cancelButton.requestFocusInWindow();
            dialog.getRootPane().setDefaultButton(cancelButton);
            
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
        }
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(contentPanel);
        
        // Configure dialog
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.pack();
        
        // Center the dialog on screen (or relative to parent if provided)
        if (parentComponent != null) {
            dialog.setLocationRelativeTo(parentComponent);
        } else {
            dialog.setLocationRelativeTo(null);
        }
        
        // Force maximum visibility
        forceDialogToFront(dialog);
        
        // Show dialog and wait for result
        dialog.setVisible(true);
        
        return result[0];
    }
    
    /**
     * Shows an information dialog at maximum window level
     */
    public static void showHighLevelMessageDialog(Component parentComponent,
                                                 String message,
                                                 String title,
                                                 int messageType) {
        JDialog dialog = new JDialog((Frame) null, title, true);
        setMaximumDialogLevel(dialog);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel iconLabel = new JLabel(getIconForMessageType(messageType));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        contentPanel.add(iconLabel, BorderLayout.WEST);
        
        JLabel messageLabel = new JLabel("<html><body style='width: 300px'>" + message + "</body></html>");
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(contentPanel);
        
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();
        
        if (parentComponent != null) {
            dialog.setLocationRelativeTo(parentComponent);
        } else {
            dialog.setLocationRelativeTo(null);
        }
        
        forceDialogToFront(dialog);
        dialog.setVisible(true);
    }
    
    /**
     * Sets dialog to maximum window level - higher than our clipboard GUI
     */
    private static void setMaximumDialogLevel(JDialog dialog) {
        try {
            // Reset state first
            dialog.setAlwaysOnTop(false);
            
            // Set to absolute maximum level
            dialog.setType(Window.Type.POPUP);
            dialog.setAlwaysOnTop(true);
            
            // Additional properties for maximum visibility
            dialog.setFocusableWindowState(true);
            dialog.setAutoRequestFocus(true);
            
        } catch (Exception e) {
            System.err.println("Could not set maximum dialog level: " + e.getMessage());
            // Fallback
            dialog.setAlwaysOnTop(true);
        }
    }
    
    /**
     * Forces dialog to front using multiple techniques
     */
    private static void forceDialogToFront(JDialog dialog) {
        try {
            SwingUtilities.invokeLater(() -> {
                dialog.toFront();
                dialog.requestFocus();
                
                // Additional forcing for different platforms
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    // macOS-specific focus forcing
                    dialog.setVisible(true);
                    dialog.toFront();
                    dialog.requestFocus();
                }
            });
        } catch (Exception e) {
            System.err.println("Error forcing dialog to front: " + e.getMessage());
        }
    }
    
    /**
     * Gets appropriate icon for message type
     */
    private static Icon getIconForMessageType(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return UIManager.getIcon("OptionPane.errorIcon");
            case JOptionPane.WARNING_MESSAGE:
                return UIManager.getIcon("OptionPane.warningIcon");
            case JOptionPane.INFORMATION_MESSAGE:
                return UIManager.getIcon("OptionPane.informationIcon");
            case JOptionPane.QUESTION_MESSAGE:
                return UIManager.getIcon("OptionPane.questionIcon");
            default:
                return UIManager.getIcon("OptionPane.informationIcon");
        }
    }
}