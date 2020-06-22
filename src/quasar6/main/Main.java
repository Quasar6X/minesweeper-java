package quasar6.main;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton class for making the GUI.
 */
public class Main {

    private static int flagsPlaced = 0;
    private static final JFrame app = new JFrame("Minesweeper by Quasar6");
    private static final JMenuBar bar = new JMenuBar();
    private static final JPanel clockPanel = new JPanel();
    private static final JPanel buttonPanel = new JPanel();
    private static final JLabel clockLabel = new JLabel("\u23F1 00:00:00 \u23F1");
    private static final JLabel flagsLabel = new JLabel(Integer.toString(flagsPlaced));
    private static final SpringLayout clockPanelLayout = new SpringLayout();
    private static final JRadioButtonMenuItem beginner = new JRadioButtonMenuItem(Field.BEGINNER);
    private static final JRadioButtonMenuItem intermediate = new JRadioButtonMenuItem(Field.INTERMEDIATE);
    private static final JRadioButtonMenuItem expert = new JRadioButtonMenuItem(Field.EXPERT);
    private static final JButton playPause = new JButton("\u25B6");

    /**
     * Necessary boolean for the {@link #clockTick()} method.
     * Without this the {@link #onPlayPause(ActionEvent)} method
     * could make an infinite number of threads.
     */
    private static final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * These two maps store the button values and the flag icons;
     * Keys are map entries with the coordinates of the button in the matrix.
     * {@link #buttonFlagOnPause}
     */
    private static final Map<Map.Entry<Integer, Integer>, String> buttonTextOnPause = new HashMap<>();
    private static final Map<Map.Entry<Integer, Integer>, ImageIcon> buttonFlagOnPause = new HashMap<>();

    /**
     * The measured time.
     * {@code time[0]} = hours
     * {@code time[1]} = minutes
     * {@code time[2]} = seconds
     */
    private static int[] time;
    private static MatrixJButton[][] buttons;
    private static String difficulty;
    private static boolean clockRun = false;
    private static String timeScore = "00:00:00";
    private static Main instance = null;

    /**
     * @return the single instance of this class
     */
    public static Main getInstance() {
        if (instance == null)
            instance = new Main();
        return instance;
    }

    /**
     * Construct the GUI windows and set it's parameters.
     * {@throws RuntimeException}
     */
    private Main()
    {
        if (instance != null)
            throw new RuntimeException("Singleton! Access this class through the getInstance() method");
        app.setIconImage(createIconForWindow());
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final SpringLayout mainLayout = new SpringLayout();
        mainLayout.putConstraint(SpringLayout.NORTH, clockPanel, 0, SpringLayout.NORTH, app);
        mainLayout.putConstraint(SpringLayout.NORTH, buttonPanel, 0, SpringLayout.SOUTH, clockPanel);
        clockPanelLayout.putConstraint(SpringLayout.WEST, flagsLabel, 14, SpringLayout.WEST, clockPanel);
        clockPanelLayout.putConstraint(SpringLayout.HORIZONTAL_CENTER, clockLabel, 0, SpringLayout.HORIZONTAL_CENTER, clockPanel);
        clockPanelLayout.putConstraint(SpringLayout.EAST, playPause, -18, SpringLayout.EAST, clockPanel);
        clockPanelLayout.putConstraint(SpringLayout.VERTICAL_CENTER, playPause, 25,SpringLayout.NORTH, buttonPanel);
        app.setLayout(mainLayout);
        clockPanel.setLayout(clockPanelLayout);
        app.setResizable(false);
        final JMenu diffMenu = new JMenu("Difficulty");
        final ButtonGroup radios = new ButtonGroup();
        final JButton help = new JButton("Help");
        help.setOpaque(true);
        help.setContentAreaFilled(false);
        help.setBorderPainted(false);
        help.setFocusable(false);
        help.setPreferredSize(new Dimension(5, diffMenu.getHeight()));
        help.addActionListener((ActionEvent e) -> {
            setOsTheme();
            JOptionPane.showMessageDialog(app,
                    "Press left-click to reveal a tile.\n" +
                            "Press right-click to mark a tile as potential bomb.\n" +
                            "In the top left corner you can see how many tiles you have marked.\n" +
                            "At the top you can see the clock. It starts measuring your time after the first reveal.\n" +
                            "In the top right corner there is the play/pause button.\n" +
                            "When you pause the game you will not see any state of the tiles.\n" +
                            "In the \"Difficulty\" menu you can change the difficulty any time.", "Help", JOptionPane.PLAIN_MESSAGE);
            setMetalTheme();
        });
        radios.add(beginner);
        radios.add(intermediate);
        radios.add(expert);
        diffMenu.add(beginner);
        diffMenu.add(intermediate);
        diffMenu.add(expert);
        bar.add(diffMenu);
        bar.add(help);
        app.setJMenuBar(bar);
        clockPanel.add(flagsLabel);
        clockPanel.add(clockLabel);
        clockPanel.add(playPause);
        app.add(clockPanel);
        app.add(buttonPanel);
        beginner.setSelected(true);
        beginner.addActionListener(this::onDifficultyChange);
        intermediate.addActionListener(this::onDifficultyChange);
        expert.addActionListener(this::onDifficultyChange);
        playPause.addActionListener(this::onPlayPause);
        playPause.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "Space");
        playPause.getActionMap().put("Space", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onPlayPause(e);
                playPause.requestFocusInWindow();
            }
        });
        playPause.setEnabled(false);
        clockLabel.setFont(new Font("Segoe", Font.BOLD, 34));
        flagsLabel.setFont(new Font("Segoe", Font.BOLD, 34));
        playPause.setFont(new Font("Segoe", Font.PLAIN, 20));
        playPause.setBackground(Color.DARK_GRAY);
        playPause.setForeground(Color.RED);
        playPause.setFocusable(false);
        clockLabel.setForeground(Color.RED);
        flagsLabel.setForeground(Color.RED);
        clockPanel.setBackground(Color.BLACK);
        app.setVisible(true);
        bar.setVisible(true);
        help.setVisible(true);
        expert.setVisible(true);
        beginner.setVisible(true);
        diffMenu.setVisible(true);
        playPause.setVisible(true);
        clockLabel.setVisible(true);
        flagsLabel.setVisible(true);
        clockPanel.setVisible(true);
        buttonPanel.setVisible(true);
        intermediate.setVisible(true);
    }

    /**
     * This must be called once to generate the GUI.
     *
     * @param diff The difficulty to set {@link Field#BEGINNER}
     */
    private void run(String diff)
    {
        if (diff == null || !Field.BEGINNER.equals(diff) && !Field.INTERMEDIATE.equals(diff) && !Field.EXPERT.equals(diff))
            throw new IllegalArgumentException("Wrong difficulty!");
        difficulty = diff;
        Field.generate(difficulty);
        buttons = new MatrixJButton[Field.getSizeX()][Field.getSizeY()];
        buttonPanel.setLayout(new GridLayout(Field.getSizeX(), Field.getSizeY()));
        clockPanel.setPreferredSize(new Dimension(Field.getSizeY() * 45, 50));
        for (int i = 0; i < Field.getSizeX(); i++) {
            for (int j = 0; j < Field.getSizeY(); j++) {
                buttons[i][j] = new MatrixJButton(i, j);
                buttons[i][j].setBackground(Color.DARK_GRAY);
                buttons[i][j].setForeground(Color.BLACK);
                buttons[i][j].setFont(new Font("Segoe", Font.PLAIN, 20));
                buttons[i][j].getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
                buttons[i][j].setPreferredSize(new Dimension(45, 45));
                buttons[i][j].setFocusable(false);
                buttons[i][j].addActionListener(this::onPress);
                buttons[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        if (clockRun)
                            if (e.getButton() == MouseEvent.BUTTON3) {
                                MatrixJButton btn = (MatrixJButton)e.getSource();
                                int x = btn.getRow();
                                int y = btn.getCol();
                                if (Field.getTileAt(x, y).isHidden())
                                    if(btn.isEnabled()) {
                                        if (btn.getIcon() == null) {
                                            btn.setIcon(createIcon("/quasar6/main/images/flag.gif", "flag"));
                                            ++flagsPlaced;
                                        } else {
                                            btn.setIcon(null);
                                            --flagsPlaced;
                                        }
                                        flagsLabel.setText(Integer.toString(flagsPlaced));
                                    }
                            }
                    }
                });
                buttonPanel.add(buttons[i][j]);
            }
        }
        app.setPreferredSize(new Dimension(Field.getSizeY() * 45 + app.getInsets().left + app.getInsets().right,
                Field.getSizeX() * 45 + clockPanel.getHeight() + bar.getHeight() + app.getInsets().top + app.getInsets().bottom));
        app.pack();
        centerWindow();
    }

    /**
     * Called when a button is pressed on the field.
     *
     * @param e ActionEvent received on button press
     */
    private void onPress(ActionEvent e)
    {
        if (!clockRun) {
            clockRun = true;
            playPause.setEnabled(true);
            playPause.setText("\u23F8");

            clockTick();
        }
        MatrixJButton btn = (MatrixJButton)e.getSource();
        if (btn.getIcon() != null)
            return;
        int x = btn.getRow();
        int y = btn.getCol();
        if (!Field.getTileAt(x, y).isHidden())
            return;
        if (!Field.getTileAt(x, y).isMine()) {
            if (Field.getTileAt(x, y).getRank() != 0) {
                Field.getTileAt(x, y).setHidden(false);
                btn.setBackground(Color.GRAY);
                btn.setForeground(Field.getTileAt(x, y).getColor());
                btn.setText(Integer.toString(Field.getTileAt(x, y).getRank()));
            } else {
                Field.revealTiles(x, y);
                for (int i = 0; i < Field.getSizeX(); i++)
                    for (int j = 0; j < Field.getSizeY(); j++)
                        if (!Field.getTileAt(i, j).isHidden()) {
                            if (buttons[i][j].getIcon() != null)
                                if ("flag".equals(((ImageIcon)buttons[i][j].getIcon()).getDescription()))
                                    buttons[i][j].setIcon(null);
                            buttons[i][j].setBackground(Color.GRAY);
                            if (Field.getTileAt(i, j).getRank() != 0) {
                                buttons[i][j].setForeground(Field.getTileAt(i, j).getColor());
                                buttons[i][j].setText(Integer.toString(Field.getTileAt(i, j).getRank()));
                            }
                        }
            }
        } else {
            playAudio(getClass().getResourceAsStream("/quasar6/main/sound/loose.wav"));
            clockRun = false;
            String correctFlags = Integer.toString(correctFlags());
            revealMines("/quasar6/main/images/mine.gif", "mine");
            setOsTheme();
            int restart = JOptionPane.showConfirmDialog(app, "You have successfully blown yourself up under " + timeScore
                    + "\nCorrect flags: " + correctFlags + "\nAnother game?", "Game Over", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
            setMetalTheme();
            if (restart != 0) {
                System.exit(0);
            } else {
                resetWidgets();
                Field.generate(difficulty);
            }
        }
        if (Field.isWinningState()) {
            playAudio(getClass().getResourceAsStream("/quasar6/main/sound/win.wav"));
            clockRun = false;
            String correctFlags = Integer.toString(correctFlags());
            revealMines("/quasar6/main/images/flag.gif", "flag");
            setOsTheme();
            int restart = JOptionPane.showConfirmDialog(app, "You win!\n" + "You have solved the " + difficulty
                    + " difficulty under " + timeScore + "\nCorrect flags: " + correctFlags + "\nAnother game?", "Winner", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
            setMetalTheme();
            if (restart != 0) {
                System.exit(0);
            } else {
                resetWidgets();
                Field.generate(difficulty);
            }
        }
    }

    /**
     * Called when the difficulty changed from the dropdown menu.
     *
     * @param e ActionEvent received on button press
     */
    private void onDifficultyChange(ActionEvent e)
    {
        JRadioButtonMenuItem btn = (JRadioButtonMenuItem)e.getSource();
        switch (btn.getText()) {
            case Field.BEGINNER -> {
                clockRun = false;
                resetWidgets();
                removeButtons();
                run(Field.BEGINNER);
            }
            case Field.INTERMEDIATE -> {
                clockRun = false;
                resetWidgets();
                removeButtons();
                run(Field.INTERMEDIATE);
            }
            case Field.EXPERT -> {
                clockRun = false;
                resetWidgets();
                removeButtons();
                run(Field.EXPERT);
            }
        }
    }

    /**
     * This is called when the play/pause button is pressed.
     * This method saves the state of the field into {@link #buttonFlagOnPause}
     * and {@link #buttonTextOnPause}, and it also stops the clock.
     *
     * @param e ActionEvent received on button press
     */
    private void onPlayPause(ActionEvent e)
    {
        JButton btn = (JButton)e.getSource();
        if (!clockRun) {
            for (Component c : buttonPanel.getComponents()) {
                MatrixJButton matrixBtn = (MatrixJButton)c;
                if (!Field.getTileAt(matrixBtn.getRow(), matrixBtn.getCol()).isHidden()) {
                    matrixBtn.setText(buttonTextOnPause.get(new AbstractMap.SimpleImmutableEntry<>(matrixBtn.getRow(), matrixBtn.getCol())));
                    matrixBtn.setBackground(Color.GRAY);
                }
                matrixBtn.setIcon(buttonFlagOnPause.get(new AbstractMap.SimpleImmutableEntry<>(matrixBtn.getRow(), matrixBtn.getCol())));
                matrixBtn.setEnabled(true);
            }
            btn.setText("\u23F8");
            clockRun = true;
            clockTick();
        } else {
            for (Component c : buttonPanel.getComponents()) {
                MatrixJButton matrixBtn = (MatrixJButton)c;
                if (!Field.getTileAt(matrixBtn.getRow(), matrixBtn.getCol()).isHidden()) {
                    Map.Entry<Integer, Integer> key = new AbstractMap.SimpleImmutableEntry<>(matrixBtn.getRow(), matrixBtn.getCol());
                    buttonTextOnPause.put(key, matrixBtn.getText());
                    matrixBtn.setText("");
                    matrixBtn.setBackground(Color.DARK_GRAY);
                } else if (matrixBtn.getIcon() != null) {
                    if ("flag".equals(((ImageIcon)matrixBtn.getIcon()).getDescription())) {
                        Map.Entry<Integer, Integer> key = new AbstractMap.SimpleImmutableEntry<>(matrixBtn.getRow(), matrixBtn.getCol());
                        buttonFlagOnPause.put(key, (ImageIcon)matrixBtn.getIcon());
                        matrixBtn.setIcon(null);
                    }
                }
                matrixBtn.setEnabled(false);
            }
            btn.setText("\u25B6");
            clockRun = false;
        }
    }

    /**
     * This method runs the timer. It creates a new Thread and updates the JLabel every second.
     * A ScheduledExecutorService is used because Thread.sleep(1000) in a loop is not consistent.
     */
    private void clockTick()
    {
        if(!started.get()) {
            started.set(true);
            final Thread clockThread = new Thread(() -> {
                final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
                service.scheduleAtFixedRate(() -> {
                    if (time == null)
                        time = new int[]{0, 0, 1};
                    if (!clockRun) {
                        started.set(false);
                        service.shutdown();
                        return;
                    }
                    timeScore = String.format("%02d", time[0]) + ":" + String.format("%02d", time[1]) + ":" + String.format("%02d", time[2]);
                    clockLabel.setText("\u23F1 " + timeScore + " \u23F1");
                    time[2] += 1;
                    if (time[2] == 60) {
                        time[2] = 0;
                        time[1] += 1;
                    }
                    if (time[1] == 60) {
                        time[1] = 0;
                        time[0] += 1;
                    }
                    if (time[0] == 24) {
                        time[0] = 0;
                        time[1] = 0;
                        time[2] = 0;
                    }

                }, 0, 1, TimeUnit.SECONDS);
            });
            clockThread.start();
        }
    }

    /**
     * Plays audio from the specified InputStream.
     * The InputStream must be decorated as a BufferedInputStream,
     * because the AudioInputStream requires mark/reset support.
     *
     * @param audioSrc the incoming stream
     */
    private static void playAudio(InputStream audioSrc)
    {
        InputStream bufferedIn = new BufferedInputStream(audioSrc);
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn) ) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (LineUnavailableException | IOException | IllegalArgumentException | UnsupportedAudioFileException exc) {
            System.err.println(exc.getMessage());
            exc.printStackTrace();
        }
    }

    /**
     * Removes all buttons buttons from the field.
     */
    private static void removeButtons()
    {
        buttons = null;
        buttonPanel.removeAll();
        app.revalidate();
        app.repaint();
    }

    /**
     * Resets everything to default except the difficulty.
     * This method clears all caches.
     */
    private static void resetWidgets()
    {
        buttonTextOnPause.clear();
        buttonFlagOnPause.clear();
        clockRun = false;
        time = null;
        playPause.setText("\u25B6");
        playPause.setEnabled(false);
        flagsPlaced = 0;
        flagsLabel.setText(Integer.toString(flagsPlaced));
        timeScore = "00:00:00";
        for (int i = 0; i < Field.getSizeX(); i++)
            for (int j = 0; j < Field.getSizeY(); j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackground(Color.DARK_GRAY);
                buttons[i][j].setForeground(Color.BLACK);
                if (buttons[i][j].getIcon() != null)
                    buttons[i][j].setIcon(null);
            }
        clockLabel.setText("\u23F1 " + timeScore + " \u23F1");
    }

    /**
     * Counts how many flags have been placed correctly.
     * The incorrectly placed ones will be highlighted with red.
     * @return The numbered of correctly placed flags.
     */
    private static int correctFlags()
    {
        int correctFlags = 0;
        for (int i = 0; i < Field.getSizeX(); i++) {
            for (int j = 0; j < Field.getSizeY(); j++) {
                if (buttons[i][j].getIcon() != null) {
                    ImageIcon icon = (ImageIcon) buttons[i][j].getIcon();
                    if ("flag".equals(icon.getDescription()))
                        if (Field.getTileAt(i, j).isMine())
                            ++correctFlags;
                        else
                            buttons[i][j].setBackground(Color.red);
                }
            }
        }
        return correctFlags;
    }

    /**
     * Reveals mines with the specified icon.
     * When the player wins it is called with flag icons,
     * otherwise with mine icons.
     * @param path path to the image
     * @param desc description of the image
     */
    private void revealMines(String path, String desc)
    {
        for (int i = 0; i < Field.getSizeX(); i++)
            for (int j = 0; j < Field.getSizeY(); j++)
                if (Field.getTileAt(i, j).isMine()) {
                    buttons[i][j].setText("");
                    buttons[i][j].setIcon(createIcon(path, desc));
                }
    }

    /**
     * Sets the Look and Feel to the Swing default.
     */
    private static void setMetalTheme()
    {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Sets the Look and Feel to the OS default.
     */
    private static void setOsTheme()
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Creates an ImageIcon with the specified path
     * and description if it exists, otherwise returns null.
     * @param path path to the icon
     * @param desc description of the icon
     * @return the ImageIcon or null
     */
    private ImageIcon createIcon(String path, String desc)
    {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null)
            return new ImageIcon(imgURL, desc);
        System.err.println("Couldn't find file: " + path);
        return null;
    }

    /**
     * @return image for the window icon
     */
    private Image createIconForWindow() {
        ImageIcon imageIcon = createIcon("/quasar6/main/images/mine.gif", "mine");
        if (imageIcon != null)
            return imageIcon.getImage();
        return null;
    }

    /**
     * Places the windows horizontally and vertically in the center.
     */
    private static void centerWindow()
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) ((dim.getWidth() - app.getWidth()) / 2);
        int height = (int) ((dim.getHeight() - app.getHeight()) / 2);
        app.setLocation(width, height);
    }

    public static void main(String[] args)
    {
        Main app = Main.getInstance();
        app.run(Field.BEGINNER);
    }
}
