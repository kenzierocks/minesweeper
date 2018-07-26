package me.kenzierocks.minesweeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class Main {

    private static final int CELL_SIZE = 32;
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int BOMBS = 5;

    @FunctionalInterface
    private interface IntBiConsumer {

        void accept(int a, int b);

    }

    private static void doForAround(IntBiConsumer cons) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                cons.accept(i, j);
            }
        }
    }

    private enum Type {
        BOMB(Color.BLACK), UNKNOWN(Color.LIGHT_GRAY), EMPTY(Color.WHITE), _1,
        _2, _3, _4, _5, _6, _7, _8, _9;

        private final Color color;

        Type() {
            this(Color.GREEN);
        }

        Type(Color color) {
            this.color = color;
        }

        public int getNumber() {
            if (this == BOMB || this == EMPTY || this == UNKNOWN) {
                throw new IllegalStateException("Not a number.");
            }
            return Integer.parseInt(name().substring(1));
        }

        public static Type fromNumber(int number) {
            return valueOf("_" + number);
        }

    };

    private static Type getOrDefault(Type[][] arr, int x, int y, Type def) {
        if (outOfBounds(arr, x, y)) {
            return def;
        }
        return arr[x][y];
    }

    private static boolean outOfBounds(Type[][] arr, int x, int y) {
        return x < 0 || x >= arr.length || y < 0 || y >= arr[0].length;
    }

    private static final Type[][] REVEALED_GRID = new Type[ROWS][COLS];
    private static final Type[][] REAL_GRID = new Type[ROWS][COLS];
    static {
        for (Type[] types : REVEALED_GRID) {
            Arrays.fill(types, Type.UNKNOWN);
        }
        Random r = new Random();
        for (int i = 0; i < BOMBS; i++) {
            int x = r.nextInt(ROWS);
            int y = r.nextInt(COLS);
            if (REAL_GRID[x][y] == Type.BOMB) {
                i--;
                continue;
            }
            REAL_GRID[x][y] = Type.BOMB;
        }
        for (int x = 0; x < REAL_GRID.length; x++) {
            Type[] types = REAL_GRID[x];
            for (int y = 0; y < types.length; y++) {
                Type type = types[y];
                if (type == null) {
                    final int xc = x;
                    final int yc = y;
                    int[] count = { 0 };
                    doForAround((i, j) -> {
                        if (getOrDefault(REAL_GRID, xc + i, yc + j,
                                Type.EMPTY) == Type.BOMB) {
                            count[0]++;
                        }
                    });
                    types[y] = count[0] == 0 ? Type.EMPTY
                            : Type.fromNumber(count[0]);
                }
            }
        }
    }

    private static boolean revealRecursive(int x, int y) {
        if (REVEALED_GRID[x][y] != Type.UNKNOWN) {
            return true;
        }
        Type revealed = REAL_GRID[x][y];
        if (revealed == Type.BOMB) {
            return false;
        }
        boolean[] all = { true };
        REVEALED_GRID[x][y] = revealed;
        if (revealed == Type.EMPTY) {
            doForAround((i, j) -> {
                int cx = x + i;
                int cy = y + j;
                if (outOfBounds(REAL_GRID, cx, cy)) {
                    return;
                }
                if (REAL_GRID[cx][cy] == Type.BOMB) {
                    return;
                }
                all[0] &= revealRecursive(cx, cy);
            });
        }
        return all[0];
    }

    private static boolean winCheck() {
        int spacesNotClicked = 0;
        for (int i = 0; i < REVEALED_GRID.length; i++) {
            Type[] reveals = REVEALED_GRID[i];
            for (int j = 0; j < reveals.length; j++) {
                if (reveals[j] == Type.UNKNOWN) {
                    spacesNotClicked++;
                }
            }
        }
        return spacesNotClicked == BOMBS;
    }

    private static void showAsync(String msg) {
        SwingUtilities
                .invokeLater(() -> JOptionPane.showMessageDialog(null, msg));
    }

    private static final class GridPanel extends JPanel {

        private static final long serialVersionUID = -7135097532420581660L;
        private static final Font font;
        static {
            try (InputStream stream =
                    Main.class.getResourceAsStream("/anonpro.ttf")) {
                font = Font.createFont(Font.TRUETYPE_FONT, stream)
                        .deriveFont(20f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        {
            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent e) {
                    Runnable finish = () -> {
                        for (int i = 0; i < ROWS; i++) {
                            for (int j = 0; j < COLS; j++) {
                                REVEALED_GRID[i][j] = REAL_GRID[i][j];
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            repaint();
                        });
                    };
                    int x = e.getX() / CELL_SIZE;
                    int y = e.getY() / CELL_SIZE;
                    boolean doneCond = false;
                    if (doneCond = !revealRecursive(x, y)) {
                        // BOOM
                        showAsync("You lose");
                    } else if (doneCond = winCheck()) {
                        showAsync("You win");
                    }
                    if (doneCond) {
                        Thread t = new Thread(finish);
                        t.start();
                    }
                    repaint();
                }

            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setFont(font);
            for (int i = 0; i < REVEALED_GRID.length; i++) {
                Type[] types = REVEALED_GRID[i];
                for (int j = 0; j < types.length; j++) {
                    Type type = types[j];
                    g.setColor(type.color);
                    g.fillRect(i * CELL_SIZE, j * CELL_SIZE, CELL_SIZE,
                            CELL_SIZE);
                    if (type != Type.BOMB && type != Type.EMPTY
                            && type != Type.UNKNOWN) {
                        g.setColor(Color.RED);
                        char number = (char) (type.getNumber() + '0');
                        FontMetrics metrics = g.getFontMetrics();
                        int xoff = metrics.charWidth(number) / 2;
                        int yoff = metrics.getHeight();
                        g.drawString(String.valueOf(number),
                                i * CELL_SIZE + xoff, j * CELL_SIZE + yoff);
                    }
                }
            }
            g.setColor(Color.BLUE);
            for (int i = 0; i <= ROWS; i++) {
                g.drawLine(0, i * CELL_SIZE, CELL_SIZE * ROWS, i * CELL_SIZE);
            }
            for (int i = 0; i <= COLS; i++) {
                g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, CELL_SIZE * COLS);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setTitle("Minesweeper");
            JPanel panel = new GridPanel();
            panel.setLayout(null);
            frame.add(panel);
            panel.setSize(CELL_SIZE * ROWS, CELL_SIZE * COLS);
            panel.setPreferredSize(panel.getSize());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}
