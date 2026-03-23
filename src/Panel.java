import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class Panel {

    private static final String MAESTRO_JAR = "/home/linaro/Desktop/maestro_patched_v4.jar";
    private static final String WEB_URL = "https://luxes.es";

    // Ajusta estos títulos según lo que te devuelva `wmctrl -l`
    private static final String MAESTRO_WINDOW_TITLE = "Maestro";
    private static final String FIREFOX_WINDOW_TITLE = "Mozilla Firefox";

    private static final AtomicReference<Process> maestroProcess = new AtomicReference<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel btn1 = new JPanel();
        JPanel btn2 = new JPanel();
        frame.setUndecorated(true);

        ImageIcon img = null;

        // CUSTOM CURSOR
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor"
        );

        // Set layouts
        btn1.setLayout(new BoxLayout(btn1, BoxLayout.Y_AXIS));
        btn2.setLayout(new BoxLayout(btn2, BoxLayout.Y_AXIS));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        int btnWidth = (int) (screenWidth * 0.10);
        int btnHeight = (int) (screenHeight * 0.3);

        // Image
        URL imageUrl = Panel.class.getResource("/images/logo_luxes.png");
        if (imageUrl != null) {
            img = new ImageIcon(imageUrl);
        } else {
            System.err.println("Could not find image file!");
        }

        // TOP BAR
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
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);
        });

        topBar.add(backBtn);

        // BUTTON 1 - MAESTRO
        JButton button1 = new JButton(img);
        button1.setBorder(null);
        JLabel label1 = new JLabel("Maestro");
        label1.setFont(new Font("Arial", Font.BOLD, 14));
        button1.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button1);

        button1.addActionListener(e -> {
            try {
                launchOrFocusMaestro();
                frame.setVisible(false);
                topBar.setVisible(true);
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error launching Maestro: " + ex.getMessage());
            }
        });

        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn1.add(button1);
        btn1.add(Box.createVerticalStrut(10));
        btn1.add(label1);

        frame.add(btn1);

        // BUTTON 2 - FIREFOX
        JButton button2 = new JButton(img);
        button2.setBorder(null);
        JLabel label2 = new JLabel("Luxes - Expertos en Iluminación y soluciones a medida");
        label2.setFont(new Font("Arial", Font.BOLD, 14));
        button2.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button2);

        button2.addActionListener(e -> {
            try {
                launchOrFocusFirefox();
                frame.setVisible(false);
                topBar.setVisible(true);
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error launching Firefox: " + ex.getMessage());
            }
        });

        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn2.add(button2);
        btn2.add(Box.createVerticalStrut(10));
        btn2.add(label2);

        frame.add(btn2);

        // FRAME
        Dimension panelSize = new Dimension(screenWidth / 4, screenHeight / 3);
        btn1.setPreferredSize(panelSize);
        btn2.setPreferredSize(panelSize);

        frame.setLayout(new FlowLayout(FlowLayout.CENTER, screenWidth / 8, screenHeight / 3));

        // Optional: hide cursor
        // frame.getContentPane().setCursor(blankCursor);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void mouseAdapter(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() + 2);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() - 2);
            }
        });
    }

    private static void launchOrFocusMaestro() throws IOException, InterruptedException {
        Process process = maestroProcess.get();

        if (process == null || !process.isAlive()) {
            Process newProcess = new ProcessBuilder(
                    "/usr/bin/java",
                    "-jar",
                    MAESTRO_JAR
            ).start();

            maestroProcess.set(newProcess);

            // Espera breve para que aparezca la ventana
            Thread.sleep(1200);
        }

        focusWindow(MAESTRO_WINDOW_TITLE);
    }

    private static void launchOrFocusFirefox() throws IOException, InterruptedException {
        if (windowExists(FIREFOX_WINDOW_TITLE)) {
            focusWindow(FIREFOX_WINDOW_TITLE);
        } else {
            new ProcessBuilder(
                    "firefox",
                    "-new-window",
                    "-kiosk",
                    WEB_URL
            ).start();

            // Espera breve para que aparezca la ventana
            Thread.sleep(1500);
            focusWindow(FIREFOX_WINDOW_TITLE);
        }
    }

    private static boolean windowExists(String title) throws IOException, InterruptedException {
        String command = "wmctrl -l | grep -i \"" + title.replace("\"", "\\\"") + "\"";
        Process process = new ProcessBuilder("bash", "-c", command).start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private static void focusWindow(String title) throws IOException, InterruptedException {
        new ProcessBuilder("wmctrl", "-a", title).start().waitFor();
    }
}