import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

class Panel {
    public static void main(String[] args){
        // Creating instance of JFrame and JPanel
        JFrame frame = new JFrame();
        JPanel btn1 = new JPanel();
        JPanel btn2 = new JPanel();
        frame.setUndecorated(true); //borderless

        ImageIcon img = null;

        //CUSTOM CURSOR
        // Create a 1x1 transparent image
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a blank cursor
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        AtomicReference<Process> currentProcess = new AtomicReference<>();

        //set layouts
        btn1.setLayout(new BoxLayout(btn1, BoxLayout.Y_AXIS));
        btn2.setLayout(new BoxLayout(btn2, BoxLayout.Y_AXIS));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();

        // Calculate button size (e.g., 10% of width, 5% of height)
        int btnWidth = (int) (screenWidth * 0.10);
        int btnHeight = (int) (screenHeight * 0.3);

        //adding an image
        URL imageUrl = Panel.class.getResource("/images/logo_luxes.png");
        if (imageUrl != null) {
            img = new ImageIcon(imageUrl);
            // Use your icon here
        } else {
            System.err.println("Could not find image file!");
        }

        //TOPBAR
        // Create a small bar that stays at the top
        JDialog topBar = new JDialog(frame, "Navigation");
        topBar.setUndecorated(true);
        topBar.setSize(110, 40);
        topBar.setLocation(screenWidth-110, 0);
        topBar.setAlwaysOnTop(true); // This keeps it above other apps
        topBar.setLayout(new FlowLayout(FlowLayout.LEFT));

        topBar.getContentPane().setCursor(blankCursor);

        JButton backBtn = new JButton("Back to menu");
        backBtn.setBorder(null);
        backBtn.setPreferredSize(new Dimension(100, 30));
        backBtn.addActionListener(e -> {
            // Bring the main menu to front and hide this bar
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            topBar.setVisible(false);

            // Optional: Kill the current running program when returning
            if (currentProcess.get() != null) currentProcess.get().destroy();
        });

        topBar.add(backBtn);

        //topBar.setVisible(true); //uncomment to test topbar

        // JBUTTON1
        JButton button1 = new JButton(img);
        button1.setBorder(null);
        JLabel label1 = new JLabel("Maestro");
        label1.setFont(new Font("Arial", Font.BOLD, 14));

        button1.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button1); //calls a method to add functionality to the button
        button1.addActionListener(e -> {
            try {
                // 1. Launch the Linux program
                // This tells Linux: "Hey Bash, please run this string for me."
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", "java -jar ~/Desktop/maestro_patched_v4.jar");
                currentProcess.set(pb.start());

                // 2. Hide the main menu and show the overlay
                frame.setVisible(false);
                topBar.setVisible(true);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
            }
        });

        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn1.add(button1);
        btn1.add(Box.createVerticalStrut(10));
        btn1.add(label1);

        frame.add(btn1); //adds button to frame

        //JBUTTON2
        JButton button2 = new JButton(img);
        button2.setBorder(null);
        JLabel label2 = new JLabel("Luxes - Expertos en Iluminación y soluciones a medida");
        label2.setFont(new Font("Arial", Font.BOLD, 14));

        button2.setPreferredSize(new Dimension(btnWidth, btnHeight));

        mouseAdapter(button2); //calls a method to add functionality to the button
        button2.addActionListener(e -> {
            try {
                // 1. Launch the Linux program
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", "xdg-open ~/home/linaro/Desktop/web.desktop");
                currentProcess.set(pb.start());

                // 2. Hide the main menu and show the overlay
                frame.setVisible(false);
                topBar.setVisible(true);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
            }
        });

        //aligns button and label
        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn2.add(button2);
        btn2.add(Box.createVerticalStrut(10));
        btn2.add(label2);

        frame.add(btn2); //adds button to frame

        //FRAME
        // Set a preferred size for the PANELS, not just the buttons
        Dimension panelSize = new Dimension(screenWidth / 4, screenHeight / 3);
        btn1.setPreferredSize(panelSize);
        btn2.setPreferredSize(panelSize);
        // set frame layout
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, screenWidth/8, screenHeight/3));

        //CURSORLESS
        // Apply it to your frame
        //frame.getContentPane().setCursor(blankCursor); //COMMENT TO SEE CURSOR

        //could setup another shortcut for closing, but alt f4 works fine so could be this for now

        // making the frame visible
        frame.setExtendedState(Frame.MAXIMIZED_BOTH); //extends to all of the screen
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //exits the program when window is closed
        frame.setVisible(true); //comment to test topbar
    }

    private static void mouseAdapter(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            // When the mouse is pressed, move the button down 2 pixels (the "click" effect)
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() + 2);
            }

            // When released, move it back up
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setLocation(button.getX(), button.getY() - 2);
            }
        });
    }
}