import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

class Panel {
    private static final AtomicReference<Process> maestroProc = new AtomicReference<>();
    private static final AtomicReference<Process> webProc = new AtomicReference<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel btnContainer1 = new JPanel();
        JPanel btnContainer2 = new JPanel();
        frame.setUndecorated(true);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        // --- ESCALADO 1.3x ---
        int btnWidth = (int) (screenWidth * 0.195);
        int btnHeight = (int) (screenHeight * 0.325);
        Dimension strictBtnSize = new Dimension(btnWidth, btnHeight);

        // --- PANTALLA DE CARGA ---
        JDialog loadingScreen = new JDialog(frame, "Cargando", true);
        loadingScreen.setUndecorated(true);
        loadingScreen.setSize(screenSize);
        loadingScreen.setAlwaysOnTop(true);
        loadingScreen.getContentPane().setBackground(Color.BLACK);
        loadingScreen.setLayout(new GridBagLayout());

        JLabel loadingLabel = new JLabel("Iniciando módulos, por favor espere...");
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 30));
        loadingScreen.add(loadingLabel);

        // --- UI SETUP ---
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        btnContainer1.setLayout(new BoxLayout(btnContainer1, BoxLayout.Y_AXIS));
        btnContainer2.setLayout(new BoxLayout(btnContainer2, BoxLayout.Y_AXIS));

        // Las imágenes se estiran al 100%
        URL imageUrlBtn1 = Panel.class.getResource("/images/1.png");
        ImageIcon scaledImgBtn1 = getScaledIcon(imageUrlBtn1, btnWidth, btnHeight);

        URL imageUrlBtn2 = Panel.class.getResource("/images/2.png");
        ImageIcon scaledImgBtn2 = getScaledIcon(imageUrlBtn2, btnWidth, btnHeight);

        // --- TOPBAR / NAVEGACIÓN (Escudo Invisible) ---
        JDialog topBar = new JDialog((Frame)null, "Navigation");
        topBar.setUndecorated(true);
        // Ocupa todo el ancho de la pantalla y le damos 50px de alto para tapar bien las pestañas
        topBar.setSize(screenWidth, 50);
        topBar.setLocation(0, 0);
        topBar.setAlwaysOnTop(true);

        // Hacemos que la ventana en sí sea transparente para el sistema
        topBar.setBackground(new Color(0, 0, 0, 0));
        topBar.setLayout(new BorderLayout());
        topBar.getContentPane().setCursor(blankCursor);

        // ESCUDO: Este panel se traga los clics para que no lleguen a la app de abajo
        JPanel clickBlocker = new JPanel();
        // Alpha de 1/255: Invisible a la vista, pero X11 lo trata como un muro sólido
        clickBlocker.setBackground(new Color(0, 0, 0, 1));
        clickBlocker.addMouseListener(new java.awt.event.MouseAdapter() {}); // Absorbe los toques

        JButton backBtn = new JButton("Volver al menú");
        backBtn.setBorder(null);
        backBtn.setFocusPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setOpaque(true);
        backBtn.setPreferredSize(new Dimension(150, 50)); // Dimensionamos el botón

        backBtn.setBackground(Color.WHITE);
        backBtn.setForeground(Color.BLACK);
        backBtn.setFont(new Font("Arial", Font.BOLD, 14));

        backBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                backBtn.setBackground(new Color(173, 216, 230));
            }
            public void mouseReleased(java.awt.event.MouseEvent e) {
                backBtn.setBackground(Color.WHITE);
            }
        });

        backBtn.addActionListener(e -> {
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);

            focusWindow("Controller Remote", true, null, null);
            focusWindow("Luxes", true, null, null);
        });

        // Ensamblamos la barra: El escudo al centro (ocupa todo el espacio sobrante) y el botón a la derecha
        topBar.add(clickBlocker, BorderLayout.CENTER);
        topBar.add(backBtn, BorderLayout.EAST);

        // --- BOTÓN 1: MAESTRO ---
        JButton button1 = new JButton(scaledImgBtn1);
        configurarBotonEstricto(button1, strictBtnSize);

        mouseAdapter(button1);
        button1.addActionListener(e -> {
            focusWindow("Controller Remote", false, topBar, frame);
        });

        button1.setFocusPainted(false);
        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer1.add(button1);
        btnContainer1.add(Box.createVerticalStrut(2));

        // --- BOTÓN 2: WEB ---
        JButton button2 = new JButton(scaledImgBtn2);
        configurarBotonEstricto(button2, strictBtnSize);

        mouseAdapter(button2);
        button2.addActionListener(e -> {
            focusWindow("Luxes", false, topBar, frame);
        });

        button2.setFocusPainted(false);
        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer2.add(button2);
        btnContainer2.add(Box.createVerticalStrut(2));

        // --- CONFIGURACIÓN FINAL DEL FRAME ---
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, screenWidth / 8, screenHeight / 3));
        frame.add(btnContainer1);
        frame.add(btnContainer2);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setCursor(blankCursor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (maestroProc.get() != null) maestroProc.get().destroyForcibly();
            try {
                new ProcessBuilder("wmctrl", "-c", "Luxes").start();
            } catch (IOException e) {}
        }));

        initPersistentProcesses();
        monitorBackgroundProcesses(loadingScreen, frame, topBar);


        frame.setVisible(true);
        loadingScreen.setVisible(true);
    }

    // --- MONITOR DE CARGA ---
    private static void monitorBackgroundProcesses(JDialog loadingScreen, JFrame frame, JDialog topBar) {
        new Thread(() -> {
            boolean maestroReady = false;
            boolean webReady = false;

            while (!maestroReady || !webReady) {
                try {
                    Process p = new ProcessBuilder("wmctrl", "-l").start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String lowerline = line.toLowerCase();

                        if (lowerline.contains("controller remote")) maestroReady = true;
                        if (lowerline.contains("luxes")) webReady = true;
                    }
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.err.println("Error monitoreando ventanas: " + e.getMessage());
                }
            }

            focusWindow("Controller Remote", true, null, null);
            focusWindow("Luxes", true, null, null);

            SwingUtilities.invokeLater(() -> {
                loadingScreen.dispose();
                focusWindow("Controller Remote", false, topBar, frame);
            });

        }).start();
    }

    // --- REDIMENSIONADO DE IMAGEN ---
    private static ImageIcon getScaledIcon(URL imageUrl, int width, int height) {
        if (imageUrl != null) {
            ImageIcon original = new ImageIcon(imageUrl);
            Image img = original.getImage();
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return null;
    }

    // --- FORZAR TAMAÑO DE BOTÓN 100% IMAGEN ---
    private static void configurarBotonEstricto(JButton button, Dimension dim) {
        button.setBorder(null);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setMinimumSize(dim);
        button.setMaximumSize(dim);
        button.setPreferredSize(dim);
    }

    private static void initPersistentProcesses() {
        try {
            maestroProc.set(new ProcessBuilder("bash", "-c", "java -jar ~/Desktop/maestro_patched_v4_undecorated.jar").start());
            webProc.set(new ProcessBuilder("bash", "-c", "xdg-open ~/Desktop/web.desktop").start());

        } catch (IOException e) {
            System.err.println("Error starting processes: " + e.getMessage());
        }
    }

    // --- FOCUS WINDOW REESCRITO PARA ELIMINAR PARPADEOS ---
    private static void focusWindow(String title, boolean hide, JDialog topBar, JFrame mainFrame) {
        new Thread(() -> {
            try {
                if (hide) {
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "add,hidden").start().waitFor();
                } else {
                    new ProcessBuilder("wmctrl", "-a", title).start().waitFor();
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "remove,hidden").start().waitFor();

                    Thread.sleep(400);

                    SwingUtilities.invokeLater(() -> {
                        if (mainFrame != null) {
                            mainFrame.setAlwaysOnTop(false);
                            mainFrame.setVisible(false);
                        }

                        if (topBar != null) {
                            topBar.setLocation(0, 0);
                            topBar.setVisible(true);
                            topBar.setAlwaysOnTop(false);
                            topBar.setAlwaysOnTop(true);
                            topBar.toFront();
                            topBar.repaint();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("wmctrl failed: " + e.getMessage());
            }
        }).start();
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
