import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

class Panel {
    // Referencias separadas para cada proceso
    private static final AtomicReference<Process> maestroProc = new AtomicReference<>();
    private static final AtomicReference<Process> webProc = new AtomicReference<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel btnContainer1 = new JPanel();
        JPanel btnContainer2 = new JPanel();
        frame.setUndecorated(true);

        // 1. INICIALIZAR PROCESOS (quedarán por debajo inicialmente)
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
            // NUEVO: Volver a forzar el menú por encima de todo
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);

            // Ocultar las ventanas de fondo
            focusWindow("Maestro", true, topBar);
            focusWindow("Luxes", true, topBar);
        });
        topBar.add(backBtn);

        // --- BOTÓN 1: MAESTRO ---
        JButton button1 = new JButton(img);
        button1.setBorder(null);
        JLabel label1 = new JLabel("Maestro");
        label1.setFont(new Font("Arial", Font.BOLD, 14));
        button1.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button1);
        button1.addActionListener(e -> {
            // NUEVO: Quitar el alwaysOnTop antes de ocultar el menú
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
        JButton button2 = new JButton(img);
        button2.setBorder(null);
        JLabel label2 = new JLabel("Luxes - Expertos en Iluminación");
        label2.setFont(new Font("Arial", Font.BOLD, 14));
        button2.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button2);
        button2.addActionListener(e -> {
            // NUEVO: Quitar el alwaysOnTop antes de ocultar el menú
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
        // NUEVO: El menú arranca por encima de todo para evitar que los programas lentos lo tapen
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // NUEVO: ShutdownHook corregido para cerrar xdg-open indirectamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (maestroProc.get() != null) maestroProc.get().destroyForcibly();

            // Cerrar la ventana del navegador enviando la señal con wmctrl
            try {
                new ProcessBuilder("wmctrl", "-c", "Luxes").start();
            } catch (IOException e) {
                System.err.println("No se pudo cerrar el navegador: " + e.getMessage());
            }
        }));

        frame.setVisible(true);
    }

    private static void initPersistentProcesses() {
        try {
            // Lanzar Maestro
            Process maestro = new ProcessBuilder("bash", "-c", "java -jar ~/Desktop/maestro_patched_v4.jar").start();
            maestroProc.set(maestro);

            // Lanzar Web (xdg-open delega el proceso al sistema)
            Process web = new ProcessBuilder("bash", "-c", "xdg-open ~/Desktop/web.desktop").start();
            webProc.set(web);
        } catch (IOException e) {
            System.err.println("Error starting background processes: " + e.getMessage());
        }
    }

    /**
     * Utiliza wmctrl para manipular ventanas externas en un hilo separado.
     * @param title El título de la ventana objetivo.
     * @param hide True para minimizar, false para traer al frente.
     * @param topBar El diálogo de navegación que debe quedar por encima (puede ser null).
     */
    private static void focusWindow(String title, boolean hide, JDialog topBar) {
        new Thread(() -> {
            try {
                if (hide) {
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "add,hidden").start().waitFor();
                } else {
                    // 1. Quitar el estado 'hidden'
                    new ProcessBuilder("wmctrl", "-r", title, "-b", "remove,hidden").start().waitFor();

                    // 2. Traer la ventana al frente
                    new ProcessBuilder("wmctrl", "-a", title).start().waitFor();

                    // 3. Si necesitamos que la barra superior sobreviva, la forzamos a subir
                    if (topBar != null) {
                        Thread.sleep(300); // Pausa táctica para que Linux termine sus animaciones/foco

                        SwingUtilities.invokeLater(() -> {
                            // Resetear el estado fuerza al SO a repintarla en la capa más alta
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