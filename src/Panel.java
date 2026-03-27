import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.RenderingHints;
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

        // --- UI SETUP ---
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
        topBar.setSize(screenWidth, 50);
        topBar.setLocation(0, 0);
        topBar.setAlwaysOnTop(true);
        topBar.setBackground(new Color(0, 0, 0, 0));
        topBar.setLayout(new BorderLayout());

        JDialog backOverlay = new JDialog((Frame)null, "BackOverlay");
        backOverlay.setUndecorated(true);
        backOverlay.setSize(150, 50);
        backOverlay.setLocation(0, screenHeight - 50);
        backOverlay.setAlwaysOnTop(true);
        backOverlay.setBackground(new Color(0, 0, 0, 0));
        backOverlay.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        // ESCUDO
        JPanel clickBlocker = new JPanel();
        clickBlocker.setBackground(new Color(0, 0, 0, 1));
        clickBlocker.addMouseListener(new java.awt.event.MouseAdapter() {});

        JButton backBtn = new JButton("Volver al menú");
        backBtn.setBorder(null);
        backBtn.setFocusPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setOpaque(true);
        backBtn.setPreferredSize(new Dimension(150, 50));
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
            backOverlay.setVisible(false);

            focusWindow("Controller Remote", true, null, null, null);
            focusWindow("Luxes", true, null, null, null);
        });

        topBar.add(clickBlocker, BorderLayout.CENTER);
        backOverlay.add(backBtn);

        // --- BOTÓN 1: MAESTRO ---
        JButton button1 = new JButton(scaledImgBtn1);
        configurarBotonEstricto(button1, strictBtnSize);

        mouseAdapter(button1);
        button1.addActionListener(e -> {
            focusWindow("Controller Remote", false, topBar, backOverlay, frame);
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
            focusWindow("Luxes", false, topBar, backOverlay, frame);
        });

        button2.setFocusPainted(false);
        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer2.add(button2);
        btnContainer2.add(Box.createVerticalStrut(2));

        // --- CONFIGURACIÓN FINAL DEL FRAME CON BOTÓN DE REINICIO FLOTANTE ---
        // Usamos un ContentPane con "Layout Nulo" para poner posiciones absolutas y Z-Order
        JPanel mainContent = new JPanel(null);

        // 1. El botón flotante (Capa superior)
        JButton restartBtn = new JButton("Reiniciar Módulos");
        restartBtn.setFont(new Font("Arial", Font.BOLD, 14));
        restartBtn.setBackground(new Color(200, 50, 50));
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setFocusPainted(false);

        // Coordenadas manuales: X=20, Y=20, Ancho=160, Alto=40
        restartBtn.setBounds(20, 20, 200, 40);

        restartBtn.addActionListener(e -> {
            JDialog loading = createLoadingScreen(frame, screenSize);
            new Thread(() -> {
                killProcesses();
                initPersistentProcesses();
                monitorBackgroundProcesses(loading, frame, topBar, backOverlay);
            }).start();
            loading.setVisible(true);
        });

        // 2. Panel central (Capa base, ocupa el 100% de la pantalla)
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, screenWidth / 8, screenHeight / 3));
        centerPanel.setBounds(0, 0, screenWidth, screenHeight); // Tamaño idéntico a la pantalla real
        centerPanel.add(btnContainer1);
        centerPanel.add(btnContainer2);

        // 3. Ensamblamos forzando el orden de las capas (Z-Order)
        mainContent.add(restartBtn);
        mainContent.add(centerPanel);

        // Importante: El índice 0 significa que se pinta por encima de todo
        mainContent.setComponentZOrder(restartBtn, 0);
        mainContent.setComponentZOrder(centerPanel, 1);

        // Aplicamos el panel maestro al frame
        frame.setContentPane(mainContent);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            killProcesses();
        }));

        // Primera carga al arrancar
        JDialog initialLoading = createLoadingScreen(frame, screenSize);
        initPersistentProcesses();
        monitorBackgroundProcesses(initialLoading, frame, topBar, backOverlay);

        frame.setVisible(true);
        initialLoading.setVisible(true);
    }

    // --- NUEVO: CREAR PANTALLA DE CARGA BAJO DEMANDA ---
    private static JDialog createLoadingScreen(JFrame frame, Dimension screenSize) {
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
        return loadingScreen;
    }

    // --- NUEVO: DESTRUCCIÓN SEGURA DE PROCESOS ---
    private static void killProcesses() {
        try {
            if (maestroProc.get() != null) maestroProc.get().destroyForcibly();

            // Cerramos la web de forma amigable con el gestor de ventanas
            new ProcessBuilder("wmctrl", "-c", "Luxes").start().waitFor();

            // OBLIGATORIO: Esperar 2 segundos para dar tiempo a limpiar ventanas cerradas
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("Error matando procesos: " + e.getMessage());
        }
    }

    // --- MONITOR DE CARGA ---
    private static void monitorBackgroundProcesses(JDialog loadingScreen, JFrame frame, JDialog topBar, JDialog backOverlay) {
        new Thread(() -> {
            boolean maestroReady = false;
            boolean webReady = false;
            long startMs = System.currentTimeMillis();
            long maestroTimeoutMs = 30000;
            long webTimeoutMs = 12000;

            while (true) {
                long elapsed = System.currentTimeMillis() - startMs;
                boolean mustWaitMaestro = !maestroReady && elapsed < maestroTimeoutMs;
                boolean mustWaitWeb = !webReady && elapsed < webTimeoutMs;
                if (!mustWaitMaestro && !mustWaitWeb) break;

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

            if (maestroReady) {
                focusWindow("Controller Remote", true, null, null, null);
            }
            if (webReady) {
                focusWindow("Luxes", true, null, null, null);
            }

            final boolean finalMaestroReady = maestroReady;
            SwingUtilities.invokeLater(() -> {
                loadingScreen.dispose();
                if (finalMaestroReady) {
                    focusWindow("Controller Remote", false, topBar, backOverlay, frame);
                } else {
                    System.err.println("Maestro no estuvo listo antes del timeout. Mostrando menu principal.");
                    frame.setAlwaysOnTop(true);
                    frame.setVisible(true);
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    topBar.setVisible(false);
                    backOverlay.setVisible(false);
                }
            });

        }).start();
    }

    // --- REDIMENSIONADO DE IMAGEN (100% SÍNCRONO Y SEGURO) ---
    private static ImageIcon getScaledIcon(URL imageUrl, int width, int height) {
        if (imageUrl != null) {
            try {
                BufferedImage original = ImageIO.read(imageUrl);
                BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = resized.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(original, 0, 0, width, height, null);
                g2d.dispose();
                return new ImageIcon(resized);
            } catch (IOException e) {
                System.err.println("Error crítico cargando imagen " + imageUrl + ": " + e.getMessage());
            }
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
    private static void focusWindow(String title, boolean hide, JDialog topBar, JDialog backOverlay, JFrame mainFrame) {
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

                        if (backOverlay != null) {
                            backOverlay.setVisible(true);
                            backOverlay.setAlwaysOnTop(false);
                            backOverlay.setAlwaysOnTop(true);
                            backOverlay.toFront();
                            backOverlay.repaint();
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