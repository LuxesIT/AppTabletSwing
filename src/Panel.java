import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

class Panel {
    // Separate references for each process so they stay running
    private static final AtomicReference<Process> maestroProc = new AtomicReference<>();
    private static final AtomicReference<Process> webProc = new AtomicReference<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel btnContainer1 = new JPanel();
        JPanel btnContainer2 = new JPanel();
        frame.setUndecorated(true);

        // 1. INITIALIZE PROCESSES AT STARTUP
        // These will start behind your main frame
        initPersistentProcesses();

        // --- UI SETUP ---
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        btnContainer1.setLayout(new BoxLayout(btnContainer1, BoxLayout.Y_AXIS));
        btnContainer2.setLayout(new BoxLayout(btnContainer2, BoxLayout.Y_AXIS));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        int btnWidth = (int) (screenWidth * 0.10);
        int btnHeight = (int) (screenHeight * 0.3);

        ImageIcon img = null;
        URL imageUrl = Panel.class.getResource("/images/logo_luxes.png");
        if (imageUrl != null) {
            img = new ImageIcon(imageUrl);
        }

        // --- TOPBAR / NAVIGATION ---
        JDialog topBar = new JDialog(frame, "Navigation");
        topBar.setUndecorated(true);
        topBar.setSize(110, 40);
        topBar.setLocation(screenWidth - 110, 0);
        topBar.setAlwaysOnTop(true);
        topBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        topBar.getContentPane().setCursor(blankCursor);

        JButton backBtn = new JButton("Back to menu");
        backBtn.setBorder(null);
        backBtn.setPreferredSize(new Dimension(100, 30));
        backBtn.addActionListener(e -> {
            // Simply cover the background apps by showing the main menu
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);

            // Optional: Minimize the Maestro window so it doesn't peek through
            focusWindow("Maestro", true);
        });
        topBar.add(backBtn);

        // --- BUTTON 1: MAESTRO ---
        JButton button1 = new JButton(img);
        button1.setBorder(null);
        JLabel label1 = new JLabel("Maestro");
        label1.setFont(new Font("Arial", Font.BOLD, 14));
        button1.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button1);
        button1.addActionListener(e -> {
            // Switch to the existing Maestro window
            focusWindow("Maestro", false);
            frame.setVisible(false);
            topBar.setVisible(true);
        });

        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer1.add(button1);
        btnContainer1.add(Box.createVerticalStrut(10));
        btnContainer1.add(label1);

        // --- BUTTON 2: WEB ---
        JButton button2 = new JButton(img);
        button2.setBorder(null);
        JLabel label2 = new JLabel("Luxes - Expertos en Iluminación");
        label2.setFont(new Font("Arial", Font.BOLD, 14));
        button2.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button2);
        button2.addActionListener(e -> {
            // Switch to the existing Web/Browser window
            // Note: Use a unique part of the browser's window title here
            focusWindow("Luxes", false);
            frame.setVisible(false);
            topBar.setVisible(true);
        });

        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer2.add(button2);
        btnContainer2.add(Box.createVerticalStrut(10));
        btnContainer2.add(label2);

        // --- FRAME FINAL SETUP ---
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, screenWidth / 8, screenHeight / 3));
        frame.add(btnContainer1);
        frame.add(btnContainer2);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Ensure processes die when the main app closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (maestroProc.get() != null) maestroProc.get().destroyForcibly();
            if (webProc.get() != null) webProc.get().destroyForcibly();
        }));

        frame.setVisible(true);
    }

    private static void initPersistentProcesses() {
        try {
            // Launch Maestro
            maestroProc.set(new ProcessBuilder("bash", "-c", "java -jar ~/Desktop/maestro_patched_v4.jar").start());

            // Launch Web
            webProc.set(new ProcessBuilder("bash", "-c", "xdg-open ~/Desktop/web.desktop").start());
        } catch (IOException e) {
            System.err.println("Error starting background processes: " + e.getMessage());
        }
    }

    /**
     * Uses wmctrl to manipulate external windows.
     * @param title The title (or partial title) of the window.
     * @param hide If true, minimizes the window. If false, brings it to front.
     */
    private static void focusWindow(String title, boolean hide) {
        try {
            if (hide) {
                // Minimize window
                new ProcessBuilder("wmctrl", "-r", title, "-b", "add,hidden").start();
            } else {
                // Bring window to front
                new ProcessBuilder("wmctrl", "-a", title).start();
            }
        } catch (IOException e) {
            System.err.println("wmctrl failed: " + e.getMessage());
        }
    }

    private static void mouseAdapter(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() + 2);
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() - 2);
            }
        });
    }
}