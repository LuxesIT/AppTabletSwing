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

        // Calcular tamaños estrictos
        int btnWidth = (int) (screenWidth * 0.15); // Un poco más ancho para que quepa bien
        int btnHeight = (int) (screenHeight * 0.25);
        Dimension strictBtnSize = new Dimension(btnWidth, btnHeight);

        // --- PANTALLA DE CARGA ---
        JDialog loadingScreen = new JDialog(frame, "Cargando", true); // Modal
        loadingScreen.setUndecorated(true);
        loadingScreen.setSize(screenSize);
        loadingScreen.setAlwaysOnTop(true);
        loadingScreen.getContentPane().setBackground(Color.BLACK);
        loadingScreen.setLayout(new GridBagLayout());

        JLabel loadingLabel = new JLabel("Iniciando módulos, por favor espere...");
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("Arial", Font.BOLD, 24));
        loadingScreen.add(loadingLabel);

        // --- UI SETUP ---
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        btnContainer1.setLayout(new BoxLayout(btnContainer1, BoxLayout.Y_AXIS));
        btnContainer2.setLayout(new BoxLayout(btnContainer2, BoxLayout.Y_AXIS));

        // Cargar y redimensionar imagen (dejamos un margen del 20% para que respire dentro del botón)
        URL imageUrl = Panel.class.getResource("/images/logo_luxes.png");
        ImageIcon scaledImg = getScaledIcon(imageUrl, (int)(btnWidth * 0.8), (int)(btnHeight * 0.8));

        // --- TOPBAR / NAVEGACIÓN ---
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
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);

            focusWindow("Maestro", true, null);
            focusWindow("Luxes", true, null);
        });
        topBar.add(backBtn);

        // --- BOTÓN 1: MAESTRO ---
        JButton button1 = new JButton(scaledImg); // Usamos la imagen escalada
        configurarBotonEstricto(button1, strictBtnSize); // Forzamos tamaño

        JLabel label1 = new JLabel("Maestro");
        label1.setFont(new Font("Arial", Font.BOLD, 14));

        mouseAdapter(button1);
        button1.addActionListener(e -> {
            frame.setAlwaysOnTop(false);
            frame.setVisible(false);
            topBar.setVisible(true);
            focusWindow("Maestro", false, topBar);
        });

        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer1.add(button1);
        btnContainer1.add(Box.createVerticalStrut(10));
        btnContainer1.add(label1);

        // --- BOTÓN 2: WEB ---
        JButton button2 = new JButton(scaledImg);
        configurarBotonEstricto(button2, strictBtnSize);

        JLabel label2 = new JLabel("Luxes - Expertos en Iluminación");
        label2.setFont(new Font("Arial", Font.BOLD, 14));

        mouseAdapter(button2);
        button2.addActionListener(e -> {
            frame.setAlwaysOnTop(false);
            frame.setVisible(false);
            topBar.setVisible(true);
            focusWindow("Luxes", false, topBar);
        });

        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnContainer2.add(button2);
        btnContainer2.add(Box.createVerticalStrut(10));
        btnContainer2.add(label2);

        // --- CONFIGURACIÓN FINAL DEL FRAME ---
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, screenWidth / 8, screenHeight / 3));
        frame.add(btnContainer1);
        frame.add(btnContainer2);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (maestroProc.get() != null) maestroProc.get().destroyForcibly();
            try {
                new ProcessBuilder("wmctrl", "-c", "Luxes").start();
            } catch (IOException e) {}
        }));

        // 1. Iniciamos los procesos
        initPersistentProcesses();

        // 2. Iniciamos el hilo que vigila cuándo las ventanas están listas
        monitorBackgroundProcesses(loadingScreen);

        // 3. Mostramos el frame base y bloqueamos con la pantalla de carga
        frame.setVisible(true);
        loadingScreen.setVisible(true); // Esto detiene la ejecución del hilo principal hasta que el monitor la cierre
    }

    // --- NUEVO: MONITOR DE CARGA ---
    private static void monitorBackgroundProcesses(JDialog loadingScreen) {
        new Thread(() -> {
            boolean maestroReady = false;
            boolean webReady = false;

            while (!maestroReady || !webReady) {
                try {
                    Process p = new ProcessBuilder("wmctrl", "-l").start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("Controller Remote (v0.11) - test")) maestroReady = true;
                        if (line.contains("Luxes - Expertos en Iluminación y soluciones a medida")) webReady = true;
                    }
                    Thread.sleep(500); // Esperar medio segundo antes de volver a preguntar
                } catch (Exception e) {
                    System.err.println("Error monitoreando ventanas: " + e.getMessage());
                }
            }

            // Una vez que ambas existen, las ocultamos y quitamos la pantalla de carga
            focusWindow("Maestro", true, null);
            focusWindow("Luxes", true, null);

            SwingUtilities.invokeLater(() -> {
                loadingScreen.dispose(); // Cierra la pantalla de carga y libera el menú principal
            });

        }).start();
    }

    // --- NUEVO: REDIMENSIONADO DE IMAGEN ---
    private static ImageIcon getScaledIcon(URL imageUrl, int width, int height) {
        if (imageUrl != null) {
            ImageIcon original = new ImageIcon(imageUrl);
            Image img = original.getImage();
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return null;
    }

    // --- NUEVO: FORZAR TAMAÑO DE BOTÓN ---
    private static void configurarBotonEstricto(JButton button, Dimension dim) {
        button.setBorder(null);
        button.setMinimumSize(dim);
        button.setMaximumSize(dim);
        button.setPreferredSize(dim);
        button.setBackground(Color.WHITE); // Opcional, para que el fondo del botón sea limpio
    }

    private static void initPersistentProcesses() {
        try {
            maestroProc.set(new ProcessBuilder("bash", "-c", "java -jar ~/Desktop/maestro_patched_v4_undecorated.jar").start());
            webProc.set(new ProcessBuilder("bash", "-c", "xdg-open ~/Desktop/web.desktop").start());
        } catch (IOException e) {
            System.err.println("Error starting processes: " + e.getMessage());
        }
    }

    private static void focusWindow(String title, boolean hide, JDialog topBar) {
        new Thread(() -> {
            try {
                if (hide) {
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "add,hidden").start().waitFor();
                } else {
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "remove,hidden").start().waitFor();
                    new ProcessBuilder("wmctrl", "-a", title).start().waitFor();

                    if (topBar != null) {
                        Thread.sleep(300);
                        SwingUtilities.invokeLater(() -> {
                            topBar.setAlwaysOnTop(false);
                            topBar.setAlwaysOnTop(true);
                            topBar.toFront();
                            topBar.repaint();
                        });
                    }
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